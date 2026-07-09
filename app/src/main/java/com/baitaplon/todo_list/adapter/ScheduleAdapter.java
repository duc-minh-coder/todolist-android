package com.baitaplon.todo_list.adapter;

import android.content.Context;
import android.util.Log; // Thêm Log
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.baitaplon.todo_list.R;
import com.baitaplon.todo_list.model.Schedule;
import com.google.firebase.Timestamp; // Import Timestamp

import java.text.SimpleDateFormat; // Import SimpleDateFormat
import java.time.LocalDate;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale; // Import Locale
import java.util.TimeZone; // Import TimeZone

public class ScheduleAdapter extends RecyclerView.Adapter<ScheduleAdapter.ScheduleViewHolder> {
    private static final int VIEW_TYPE_PRIMARY = 1;
    private static final int VIEW_TYPE_SECONDARY = 2;
    private Context mContext;
    private List<Schedule> scheduleList;
    private OnScheduleItemClickListener listener;

    private SimpleDateFormat timeFormatter = new SimpleDateFormat("HH:mm", Locale.getDefault());
    private SimpleDateFormat dayFormatter = new SimpleDateFormat("dd", Locale.getDefault());
    private SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    public interface OnScheduleItemClickListener {
        void onItemClick(Schedule schedule);
        void onCheckboxClick(Schedule schedule, boolean isChecked);
    }

    public ScheduleAdapter(Context mContext, List<Schedule> scheduleList, OnScheduleItemClickListener listener) {
        this.mContext = mContext;
        this.scheduleList = scheduleList;
        this.listener = listener;
    }

    public void setData(List<Schedule> newList) {
        this.scheduleList = newList;
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        Schedule item = scheduleList.get(position);
        if (item.getStartTime() == null) {
            return VIEW_TYPE_SECONDARY;
        }

        Calendar todayCal = Calendar.getInstance();
        String todayDateString = dateFormatter.format(todayCal.getTime());

        Date itemStartDate = item.getStartTime().toDate();
        String itemDateString = dateFormatter.format(itemStartDate);

        if (itemDateString.equals(todayDateString)) {
            return VIEW_TYPE_PRIMARY;
        } else {
            return VIEW_TYPE_SECONDARY;
        }
    }

    @NonNull
    @Override
    public ScheduleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == VIEW_TYPE_PRIMARY) {
            view = inflater.inflate(R.layout.item_schedule_primary, parent, false);
        } else {
            view = inflater.inflate(R.layout.item_schedule_secondary, parent, false);
        }
        return new ScheduleViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ScheduleViewHolder holder, int position) {
        Schedule currentSchedule = scheduleList.get(position);
        if (currentSchedule == null) return;

        holder.tvTitle.setText(currentSchedule.getTitle());
        holder.tvPlace.setText(currentSchedule.getPlace());
        holder.tvNotes.setText(currentSchedule.getNotes());
        holder.checkBox.setChecked(currentSchedule.isMyCompletedStatus() != null && currentSchedule.isMyCompletedStatus());
        holder.tvTime.setText(currentSchedule.getTimeFromToString());

        // --- XỬ LÝ ẨN HIỆN NGÀY (Timestamp) ---
        String dayOfMonth = "?";
        if (currentSchedule.getStartTime() != null) {
            dayOfMonth = dayFormatter.format(currentSchedule.getStartTime().toDate());
//            dayOfMonth = dayFormatter.format(LocalDate.now().getDayOfMonth());
        }
        holder.tvDay.setText(dayOfMonth);

        if (position == 0) {
            holder.tvDay.setVisibility(View.VISIBLE);
        } else {
            Schedule previousSchedule = scheduleList.get(position - 1);
            if (currentSchedule.getStartTime() != null && previousSchedule.getStartTime() != null) {
                String currentDatePart = dateFormatter.format(currentSchedule.getStartTime().toDate());
                String previousDatePart = dateFormatter.format(previousSchedule.getStartTime().toDate());

                if (currentDatePart.equals(previousDatePart)) {
                    holder.tvDay.setVisibility(View.INVISIBLE);
                } else {
                    holder.tvDay.setVisibility(View.VISIBLE);
                }
            } else {
                holder.tvDay.setVisibility(View.VISIBLE);
            }
        }

        if (listener != null) {
            holder.itemView.setOnClickListener(v -> {
                if (currentSchedule.getId() != null) {
                    listener.onItemClick(currentSchedule);
                } else {
                    Log.e("ScheduleAdapter", "Item clicked but schedule ID is null!");
                    Toast.makeText(mContext, "Lỗi: Không tìm thấy ID lịch trình", Toast.LENGTH_SHORT).show();
                }
            });

            holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (buttonView.isPressed()) {
                    if (currentSchedule.getId() != null) {
                        listener.onCheckboxClick(currentSchedule, isChecked);
                    } else {
                        Log.e("ScheduleAdapter", "Checkbox clicked but schedule ID is null!");
                        Toast.makeText(mContext, "Lỗi: Không tìm thấy ID lịch trình", Toast.LENGTH_SHORT).show();
                        buttonView.setChecked(!isChecked);
                    }
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return scheduleList != null ? scheduleList.size() : 0;
    }

    public static class ScheduleViewHolder extends RecyclerView.ViewHolder {
        TextView tvDay, tvTitle, tvTime, tvPlace, tvNotes;
        CheckBox checkBox;
        CardView cardView;

        public ScheduleViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDay = itemView.findViewById(R.id.tv_schedule_day);
            tvTitle = itemView.findViewById(R.id.tv_schedule_title);
            tvTime = itemView.findViewById(R.id.tv_schedule_time);
            tvPlace = itemView.findViewById(R.id.tv_schedule_place);
            tvNotes = itemView.findViewById(R.id.tv_schedule_notes);
            checkBox = itemView.findViewById(R.id.checkbox_schedule_complete);
            cardView = itemView.findViewById(R.id.card_schedule_item);
        }
    }
}