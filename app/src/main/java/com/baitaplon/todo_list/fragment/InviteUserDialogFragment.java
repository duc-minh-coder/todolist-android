package com.baitaplon.todo_list.fragment;

import android.os.Bundle;
import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.baitaplon.todo_list.R;
import com.baitaplon.todo_list.data.local.LocalDataStore;
import com.baitaplon.todo_list.adapter.UserSearchAdapter;
import com.baitaplon.todo_list.model.Note;
import com.baitaplon.todo_list.model.NoteInvitation;
import com.baitaplon.todo_list.model.Notification;
import com.baitaplon.todo_list.model.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Date;
import java.util.UUID;

public class InviteUserDialogFragment extends DialogFragment implements UserSearchAdapter.OnInviteClickListener {
    private static final String TAG = "InviteUserDialog";
    private static final String ARG_NOTE_ID = "note_id";
    private static final String ARG_NOTE_TITLE = "note_title";
    private static final String ARG_CURRENT_USER_NAME = "current_user_name";

    private EditText edtSearchUser;
    private RecyclerView recyclerViewSearchResults;
    private ProgressBar progressBarSearch;
    private TextView tvNoResults;
    private LocalDataStore localDataStore;
    private UserSearchAdapter adapter;
    private List<User> userList;

    private String noteId;
    private String noteTitle;
    private String currentUserId;
    private String currentUserName;

    public static InviteUserDialogFragment newInstance(String noteId, String noteTitle, String currentUserName) {
        InviteUserDialogFragment fragment = new InviteUserDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_NOTE_ID, noteId);
        args.putString(ARG_NOTE_TITLE, noteTitle);
        args.putString(ARG_CURRENT_USER_NAME, currentUserName);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        localDataStore = LocalDataStore.getInstance(requireContext());
        currentUserId = getActivity() != null ? getActivity().getSharedPreferences("AppPreferences", Context.MODE_PRIVATE).getString("current_user_id", null) : null;

        if (getArguments() != null) {
            noteId = getArguments().getString(ARG_NOTE_ID);
            noteTitle = getArguments().getString(ARG_NOTE_TITLE);
            currentUserName = getArguments().getString(ARG_CURRENT_USER_NAME);
        }
        userList = new ArrayList<>();
        adapter = new UserSearchAdapter(userList, this);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_invite_user, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        edtSearchUser = view.findViewById(R.id.edt_search_user);
        recyclerViewSearchResults = view.findViewById(R.id.recycler_view_search_results);
        progressBarSearch = view.findViewById(R.id.progress_bar_search);
        tvNoResults = view.findViewById(R.id.tv_no_results);

        recyclerViewSearchResults.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewSearchResults.setAdapter(adapter);

        edtSearchUser.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchUsers(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void searchUsers(String query) {
        if (query.isEmpty()) {
            userList.clear();
            adapter.setData(userList);
            tvNoResults.setVisibility(View.GONE);
            recyclerViewSearchResults.setVisibility(View.GONE);
            return;
        }

        progressBarSearch.setVisibility(View.VISIBLE);
        tvNoResults.setVisibility(View.GONE);
        recyclerViewSearchResults.setVisibility(View.GONE);

        userList.clear();
        for (User user : localDataStore.users().search(query)) {
            if (user.getUid() != null && !user.getUid().equals(currentUserId)) {
                userList.add(user);
            }
        }
        updateUIBasedOnResults();
    }

    private void updateUIBasedOnResults() {
        progressBarSearch.setVisibility(View.GONE);
        if (userList.isEmpty()) {
            tvNoResults.setVisibility(View.VISIBLE);
            recyclerViewSearchResults.setVisibility(View.GONE);
        } else {
            tvNoResults.setVisibility(View.GONE);
            recyclerViewSearchResults.setVisibility(View.VISIBLE);
            adapter.setData(userList);
        }
    }

    @Override
    public void onInviteClick(User user) {
        if (noteId == null || currentUserId == null || currentUserName == null) {
            Toast.makeText(getContext(), "Lỗi: Không thể gửi lời mời", Toast.LENGTH_SHORT).show();
            return;
        }

        NoteInvitation invitation = new NoteInvitation();
        invitation.setId(UUID.randomUUID().toString());
        invitation.setNoteId(noteId);
        invitation.setNoteTitle(noteTitle);
        invitation.setHostId(currentUserId);
        invitation.setInvitedUserId(user.getUid());
        invitation.setStatus(0); // 0 = pending

        localDataStore.noteInvitations().insert(invitation);

        Notification notification = new Notification();
        notification.setId(UUID.randomUUID().toString());
        notification.setUserId(user.getUid());
        notification.setType("note_invitation");
        String message = currentUserName + " đã mời bạn cộng tác vào ghi chú: " + noteTitle;
        notification.setMessage(message);
        notification.setReferenceId(invitation.getId());
        notification.setRead(false);
        notification.setCreatedAt(new Date());

        localDataStore.notifications().insert(notification);

        Note note = localDataStore.notes().findById(noteId);
        if (note != null) {
            if (note.getInvitedUserIds() == null) {
                note.setInvitedUserIds(new ArrayList<>());
            }
            if (!note.getInvitedUserIds().contains(user.getUid())) {
                note.getInvitedUserIds().add(user.getUid());
                localDataStore.notes().insert(note);
            }
        }

        Toast.makeText(getContext(), "Đã gửi lời mời tới " + user.getUsername(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null) {
            getDialog().getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }
}