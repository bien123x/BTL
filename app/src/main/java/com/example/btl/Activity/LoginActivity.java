package com.example.btl.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseUser;
import com.example.btl.Domain.Repository.AuthRepository;
import com.example.btl.Domain.Repository.UserRepository;
import com.example.btl.Helper.NetworkHelper;
import com.example.btl.databinding.ActivityLoginBinding;

public class LoginActivity extends AppCompatActivity {
    private ActivityLoginBinding binding;
    private AuthRepository authRepository;
    private UserRepository userRepository;
    private static final String TAG = "LoginActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Khởi tạo repository
        authRepository = new AuthRepository();
        userRepository = new UserRepository();

        // Kiểm tra nếu người dùng đã đăng nhập
        FirebaseUser currentUser = authRepository.getCurrentUser();
//        if (currentUser != null) {
//            Log.d(TAG, "User already logged in: " + currentUser.getUid());
//            startActivity(new Intent(LoginActivity.this, MainActivity.class));
//            finish();
//            return;
//        }

        // Sự kiện nhấn nút đăng nhập
        binding.loginButton.setOnClickListener(v -> {
            String email = binding.emailLogin.getText().toString().trim();
            String password = binding.passwordLogin.getText().toString().trim();

            // Kiểm tra dữ liệu đầu vào
            if (!validateInput(email, password)) {
                return;
            }

            // Kiểm tra kết nối mạng
            if (!NetworkHelper.isNetworkAvailable(this)) {
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

    // Hiển thị/Ẩn loading indicator
    private void showLoading(boolean isLoading) {
        binding.loginButton.setEnabled(!isLoading);
        binding.loginButton.setText(isLoading ? "Đang xử lý..." : "Đăng nhập");
//        binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }

    // Đăng nhập người dùng
    private void login(String email, String password) {
        authRepository.login(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = task.getResult();
                        // Cập nhật trạng thái online
                        userRepository.updateOnlineStatus(user.getUid(), true);

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
}