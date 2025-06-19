package com.example.temudarah.activity;

import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.example.temudarah.R;
import com.example.temudarah.databinding.ActivityMainBinding;
import com.example.temudarah.fragment.BerandaFragment;
import com.example.temudarah.fragment.KegiatanFragment;
import com.example.temudarah.fragment.ProfileFragment;
import com.example.temudarah.fragment.RiwayatFragment;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new BerandaFragment())
                    .commit();
        }

        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;

            int id = item.getItemId();
            if (id == R.id.beranda) {
                selectedFragment = new BerandaFragment();
            } else if (id == R.id.kegiatan) {
                selectedFragment = new KegiatanFragment();
            } else if (id == R.id.riwayat) {
                selectedFragment = new RiwayatFragment();
            }
            else if (id == R.id.profile) {
                selectedFragment = new ProfileFragment();
            }

            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();
            }

            return true;
        });

        getSupportFragmentManager().addOnBackStackChangedListener(() -> {
            Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
            if (currentFragment instanceof BerandaFragment ||
                    currentFragment instanceof KegiatanFragment ||
                    currentFragment instanceof RiwayatFragment ||
                    currentFragment instanceof ProfileFragment) {
                showBottomNav();
            }
        });
    }

    public void hideBottomNav() {
        findViewById(R.id.bottom_navigation).setVisibility(View.GONE);
    }

    public void showBottomNav() {
        findViewById(R.id.bottom_navigation).setVisibility(View.VISIBLE);
    }
}