package com.example.btl.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.example.btl.Adapter.CollectedArtifactAdapter;
import com.example.btl.Domain.Model.Artifact;
import com.example.btl.Domain.Model.User;
import com.example.btl.Domain.Repository.ArtifactRepository;
import com.example.btl.R;
import java.util.ArrayList;
import java.util.List;

public class UserProfileActivity extends AppCompatActivity {
    private static final String TAG = "UserProfileActivity";
    private ImageView userAvatar;
    private TextView userName;
    private TextView collectedItemsCount;
    private TextView userScore;
    private GridView collectedItemsGrid;
    private ImageView backButton; // Đổi từ ImageButton thành ImageView
    private ImageButton messageButton;
    private User user;
    private ArtifactRepository artifactRepository;
    private List<Artifact> collectedArtifacts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        // Ánh xạ các view
        userAvatar = findViewById(R.id.userAvatar);
        userName = findViewById(R.id.userName);
        collectedItemsCount = findViewById(R.id.collectedItemsCount);
        userScore = findViewById(R.id.userScore);
        collectedItemsGrid = findViewById(R.id.collectedItemsGrid);
        backButton = findViewById(R.id.backButton);
        messageButton = findViewById(R.id.messageButton);

        // Khởi tạo
        artifactRepository = new ArtifactRepository();
        collectedArtifacts = new ArrayList<>();

        // Lấy thông tin người dùng từ Intent
        user = (User) getIntent().getSerializableExtra("user");
        if (user == null) {
            Log.e(TAG, "User is null");
            finish();
            return;
        }

        // Hiển thị thông tin người dùng
        userName.setText(user.getName());
        if (user.getAvatar() != null && !user.getAvatar().isEmpty()) {
            Glide.with(this)
                    .load(user.getAvatar())
                    .placeholder(R.drawable.default_avatar)
                    .into(userAvatar);
        } else {
            userAvatar.setImageResource(R.drawable.default_avatar);
        }

        // Tải danh sách vật phẩm đã thu thập và tính điểm
        loadCollectedArtifacts();

        // Xử lý nút quay lại
        backButton.setOnClickListener(v -> finish());

        // Xử lý nút nhắn tin
        messageButton.setOnClickListener(v -> {
            Intent intent = new Intent(UserProfileActivity.this, ChatActivity.class);
            intent.putExtra("otherUser", user);
            startActivity(intent);
        });
    }

    private void loadCollectedArtifacts() {
        artifactRepository.getCollectedArtifacts(user.getId())
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        collectedArtifacts.clear();
                        collectedArtifacts.addAll(task.getResult());
                        collectedItemsCount.setText("Số vật phẩm thu thập: " + collectedArtifacts.size());

                        // Tính tổng điểm từ danh sách cổ vật
                        int totalPoints = 0;
                        for (Artifact artifact : collectedArtifacts) {
                            totalPoints += artifact.getPoints();
                        }
                        userScore.setText("Điểm: " + totalPoints);

                        // Hiển thị danh sách cổ vật
                        CollectedArtifactAdapter adapter = new CollectedArtifactAdapter(UserProfileActivity.this, collectedArtifacts);
                        collectedItemsGrid.setAdapter(adapter);
                    } else {
                        Log.e(TAG, "Failed to load collected artifacts: " + task.getException().getMessage());
                        userScore.setText("Điểm: 0"); // Hiển thị mặc định nếu có lỗi
                    }
                });
    }
}