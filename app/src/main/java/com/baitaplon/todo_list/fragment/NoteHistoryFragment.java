package com.baitaplon.todo_list.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.baitaplon.todo_list.R;
import com.baitaplon.todo_list.adapter.NoteHistoryAdapter;
import com.baitaplon.todo_list.model.NoteVersion;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class NoteHistoryFragment extends Fragment implements NoteHistoryAdapter.OnHistoryItemClickListener {

    private static final String TAG = "NoteHistoryFragment";

    private Toolbar toolbar;
    private RecyclerView recyclerViewHistory;
    private TextView tvNoHistory;
    private NoteHistoryAdapter adapter;
    private List<NoteVersion> versionList = new ArrayList<>();

    private FirebaseFirestore db;
    private ListenerRegistration historyListener;
    private String noteId;
    private String currentUserId, currentUserName;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_note_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("AppPreferences", Context.MODE_PRIVATE);
        currentUserId = sharedPreferences.getString("current_user_id", null);
        currentUserName = sharedPreferences.getString("current_user_name", "Bạn");

        toolbar = view.findViewById(R.id.toolbar_note_history);
        recyclerViewHistory = view.findViewById(R.id.recyclerView_history);
        tvNoHistory = view.findViewById(R.id.tv_no_history);

        setupToolbar();

        if (getArguments() == null || (noteId = getArguments().getString("note_id")) == null) {
            Toast.makeText(getContext(), "Lỗi: Không tìm thấy ID ghi chú", Toast.LENGTH_SHORT).show();
            getParentFragmentManager().popBackStack();
            return;
        }

        setupRecyclerView();
        loadHistory();
    }

    private void setupToolbar() {
        toolbar.setNavigationOnClickListener(v -> getParentFragmentManager().popBackStack());
    }

    private void setupRecyclerView() {
        adapter = new NoteHistoryAdapter(requireContext(), versionList, this);
        recyclerViewHistory.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerViewHistory.setAdapter(adapter);
    }

    private void loadHistory() {
        if (historyListener != null) historyListener.remove();

        // Query: Lấy các phiên bản, SẮP XẾP MỚI NHẤT LÊN TRÊN
        Query historyQuery = db.collection("notes").document(noteId)
                .collection("versions")
                .orderBy("editedAt", Query.Direction.DESCENDING);

        historyListener = historyQuery.addSnapshotListener((snapshots, e) -> {
            if (e != null) {
                Log.e(TAG, "Listen failed for history.", e);
                return;
            }

            versionList.clear();
            if (snapshots != null) {
                for (QueryDocumentSnapshot doc : snapshots) {
                    NoteVersion version = doc.toObject(NoteVersion.class);
                    version.setId(doc.getId());
                    versionList.add(version);
                }
            }

            adapter.setData(versionList);
            tvNoHistory.setVisibility(versionList.isEmpty() ? View.VISIBLE : View.GONE);
        });
    }

    @Override
    public void onRollbackClick(NoteVersion version) {
        // Khi rollback, chúng ta thực hiện 2 việc:
        // 1. Cập nhật ghi chú gốc (main note) về nội dung của phiên bản (version) này.
        // 2. Tạo một phiên bản (version) MỚI để ghi lại hành động "Rollback" này.

        WriteBatch batch = db.batch();

        // 1. Cập nhật note gốc
        Map<String, Object> updates = new HashMap<>();
        updates.put("title", version.getTitle());
        updates.put("content", version.getContent());
        updates.put("lastEditedBy", currentUserName + " (Khôi phục)");
        updates.put("lastEdited", FieldValue.serverTimestamp());
        batch.update(db.collection("notes").document(noteId), updates);

        // 2. Tạo phiên bản mới (cho hành động rollback)
        NoteVersion rollbackVersion = new NoteVersion(
                noteId,
                version.getTitle(),
                version.getContent(),
                currentUserId,
                currentUserName + " (Khôi phục)"
        );
        batch.set(db.collection("notes").document(noteId).collection("versions").document(), rollbackVersion);

        // Commit batch
        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Đã khôi phục phiên bản", Toast.LENGTH_SHORT).show();
                    // Listener sẽ tự cập nhật UI, không cần thoát
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Lỗi khôi phục", Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (historyListener != null) historyListener.remove();
    }
}