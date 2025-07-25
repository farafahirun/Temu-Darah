package com.example.temudarah.activity;

import android.os.Bundle;
import android.util.Log;
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
import com.example.temudarah.fragment.ChatsFragment;
import com.example.temudarah.fragment.ProfileFragment;
import com.example.temudarah.fragment.RiwayatFragment;
import com.google.android.material.badge.BadgeDrawable;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private ActivityMainBinding binding;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private ListenerRegistration notificationListener;
    private BadgeDrawable badgePesan;

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

        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        badgePesan = binding.bottomNavigation.getOrCreateBadge(R.id.pesan);
        badgePesan.setBackgroundColor(getResources().getColor(R.color.utama, getTheme()));
        badgePesan.setBadgeTextColor(getResources().getColor(android.R.color.white, getTheme()));
        badgePesan.setVisible(false);

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
            } else if (id == R.id.pesan) {
                selectedFragment = new ChatsFragment();
            } else if (id == R.id.riwayat) {
                selectedFragment = new RiwayatFragment();
            } else if (id == R.id.profile) {
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
                    currentFragment instanceof ChatsFragment ||
                    currentFragment instanceof RiwayatFragment ||
                    currentFragment instanceof ProfileFragment) {
                showBottomNav();
            }
        });
        setupNotificationListener();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (currentUser != null) {
            updateNotificationBadge();
        }
    }

    private void setupNotificationListener() {
        if (currentUser == null) return;

        if (notificationListener != null) {
            notificationListener.remove();
        }

        notificationListener = db.collection("users")
                .document(currentUser.getUid())
                .collection("notifikasi")
                .whereEqualTo("sudahDibaca", false)
                .whereEqualTo("tipe", "pesan")
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.w(TAG, "Listen for notifications failed.", e);
                        return;
                    }

                    if (snapshots != null) {
                        int unreadCount = snapshots.size();
                        updateBadgeCount(unreadCount);
                    }
                });
    }

    private void updateNotificationBadge() {
        if (currentUser == null) return;

        db.collection("users")
                .document(currentUser.getUid())
                .collection("notifikasi")
                .whereEqualTo("sudahDibaca", false)
                .whereEqualTo("tipe", "pesan")
                .get()
                .addOnSuccessListener(snapshots -> {
                    int unreadCount = snapshots.size();
                    updateBadgeCount(unreadCount);
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error counting notifications", e));
    }

    private void updateBadgeCount(int count) {
        if (badgePesan != null) {
            if (count > 0) {
                badgePesan.setNumber(count);
                badgePesan.setVisible(true);
            } else {
                badgePesan.setVisible(false);
            }
        }
    }

    public void hideBottomNav() {
        findViewById(R.id.bottom_navigation).setVisibility(View.GONE);
    }

    public void showBottomNav() {
        findViewById(R.id.bottom_navigation).setVisibility(View.VISIBLE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (notificationListener != null) {
            notificationListener.remove();
        }
    }
}