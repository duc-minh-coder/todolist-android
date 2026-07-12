package com.baitaplon.todo_list.fragment;

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
import com.baitaplon.todo_list.activity.AuthActivity;
import com.baitaplon.todo_list.data.local.LocalAuthRepository;
import com.baitaplon.todo_list.model.User;

import java.util.UUID;

public class RegisterFragment extends Fragment {
    TextView tvGoToLogin;
    EditText edtFullName, edtEmail, edtPassword, edtConfirmPassword;
    Button btnRegister;
    private LocalAuthRepository authRepository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_register, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initUI(view);
        authRepository = LocalAuthRepository.getInstance(requireContext());

        tvGoToLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getActivity() instanceof AuthActivity) {
                    ((AuthActivity) getActivity()).goToLoginFragment();
                }
            }
        });

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickRegister();
            }
        });

    }

    private void clickRegister() {
        String username = edtFullName.getText().toString().trim();
        String email = edtEmail.getText().toString().trim();
        String password = edtPassword.getText().toString().trim();
        String confirmPassword = edtConfirmPassword.getText().toString().trim();
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(getContext(), "Email hoặc mật khẩu không được bỏ trống!", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!password.equals(confirmPassword)) {
            Toast.makeText(getContext(), "Mật khẩu không khớp!", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = UUID.randomUUID().toString();
        authRepository.register(uid, username, email, password, new LocalAuthRepository.Callback<User>() {
            @Override
            public void onSuccess(User user) {
                if (!isAdded()) {
                    return;
                }

                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Đăng ký thành công", Toast.LENGTH_SHORT).show();
                    edtFullName.setText("");
                    edtEmail.setText("");
                    edtPassword.setText("");
                    edtConfirmPassword.setText("");
                    ((AuthActivity) getActivity()).goToLoginFragment();
                });
            }

            @Override
            public void onError(String message) {
                if (!isAdded()) {
                    return;
                }

                requireActivity().runOnUiThread(() ->
                        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void initUI(View view) {
        tvGoToLogin = view.findViewById(R.id.tv_go_to_login);
        edtFullName = view.findViewById(R.id.edt_reg_name);
        edtEmail = view.findViewById(R.id.edt_reg_email);
        edtPassword = view.findViewById(R.id.edt_reg_password);
        edtConfirmPassword = view.findViewById(R.id.edt_reg_confirm_password);
        btnRegister = view.findViewById(R.id.btn_register);
    }

}
