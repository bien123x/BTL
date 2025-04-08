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
    private static final String USERS_COLLECTION = "users";

    public ArtifactRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    // Lấy danh sách tất cả cổ vật
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
                            artifact.setImageUrl(document.getString("imageUrl"));
                            artifact.setLatitude(document.getDouble("latitude") != null ? document.getDouble("latitude") : 0.0);
                            artifact.setLongitude(document.getDouble("longitude") != null ? document.getDouble("longitude") : 0.0);
                            artifact.setRarity(document.getLong("rarity") != null ? document.getLong("rarity").intValue() : 0);
                            artifact.setPoints(document.getLong("points") != null ? document.getLong("points").intValue() : 0);
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

    // Kiểm tra xem người dùng đã thu thập cổ vật chưa
    public Task<Boolean> hasUserCollectedArtifact(String userId, String artifactId) {
        return db.collection(USERS_COLLECTION).document(userId)
                .get()
                .continueWith(task -> {
                    if (task.isSuccessful()) {
                        Map<String, Object> collectedArtifacts = (Map<String, Object>) task.getResult().get("collectedArtifacts");
                        if (collectedArtifacts != null && collectedArtifacts.containsKey(artifactId)) {
                            return (Boolean) collectedArtifacts.get(artifactId);
                        }
                        return false;
                    } else {
                        Log.e(TAG, "Failed to check collected artifact: " + task.getException().getMessage());
                        throw task.getException();
                    }
                });
    }

    // Thu thập cổ vật
    public Task<Void> collectArtifact(String userId, String artifactId, int points) {
        Map<String, Object> collectedData = new HashMap<>();
        collectedData.put("collectedArtifacts." + artifactId, true);

        // Cộng điểm cho người dùng
        return db.collection(USERS_COLLECTION).document(userId)
                .update(collectedData)
                .continueWithTask(task -> {
                    if (task.isSuccessful()) {
                        return db.collection(USERS_COLLECTION).document(userId)
                                .update("score", com.google.firebase.firestore.FieldValue.increment(points));
                    } else {
                        throw task.getException();
                    }
                })
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Artifact " + artifactId + " collected by user " + userId))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to collect artifact: " + e.getMessage()));
    }
}
