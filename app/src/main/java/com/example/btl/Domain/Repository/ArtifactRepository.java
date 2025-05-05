package com.example.btl.Domain.Repository;

import android.util.Log;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.example.btl.Domain.Model.Artifact;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ArtifactRepository {
    private static final String TAG = "ArtifactRepository";
    private final FirebaseFirestore db;
    private static final String ARTIFACTS_COLLECTION = "artifacts";
    private static final String USERS_COLLECTION = "users";
    private UserRepository userRepository;

    public ArtifactRepository() {
        this.db = FirebaseFirestore.getInstance();
        this.userRepository = new UserRepository();
    }

    // Thêm cổ vật vào collection artifacts và cập nhật điểm số người dùng
    public Task<Void> addArtifact(Artifact artifact, String userId) {
        // Kiểm tra xem cổ vật đã được thu thập chưa
        return hasUserCollectedArtifact(userId, artifact.getId())
                .continueWithTask(task -> {
                    if (task.getResult()) {
                        // Nếu đã thu thập, không làm gì
                        return Tasks.forResult(null);
                    }
                    // Chuẩn bị dữ liệu cổ vật
                    Map<String, Object> artifactData = new HashMap<>();
                    artifactData.put("id", artifact.getId());
                    artifactData.put("name", artifact.getName());
                    artifactData.put("description", artifact.getDescription());
                    artifactData.put("imageUrl", artifact.getImageUrl());
                    artifactData.put("rarity", artifact.getRarity());
                    artifactData.put("points", artifact.getPoints());
                    artifactData.put("collectedBy", userId);
                    artifactData.put("collectedAt", Timestamp.now());

                    // Lưu cổ vật vào Firestore
                    Task<Void> artifactTask = db.collection(ARTIFACTS_COLLECTION)
                            .document(artifact.getId())
                            .set(artifactData);

                    // Tính tổng điểm và cập nhật score
                    Task<Void> userTask = getCollectedArtifacts(userId)
                            .continueWithTask(artifactsTask -> {
                                if (artifactsTask.isSuccessful()) {
                                    List<Artifact> artifacts = artifactsTask.getResult();
                                    int totalPoints = artifacts.stream().mapToInt(Artifact::getPoints).sum();
                                    int itemCount = artifacts.size();
                                    Map<String, Object> updates = new HashMap<>();
                                    updates.put("score", totalPoints);
                                    updates.put("itemCount", itemCount);
                                    return userRepository.updateUser(userId, updates)
                                            .addOnSuccessListener(aVoid -> Log.d(TAG, "Score updated to " + totalPoints + ", ItemCount updated to " + itemCount))
                                            .addOnFailureListener(e -> Log.e(TAG, "Failed to update user data: " + e.getMessage()))
                                            ;
                                } else {
                                    throw artifactsTask.getException();
                                }
                            });

                    // Chờ cả hai task hoàn thành
                    return Tasks.whenAll(artifactTask, userTask)
                            .addOnSuccessListener(aVoid -> Log.d(TAG, "Added artifact and updated score for user " + userId))
                            .addOnFailureListener(e -> Log.e(TAG, "Failed to add artifact or update score: " + e.getMessage()))
                            ;
                });
    }

    // Lấy tất cả cổ vật
    public Task<List<Artifact>> getAllArtifacts() {
        return db.collection(ARTIFACTS_COLLECTION)
                .get()
                .continueWith(task -> {
                    if (task.isSuccessful()) {
                        List<Artifact> artifacts = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Artifact artifact = new Artifact();
                            artifact.setId(document.getId());
                            artifact.setName(document.getString("name"));
                            artifact.setDescription(document.getString("description"));
                            artifact.setRarity(document.getString("rarity"));
                            artifact.setPoints(document.getLong("points") != null ? document.getLong("points").intValue() : 0);
                            artifact.setImageUrl(document.getString("imageUrl"));
                            artifact.setLatitude(0.0);
                            artifact.setLongitude(0.0);
                            artifact.setCollectedBy(document.getString("collectedBy"));
                            artifact.setCollectedAt(document.getTimestamp("collectedAt"));
                            artifacts.add(artifact);
                        }
//                        Log.d(TAG, "Loaded " + artifacts.size() + " artifacts");
                        return artifacts;
                    } else {
//                        Log.e(TAG, "Failed to load artifacts: " + task.getException().getMessage());
                        throw task.getException();
                    }
                });
    }

    // Kiểm tra xem user đã thu thập cổ vật hay chưa
    public Task<Boolean> hasUserCollectedArtifact(String userId, String artifactId) {
        return db.collection(ARTIFACTS_COLLECTION)
                .document(artifactId)
                .get()
                .continueWith(task -> {
                    if (task.isSuccessful() && task.getResult().exists()) {
                        String collectedBy = task.getResult().getString("collectedBy");
                        return userId != null && userId.equals(collectedBy);
                    }
                    return false;
                });
    }

    // Lấy danh sách cổ vật đã thu thập của user
    public Task<List<Artifact>> getCollectedArtifacts(String userId) {
        return db.collection(ARTIFACTS_COLLECTION)
                .whereEqualTo("collectedBy", userId)
                .get()
                .continueWith(task -> {
                    if (task.isSuccessful()) {
                        List<Artifact> artifacts = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Artifact artifact = new Artifact();
                            artifact.setId(document.getId());
                            artifact.setName(document.getString("name"));
                            artifact.setDescription(document.getString("description"));
                            artifact.setRarity(document.getString("rarity"));
                            artifact.setPoints(document.getLong("points") != null ? document.getLong("points").intValue() : 0);
                            artifact.setImageUrl(document.getString("imageUrl"));
                            artifact.setLatitude(0.0);
                            artifact.setLongitude(0.0);
                            artifact.setCollectedBy(document.getString("collectedBy"));
                            artifact.setCollectedAt(document.getTimestamp("collectedAt"));
                            artifacts.add(artifact);
                        }
//                        Log.d(TAG, "Loaded " + artifacts.size() + " collected artifacts for user " + userId);
                        return artifacts;
                    } else {
//                        Log.e(TAG, "Failed to load collected artifacts: " + task.getException().getMessage());
                        throw task.getException();
                    }
                });
    }

    // Lấy điểm số của người dùng
    public Task<Integer> getUserScore(String userId) {
        return db.collection(USERS_COLLECTION)
                .document(userId)
                .get()
                .continueWith(task -> {
                    if (task.isSuccessful() && task.getResult().exists()) {
                        Long score = task.getResult().getLong("score");
                        return score != null ? score.intValue() : 0;
                    } else {
//                        Log.e(TAG, "Failed to load user score: " + task.getException().getMessage());
                        return 0;
                    }
                });
    }
}