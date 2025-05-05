package com.example.btl.Activity;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.bumptech.glide.Glide;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.example.btl.Adapter.CollectedArtifactAdapter;
import com.example.btl.Domain.Model.Artifact;
import com.example.btl.Domain.Model.User;
import com.example.btl.Domain.Repository.ArtifactRepository;
import com.example.btl.Domain.Repository.AuthRepository;
import com.example.btl.Domain.Repository.UserRepository;
import com.example.btl.R;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MyProfileActivity extends AppCompatActivity implements SensorEventListener {
    private static final String TAG = "MyProfileActivity";
    private static final int STORAGE_PERMISSION_CODE = 100;
    private ImageView userAvatar;
    private TextView userName;
    private TextView collectedItemsCount;
    private TextView userScore;
    private GridView collectedItemsGrid;
    private ImageView backButton;
    private ImageButton editButton;
    private User currentUser;
    private AuthRepository authRepository;
    private UserRepository userRepository;
    private ArtifactRepository artifactRepository;
    private List<Artifact> collectedArtifacts;
    private Uri selectedImageUri;
    private ImageView avatarPreview;
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private float lastX, lastY, lastZ;
    private boolean isSensorInitialized = false;
    private static final float SHAKE_THRESHOLD = 15.0f;
    private long lastShakeTime = 0;
    private static final long SHAKE_COOLDOWN = 2000;

    private final ActivityResultLauncher<Intent> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    if (selectedImageUri != null) {
                        Glide.with(this)
                                .load(selectedImageUri)
                                .placeholder(R.drawable.default_avatar)
                                .into(avatarPreview);
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_my_profile);
        } catch (Exception e) {
            Log.e(TAG, "Error inflating layout: " + e.getMessage(), e);
            Toast.makeText(this, "Không thể tải giao diện. Vui lòng thử lại.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        try {
            userAvatar = findViewById(R.id.userAvatar);
            userName = findViewById(R.id.userName);
            collectedItemsCount = findViewById(R.id.collectedItemsCount);
            userScore = findViewById(R.id.userScore);
            collectedItemsGrid = findViewById(R.id.collectedItemsGrid);
            backButton = findViewById(R.id.backButton);
            editButton = findViewById(R.id.editButton);
        } catch (Exception e) {
            Log.e(TAG, "Error finding views: " + e.getMessage(), e);
            Toast.makeText(this, "Lỗi khi ánh xạ giao diện.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        authRepository = new AuthRepository();
        userRepository = new UserRepository();
        artifactRepository = new ArtifactRepository();
        collectedArtifacts = new ArrayList<>();

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        } else {
            Toast.makeText(this, "Thiết bị không hỗ trợ cảm biến gia tốc", Toast.LENGTH_SHORT).show();
        }

        String userId = authRepository.getCurrentUser().getUid();
        loadUserInfo(userId);
        loadCollectedArtifacts(userId);

        backButton.setOnClickListener(v -> finish());
        editButton.setOnClickListener(v -> showEditProfileDialog());

        collectedItemsGrid.setOnItemClickListener((parent, view, position, id) -> {
            Artifact artifact = collectedArtifacts.get(position);
            showArtifactDetailDialog(artifact);
        });
    }

    private void loadUserInfo(String userId) {
        userRepository.getUser(userId)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        currentUser = task.getResult();
                        if (currentUser != null) {
                            userName.setText(currentUser.getName());
                            userScore.setText("Điểm: " + currentUser.getScore());
                            collectedItemsCount.setText("Số vật phẩm: " + currentUser.getItemCount());
                            if (currentUser.getAvatar() != null && !currentUser.getAvatar().isEmpty()) {
                                Glide.with(this)
                                        .load(currentUser.getAvatar())
                                        .placeholder(R.drawable.default_avatar)
                                        .into(userAvatar);
                            } else {
                                userAvatar.setImageResource(R.drawable.default_avatar);
                            }
                        }
                    } else {
                        Log.e(TAG, "Failed to load user info: " + task.getException().getMessage());
                        Toast.makeText(this, "Không thể tải thông tin người dùng", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadCollectedArtifacts(String userId) {
        artifactRepository.getCollectedArtifacts(userId)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        collectedArtifacts.clear();
                        collectedArtifacts.addAll(task.getResult());
                        CollectedArtifactAdapter adapter = new CollectedArtifactAdapter(MyProfileActivity.this, collectedArtifacts);
                        collectedItemsGrid.setAdapter(adapter);

                        int totalPoints = collectedArtifacts.stream().mapToInt(Artifact::getPoints).sum();
                        int itemCount = collectedArtifacts.size();
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("score", totalPoints);
                        updates.put("itemCount", itemCount);
                        userRepository.updateUser(userId, updates)
                                .addOnSuccessListener(aVoid -> {
                                    Log.d(TAG, "Updated score: " + totalPoints + ", itemCount: " + itemCount);
                                    loadUserInfo(userId);
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Failed to update score/itemCount: " + e.getMessage());
                                    Toast.makeText(this, "Lỗi cập nhật điểm", Toast.LENGTH_SHORT).show();
                                });
                    } else {
                        Log.e(TAG, "Failed to load collected artifacts: " + task.getException().getMessage());
                        Toast.makeText(this, "Không thể tải danh sách cổ vật", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() != Sensor.TYPE_ACCELEROMETER) return;

        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];

        if (!isSensorInitialized) {
            lastX = x;
            lastY = y;
            lastZ = z;
            isSensorInitialized = true;
            return;
        }

        float deltaX = Math.abs(x - lastX);
        float deltaY = Math.abs(y - lastY);
        float deltaZ = Math.abs(z - lastZ);

        long currentTime = System.currentTimeMillis();
        if (deltaX > SHAKE_THRESHOLD || deltaY > SHAKE_THRESHOLD || deltaZ > SHAKE_THRESHOLD) {
            if (currentTime - lastShakeTime < SHAKE_COOLDOWN) {
                return;
            }
            lastShakeTime = currentTime;
            Log.d(TAG, "Shake detected, saving sensor data");
            String userId = authRepository.getCurrentUser().getUid();
            artifactRepository.getCollectedArtifacts(userId)
                    .addOnSuccessListener(artifacts -> {
                        int totalPoints = artifacts.stream().mapToInt(Artifact::getPoints).sum();
                        int itemCount = artifacts.size();
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("score", totalPoints);
                        updates.put("itemCount", itemCount);
                        userRepository.updateUser(userId, updates)
                                .addOnSuccessListener(aVoid -> {
                                    userRepository.getUser(userId)
                                            .addOnSuccessListener(user -> {
                                                // Lưu dữ liệu vào sensor_data
                                                Map<String, Object> sensorData = new HashMap<>();
                                                sensorData.put("name", user.getName());
                                                sensorData.put("score", user.getScore());
                                                sensorData.put("itemCount", user.getItemCount());
                                                sensorData.put("sensorName", ""); // Để trống
                                                sensorData.put("sensorDesc", ""); // Để trống
                                                sensorData.put("timestamp", System.currentTimeMillis());

                                                userRepository.updateSensorData(userId, sensorData)
                                                        .addOnSuccessListener(aVoid2 -> {
//                                                            Log.d(TAG, "Saved sensor data to Firestore");
                                                            Toast.makeText(this, "lưu sensor vào Firebase thành công", Toast.LENGTH_SHORT).show();
                                                            Intent intent = new Intent(MyProfileActivity.this, InputActivity.class);
                                                            intent.putExtra("userId", userId);
                                                            startActivity(intent);
                                                        })
                                                        .addOnFailureListener(e -> {
                                                            Log.e(TAG, "Failed to save sensor data: " + e.getMessage());
                                                            Toast.makeText(this, "Lỗi lưu dữ liệu cảm biến", Toast.LENGTH_SHORT).show();
                                                        });
                                            });
                                });
                    });
        }

        lastX = x;
        lastY = y;
        lastZ = z;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Không cần xử lý
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    private void showArtifactDetailDialog(Artifact artifact) {
        Dialog dialog = new Dialog(this);
        try {
            dialog.setContentView(R.layout.dialog_artifact_detail);
        } catch (Exception e) {
            Log.e(TAG, "Error inflating dialog layout: " + e.getMessage(), e);
            Toast.makeText(this, "Không thể mở chi tiết cổ vật.", Toast.LENGTH_LONG).show();
            return;
        }

        TextView artifactName = dialog.findViewById(R.id.artifactName);
        TextView artifactDescription = dialog.findViewById(R.id.artifactDescription);
        TextView artifactRarity = dialog.findViewById(R.id.artifactRarity);
        TextView artifactPoints = dialog.findViewById(R.id.artifactPoints);
        ImageView artifactImage = dialog.findViewById(R.id.artifactImage);
        TextView collectedAtText = dialog.findViewById(R.id.collectedAtText);
        Button closeButton = dialog.findViewById(R.id.closeButton);

        artifactName.setText(artifact.getName());
        artifactDescription.setText(artifact.getDescription());
        artifactRarity.setText("Độ hiếm: " + artifact.getRarity());
        artifactPoints.setText("Điểm: " + artifact.getPoints());

        if (artifact.getCollectedAt() != null) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
            String collectedAtStr = dateFormat.format(artifact.getCollectedAt().toDate());
            collectedAtText.setText("Thu thập vào: " + collectedAtStr);
        } else {
            collectedAtText.setText("Thu thập vào: Không xác định");
        }

        if (artifact.getImageUrl() != null && !artifact.getImageUrl().isEmpty()) {
            Glide.with(this)
                    .load(artifact.getImageUrl())
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.error_image)
                    .into(artifactImage);
        } else {
            artifactImage.setImageResource(R.drawable.error_image);
        }

        closeButton.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void showEditProfileDialog() {
        Dialog dialog = new Dialog(this);
        try {
            dialog.setContentView(R.layout.dialog_edit_profile);
        } catch (Exception e) {
            Log.e(TAG, "Error inflating dialog layout: " + e.getMessage(), e);
            Toast.makeText(this, "Không thể mở dialog chỉnh sửa.", Toast.LENGTH_LONG).show();
            return;
        }

        EditText nameEditText = dialog.findViewById(R.id.nameEditText);
        avatarPreview = dialog.findViewById(R.id.avatarPreview);
        Button selectAvatarButton = dialog.findViewById(R.id.selectAvatarButton);
        Button cancelButton = dialog.findViewById(R.id.cancelButton);
        Button saveButton = dialog.findViewById(R.id.saveButton);

        nameEditText.setText(currentUser.getName());
        if (currentUser.getAvatar() != null && !currentUser.getAvatar().isEmpty()) {
            Glide.with(this)
                    .load(currentUser.getAvatar())
                    .placeholder(R.drawable.default_avatar)
                    .into(avatarPreview);
        } else {
            avatarPreview.setImageResource(R.drawable.default_avatar);
        }

        selectAvatarButton.setOnClickListener(v -> {
            if (checkStoragePermission()) {
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                pickImageLauncher.launch(intent);
            } else {
                requestStoragePermission();
            }
        });

        cancelButton.setOnClickListener(v -> {
            selectedImageUri = null;
            dialog.dismiss();
        });

        saveButton.setOnClickListener(v -> {
            String newName = nameEditText.getText().toString().trim();

            if (newName.isEmpty()) {
                nameEditText.setError("Vui lòng nhập họ tên");
                nameEditText.requestFocus();
                return;
            }

            Map<String, Object> updates = new HashMap<>();
            updates.put("name", newName);

            if (selectedImageUri != null) {
                uploadImageToCloudinary(selectedImageUri, newName, dialog);
            } else {
                updates.put("avatar", currentUser.getAvatar() != null ? currentUser.getAvatar() : "");
                updateUserInfo(updates, newName, dialog);
            }
        });

        dialog.show();
    }

    private void uploadImageToCloudinary(Uri imageUri, String newName, Dialog dialog) {
        String userId = authRepository.getCurrentUser().getUid();

        MediaManager.get().upload(imageUri)
                .unsigned("covat-upload")
                .option("folder", "avatars/" + userId)
                .callback(new UploadCallback() {
                    @Override
                    public void onStart(String requestId) {
                        Log.d(TAG, "Upload started: " + requestId);
                    }

                    @Override
                    public void onProgress(String requestId, long bytes, long totalBytes) {
                        Log.d(TAG, "Uploading: " + bytes + "/" + totalBytes);
                    }

                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        String imageUrl = resultData.get("secure_url").toString();
                        Log.d(TAG, "Upload success, URL: " + imageUrl);
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("name", newName);
                        updates.put("avatar", imageUrl);
                        updateUserInfo(updates, newName, dialog);
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        Log.e(TAG, "Upload error: " + error.getDescription());
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("name", newName);
                        updates.put("avatar", currentUser.getAvatar() != null ? currentUser.getAvatar() : "");
                        updateUserInfo(updates, newName, dialog);
                        runOnUiThread(() -> Toast.makeText(MyProfileActivity.this, "Tải ảnh lên thất bại: " + error.getDescription(), Toast.LENGTH_SHORT).show());
                    }

                    @Override
                    public void onReschedule(String requestId, ErrorInfo error) {
                        Log.d(TAG, "Upload rescheduled: " + error.getDescription());
                    }
                })
                .dispatch(this);
    }

    private void updateUserInfo(Map<String, Object> updates, String newName, Dialog dialog) {
        String userId = authRepository.getCurrentUser().getUid();
        userRepository.updateUser(userId, updates)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        runOnUiThread(() -> {
                            Toast.makeText(this, "Cập nhật thông tin thành công", Toast.LENGTH_SHORT).show();
                            currentUser.setName(newName);
                            currentUser.setAvatar(updates.get("avatar").toString());
                            userName.setText(newName);
                            if (currentUser.getAvatar() != null && !currentUser.getAvatar().isEmpty()) {
                                Glide.with(this)
                                        .load(currentUser.getAvatar())
                                        .placeholder(R.drawable.default_avatar)
                                        .into(userAvatar);
                            } else {
                                userAvatar.setImageResource(R.drawable.default_avatar);
                            }
                            selectedImageUri = null;
                            dialog.dismiss();
                        });
                    } else {
                        runOnUiThread(() -> {
                            Toast.makeText(this, "Cập nhật thất bại: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "Failed to update user info: " + task.getException().getMessage());
                            dialog.dismiss();
                        });
                    }
                })
                .addOnFailureListener(e -> {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Update user failed: " + e.getMessage());
                        dialog.dismiss();
                    });
                });
    }

    private boolean checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED;
        } else {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_MEDIA_IMAGES}, STORAGE_PERMISSION_CODE);
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, STORAGE_PERMISSION_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Quyền truy cập bộ nhớ đã được cấp", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                pickImageLauncher.launch(intent);
            } else {
                Toast.makeText(this, "Ứng dụng cần quyền truy cập bộ nhớ để chọn ảnh", Toast.LENGTH_LONG).show();
            }
        }
    }
}