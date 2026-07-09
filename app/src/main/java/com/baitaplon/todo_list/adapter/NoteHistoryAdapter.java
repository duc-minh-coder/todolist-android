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
import com.baitaplon.todo_list.model.NoteVersion;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class NoteHistoryAdapter extends RecyclerView.Adapter<NoteHistoryAdapter.HistoryViewHolder> {

    private Context mContext;
    private List<NoteVersion> versionList;
    private OnHistoryItemClickListener listener;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm dd/MM/yyyy", Locale.getDefault());

    public interface OnHistoryItemClickListener {
        void onRollbackClick(NoteVersion version);
    }

    public NoteHistoryAdapter(Context mContext, List<NoteVersion> versionList, OnHistoryItemClickListener listener) {
        this.mContext = mContext;
        this.versionList = versionList;
        this.listener = listener;
    }

    public void setData(List<NoteVersion> newList) {
        this.versionList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.item_note_version, parent, false);
        return new HistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        NoteVersion version = versionList.get(position);
        if (version == null) return;

        // Hiển thị thông tin chỉnh sửa
        String info = "Chỉnh sửa bởi " + version.getEditorName();
        if (version.getEditedAt() != null) {
            info += " lúc " + dateFormat.format(version.getEditedAt().toDate());
        }
        holder.tvVersionInfo.setText(info);

        holder.tvVersionTitle.setText(version.getTitle());
        holder.tvVersionContent.setText(version.getContent());

        // Nút khôi phục
        holder.btnRollback.setOnClickListener(v -> {
            if (listener != null) {
                // Vô hiệu hóa nút để tránh click đúp
                holder.btnRollback.setEnabled(false);
                holder.btnRollback.setText("Đang khôi phục...");
                listener.onRollbackClick(version);
            }
        });

        // Chỉ phiên bản mới nhất (ở vị trí 0) là không thể khôi phục
        if (position == 0) {
            holder.btnRollback.setVisibility(View.GONE);
        } else {
            holder.btnRollback.setVisibility(View.VISIBLE);
            holder.btnRollback.setEnabled(true);
            holder.btnRollback.setText("Khôi phục");
        }
    }

    @Override
    public int getItemCount() {
        return versionList != null ? versionList.size() : 0;
    }

    public static class HistoryViewHolder extends RecyclerView.ViewHolder {
        TextView tvVersionInfo, tvVersionTitle, tvVersionContent;
        Button btnRollback;

        public HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvVersionInfo = itemView.findViewById(R.id.tv_version_info);
            tvVersionTitle = itemView.findViewById(R.id.tv_version_title);
            tvVersionContent = itemView.findViewById(R.id.tv_version_content);
            btnRollback = itemView.findViewById(R.id.btn_rollback);
        }
    }
}