package com.baitaplon.todo_list.data.local;

import android.content.Context;

import com.baitaplon.todo_list.model.User;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LocalAuthRepository {

    public interface Callback<T> {
        void onSuccess(T value);

        void onError(String message);
    }

    private static volatile LocalAuthRepository instance;

    private final UserDao userDao;
    private final ExecutorService executorService;

    private LocalAuthRepository(Context context) {
        AppDatabase database = AppDatabase.getInstance(context);
        userDao = database.userDao();
        executorService = Executors.newSingleThreadExecutor();
    }

    public static LocalAuthRepository getInstance(Context context) {
        if (instance == null) {
            synchronized (LocalAuthRepository.class) {
                if (instance == null) {
                    instance = new LocalAuthRepository(context);
                }
            }
        }
        return instance;
    }

    public void register(String uid, String username, String email, String password, Callback<User> callback) {
        executorService.execute(() -> {
            User existingUser = userDao.findByEmail(email);
            if (existingUser != null) {
                callback.onError("Email đã tồn tại trong CSDL local");
                return;
            }

            String salt = PasswordUtils.createSalt();
            User user = new User(username, email);
            user.setUid(uid);
            user.setPasswordSalt(salt);
            user.setPasswordHash(PasswordUtils.hashPassword(password, salt));
            userDao.insert(user);
            callback.onSuccess(user);
        });
    }

    public void login(String email, String password, Callback<User> callback) {
        executorService.execute(() -> {
            User user = userDao.findByEmail(email);
            if (user == null) {
                callback.onError("Không tìm thấy tài khoản local");
                return;
            }

            if (!PasswordUtils.verifyPassword(password, user.getPasswordSalt(), user.getPasswordHash())) {
                callback.onError("Mật khẩu không đúng");
                return;
            }

            callback.onSuccess(user);
        });
    }

    public void findById(String uid, Callback<User> callback) {
        executorService.execute(() -> {
            User user = userDao.findById(uid);
            if (user == null) {
                callback.onError("Không tìm thấy người dùng local");
            } else {
                callback.onSuccess(user);
            }
        });
    }
}