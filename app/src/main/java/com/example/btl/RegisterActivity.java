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

        // Khởi tạo Firebase
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Kiểm tra FirebaseAuth
        if (auth == null) {
            Log.e(TAG, "FirebaseAuth instance is null");
            Toast.makeText(this, "Lỗi khởi tạo FirebaseAuth", Toast.LENGTH_LONG).show();
            return;
        }

        // Sự kiện nhấn nút quay lại
        binding.backButton.setOnClickListener(v -> {
            Log.d(TAG, "Back button clicked, navigating to LoginActivity");
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            finish();
        });

        // Sự kiện nhấn nút đăng ký
        binding.registerButton.setOnClickListener(v -> {
            String name = binding.nameRegister.getText().toString().trim();
            String email = binding.emailRegister.getText().toString().trim();
            String password = binding.passwordRegister.getText().toString().trim();
            String confirmPass = binding.confirmPassword.getText().toString().trim();

            // Kiểm tra dữ liệu đầu vào
            if (!validateInput(name, email, password, confirmPass)) {
                return;
            }

            // Kiểm tra kết nối mạng
            if (!isNetworkAvailable()) {
                Toast.makeText(this, "Không có kết nối mạng. Vui lòng kiểm tra lại!", Toast.LENGTH_LONG).show();
                return;
            }

            // Hiển thị loading
            showLoading(true);
            Log.d(TAG, "Attempting to register with email: " + email);
            register(name, email, password);
        });

        // Chuyển đến màn hình đăng nhập
        binding.goToLogin.setOnClickListener(v -> {
            Log.d(TAG, "Navigating to LoginActivity");
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            finish();
        });
    }

    // Kiểm tra dữ liệu đầu vào
    private boolean validateInput(String name, String email, String password, String confirmPass) {
        if (name.isEmpty()) {
            binding.nameRegister.setError("Vui lòng nhập tên người dùng");
            binding.nameRegister.requestFocus();
            return false;
        }

        if (email.isEmpty()) {
            binding.emailRegister.setError("Vui lòng nhập email");
            binding.emailRegister.requestFocus();
            return false;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailRegister.setError("Email không hợp lệ");
            binding.emailRegister.requestFocus();
            return false;
        }

        if (password.isEmpty()) {
            binding.passwordRegister.setError("Vui lòng nhập mật khẩu");
            binding.passwordRegister.requestFocus();
            return false;
        }

        if (password.length() < 6) {
            binding.passwordRegister.setError("Mật khẩu phải có ít nhất 6 ký tự");
            binding.passwordRegister.requestFocus();
            return false;
        }

        if (confirmPass.isEmpty()) {
            binding.confirmPassword.setError("Vui lòng xác nhận mật khẩu");
            binding.confirmPassword.requestFocus();
            return false;
        }

        if (!password.equals(confirmPass)) {
            binding.confirmPassword.setError("Mật khẩu xác nhận không khớp");
            binding.confirmPassword.requestFocus();
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
        binding.registerButton.setEnabled(!isLoading);
        binding.registerButton.setText(isLoading ? "Đang xử lý..." : "Đăng ký");
        binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }

    // Đăng ký người dùng
    private void register(String name, String email, String password) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "User registered successfully with email: " + email);
                        FirebaseUser user = auth.getCurrentUser();
                        if (user == null) {
                            Log.e(TAG, "FirebaseUser is null after registration");
                            Toast.makeText(this, "Đăng ký thất bại: Không thể lấy thông tin người dùng", Toast.LENGTH_LONG).show();
                            showLoading(false);
                            return;
                        }

                        // Lưu dữ liệu người dùng vào Firestore
                        Map<String, Object> userData = new HashMap<>();
                        userData.put("name", name);
                        userData.put("email", email);
                        userData.put("avatar", "default_avatar.png");
                        userData.put("score", 0);
                        userData.put("online", true);

                        db.collection("users").document(user.getUid())
                                .set(userData)
                                .addOnCompleteListener(task1 -> {
                                    showLoading(false);
                                    if (task1.isSuccessful()) {
                                        Log.d(TAG, "User data saved successfully for UID: " + user.getUid());
                                        Toast.makeText(this, "Đăng ký thành công! Vui lòng đăng nhập.", Toast.LENGTH_SHORT).show();
                                        startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                                        finish();
                                    } else {
                                        String errorMessage = task1.getException() != null ? task1.getException().getMessage() : "Lỗi không xác định";
                                        Log.e(TAG, "Failed to save user data: " + errorMessage);
                                        Toast.makeText(this, "Đăng ký thành công nhưng không thể lưu dữ liệu: " + errorMessage, Toast.LENGTH_LONG).show();
                                        startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                                        finish();
                                    }
                                });
                    } else {
                        showLoading(false);
                        String errorMessage = task.getException() != null ? task.getException().getMessage() : "Lỗi không xác định";
                        Log.e(TAG, "Registration failed: " + errorMessage);
                        Toast.makeText(this, "Đăng ký thất bại: " + errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
    }
}