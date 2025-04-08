package com.example.btl.Domain.Repository;

import android.util.Log;
import com.google.firebase.firestore.FirebaseFirestore;
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
    private static final String USER_ARTIFACTS_COLLECTION = "user_artifacts";

    public ArtifactRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

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
                            artifact.setRarity(document.getLong("rarity") != null ? document.getLong("rarity").intValue() : 0);
                            artifact.setPoints(document.getLong("points") != null ? document.getLong("points").intValue() : 0);
                            artifact.setImageUrl(document.getString("imageUrl"));
                            artifact.setLatitude(document.getDouble("latitude") != null ? document.getDouble("latitude") : 0.0);
                            artifact.setLongitude(document.getDouble("longitude") != null ? document.getDouble("longitude") : 0.0);
                            artifacts.add(artifact);
                        }
                        Log.d(TAG, "Loaded " + artifacts.size() + " artifacts");
                        return artifacts;
                    } else {
                        Log.e(TAG, "Failed to load artifacts: " + task.getException().getMessage());
                        throw task.getException();
                    }
                });
    }

    public Task<Boolean> hasUserCollectedArtifact(String userId, String artifactId) {
        return db.collection(USER_ARTIFACTS_COLLECTION)
                .document(userId)
                .collection("artifacts")
                .document(artifactId)
                .get()
                .continueWith(task -> task.isSuccessful() && task.getResult().exists());
    }

    public Task<Void> collectArtifact(String userId, String artifactId, int points) {
        Map<String, Object> data = new HashMap<>();
        data.put("collectedAt", System.currentTimeMillis());
        data.put("points", points);

        return db.collection(USER_ARTIFACTS_COLLECTION)
                .document(userId)
                .collection("artifacts")
                .document(artifactId)
                .set(data)
                .continueWithTask(task -> {
                    if (task.isSuccessful()) {
                        return db.collection("users")
                                .document(userId)
                                .update("score", com.google.firebase.firestore.FieldValue.increment(points));
                    } else {
                        throw task.getException();
                    }
                });
    }

    // Thêm phương thức để lấy danh sách vật phẩm đã thu thập của một người dùng
    public Task<List<Artifact>> getCollectedArtifacts(String userId) {
        return db.collection(USER_ARTIFACTS_COLLECTION)
                .document(userId)
                .collection("artifacts")
                .get()
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        Log.e(TAG, "Failed to load collected artifact IDs: " + task.getException().getMessage());
                        throw task.getException();
                    }

                    List<String> artifactIds = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : task.getResult()) {
                        artifactIds.add(doc.getId());
                    }

                    if (artifactIds.isEmpty()) {
                        return Tasks.forResult(new ArrayList<>());
                    }

                    // Lấy thông tin chi tiết của các vật phẩm
                    return db.collection(ARTIFACTS_COLLECTION)
                            .whereIn("id", artifactIds)
                            .get()
                            .continueWith(innerTask -> {
                                if (innerTask.isSuccessful()) {
                                    List<Artifact> artifacts = new ArrayList<>();
                                    for (QueryDocumentSnapshot document : innerTask.getResult()) {
                                        Artifact artifact = new Artifact();
                                        artifact.setId(document.getId());
                                        artifact.setName(document.getString("name"));
                                        artifact.setDescription(document.getString("description"));
                                        artifact.setRarity(document.getLong("rarity") != null ? document.getLong("rarity").intValue() : 0);
                                        artifact.setPoints(document.getLong("points") != null ? document.getLong("points").intValue() : 0);
                                        artifact.setImageUrl(document.getString("imageUrl"));
                                        artifact.setLatitude(document.getDouble("latitude") != null ? document.getDouble("latitude") : 0.0);
                                        artifact.setLongitude(document.getDouble("longitude") != null ? document.getDouble("longitude") : 0.0);
                                        artifacts.add(artifact);
                                    }
                                    Log.d(TAG, "Loaded " + artifacts.size() + " collected artifacts for user " + userId);
                                    return artifacts;
                                } else {
                                    Log.e(TAG, "Failed to load artifact details: " + innerTask.getException().getMessage());
                                    throw innerTask.getException();
                                }
                            });
                });
    }
}