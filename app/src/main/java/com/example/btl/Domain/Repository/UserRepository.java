package com.example.btl.Domain.Repository;

import android.util.Log;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.example.btl.Domain.Model.User;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class UserRepository {
    private static final String TAG = "UserRepository";
    private final FirebaseFirestore db;
    private static final String USERS_COLLECTION = "users";
    private ListenerRegistration onlineUsersListener;

    public UserRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    public Task<User> getUser(String userId) {
        return db.collection(USERS_COLLECTION)
                .document(userId)
                .get()
                .continueWith(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            User user = new User();
                            user.setId(document.getId());
                            user.setName(document.getString("name"));
                            user.setEmail(document.getString("email"));
                            user.setAvatar(document.getString("avatar"));
                            user.setScore(document.getLong("score") != null ? document.getLong("score") : 0);
                            user.setOnline(document.getBoolean("online") != null ? document.getBoolean("online") : false);
                            user.setLatitude(document.getDouble("latitude") != null ? document.getDouble("latitude") : 0.0);
                            user.setLongitude(document.getDouble("longitude") != null ? document.getDouble("longitude") : 0.0);
                            return user;
                        } else {
                            throw new Exception("User not found");
                        }
                    } else {
                        throw task.getException();
                    }
                });
    }

    public Task<Void> saveUser(String userId, User user) {
        return db.collection(USERS_COLLECTION)
                .document(userId)
                .set(user);
    }

    public Task<Void> updateUserLocation(String userId, Map<String, Object> locationData) {
        return db.collection(USERS_COLLECTION)
                .document(userId)
                .update(locationData);
    }

    public Task<Void> updateOnlineStatus(String userId, boolean online) {
        return db.collection(USERS_COLLECTION)
                .document(userId)
                .update("online", online);
    }

    public Task<List<User>> getOnlineUsers() {
        return db.collection(USERS_COLLECTION)
                .whereEqualTo("online", true)
                .get()
                .continueWith(task -> {
                    if (task.isSuccessful()) {
                        List<User> users = new ArrayList<>();
                        for (DocumentSnapshot document : task.getResult()) {
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
                        return users;
                    } else {
                        throw task.getException();
                    }
                });
    }

    // Thêm phương thức để cập nhật thông tin người dùng
    public Task<Void> updateUser(String userId, Map<String, Object> updates) {
        return db.collection(USERS_COLLECTION)
                .document(userId)
                .update(updates)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "User info updated successfully"))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to update user info: " + e.getMessage()));
    }
}