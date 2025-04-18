package com.example.btl.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.example.btl.Adapter.MessageUserAdapter;
import com.example.btl.Domain.Model.User;
import com.example.btl.Domain.Repository.AuthRepository;
import com.example.btl.Domain.Repository.UserRepository;
import com.example.btl.R;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MessageListActivity extends AppCompatActivity {
    private static final String TAG = "MessageListActivity";
    private RecyclerView messageListRecyclerView;
    private MessageUserAdapter adapter;
    private List<User> userList;
    private UserRepository userRepository;
    private AuthRepository authRepository;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message_list);

        // Kiểm tra Google Play Services
        GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = googleApiAvailability.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            Log.e(TAG, "Google Play Services không khả dụng, mã lỗi: " + resultCode);
            Toast.makeText(this, "Google Play Services không khả dụng", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        messageListRecyclerView = findViewById(R.id.messageListRecyclerView);
        userList = new ArrayList<>();
        userRepository = new UserRepository();
        authRepository = new AuthRepository();
        db = FirebaseFirestore.getInstance();

        // Kiểm tra trạng thái đăng nhập
        if (authRepository.getCurrentUser() == null) {
            Log.e(TAG, "Người dùng chưa đăng nhập");
            Toast.makeText(this, "Vui lòng đăng nhập lại", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        // Thiết lập RecyclerView
        messageListRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MessageUserAdapter(userList, user -> {
            // Mở ChatActivity khi nhấn vào một người dùng
            Intent intent = new Intent(MessageListActivity.this, ChatActivity.class);
            intent.putExtra("otherUser", user);
            startActivity(intent);
        });
        messageListRecyclerView.setAdapter(adapter);

        // Tải danh sách người nhắn tin
        loadMessageUsers();
    }

    private void loadMessageUsers() {
        String currentUserId = authRepository.getCurrentUser().getUid();

        // Lấy danh sách các cuộc trò chuyện mà người dùng hiện tại tham gia
        db.collection("conversations")
                .whereArrayContains("participants", currentUserId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Set<String> userIds = new HashSet<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            List<String> participants = (List<String>) document.get("participants");
                            if (participants != null) {
                                for (String participant : participants) {
                                    if (!participant.equals(currentUserId)) {
                                        userIds.add(participant);
                                    }
                                }
                            }
                        }

                        // Lấy thông tin người dùng từ danh sách userIds
                        if (!userIds.isEmpty()) {
                            for (String userId : userIds) {
                                userRepository.getUser(userId)
                                        .addOnCompleteListener(userTask -> {
                                            if (userTask.isSuccessful()) {
                                                User user = userTask.getResult();
                                                if (user != null) {
                                                    userList.add(user);
                                                    adapter.notifyDataSetChanged();
                                                }
                                            } else {
                                                Log.e(TAG, "Failed to load user " + userId + ": " + userTask.getException().getMessage());
                                            }
                                        });
                            }
                        }
                    } else {
                        Log.e(TAG, "Failed to load messages: " + task.getException().getMessage());
                    }
                });
    }
}