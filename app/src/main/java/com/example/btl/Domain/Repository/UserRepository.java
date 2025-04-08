package com.example.btl.Domain.Repository;

import android.util.Log;
import com.google.firebase.firestore.FirebaseFirestore;
import com.example.btl.Domain.Model.User;
import com.google.android.gms.tasks.Task;
import java.util.HashMap;
import java.util.Map;

public class UserRepository {
    private static final String TAG = "UserRepository";
    private final FirebaseFirestore db;
    private static final String USERS_COLLECTION = "users";

    public UserRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    // Lưu thông tin người dùng vào Firestore
    public Task<Void> saveUser(String userId, User user) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("name", user.getName());
        userData.put("email", user.getEmail());
        userData.put("avatar", user.getAvatar());
        userData.put("score", user.getScore());
        userData.put("online", user.isOnline());

        return db.collection(USERS_COLLECTION).document(userId)
                .set(userData)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "User data saved successfully for UID: " + userId))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to save user data: " + e.getMessage()));
    }

    // Cập nhật trạng thái online
    public Task<Void> updateOnlineStatus(String userId, boolean online) {
        Map<String, Object> status = new HashMap<>();
        status.put("online", online);

        return db.collection(USERS_COLLECTION).document(userId)
                .update(status)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Updated online status to " + online + " for UID: " + userId))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to update online status: " + e.getMessage()));
    }
}
