package com.baitaplon.todo_list.activity;


import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.baitaplon.todo_list.R;
import com.baitaplon.todo_list.fragment.LoginFragment;
import com.baitaplon.todo_list.fragment.RegisterFragment;

public class AuthActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        // kiểm tra activity có đc tạo mới hoàn toàn k
        if (savedInstanceState == null) {
            replaceFragment(new LoginFragment(), false); // hiện màn đăng nhập
        }
    }

    public void goToRegisterFragment() {
        replaceFragment(new RegisterFragment(), true);
    }

    public void goToLoginFragment() {
        getSupportFragmentManager().popBackStack();
    }

    private void replaceFragment(Fragment fragment, boolean addToBackStack) {
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();

        ft.setCustomAnimations(
                R.anim.slide_in_right,
                R.anim.slide_out_left,
                R.anim.slide_in_left,
                R.anim.slide_out_right
        );

        ft.replace(R.id.fragment_container_auth, fragment);

        if (addToBackStack) {
            ft.addToBackStack(null);
        }

        ft.commit();
    }
}
