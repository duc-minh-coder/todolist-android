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
import com.baitaplon.todo_list.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class RegisterFragment extends Fragment {
    TextView tvGoToLogin;
    EditText edtFullName, edtEmail, edtPassword, edtConfirmPassword;
    Button btnRegister;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_register, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initUI(view);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

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

        mAuth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener(requireActivity(), task -> {
                            if(task.isSuccessful()){
                                // Đăng ký thành công trên Authentication
                                FirebaseUser firebaseUser = mAuth.getCurrentUser();
                                if(firebaseUser != null){
                                    String uid = firebaseUser.getUid();

                                    User newUser = new User(username, email);
                                    //save user vao firebase
                                    db.collection("users").document(uid).set(newUser)
                                            .addOnSuccessListener(aVoid ->{
                                                Toast.makeText(getContext(), "Đăng ký thành công", Toast.LENGTH_SHORT).show();
                                                edtFullName.setText("");
                                                edtEmail.setText("");
                                                edtPassword.setText("");
                                                edtConfirmPassword.setText("");
                                                ((AuthActivity) getActivity()).goToLoginFragment();
                                            })
                                            .addOnFailureListener(e->{
                                                Toast.makeText(getContext(), "Lỗi khi lưu thông tin user: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                            });
                                }else{
                                    Toast.makeText(getContext(), "Lỗi: Không lấy được thông tin người dùng.", Toast.LENGTH_SHORT).show();
                                }
                            }else{
                                // Đăng ký thất bại (email đã tồn tại, mật khẩu yếu,...)
                                Toast.makeText(getContext(), "Đăng ký thất bại: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
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
