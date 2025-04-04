package com.example.btl;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.example.btl.databinding.ActivityRegisterBinding;
import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {
    private ActivityRegisterBinding binding;
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private static final String TAG = "RegisterActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        if (auth == null) {
            Log.e(TAG, "FirebaseAuth instance is null");
            Toast.makeText(this, "FirebaseAuth not initialized", Toast.LENGTH_LONG).show();
            return;
        }

        binding.registerButton.setOnClickListener(v -> {
            String name = binding.nameRegister.getText().toString().trim();
            String email = binding.emailRegister.getText().toString().trim();
            String password = binding.passwordRegister.getText().toString().trim();
            String confirmPass = binding.confirmPassword.getText().toString().trim();

            if (name.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập tên người dùng", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!email.isEmpty() && !password.isEmpty() && password.equals(confirmPass)) {
                Log.d(TAG, "Attempting to register with email: " + email);
                register(name, email, password);
            } else {
                Toast.makeText(this, "Thông tin không hợp lệ. Kiểm tra email, mật khẩu và xác nhận mật khẩu.", Toast.LENGTH_SHORT).show();
            }
        });

        binding.goToLogin.setOnClickListener(v -> {
            Log.d(TAG, "Navigating to LoginActivity");
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            finish();
        });
    }

    private void register(String name, String email, String password) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "User registered successfully with email: " + email);
                        FirebaseUser user = auth.getCurrentUser();
                        if (user == null) {
                            Log.e(TAG, "FirebaseUser is null after registration");
                            Toast.makeText(this, "Đăng ký thất bại: Không thể lấy thông tin người dùng", Toast.LENGTH_LONG).show();
                            return;
                        }

                        // Lưu dữ liệu người dùng vào Firestore
                        Map<String, Object> userData = new HashMap<>();
                        userData.put("name", name);
                        userData.put("email", email);
                        userData.put("avatar", "default_avatar.png");
                        userData.put("score", 0);

                        db.collection("users").document(user.getUid())
                                .set(userData)
                                .addOnCompleteListener(task1 -> {
                                    if (task1.isSuccessful()) {
                                        Log.d(TAG, "User data saved successfully for UID: " + user.getUid());
                                        Toast.makeText(this, "Đăng ký thành công!", Toast.LENGTH_SHORT).show();
                                        startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                                        finish();
                                    } else {
                                        String errorMessage = task1.getException() != null ? task1.getException().getMessage() : "Unknown error";
                                        Log.e(TAG, "Failed to save user data: " + errorMessage);
                                        Toast.makeText(this, "Đăng ký thành công nhưng không thể lưu dữ liệu: " + errorMessage, Toast.LENGTH_LONG).show();
                                        startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                                        finish();
                                    }
                                });
                    } else {
                        String errorMessage = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                        Log.e(TAG, "Registration failed: " + errorMessage);
                        Toast.makeText(this, "Đăng ký thất bại: " + errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
    }
}