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
import com.baitaplon.todo_list.data.local.LocalDataStore;
import com.baitaplon.todo_list.adapter.NoteHistoryAdapter;
import com.baitaplon.todo_list.model.Note;
import com.baitaplon.todo_list.model.NoteVersion;

import java.util.ArrayList;
import java.util.List;
import java.util.Date;
import java.util.UUID;

public class NoteHistoryFragment extends Fragment implements NoteHistoryAdapter.OnHistoryItemClickListener {

    private static final String TAG = "NoteHistoryFragment";

    private Toolbar toolbar;
    private RecyclerView recyclerViewHistory;
    private TextView tvNoHistory;
    private NoteHistoryAdapter adapter;
    private List<NoteVersion> versionList = new ArrayList<>();

    private LocalDataStore localDataStore;
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

        localDataStore = LocalDataStore.getInstance(requireContext());
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
        versionList.clear();
        List<NoteVersion> storedVersions = localDataStore.noteVersions().getByNoteId(noteId);
        if (storedVersions != null) {
            versionList.addAll(storedVersions);
        }
        adapter.setData(versionList);
        tvNoHistory.setVisibility(versionList.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onRollbackClick(NoteVersion version) {
        Note note = localDataStore.notes().findById(noteId);
        if (note == null) {
            Toast.makeText(getContext(), "Không tìm thấy ghi chú", Toast.LENGTH_SHORT).show();
            return;
        }

        note.setTitle(version.getTitle());
        note.setContent(version.getContent());
        note.setLastEditedBy(currentUserName + " (Khôi phục)");
        note.setLastEdited(new Date());
        localDataStore.notes().insert(note);

        NoteVersion rollbackVersion = new NoteVersion(
            noteId,
            version.getTitle(),
            version.getContent(),
            currentUserId,
            currentUserName + " (Khôi phục)"
        );
        rollbackVersion.setId(UUID.randomUUID().toString());
        rollbackVersion.setEditedAt(new Date());
        localDataStore.noteVersions().insert(rollbackVersion);

        Toast.makeText(getContext(), "Đã khôi phục phiên bản", Toast.LENGTH_SHORT).show();
        loadHistory();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }
}