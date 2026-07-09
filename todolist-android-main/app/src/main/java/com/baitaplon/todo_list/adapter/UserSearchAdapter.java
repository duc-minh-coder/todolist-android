package com.baitaplon.todo_list.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.baitaplon.todo_list.R;
import com.baitaplon.todo_list.model.User;

import java.util.List;

public class UserSearchAdapter extends RecyclerView.Adapter<UserSearchAdapter.UserViewHolder> {

    private List<User> userList;
    private OnInviteClickListener listener;

    // Interface để xử lý sự kiện click "Mời"
    public interface OnInviteClickListener {
        void onInviteClick(User user);
    }

    public UserSearchAdapter(List<User> userList, OnInviteClickListener listener) {
        this.userList = userList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user_search, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = userList.get(position);
        holder.tvUserName.setText(user.getUsername());
        holder.tvUserEmail.setText(user.getEmail());

        holder.btnInvite.setOnClickListener(v -> {
            listener.onInviteClick(user);
            // Vô hiệu hóa nút sau khi mời
            holder.btnInvite.setText("Đã mời");
            holder.btnInvite.setEnabled(false);
        });
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    // Hàm để cập nhật dữ liệu khi tìm kiếm
    public void setData(List<User> newList) {
        this.userList = newList;
        notifyDataSetChanged();
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView tvUserName, tvUserEmail;
        Button btnInvite;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUserName = itemView.findViewById(R.id.tv_user_name);
            tvUserEmail = itemView.findViewById(R.id.tv_user_email);
            btnInvite = itemView.findViewById(R.id.btn_invite_user);
        }
    }
}