package com.example.btl.Activity;

import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.btl.Adapter.MessageAdapter;
import com.example.btl.Domain.Model.Message;
import com.example.btl.Domain.Model.User;
import com.example.btl.Domain.Repository.AuthRepository;
import com.example.btl.R;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatActivity extends AppCompatActivity {
    private static final String TAG = "ChatActivity";
    private RecyclerView chatRecyclerView;
    private EditText messageInput;
    private ImageButton sendButton;
    private ImageView userAvatar;
    private TextView userName;
    private ImageButton backButton;
    private List<Message> messageList;
    private MessageAdapter adapter;
    private AuthRepository authRepository;
    private FirebaseFirestore db;
    private String currentUserId;
    private User otherUser;
    private String conversationId;
    private ListenerRegistration messageListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // Ánh xạ các view
        chatRecyclerView = findViewById(R.id.chatRecyclerView);
        messageInput = findViewById(R.id.messageInput);
        sendButton = findViewById(R.id.sendButton);
        userAvatar = findViewById(R.id.userAvatar);
        userName = findViewById(R.id.userName);
        backButton = findViewById(R.id.backButton);

        // Khởi tạo
        authRepository = new AuthRepository();
        db = FirebaseFirestore.getInstance();
        currentUserId = authRepository.getCurrentUser().getUid();
        messageList = new ArrayList<>();

        // Lấy thông tin người dùng được chọn từ Intent
        otherUser = (User) getIntent().getSerializableExtra("otherUser");
        if (otherUser == null) {
            Log.e(TAG, "Other user is null");
            finish();
            return;
        }

        // Hiển thị thông tin người dùng
        userName.setText(otherUser.getName());
        if (otherUser.getAvatar() != null && !otherUser.getAvatar().isEmpty()) {
            Glide.with(this)
                    .load(otherUser.getAvatar())
                    .placeholder(R.drawable.default_avatar)
                    .into(userAvatar);
        } else {
            userAvatar.setImageResource(R.drawable.default_avatar);
        }

        // Thiết lập RecyclerView
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        chatRecyclerView.setLayoutManager(layoutManager);
        adapter = new MessageAdapter(messageList, currentUserId, otherUser);
        chatRecyclerView.setAdapter(adapter);

        // Tìm hoặc tạo conversationId
        findOrCreateConversation();

        // Xử lý nút quay lại
        backButton.setOnClickListener(v -> finish());

        // Xử lý nút gửi tin nhắn
        sendButton.setOnClickListener(v -> sendMessage());
    }

    private void findOrCreateConversation() {
        String otherUserId = otherUser.getId();
        String[] participants = new String[]{currentUserId, otherUserId};
        java.util.Arrays.sort(participants); // Sắp xếp để đảm bảo conversationId nhất quán
        conversationId = participants[0] + "_" + participants[1];

        // Kiểm tra xem conversation đã tồn tại chưa
        DocumentReference conversationRef = db.collection("conversations").document(conversationId);
        conversationRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && !task.getResult().exists()) {
                // Tạo conversation mới nếu chưa tồn tại
                Map<String, Object> conversationData = new HashMap<>();
                conversationData.put("participants", java.util.Arrays.asList(currentUserId, otherUserId));
                conversationData.put("lastMessage", "");
                conversationData.put("timestamp", System.currentTimeMillis());
                conversationRef.set(conversationData)
                        .addOnSuccessListener(aVoid -> Log.d(TAG, "Created new conversation: " + conversationId))
                        .addOnFailureListener(e -> Log.e(TAG, "Failed to create conversation: " + e.getMessage()));
            }
            // Lắng nghe tin nhắn
            listenForMessages();
        });
    }

    private void listenForMessages() {
        messageListener = db.collection("conversations")
                .document(conversationId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Listen failed: " + e.getMessage());
                        return;
                    }
                    if (snapshots != null) {
                        messageList.clear();
                        for (QueryDocumentSnapshot doc : snapshots) {
                            Message message = new Message();
                            message.setSenderId(doc.getString("senderId"));
                            message.setContent(doc.getString("content"));
                            message.setTimestamp(doc.getLong("timestamp") != null ? doc.getLong("timestamp") : 0);
                            messageList.add(message);
                        }
                        adapter.notifyDataSetChanged();
                        chatRecyclerView.scrollToPosition(messageList.size() - 1);
                    }
                });
    }

    private void sendMessage() {
        String content = messageInput.getText().toString().trim();
        if (content.isEmpty()) {
            return;
        }

        Message message = new Message(currentUserId, content, System.currentTimeMillis());
        DocumentReference messageRef = db.collection("conversations")
                .document(conversationId)
                .collection("messages")
                .document();
        messageRef.set(message)
                .addOnSuccessListener(aVoid -> {
                    // Cập nhật lastMessage và timestamp trong conversation
                    Map<String, Object> updateData = new HashMap<>();
                    updateData.put("lastMessage", content);
                    updateData.put("timestamp", System.currentTimeMillis());
                    db.collection("conversations").document(conversationId)
                            .update(updateData);
                    messageInput.setText("");
                })
                .addOnFailureListener(e -> Log.e(TAG, "Failed to send message: " + e.getMessage()));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (messageListener != null) {
            messageListener.remove();
        }
    }
}
