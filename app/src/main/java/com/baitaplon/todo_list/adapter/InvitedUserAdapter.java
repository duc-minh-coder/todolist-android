package com.baitaplon.todo_list.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.baitaplon.todo_list.R;
import com.baitaplon.todo_list.model.User;
import com.google.android.material.chip.Chip;

import java.util.List;

public class InvitedUserAdapter extends RecyclerView.Adapter<InvitedUserAdapter.UserChipViewHolder> {

    private List<User> invitedUsers;
    private OnRemoveUserClickListener listener;
    private boolean isReadOnly = false;

    public interface OnRemoveUserClickListener {
        void onRemoveUser(User user, int position);
    }

    public InvitedUserAdapter(List<User> invitedUsers, OnRemoveUserClickListener listener) {
        this.invitedUsers = invitedUsers;
        this.listener = listener;
    }

    public void setReadOnly(boolean readOnly) {
        isReadOnly = readOnly;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public UserChipViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_invited_user, parent, false);
        return new UserChipViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserChipViewHolder holder, int position) {
        User user = invitedUsers.get(position);

        if (user.getUsername() != null && !user.getUsername().isEmpty()) {
            holder.chip.setText(user.getUsername());
        } else {
            holder.chip.setText(user.getEmail());
        }

        if (isReadOnly) {
            holder.chip.setCloseIconVisible(false); // Ẩn nút "x"
            holder.chip.setOnCloseIconClickListener(null); // Tắt sự kiện click
        } else {
            holder.chip.setCloseIconVisible(true);
            holder.chip.setOnCloseIconClickListener(v -> {
                int currentPosition = holder.getAdapterPosition();
                if (currentPosition != RecyclerView.NO_POSITION) {
                    listener.onRemoveUser(invitedUsers.get(currentPosition), currentPosition);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return invitedUsers.size();
    }

    public void setData(List<User> newList) {
        this.invitedUsers = newList;
        notifyDataSetChanged();
    }

    static class UserChipViewHolder extends RecyclerView.ViewHolder {
        Chip chip;
        public UserChipViewHolder(@NonNull View itemView) {
            super(itemView);
            chip = (Chip) itemView;
        }
    }
}