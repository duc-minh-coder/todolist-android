package com.baitaplon.todo_list.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.appcompat.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.baitaplon.todo_list.R;
import com.baitaplon.todo_list.adapter.NoteAdapter;
import com.baitaplon.todo_list.model.Note;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class NotesFragment extends Fragment implements NoteAdapter.OnNoteClickListener {
    private static final String TAG = "NotesFragment";
    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView recyclerViewNotes;
    private NoteAdapter noteAdapter;
    private TextView tvNoNotes;
    private FirebaseFirestore db;
    private String currentUserId;
    private String currentUserName;
    private SearchView searchViewNotes;
    private String currentQuery = ""; // Lưu trữ từ khóa tìm kiếm

    private Map<String, Note> combinedNotesMap = new HashMap<>();

    private ListenerRegistration myNotesListener;
    private ListenerRegistration sharedInvitationsListener;

    private Map<String, ListenerRegistration> sharedNoteDetailListeners = new HashMap<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_notes, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("AppPreferences", Context.MODE_PRIVATE);
        currentUserId = sharedPreferences.getString("current_user_id", null);
        currentUserName = sharedPreferences.getString("current_user_name", null);

        if (currentUserId == null) {
            Toast.makeText(getContext(), "Lỗi: Chưa đăng nhập!", Toast.LENGTH_SHORT).show();
            return;
        }
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout);
        recyclerViewNotes = view.findViewById(R.id.recycler_view_notes);
        tvNoNotes = view.findViewById(R.id.tv_no_note);
        searchViewNotes = view.findViewById(R.id.search_view_notes);

        setupRecyclerView();
        setupSearchListener();
        setupSwipeRefresh();
        loadNotes();
    }

    private void setupSwipeRefresh() {
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                Log.d(TAG, "onRefresh called.");
                loadNotes();
            }
        });
    }

    private void setupSearchListener() {
        searchViewNotes.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                currentQuery = newText;
                mergeAndDisplayNotes();
                return true;
            }
        });
    }

    private void setupRecyclerView() {
        noteAdapter = new NoteAdapter(requireContext(), new ArrayList<>(), currentUserId, currentUserName, this);
        recyclerViewNotes.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerViewNotes.setAdapter(noteAdapter);
    }

    private void loadNotes() {
        if (myNotesListener != null) myNotesListener.remove();
        if (sharedInvitationsListener != null) sharedInvitationsListener.remove();
        clearSharedNoteDetailListeners();

        combinedNotesMap.clear();

        // Query 1: my note
        Query myNotesQuery = db.collection("notes")
                .whereEqualTo("creatorId", currentUserId);

        myNotesListener = myNotesQuery.addSnapshotListener((snapshots, e) -> {
            if (e != null) {
                Log.e(TAG, "Listen failed for my notes.", e);
                return;
            }
            if (snapshots != null) {
                List<String> newNoteIds = new ArrayList<>();
                for (QueryDocumentSnapshot doc : snapshots) {
                    Note note = doc.toObject(Note.class);
                    note.setId(doc.getId());
                    combinedNotesMap.put(note.getId(), note); // Thêm hoặc cập nhật
                    newNoteIds.add(note.getId());
                }

                List<String> idsToRemove = new ArrayList<>();
                for (Map.Entry<String, Note> entry : combinedNotesMap.entrySet()) {
                    if (entry.getValue().getCreatorId().equals(currentUserId)) {
                        if (!newNoteIds.contains(entry.getKey())) {
                            idsToRemove.add(entry.getKey());
                        }
                    }
                }

                for (String id : idsToRemove) {
                    Log.d(TAG, "Removing deleted 'my note' from map: " + id);
                    combinedNotesMap.remove(id);
                }
            }
            mergeAndDisplayNotes();
        });

        // Query 2: Ghi chú status == 1
        Query sharedInvitationsQuery = db.collection("note_invitations")
                .whereEqualTo("invitedUserId", currentUserId)
                .whereEqualTo("status", 1); // 1 = accepted

        sharedInvitationsListener = sharedInvitationsQuery.addSnapshotListener((snapshots, e) -> {
            if (e != null) {
                Log.e(TAG, "Listen failed for shared invitations.", e);
                return;
            }

            if (snapshots == null || snapshots.isEmpty()) {
                clearSharedNoteDetailListeners();
                mergeAndDisplayNotes();
                return;
            }

            List<String> currentSharedNoteIds = new ArrayList<>();
            for (QueryDocumentSnapshot doc : snapshots) {
                String noteId = doc.getString("noteId");
                if (noteId != null && !noteId.isEmpty()) {
                    currentSharedNoteIds.add(noteId);

                    if (!sharedNoteDetailListeners.containsKey(noteId)) {
                        Log.d(TAG, "Adding new listener for shared note: " + noteId);

                        ListenerRegistration detailListener = db.collection("notes").document(noteId)
                                .addSnapshotListener((noteSnapshot, error) -> {
                                    if (error != null) {
                                        Log.w(TAG, "Listen failed for shared note " + noteId, error);
                                        combinedNotesMap.remove(noteId);
                                        mergeAndDisplayNotes();
                                        return;
                                    }

                                    if (noteSnapshot != null && noteSnapshot.exists()) {
                                        Note note = noteSnapshot.toObject(Note.class);
                                        note.setId(noteSnapshot.getId());
                                        combinedNotesMap.put(note.getId(), note);
                                    } else {
                                        combinedNotesMap.remove(noteId);
                                    }
                                    mergeAndDisplayNotes();
                                });

                        sharedNoteDetailListeners.put(noteId, detailListener);
                    }
                }
            }

            List<String> listenersToRemove = new ArrayList<>();
            for (String listeningNoteId : sharedNoteDetailListeners.keySet()) {
                if (!currentSharedNoteIds.contains(listeningNoteId)) {
                    listenersToRemove.add(listeningNoteId);
                }
            }

            for (String noteIdToRemove : listenersToRemove) {
                Log.d(TAG, "Removing old listener for shared note: " + noteIdToRemove);
                sharedNoteDetailListeners.get(noteIdToRemove).remove();
                sharedNoteDetailListeners.remove(noteIdToRemove);
                combinedNotesMap.remove(noteIdToRemove);
            }

            mergeAndDisplayNotes();
        });
    }

    private void clearSharedNoteDetailListeners() {
        for (ListenerRegistration listener : sharedNoteDetailListeners.values()) {
            listener.remove();
        }
        sharedNoteDetailListeners.clear();
    }

    private void mergeAndDisplayNotes() {
        List<Note> combinedList = new ArrayList<>(combinedNotesMap.values());

        List<Note> filteredList = new ArrayList<>();
        if (currentQuery.isEmpty()) {
            filteredList.addAll(combinedList);
        } else {
            String lowerCaseQuery = currentQuery.toLowerCase(Locale.getDefault());
            for (Note note : combinedList) {
                if (note.getTitle() != null && note.getTitle().toLowerCase(Locale.getDefault()).contains(lowerCaseQuery)) {
                    filteredList.add(note);
                } else if (note.getContent() != null && note.getContent().toLowerCase(Locale.getDefault()).contains(lowerCaseQuery)) {
                    filteredList.add(note);
                }
            }
        }

        Collections.sort(filteredList, (o1, o2) -> {
            if (o1.isPinned() != o2.isPinned()) {
                return o1.isPinned() ? -1 : 1;
            }
            if (o1.getLastEdited() != null && o2.getLastEdited() != null) {
                return o2.getLastEdited().compareTo(o1.getLastEdited());
            }
            if (o1.getCreatedAt() != null && o2.getCreatedAt() != null) {
                return o2.getCreatedAt().compareTo(o1.getCreatedAt());
            }
            return 0;
        });

        if (noteAdapter != null) {
            noteAdapter.setData(filteredList, currentQuery);
        }
        if (tvNoNotes != null) {
            tvNoNotes.setVisibility(filteredList.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }

    private void openAddEditFragment(String noteId) {
        AddEditNoteFragment addEditNoteFragment = new AddEditNoteFragment();
        Bundle bundle = new Bundle();
        if (noteId != null) {
            bundle.putString("note_id", noteId);
        }
        addEditNoteFragment.setArguments(bundle);

        FragmentManager fragmentManager = getParentFragmentManager();
        fragmentManager.beginTransaction()
                .add(android.R.id.content, addEditNoteFragment)
                .addToBackStack(null)
                .hide(this) // <-- Chính xác
                .commit();
    }

    @Override
    public void onItemClick(Note note) {
        openAddEditFragment(note.getId());
    }

    @Override
    public void onPinClick(Note note, boolean isPinned) {
        if (note.getId() == null) {
            Toast.makeText(getContext(), "Lỗi: Không tìm thấy ID ghi chú", Toast.LENGTH_SHORT).show();
            return;
        }
        db.collection("notes").document(note.getId())
                .update("pinned", isPinned)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Note pin status updated."))
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Lỗi ghim ghi chú", Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onDeleteClick(Note note) {
        if (note.getId() == null) {
            Toast.makeText(getContext(), "Lỗi: Không tìm thấy ID ghi chú", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!note.getCreatorId().equals(currentUserId)) {
            Toast.makeText(getContext(), "Chỉ chủ sở hữu mới có thể xóa ghi chú này", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Xóa Ghi chú")
                .setMessage("Bạn có chắc muốn xóa ghi chú này? Mọi lịch sử và chia sẻ sẽ bị mất vĩnh viễn.")
                .setPositiveButton("Xóa", (dialog, which) -> deleteNoteAndHistory(note.getId()))
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void deleteNoteAndHistory(String noteId) {
        WriteBatch batch = db.batch();
        batch.delete(db.collection("notes").document(noteId));

        db.collection("notes").document(noteId).collection("versions")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            batch.delete(doc.getReference());
                        }
                    }

                    db.collection("note_invitations").whereEqualTo("noteId", noteId)
                            .get()
                            .addOnCompleteListener(invitationTask -> {
                                if (invitationTask.isSuccessful() && invitationTask.getResult() != null) {
                                    for (QueryDocumentSnapshot doc : invitationTask.getResult()) {
                                        batch.delete(doc.getReference());
                                    }
                                }

                                batch.commit()
                                        .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "Đã xóa ghi chú", Toast.LENGTH_SHORT))
                                        .addOnFailureListener(e -> Toast.makeText(getContext(), "Lỗi khi xóa ghi chú", Toast.LENGTH_SHORT).show());
                            });
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (myNotesListener != null) myNotesListener.remove();
        if (sharedInvitationsListener != null) sharedInvitationsListener.remove();
        clearSharedNoteDetailListeners();
    }
}