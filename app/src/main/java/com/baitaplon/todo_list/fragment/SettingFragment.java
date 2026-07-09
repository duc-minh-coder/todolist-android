package com.baitaplon.todo_list.fragment;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.baitaplon.todo_list.R;
import com.baitaplon.todo_list.activity.MainActivity;

public class SettingFragment extends Fragment {

    private ImageView btnBack;
    private Switch switchThongBao;
    private TextView btnAmThanh, btnTroGiup, btnVeChungToi, btnTaiKhoan;

    private SharedPreferences sharedPreferences;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_setting, container, false);

        // Ánh xạ View
        btnBack = view.findViewById(R.id.btnBack);
        switchThongBao = view.findViewById(R.id.switchThongBao);
        btnAmThanh = view.findViewById(R.id.btn_Amthanh);
        btnTroGiup = view.findViewById(R.id.btn_Trogiup);
        btnVeChungToi = view.findViewById(R.id.btn_Vechungtoi);
        btnTaiKhoan = view.findViewById(R.id.btn_Taikhoan);

        // SharedPreferences để lưu trạng thái switch
        sharedPreferences = requireContext().getSharedPreferences("AppSettings", Context.MODE_PRIVATE);
        boolean isSwitchOn = sharedPreferences.getBoolean("switch_thong_bao", false);
        switchThongBao.setChecked(isSwitchOn);

        // Xử lý bật/tắt switch
        switchThongBao.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sharedPreferences.edit().putBoolean("switch_thong_bao", isChecked).apply();
            Toast.makeText(getContext(),
                    isChecked ? "Đã bật thanh thông báo" : "Đã tắt thanh thông báo",
                    Toast.LENGTH_SHORT).show();
        });

        btnBack.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager().popBackStack();

            // Hiện lại ViewPager, top bar và FAB
            requireActivity().findViewById(R.id.main_container).setVisibility(View.GONE);
            requireActivity().findViewById(R.id.view_pager).setVisibility(View.VISIBLE);
            requireActivity().findViewById(R.id.top_bar).setVisibility(View.VISIBLE);
            requireActivity().findViewById(R.id.fab_add).setVisibility(View.VISIBLE);
        });



        // 🔹 Xử lý các nút còn lại
        btnAmThanh.setOnClickListener(v ->
                Toast.makeText(getContext(), "Tùy chọn âm thanh đang được phát triển", Toast.LENGTH_SHORT).show()
        );

        btnTroGiup.setOnClickListener(v ->
                Toast.makeText(getContext(), "Hướng dẫn sử dụng sẽ được cập nhật sớm!", Toast.LENGTH_SHORT).show()
        );

        btnVeChungToi.setOnClickListener(v ->
                Toast.makeText(getContext(), "Ứng dụng ToDoList - Phiên bản 1.0\nNhóm phát triển: Baitaplon Team", Toast.LENGTH_LONG).show()
        );

        btnTaiKhoan.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.main_container, new AccountFragment())
                    .addToBackStack(null)
                    .commit();
        });


        // ✅ Trả về view gốc đã được thiết lập
        return view;
    }
}