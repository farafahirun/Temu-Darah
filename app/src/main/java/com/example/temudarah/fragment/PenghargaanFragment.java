package com.example.temudarah.fragment;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.temudarah.R;
import com.example.temudarah.activity.MainActivity;
import com.example.temudarah.databinding.FragmentPenghargaanBinding;
import com.example.temudarah.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Locale;

public class PenghargaanFragment extends Fragment {

    private static final String TAG = "PenghargaanFragment";
    private FragmentPenghargaanBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentPenghargaanBinding.inflate(inflater, container, false);
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).hideBottomNav();
        }
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        binding.btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());

        if (currentUser != null) {
            loadUserAchievements();
        } else {
            Toast.makeText(getContext(), "Harap login untuk melihat penghargaan.", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadUserAchievements() {
        // Tampilkan loading, sembunyikan konten utama
        binding.listPendonorCard.setVisibility(View.INVISIBLE);
        binding.progressBar.setVisibility(View.VISIBLE);

        db.collection("users").document(currentUser.getUid()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!isAdded() || getContext() == null) return; // Pastikan fragment masih aktif

                    binding.progressBar.setVisibility(View.GONE);
                    binding.listPendonorCard.setVisibility(View.VISIBLE);

                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null) {
                            populateUi(user);
                        }
                    } else {
                        Toast.makeText(getContext(), "Gagal memuat data profil.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    binding.progressBar.setVisibility(View.GONE);
                    Log.e(TAG, "Gagal memuat data penghargaan", e);
                });
    }

    /**
     * Fungsi utama untuk mengisi semua data ke UI berdasarkan jumlah donasi.
     */
    private void populateUi(User user) {
        int donationCount = user.getDonationCount();

        // Atur teks header
        binding.haloUsername.setText("Halo, " + user.getFullName() + "!");
        binding.totalDonasi.setText(String.valueOf(donationCount));

        // Definisikan syarat untuk setiap lencana
        final int GOLD_GOAL = 20;
        final int SILVER_GOAL = 10;
        final int BRONZE_GOAL = 5;

        // Atur progress bar
        binding.progressGold.setProgress(donationCount);
        binding.progressSilver.setProgress(donationCount);
        binding.progressBronze.setProgress(donationCount);

        // Cek dan atur setiap lencana
        boolean bronzeUnlocked = donationCount >= BRONZE_GOAL;
        boolean silverUnlocked = donationCount >= SILVER_GOAL;
        boolean goldUnlocked = donationCount >= GOLD_GOAL;

        updateTierUi(binding.sertifikatBronze, bronzeUnlocked);
        updateTierUi(binding.sertifikatSilver, silverUnlocked);
        updateTierUi(binding.sertifikatEmas, goldUnlocked);

        // Update header utama berdasarkan lencana tertinggi yang diraih
        if (goldUnlocked) {
            binding.infoLencana.setText("Kamu Mendapatkan lencana Emas!");
            binding.lencanaSaatIni.setImageResource(R.drawable.lencana_gold);
        } else if (silverUnlocked) {
            binding.infoLencana.setText("Kamu Mendapatkan lencana Perak!");
            binding.lencanaSaatIni.setImageResource(R.drawable.lencana_silver);
        } else if (bronzeUnlocked) {
            binding.infoLencana.setText("Kamu Mendapatkan lencana Perunggu!");
            binding.lencanaSaatIni.setImageResource(R.drawable.lencana_bronze);
        } else {
            binding.infoLencana.setText("Ayo donorkan darahmu dan dapatkan lencana!");
            // Set gambar default jika belum ada lencana
            binding.lencanaSaatIni.setImageResource(R.drawable.logo_merah);
        }
    }

    /**
     * Fungsi helper untuk mengaktifkan atau menon-aktifkan tombol sertifikat.
     */
    private void updateTierUi(Button button, boolean isUnlocked) {
        button.setEnabled(isUnlocked);

        if (getContext() == null) return;

        if (isUnlocked) {
            // Jika terbuka, gunakan warna utama
            button.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(getContext(), R.color.utama)));
            button.setOnClickListener(v -> {
                // TODO: Logika untuk menampilkan atau men-download sertifikat
                Toast.makeText(getContext(), "Menampilkan sertifikat...", Toast.LENGTH_SHORT).show();
            });
        } else {
            // Jika terkunci, gunakan warna abu-abu
            button.setBackgroundTintList(ColorStateList.valueOf(Color.LTGRAY));
            button.setOnClickListener(null); // Hapus listener jika terkunci
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}