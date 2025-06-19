package com.example.temudarah.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.temudarah.databinding.ActivityLoadingPageBinding;

public class LoadingPageActivity extends AppCompatActivity {
    private ActivityLoadingPageBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityLoadingPageBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);
        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        setupActionListeners();
    }

    private void setupActionListeners() {
        binding.btnDaftar.setOnClickListener(v -> {
            Intent intent = new Intent(LoadingPageActivity.this, DaftarActivity.class);
            startActivity(intent);
        });

        binding.tvMasuk.setOnClickListener(v -> {
            Intent intent = new Intent(LoadingPageActivity.this, MasukActivity.class);
            startActivity(intent);
        });
    }
}