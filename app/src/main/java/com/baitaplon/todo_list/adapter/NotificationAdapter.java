package com.baitaplon.todo_list.adapter;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.baitaplon.todo_list.R;
import com.baitaplon.todo_list.model.Notification;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {

    private Context mContext;
    private List<Notification> notificationList;
    private NotificationClickListener listener;

    public interface NotificationClickListener {
        void onAcceptClick(Notification notification, int position);
        void onDeclineClick(Notification notification, int position);
        void onItemClick(Notification notification, int position); // Khi click vào thông báo (để đánh dấu đã đọc)
    }

    public NotificationAdapter(Context mContext, List<Notification> notificationList, NotificationClickListener listener) {
        this.mContext = mContext;
        this.notificationList = notificationList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.item_notification, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        Notification notification = notificationList.get(position);
        if (notification == null) return;

        holder.tvMessage.setText(notification.getMessage());
        holder.tvTime.setText(formatTimestamp(notification.getCreatedAt()));

        String type = notification.getType();
        if (type != null && (type.equals("schedule_invitation") || type.equals("note_invitation"))) {
            // Đây là lời mời -> Hiển thị nút
            holder.layoutActions.setVisibility(View.VISIBLE);
            if (type.equals("schedule_invitation")) {
                holder.ivIcon.setImageResource(R.drawable.ic_calendar);
            } else {
                holder.ivIcon.setImageResource(R.drawable.ic_note);
            }
        } else {
            // Đây là thông báo nhắc nhở (reminder) -> Ẩn nút
            holder.layoutActions.setVisibility(View.GONE);
            holder.ivIcon.setImageResource(R.drawable.ic_notifications);
        }

        // Xử lý trạng thái đã đọc/chưa đọc
        if (notification.isRead()) {
            holder.cardView.setCardBackgroundColor(ContextCompat.getColor(mContext, R.color.white));
            holder.tvMessage.setTypeface(null, Typeface.NORMAL);
        } else {
            holder.cardView.setCardBackgroundColor(ContextCompat.getColor(mContext, R.color.second)); // Dùng màu second
            holder.tvMessage.setTypeface(null, Typeface.BOLD);
        }

        // Bắt sự kiện click
        holder.btnAccept.setOnClickListener(v -> listener.onAcceptClick(notification, position));
        holder.btnDecline.setOnClickListener(v -> listener.onDeclineClick(notification, position));
        holder.itemView.setOnClickListener(v -> listener.onItemClick(notification, position));
    }

    public static String formatTimestamp(Date date) {
        if (date == null) {
            return "";
        }
        String pattern = "HH:mm dd-MM-yyyy";
        SimpleDateFormat sdf = new SimpleDateFormat(pattern, Locale.getDefault());
        return sdf.format(date);
    }

    @Override
    public int getItemCount() {
        return notificationList.size();
    }

    public void setData(List<Notification> newList) {
        this.notificationList = newList;
        notifyDataSetChanged();
    }

    public void removeItem(int position) {
        notificationList.remove(position);
//        notifyItemRemoved(position);
    }

    static class NotificationViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardView;
        ImageView ivIcon;
        TextView tvMessage, tvTime;
        LinearLayout layoutActions;
        MaterialButton btnAccept, btnDecline;

        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = (MaterialCardView) itemView;
            ivIcon = itemView.findViewById(R.id.iv_notification_icon);
            tvMessage = itemView.findViewById(R.id.tv_notification_message);
            tvTime = itemView.findViewById(R.id.tv_notification_time);
            layoutActions = itemView.findViewById(R.id.layout_invitation_actions);
            btnAccept = itemView.findViewById(R.id.btn_accept);
            btnDecline = itemView.findViewById(R.id.btn_decline);
        }
    }
}