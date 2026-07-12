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
import com.baitaplon.todo_list.data.local.LocalDataStore;
import com.baitaplon.todo_list.model.Note;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class NotesFragment extends Fragment implements NoteAdapter.OnNoteClickListener {
    private static final String TAG = "NotesFragment";
    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView recyclerViewNotes;
    private NoteAdapter noteAdapter;
    private TextView tvNoNotes;
    private String currentUserId;
    private String currentUserName;
    private SearchView searchViewNotes;
    private String currentQuery = ""; // Lưu trữ từ khóa tìm kiếm
    private final List<Note> combinedNotes = new ArrayList<>();
    private LocalDataStore localDataStore;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_notes, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        localDataStore = LocalDataStore.getInstance(requireContext());
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
        combinedNotes.clear();

        List<Note> myNotes = localDataStore.notes().getNotesByCreator(currentUserId);
        if (myNotes != null) {
            combinedNotes.addAll(myNotes);
        }

        List<String> sharedNoteIds = new ArrayList<>();
        localDataStore.noteInvitations().getByUserAndStatus(currentUserId, 1)
                .forEach(invitation -> {
                    if (invitation.getNoteId() != null && !sharedNoteIds.contains(invitation.getNoteId())) {
                        sharedNoteIds.add(invitation.getNoteId());
                    }
                });

        for (String sharedNoteId : sharedNoteIds) {
            Note sharedNote = localDataStore.notes().findById(sharedNoteId);
            if (sharedNote != null) {
                combinedNotes.add(sharedNote);
            }
        }

        mergeAndDisplayNotes();
    }

    private void mergeAndDisplayNotes() {
        List<Note> filteredList = new ArrayList<>();
        if (currentQuery.isEmpty()) {
            filteredList.addAll(combinedNotes);
        } else {
            String lowerCaseQuery = currentQuery.toLowerCase(Locale.getDefault());
            for (Note note : combinedNotes) {
                if (note.getTitle() != null && note.getTitle().toLowerCase(Locale.getDefault()).contains(lowerCaseQuery)) {
                    filteredList.add(note);
                } else if (note.getContent() != null && note.getContent().toLowerCase(Locale.getDefault()).contains(lowerCaseQuery)) {
                    filteredList.add(note);
                }
            }
        }

        filteredList.sort((o1, o2) -> {
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
        localDataStore.notes().updatePinned(note.getId(), isPinned);
        loadNotes();
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
        localDataStore.noteVersions().deleteByNoteId(noteId);
        localDataStore.noteInvitations().deleteByNoteId(noteId);
        localDataStore.notes().deleteById(noteId);
        loadNotes();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }
}