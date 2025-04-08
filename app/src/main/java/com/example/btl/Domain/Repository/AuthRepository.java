package com.example.btl.Domain.Repository;

import android.util.Log;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

public class AuthRepository {
    private static final String TAG = "AuthRepository";
    private final FirebaseAuth auth;

    public AuthRepository() {
        this.auth = FirebaseAuth.getInstance();
    }

    // Đăng ký người dùng
    public Task<FirebaseUser> register(String email, String password) {
        return auth.createUserWithEmailAndPassword(email, password)
                .continueWith(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = auth.getCurrentUser();
                        if (user != null) {
                            Log.d(TAG, "User registered successfully: " + user.getUid());
                            return user;
                        } else {
                            Log.e(TAG, "FirebaseUser is null after registration");
                            throw new Exception("Không thể lấy thông tin người dùng sau khi đăng ký");
                        }
                    } else {
                        Log.e(TAG, "Registration failed: " + task.getException().getMessage());
                        throw task.getException();
                    }
                });
    }

    // Đăng nhập người dùng
    public Task<FirebaseUser> login(String email, String password) {
        return auth.signInWithEmailAndPassword(email, password)
                .continueWith(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = auth.getCurrentUser();
                        if (user != null) {
                            Log.d(TAG, "User logged in successfully: " + user.getUid());
                            return user;
                        } else {
                            Log.e(TAG, "FirebaseUser is null after login");
                            throw new Exception("Không thể lấy thông tin người dùng sau khi đăng nhập");
                        }
                    } else {
                        Log.e(TAG, "Login failed: " + task.getException().getMessage());
                        throw task.getException();
                    }
                });
    }

    // Lấy người dùng hiện tại
    public FirebaseUser getCurrentUser() {
        return auth.getCurrentUser();
    }

    // Đăng xuất
    public void logout() {
        auth.signOut();
        Log.d(TAG, "User logged out");
    }
}
