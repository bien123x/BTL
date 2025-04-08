package com.example.btl.Domain.Repository;

import android.util.Log;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.example.btl.Domain.Model.User;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
        userData.put("latitude", user.getLatitude());
        userData.put("longitude", user.getLongitude());

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

    // Cập nhật vị trí người dùng
    public Task<Void> updateUserLocation(String userId, Map<String, Object> locationData) {
        return db.collection(USERS_COLLECTION).document(userId)
                .update(locationData);
    }

    // Lấy thông tin người dùng
    public Task<User> getUser(String userId) {
        return db.collection(USERS_COLLECTION).document(userId)
                .get()
                .continueWith(task -> {
                    if (task.isSuccessful()) {
                        User user = new User();
                        user.setId(userId);
                        user.setName(task.getResult().getString("name"));
                        user.setEmail(task.getResult().getString("email"));
                        user.setAvatar(task.getResult().getString("avatar"));
                        user.setScore(task.getResult().getLong("score") != null ? task.getResult().getLong("score") : 0);
                        user.setOnline(task.getResult().getBoolean("online") != null ? task.getResult().getBoolean("online") : false);
                        user.setLatitude(task.getResult().getDouble("latitude") != null ? task.getResult().getDouble("latitude") : 0.0);
                        user.setLongitude(task.getResult().getDouble("longitude") != null ? task.getResult().getDouble("longitude") : 0.0);
                        return user;
                    } else {
                        throw task.getException();
                    }
                });
    }

    // Lấy danh sách người dùng đang online
    public Task<List<User>> getOnlineUsers() {
        return db.collection(USERS_COLLECTION)
                .whereEqualTo("online", true)
                .get()
                .continueWith(task -> {
                    if (task.isSuccessful()) {
                        List<User> users = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            User user = new User();
                            user.setId(document.getId());
                            user.setName(document.getString("name"));
                            user.setEmail(document.getString("email"));
                            user.setAvatar(document.getString("avatar"));
                            user.setScore(document.getLong("score") != null ? document.getLong("score") : 0);
                            user.setOnline(document.getBoolean("online") != null ? document.getBoolean("online") : false);
                            user.setLatitude(document.getDouble("latitude") != null ? document.getDouble("latitude") : 0.0);
                            user.setLongitude(document.getDouble("longitude") != null ? document.getDouble("longitude") : 0.0);
                            users.add(user);
                        }
                        Log.d(TAG, "Loaded " + users.size() + " online users");
                        return users;
                    } else {
                        Log.e(TAG, "Failed to load online users: " + task.getException().getMessage());
                        throw task.getException();
                    }
                });
    }

    // Lấy danh sách tất cả người dùng
    public Task<List<User>> getAllUsers() {
        return db.collection(USERS_COLLECTION)
                .get()
                .continueWith(task -> {
                    if (task.isSuccessful()) {
                        List<User> users = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            User user = new User();
                            user.setId(document.getId());
                            user.setName(document.getString("name"));
                            user.setEmail(document.getString("email"));
                            user.setAvatar(document.getString("avatar"));
                            user.setScore(document.getLong("score") != null ? document.getLong("score") : 0);
                            user.setOnline(document.getBoolean("online") != null ? document.getBoolean("online") : false);
                            user.setLatitude(document.getDouble("latitude") != null ? document.getDouble("latitude") : 0.0);
                            user.setLongitude(document.getDouble("longitude") != null ? document.getDouble("longitude") : 0.0);
                            users.add(user);
                        }
                        Log.d(TAG, "Loaded " + users.size() + " users");
                        return users;
                    } else {
                        Log.e(TAG, "Failed to load users: " + task.getException().getMessage());
                        throw task.getException();
                    }
                });
    }
}