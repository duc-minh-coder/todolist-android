package com.baitaplon.todo_list.data.local;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.baitaplon.todo_list.model.User;

import java.util.List;

@Dao
public interface UserDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(User user);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<User> users);

    @Query("SELECT * FROM users WHERE uid = :uid LIMIT 1")
    User findById(String uid);

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    User findByEmail(String email);

    @Query("SELECT * FROM users WHERE email LIKE :query || '%' OR username LIKE :query || '%' ORDER BY username COLLATE NOCASE ASC")
    List<User> search(String query);

    @Query("SELECT * FROM users ORDER BY username COLLATE NOCASE ASC")
    List<User> getAll();

    @Delete
    void delete(User user);
}