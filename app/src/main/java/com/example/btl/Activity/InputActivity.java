package com.example.btl.Activity;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.btl.Domain.Repository.UserRepository;
import com.example.btl.R;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.HashMap;
import java.util.Map;

public class InputActivity extends AppCompatActivity {
    private static final String TAG = "InputActivity";
    private TextView nameTextView, scoreTextView, itemCountTextView;
    private EditText sensorNameEditText, sensorDescEditText;
    private Button updateButton;
    private UserRepository userRepository;
    private String userId;
    private String documentId; // Lưu ID của document trong sensor_data

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_input);

        nameTextView = findViewById(R.id.nameTextView);
        scoreTextView = findViewById(R.id.scoreTextView);
        itemCountTextView = findViewById(R.id.itemCountTextView);
        sensorNameEditText = findViewById(R.id.sensorNameEditText);
        sensorDescEditText = findViewById(R.id.sensorDescEditText);
        updateButton = findViewById(R.id.updateButton);
        userRepository = new UserRepository();

        userId = getIntent().getStringExtra("userId");
        loadSensorData();

        updateButton.setOnClickListener(v -> {
            String sensorName = sensorNameEditText.getText().toString();
            String sensorDesc = sensorDescEditText.getText().toString();

            Map<String, Object> updates = new HashMap<>();
            updates.put("sensorName", sensorName);
            updates.put("sensorDesc", sensorDesc);
            updates.put("timestamp", System.currentTimeMillis());

            userRepository.updateSensorData(userId, documentId, updates)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(InputActivity.this, "Cập nhật dữ liệu cảm biến thành công", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(InputActivity.this, "Lỗi cập nhật: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });
    }

    private void loadSensorData() {
        userRepository.getLatestSensorData(userId)
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot document = queryDocumentSnapshots.getDocuments().get(0);
                        documentId = document.getId();
                        String name = document.getString("name");
                        Long score = document.getLong("score");
                        Long itemCount = document.getLong("itemCount");

                        nameTextView.setText("Tên: " + (name != null ? name : "Không xác định"));
                        scoreTextView.setText("Điểm: " + (score != null ? score : 0));
                        itemCountTextView.setText("Số vật phẩm: " + (itemCount != null ? itemCount : 0));
                        sensorNameEditText.setText(document.getString("sensorName") != null ? document.getString("sensorName") : "");
                        sensorDescEditText.setText(document.getString("sensorDesc") != null ? document.getString("sensorDesc") : "");
                    } else {
                        Toast.makeText(this, "Không tìm thấy dữ liệu cảm biến", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load sensor data: " + e.getMessage());
                    Toast.makeText(this, "Lỗi tải dữ liệu cảm biến", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }
}