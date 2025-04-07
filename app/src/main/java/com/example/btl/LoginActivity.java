package com.example.btl;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.example.btl.databinding.ActivityLoginBinding;
import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {
    private ActivityLoginBinding binding;
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private static final String TAG = "LoginActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Khởi tạo Firebase
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Kiểm tra FirebaseAuth
        if (auth == null) {
            Log.e(TAG, "FirebaseAuth instance is null");
            Toast.makeText(this, "Lỗi khởi tạo FirebaseAuth", Toast.LENGTH_LONG).show();
            return;
        }

        // Kiểm tra nếu người dùng đã đăng nhập
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            Log.d(TAG, "User already logged in: " + currentUser.getUid());
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish();
            return;
        }

        // Sự kiện nhấn nút đăng nhập
        binding.loginButton.setOnClickListener(v -> {
            String email = binding.emailLogin.getText().toString().trim();
            String password = binding.passwordLogin.getText().toString().trim();

            // Kiểm tra dữ liệu đầu vào
            if (!validateInput(email, password)) {
                return;
            }

            // Kiểm tra kết nối mạng
            if (!isNetworkAvailable()) {
                Toast.makeText(this, "Không có kết nối mạng. Vui lòng kiểm tra lại!", Toast.LENGTH_LONG).show();
                return;
            }

            // Hiển thị loading
            showLoading(true);
            Log.d(TAG, "Attempting to login with email: " + email);
            login(email, password);
        });

        // Chuyển đến màn hình đăng ký
        binding.goToRegister.setOnClickListener(v -> {
            Log.d(TAG, "Navigating to RegisterActivity");
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            finish();
        });
    }

    // Kiểm tra dữ liệu đầu vào
    private boolean validateInput(String email, String password) {
        if (email.isEmpty()) {
            binding.emailLogin.setError("Vui lòng nhập email");
            binding.emailLogin.requestFocus();
            return false;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailLogin.setError("Email không hợp lệ");
            binding.emailLogin.requestFocus();
            return false;
        }

        if (password.isEmpty()) {
            binding.passwordLogin.setError("Vui lòng nhập mật khẩu");
            binding.passwordLogin.requestFocus();
            return false;
        }

        if (password.length() < 6) {
            binding.passwordLogin.setError("Mật khẩu phải có ít nhất 6 ký tự");
            binding.passwordLogin.requestFocus();
            return false;
        }

        return true;
    }

    // Kiểm tra kết nối mạng
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    // Hiển thị/Ẩn loading indicator
    private void showLoading(boolean isLoading) {
        binding.loginButton.setEnabled(!isLoading);
        binding.loginButton.setText(isLoading ? "Đang xử lý..." : "Đăng nhập");
        // Nếu bạn có ProgressBar trong layout, có thể thêm:
        // binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }

    // Đăng nhập người dùng
    private void login(String email, String password) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = auth.getCurrentUser();
                        if (user == null) {
                            Log.e(TAG, "FirebaseUser is null after login");
                            Toast.makeText(this, "Đăng nhập thất bại: Không thể lấy thông tin người dùng", Toast.LENGTH_LONG).show();
                            showLoading(false);
                            return;
                        }

                        // Cập nhật trạng thái online
                        updateOnlineStatus(user.getUid(), true);

                        Log.d(TAG, "Login successful for UID: " + user.getUid());
                        Toast.makeText(this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(LoginActivity.this, MainActivity.class));
                        finish();
                    } else {
                        String errorMessage = task.getException() != null ? task.getException().getMessage() : "Lỗi không xác định";
                        Log.e(TAG, "Login failed: " + errorMessage);
                        Toast.makeText(this, "Đăng nhập thất bại: " + errorMessage, Toast.LENGTH_LONG).show();
                    }
                    showLoading(false);
                });
    }

    // Cập nhật trạng thái online
    private void updateOnlineStatus(String userId, boolean online) {
        Map<String, Object> status = new HashMap<>();
        status.put("online", online);
        db.collection("users").document(userId)
                .update(status)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Updated online status to " + online + " for UID: " + userId))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to update online status: " + e.getMessage()));
    }
}