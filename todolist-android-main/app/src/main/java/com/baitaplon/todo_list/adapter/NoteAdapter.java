package com.baitaplon.todo_list.adapter;

import android.content.Context;
import android.graphics.Color;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.baitaplon.todo_list.R;
import com.baitaplon.todo_list.model.Note;
import java.util.List;
import java.util.Locale;

public class NoteAdapter extends RecyclerView.Adapter<NoteAdapter.NoteViewHolder> {

    private Context mContext;
    private List<Note> noteList;
    private OnNoteClickListener listener;
    private String currentUserName; // Để hiển thị "Sửa bởi Bạn"
    private String currentUserId;
    private String highlightQuery = "";

    public interface OnNoteClickListener {
        void onItemClick(Note note);
        void onPinClick(Note note, boolean isPinned);
        void onDeleteClick(Note note);
    }

    public NoteAdapter(Context mContext, List<Note> noteList, String currentUserId, String currentUserName, OnNoteClickListener listener) {
        this.mContext = mContext;
        this.noteList = noteList;
        this.currentUserName = currentUserName;
        this.listener = listener;
        this.currentUserId = currentUserId;
    }

    public void setData(List<Note> newNotes, String query) {
        this.noteList = newNotes;
        this.highlightQuery = query; // Lưu lại query
        notifyDataSetChanged();
    }

    public void setData(List<Note> newNotes) {
        setData(newNotes, "");
    }

    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.item_note, parent, false);
        return new NoteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
        Note note = noteList.get(position);
        if (note == null) return;
        // Set tiêu đề và highlight
        if (note.getTitle() != null && !note.getTitle().isEmpty()) {
            holder.tvTitle.setText(highlightText(note.getTitle(), highlightQuery));
            holder.tvTitle.setVisibility(View.VISIBLE);
        } else {
            holder.tvTitle.setVisibility(View.GONE);
        }

        // Set nội dung và highlight
        if (note.getContent() != null && !note.getContent().isEmpty()) {
            holder.tvContent.setText(highlightText(note.getContent(), highlightQuery));
            holder.tvContent.setVisibility(View.VISIBLE);
        } else {
            holder.tvContent.setVisibility(View.GONE);
        }

        // Hiển thị icon ghim
        holder.ivPinned.setVisibility(note.isPinned() ? View.VISIBLE : View.GONE);

        // Hiển thị thông tin chỉnh sửa
        if (note.getLastEditedBy() != null) {
            String editedByText = "Sửa lần cuối bởi ";
            if (note.getLastEditedBy() != null && note.getLastEditedBy().equals(currentUserName)) {
                editedByText += "Bạn";
            } else {
                editedByText += note.getLastEditedBy();
            }
            holder.tvEditedInfo.setText(editedByText);
            holder.tvEditedInfo.setVisibility(View.VISIBLE);
        } else {
            holder.tvEditedInfo.setVisibility(View.GONE);
        }

        // Click vào item
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(note);
            }
        });

        // Click vào menu "ba chấm"
        holder.btnMenu.setOnClickListener(v -> showPopupMenu(v, note));
    }

    private CharSequence highlightText(String originalText, String query) {
        if (query == null || query.isEmpty()) {
            return originalText; // Không tìm kiếm, trả về văn bản gốc
        }

        if (originalText == null) {
            return "";
        }

        String lowerOriginal = originalText.toLowerCase(Locale.getDefault());
        String lowerQuery = query.toLowerCase(Locale.getDefault());

        SpannableString spannable = new SpannableString(originalText);
        int index = lowerOriginal.indexOf(lowerQuery);

        while (index >= 0) {
            // Tô màu vàng cho từ khóa tìm thấy
            spannable.setSpan(new BackgroundColorSpan(Color.YELLOW), index, index + query.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            // Tìm từ khóa tiếp theo
            index = lowerOriginal.indexOf(lowerQuery, index + 1);
        }

        return spannable;
    }

    private void showPopupMenu(View view, Note note) {
        PopupMenu popupMenu = new PopupMenu(mContext, view);
        popupMenu.getMenuInflater().inflate(R.menu.menu_note_item_popup, popupMenu.getMenu());

        // Cập nhật tiêu đề menu "Ghim/Bỏ ghim"
        popupMenu.getMenu().findItem(R.id.action_pin_toggle)
                .setTitle(note.isPinned() ? "Bỏ ghim" : "Ghim");

        popupMenu.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.action_pin_toggle) {
                if (listener != null) {
                    listener.onPinClick(note, !note.isPinned());
                }
                return true;
            } else if (itemId == R.id.action_delete) {
                if (listener != null) {
                    listener.onDeleteClick(note);
                }
                return true;
            }
            return false;
        });
        popupMenu.show();
    }

    @Override
    public int getItemCount() {
        return noteList != null ? noteList.size() : 0;
    }

    public static class NoteViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvContent, tvEditedInfo;
        ImageView ivPinned;
        ImageButton btnMenu;

        public NoteViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_note_title);
            tvContent = itemView.findViewById(R.id.tv_note_content);
            tvEditedInfo = itemView.findViewById(R.id.tv_note_edited_info);
            ivPinned = itemView.findViewById(R.id.iv_note_pinned);
            btnMenu = itemView.findViewById(R.id.btn_note_menu);
        }
    }
}