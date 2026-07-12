package com.baitaplon.todo_list.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.baitaplon.todo_list.R;
import com.baitaplon.todo_list.data.local.LocalDataStore;
import com.baitaplon.todo_list.adapter.InvitedUserAdapter;
import com.baitaplon.todo_list.model.Note;
import com.baitaplon.todo_list.model.NoteInvitation;
import com.baitaplon.todo_list.model.NoteVersion;
import com.baitaplon.todo_list.model.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Date;
import java.util.UUID;

public class AddEditNoteFragment extends Fragment implements InvitedUserAdapter.OnRemoveUserClickListener{

    private static final String TAG = "AddEditNoteFragment";

    private Toolbar toolbar;
    private EditText etTitle, etContent;
    private LocalDataStore localDataStore;
    private String currentUserId, currentUserName;

    private String noteId; // Null nếu là note mới
    private Note currentNote;
    private boolean isPinned = false;
    private boolean isNewNote = true;
    private TextView tvSharedWith;
    private RecyclerView recyclerViewInvitedUsers;
    private InvitedUserAdapter invitedUserAdapter;
    private List<User> sharedUserList;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        return inflater.inflate(R.layout.fragment_addedit_note, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        localDataStore = LocalDataStore.getInstance(requireContext());
        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("AppPreferences", Context.MODE_PRIVATE);
        currentUserId = sharedPreferences.getString("current_user_id", null);
        currentUserName = sharedPreferences.getString("current_user_name", "Bạn");

        toolbar = view.findViewById(R.id.toolbar_addedit_note);
        etTitle = view.findViewById(R.id.et_note_title);
        etContent = view.findViewById(R.id.et_note_content);

        tvSharedWith = view.findViewById(R.id.tv_shared_with);
        recyclerViewInvitedUsers = view.findViewById(R.id.recyclerView_invited_users);
        sharedUserList = new ArrayList<>();

        // Khởi tạo adapter
        invitedUserAdapter = new InvitedUserAdapter(sharedUserList, this);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
        recyclerViewInvitedUsers.setLayoutManager(layoutManager);
        recyclerViewInvitedUsers.setAdapter(invitedUserAdapter);

        setupToolbar();

        if (getArguments() != null) {
            noteId = getArguments().getString("note_id");
            if (noteId != null) {
                isNewNote = false;
                loadNote();
            }
        }

        //chỉ user là ng tạo mới được mời và xóa user
        updateSharePermissions();
    }

    private void setupToolbar() {
        toolbar.inflateMenu(R.menu.menu_addedit_note);
        toolbar.setNavigationOnClickListener(v -> getParentFragmentManager().popBackStack());
        toolbar.setOnMenuItemClickListener(this::onOptionsItemSelected);
    }

    private void loadNote() {
        currentNote = localDataStore.notes().findById(noteId);
        if (currentNote == null) {
            Toast.makeText(getContext(), "Không tìm thấy ghi chú", Toast.LENGTH_SHORT).show();
            getParentFragmentManager().popBackStack();
            return;
        }

        etTitle.setText(currentNote.getTitle());
        etContent.setText(currentNote.getContent());
        isPinned = currentNote.isPinned();
        updatePinIcon();
        loadAcceptedUsers();
        updateSharePermissions();
    }

    //load user được chia sẻ
    private void loadAcceptedUsers() {
        if (noteId == null) return;

        sharedUserList.clear(); // Xóa danh sách cũ

        List<NoteInvitation> invitations = localDataStore.noteInvitations().getByNoteId(noteId);
        List<String> acceptedUserIds = new ArrayList<>();
        for (NoteInvitation invitation : invitations) {
            if (invitation.getStatus() == 1 && invitation.getInvitedUserId() != null) {
                acceptedUserIds.add(invitation.getInvitedUserId());
            }
        }

        if (acceptedUserIds.isEmpty()) {
            tvSharedWith.setVisibility(View.GONE);
            recyclerViewInvitedUsers.setVisibility(View.GONE);
            invitedUserAdapter.setData(sharedUserList);
            return;
        }

        tvSharedWith.setVisibility(View.VISIBLE);
        recyclerViewInvitedUsers.setVisibility(View.VISIBLE);
        fetchUserDetails(acceptedUserIds);
    }

    private void fetchUserDetails(List<String> userIds) {
        if (userIds.isEmpty()) {
            invitedUserAdapter.setData(sharedUserList);
            return;
        }

        for (String userId : userIds) {
            User user = localDataStore.users().findById(userId);
            if (user != null) {
                sharedUserList.add(user);
                invitedUserAdapter.setData(sharedUserList);
            }
        }
    }

    // Cập nhật quyền (chỉ chủ note mới được mời/xóa)
    private void updateSharePermissions() {
        if (toolbar == null) return;
        Menu menu = toolbar.getMenu();
        MenuItem inviteItem = menu.findItem(R.id.action_invite_note);

        boolean isCreator = (currentNote != null && currentNote.getCreatorId() != null && currentNote.getCreatorId().equals(currentUserId)) || isNewNote;

        if (inviteItem != null) {
            inviteItem.setVisible(isCreator);
        }

        // Set adapter sang chế độ read-only nếu không phải chủ sở hữu
        if (invitedUserAdapter != null) {
            invitedUserAdapter.setReadOnly(!isCreator);
        }
    }

    private void updatePinIcon() {
        if (toolbar == null) return;
        Menu menu = toolbar.getMenu();
        MenuItem pinItem = menu.findItem(R.id.action_pin_note);
        if (pinItem != null) {
            if (isPinned) {
                int color = ContextCompat.getColor(requireContext(), R.color.primary);
                pinItem.setIconTintList(ColorStateList.valueOf(color));
            } else {
                int color = ContextCompat.getColor(requireContext(), R.color.text_secondary);
                pinItem.setIconTintList(ColorStateList.valueOf(color));
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_save_note) {
            saveNote();
            return true;
        } else if (itemId == R.id.action_pin_note) {
            togglePin();
            return true;
        } else if (itemId == R.id.action_invite_note) {
            if (isNewNote) {
                Toast.makeText(getContext(), "Vui lòng lưu ghi chú trước khi mời", Toast.LENGTH_SHORT).show();
            } else {
                showInviteDialog();
            }
            return true;
        } else if (itemId == R.id.action_history_note) {
            if (isNewNote) {
                Toast.makeText(getContext(), "Lưu ghi chú trước khi xem lịch sử", Toast.LENGTH_SHORT).show();
            } else {
                openHistoryFragment();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showInviteDialog() {
        String title = etTitle.getText().toString().trim();
        if (title.isEmpty()) {
            title = "Ghi chú không có tiêu đề";
        }
        InviteUserDialogFragment dialog = InviteUserDialogFragment.newInstance(noteId, title, currentUserName);
        dialog.show(getParentFragmentManager(), "InviteUserDialog");
    }

    private void togglePin() {
        isPinned = !isPinned;
        updatePinIcon();
        if (!isNewNote) {
            localDataStore.notes().updatePinned(noteId, isPinned);
        }
    }

    private void saveNote() {
        String title = etTitle.getText().toString().trim();
        String content = etContent.getText().toString().trim();

        if (title.isEmpty() && content.isEmpty()) {
            Toast.makeText(getContext(), "Không thể lưu ghi chú rỗng", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isNewNote) {
            noteId = UUID.randomUUID().toString();
            isNewNote = false;

            Note newNote = new Note();
            newNote.setId(noteId);
            newNote.setCreatorId(currentUserId);
            newNote.setTitle(title);
            newNote.setContent(content);
            newNote.setPinned(isPinned);
            newNote.setLastEditedBy(currentUserName);
            newNote.setInvitedUserIds(new ArrayList<>());
            newNote.setCreatedAt(new Date());
            newNote.setLastEdited(new Date());

            currentNote = newNote;
            localDataStore.notes().insert(newNote);
            updateSharePermissions();
        } else {
            currentNote.setTitle(title);
            currentNote.setContent(content);
            currentNote.setPinned(isPinned);
            currentNote.setLastEditedBy(currentUserName);
            currentNote.setLastEdited(new Date());
            localDataStore.notes().insert(currentNote);
        }

        // --- LƯU LỊCH SỬ ---
        NoteVersion version = new NoteVersion(noteId, title, content, currentUserId, currentUserName);
        version.setId(UUID.randomUUID().toString());
        version.setEditedAt(new Date());
        localDataStore.noteVersions().insert(version);

        Toast.makeText(getContext(), "Đã lưu ghi chú", Toast.LENGTH_SHORT).show();
    }

    private void openHistoryFragment() {
        NoteHistoryFragment historyFragment = new NoteHistoryFragment();
        Bundle bundle = new Bundle();
        bundle.putString("note_id", noteId);
        historyFragment.setArguments(bundle);

        getParentFragmentManager().beginTransaction()
                .add(android.R.id.content, historyFragment)
                .hide(this)
                .addToBackStack(null)
                .commit();
    }

    private void deleteNoteAndHistory(String noteId) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Xóa Ghi chú")
                .setMessage("Bạn có chắc muốn xóa ghi chú này? Mọi lịch sử và chia sẻ sẽ bị mất vĩnh viễn.")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    localDataStore.noteVersions().deleteByNoteId(noteId);
                    localDataStore.noteInvitations().deleteByNoteId(noteId);
                    localDataStore.notes().deleteById(noteId);
                    Toast.makeText(getContext(), "Đã xóa ghi chú", Toast.LENGTH_SHORT).show();
                    getParentFragmentManager().popBackStack();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    @Override
    public void onRemoveUser(User user, int position) {
        if (currentNote == null || !currentNote.getCreatorId().equals(currentUserId)) {
            Toast.makeText(getContext(), "Bạn không có quyền xóa người này", Toast.LENGTH_SHORT).show();
            return;
        }

        String userIdToRemove = user.getUid();

        // Xóa user khỏi danh sách trong UI
        sharedUserList.remove(position);
        invitedUserAdapter.setData(sharedUserList);

        if (sharedUserList.isEmpty()) {
            tvSharedWith.setVisibility(View.GONE);
            recyclerViewInvitedUsers.setVisibility(View.GONE);
        }

        localDataStore.noteInvitations().deleteByNoteAndUser(noteId, userIdToRemove);
        if (currentNote != null && currentNote.getInvitedUserIds() != null) {
            currentNote.getInvitedUserIds().remove(userIdToRemove);
            localDataStore.notes().insert(currentNote);
        }
        Toast.makeText(getContext(), "Đã xóa " + user.getUsername(), Toast.LENGTH_SHORT).show();
    }

    private void handleRemoveUserError(User user, int position) {
        sharedUserList.add(position, user);
        invitedUserAdapter.setData(sharedUserList);
        Toast.makeText(getContext(), "Lỗi: Không thể xóa", Toast.LENGTH_SHORT).show();
    }
}