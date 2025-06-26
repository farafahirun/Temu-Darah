package com.example.temudarah.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.temudarah.databinding.ActivityMasukBinding;
import com.example.temudarah.util.AlertUtil;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class MasukActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private ActivityMasukBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityMasukBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        mAuth = FirebaseAuth.getInstance();
        setupListeners();
    }

    private void setupListeners() {
        binding.ivBack.setOnClickListener(v -> finish());
        binding.tvDaftar.setOnClickListener(v -> {
            startActivity(new Intent(MasukActivity.this, DaftarActivity.class));
        });

        binding.btnMasuk.setText("Masuk");
        binding.btnMasuk.setOnClickListener(v -> {
            if (validateInput()) {
                loginUser();
            }
        });
    }

    private boolean validateInput() {
        String email = binding.etPhoneEmail.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            binding.etPhoneEmail.setError("Email tidak boleh kosong");
            binding.etPhoneEmail.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(password)) {
            binding.etPassword.setError("Password tidak boleh kosong");
            binding.etPassword.requestFocus();
            return false;
        }
        return true;
    }

    private void loginUser() {
        String email = binding.etPhoneEmail.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();

        binding.btnMasuk.setEnabled(false);
        binding.btnMasuk.setText("Memproses...");

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    binding.btnMasuk.setEnabled(true);
                    binding.btnMasuk.setText("Masuk");

                    if (task.isSuccessful()) {
                        getAndStoreFcmToken();

                        Intent intent = new Intent(MasukActivity.this, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    } else {
                        AlertUtil.showAlert(MasukActivity.this,
                                "Login Gagal",
                                "Login gagal, Input Email dan Password dengan Benar",
                                "OK", null, null, null);
                    }
                });
    }

    private void getAndStoreFcmToken() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        com.google.firebase.messaging.FirebaseMessaging.getInstance().getToken()
                .addOnSuccessListener(token -> {
                    if (token != null) {
                        FirebaseFirestore.getInstance().collection("users").document(user.getUid())
                                .update("fcmToken", token)
                                .addOnSuccessListener(aVoid -> Log.d("MasukActivity", "FCM Token diupdate saat login."));
                    }
                });
    }
}