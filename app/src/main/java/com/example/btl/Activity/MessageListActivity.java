package com.example.btl.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.btl.Adapter.MessageUserAdapter;
import com.example.btl.Domain.Model.User;
import com.example.btl.Domain.Repository.AuthRepository;
import com.example.btl.Domain.Repository.UserRepository;
import com.example.btl.R;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MessageListActivity extends AppCompatActivity {
    private static final String TAG = "MessageListActivity";
    private RecyclerView messageListRecyclerView;
    private ImageView backButton;
    private MessageUserAdapter adapter;
    private List<User> userList;
    private UserRepository userRepository;
    private AuthRepository authRepository;
    private FirebaseFirestore db;
    private List<ListenerRegistration> userListeners;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message_list);

        // Ánh xạ các view
        messageListRecyclerView = findViewById(R.id.messageListRecyclerView);
        backButton = findViewById(R.id.backButton);
        userList = new ArrayList<>();
        userRepository = new UserRepository();
        authRepository = new AuthRepository();
        db = FirebaseFirestore.getInstance();
        userListeners = new ArrayList<>();

        // Thiết lập nút quay lại
        if (backButton != null) {
            backButton.setOnClickListener(v -> finish());
        } else {
            Log.e(TAG, "backButton không tìm thấy trong layout");
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

                        // Lắng nghe thông tin người dùng từ danh sách userIds
                        if (!userIds.isEmpty()) {
                            for (String userId : userIds) {
                                ListenerRegistration listener = db.collection("users")
                                        .document(userId)
                                        .addSnapshotListener((snapshot, e) -> {
                                            if (e != null) {
                                                Log.e(TAG, "Failed to listen for user " + userId + ": " + e.getMessage());
                                                return;
                                            }
                                            if (snapshot != null && snapshot.exists()) {
                                                User user = snapshot.toObject(User.class);
                                                if (user != null) {
                                                    // Cập nhật hoặc thêm người dùng vào danh sách
                                                    int index = -1;
                                                    for (int i = 0; i < userList.size(); i++) {
                                                        if (userList.get(i).getId().equals(user.getId())) {
                                                            index = i;
                                                            break;
                                                        }
                                                    }
                                                    if (index != -1) {
                                                        userList.set(index, user);
                                                    } else {
                                                        userList.add(user);
                                                    }
                                                    adapter.notifyDataSetChanged();
                                                }
                                            }
                                        });
                                userListeners.add(listener);
                            }
                        }
                    } else {
                        Log.e(TAG, "Failed to load messages: " + task.getException().getMessage());
                    }
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Hủy các listener để tránh rò rỉ bộ nhớ
        for (ListenerRegistration listener : userListeners) {
            listener.remove();
        }
    }
}