package com.example.btl.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseUser;
import com.example.btl.Domain.Model.User;
import com.example.btl.Domain.Repository.AuthRepository;
import com.example.btl.Domain.Repository.UserRepository;
import com.example.btl.Helper.NetworkHelper;
import com.example.btl.databinding.ActivityRegisterBinding;

public class RegisterActivity extends AppCompatActivity {
    private ActivityRegisterBinding binding;
    private AuthRepository authRepository;
    private UserRepository userRepository;
    private static final String TAG = "RegisterActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Khởi tạo repository
        authRepository = new AuthRepository();
        userRepository = new UserRepository();

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
            if (!NetworkHelper.isNetworkAvailable(this)) {
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

    // Hiển thị/Ẩn loading indicator
    private void showLoading(boolean isLoading) {
        binding.registerButton.setEnabled(!isLoading);
        binding.registerButton.setText(isLoading ? "Đang xử lý..." : "Đăng ký");
        binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }

    // Đăng ký người dùng
    private void register(String name, String email, String password) {
        authRepository.register(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = task.getResult();
                        // Tạo đối tượng User
                        User user = new User(
                                firebaseUser.getUid(),
                                name,
                                email,
                                "",
                                0,
                                true
                        );

                        // Lưu thông tin người dùng vào Firestore
                        userRepository.saveUser(firebaseUser.getUid(), user)
                                .addOnCompleteListener(task1 -> {
                                    showLoading(false);
                                    if (task1.isSuccessful()) {
                                        Log.d(TAG, "User data saved successfully for UID: " + firebaseUser.getUid());
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