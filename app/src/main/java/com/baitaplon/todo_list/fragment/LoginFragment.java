package com.baitaplon.todo_list.fragment;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.baitaplon.todo_list.R;
import com.baitaplon.todo_list.activity.MainActivity;
import com.baitaplon.todo_list.activity.AuthActivity;
import com.baitaplon.todo_list.data.local.LocalAuthRepository;
import com.baitaplon.todo_list.model.User;

public class LoginFragment extends Fragment {
    TextView tvGoToRegister;
    EditText edtEmail, edtPassword;
    Button btnLogin;
    private LocalAuthRepository authRepository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_login, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        authRepository = LocalAuthRepository.getInstance(requireContext());

        edtEmail = view.findViewById(R.id.edt_login_email);
        edtPassword = view.findViewById(R.id.edt_login_password);
        btnLogin = view.findViewById(R.id.btn_login);
        tvGoToRegister = view.findViewById(R.id.tv_go_to_register);
        tvGoToRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getActivity() instanceof AuthActivity) {
                    ((AuthActivity) getActivity()).goToRegisterFragment();
                }
            }
        });

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickLogin();
            }
        });
    }

    private void clickLogin() {
        String email = edtEmail.getText().toString().trim();
        String pass = edtPassword.getText().toString().trim();

        if (email.isEmpty() || pass.isEmpty()) {
            Toast.makeText(getContext(), "Vui lòng nhập đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }

        authRepository.login(email, pass, new LocalAuthRepository.Callback<User>() {
            @Override
            public void onSuccess(User user) {
                if (!isAdded()) {
                    return;
                }

                requireActivity().runOnUiThread(() -> {
                    String username = user.getUsername();
                    if (username == null || username.isEmpty()) {
                        username = "Người dùng";
                    }
                    saveUidAndGoToMain(user.getUid(), username);
                });
            }

            @Override
            public void onError(String message) {
                if (!isAdded()) {
                    return;
                }

                requireActivity().runOnUiThread(() ->
                        Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show());
            }
        });
    }


    private void saveUidAndGoToMain(String uid, String username) {
        if (getActivity() == null) return;

        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("AppPreferences", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("current_user_id", uid);
        editor.putString("current_user_name", username); // Lưu TÊN LẤY TỪ FIRESTORE
        editor.apply();

        Intent intent = new Intent(getActivity(), MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}