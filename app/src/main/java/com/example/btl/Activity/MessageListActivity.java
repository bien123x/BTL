package com.example.btl.Activity;

import android.os.Bundle;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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

        messageListRecyclerView = findViewById(R.id.messageListRecyclerView);
        userList = new ArrayList<>();
        userRepository = new UserRepository();
        authRepository = new AuthRepository();
        db = FirebaseFirestore.getInstance();

        // Thiết lập RecyclerView
        messageListRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MessageUserAdapter(userList, user -> {
            // Xử lý khi nhấn vào một người dùng (có thể mở chat activity)
            Log.d(TAG, "Clicked on user: " + user.getName());
        });
        messageListRecyclerView.setAdapter(adapter);

        // Tải danh sách người nhắn tin
        loadMessageUsers();
    }

    private void loadMessageUsers() {
        String currentUserId = authRepository.getCurrentUser().getUid();

        // Lấy danh sách các cuộc trò chuyện mà người dùng hiện tại tham gia
        db.collection("messages")
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