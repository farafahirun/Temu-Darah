package com.example.temudarah.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.temudarah.activity.MainActivity;
import com.example.temudarah.databinding.FragmentPengaturanNotifikasiBinding;
import com.example.temudarah.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class PengaturanNotifikasiFragment extends Fragment {

    private FragmentPengaturanNotifikasiBinding binding;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    // Penanda untuk mencegah listener berjalan saat kita mengatur status awal
    private boolean isInitialising = true;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentPengaturanNotifikasiBinding.inflate(inflater, container, false);
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).hideBottomNav();
        }
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        binding.btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());

        if (currentUser != null) {
            loadSettings();
        }
    }

    private void loadSettings() {
        db.collection("users").document(currentUser.getUid()).get().addOnSuccessListener(documentSnapshot -> {
            if (isAdded() && documentSnapshot.exists()) {
                User user = documentSnapshot.toObject(User.class);
                if (user != null) {
                    // Set kondisi awal semua switch
                    binding.switchPesanBaru.setChecked(user.isNotifPesanBaru());
                    binding.switchTawaranBantuan.setChecked(user.isNotifTawaranBantuan());
                    binding.switchPermintaanSelesai.setChecked(user.isNotifPermintaanSelesai());

                    // Cek kondisi switch utama berdasarkan kondisi switch anak
                    checkAllNotificationsSwitchState();

                    // Setelah semua di-set, matikan penanda dan pasang listener
                    isInitialising = false;
                    setupSwitchListeners();
                }
            }
        });
    }

    /**
     * Fungsi untuk memasang semua listener.
     */
    private void setupSwitchListeners() {
        // Listener untuk switch utama
        binding.switchAllNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isInitialising) return; // Jangan jalankan jika masih proses inisialisasi

            // Matikan listener anak sementara agar tidak memicu update berulang
            removeIndividualListeners();

            binding.switchPesanBaru.setChecked(isChecked);
            binding.switchTawaranBantuan.setChecked(isChecked);
            binding.switchPermintaanSelesai.setChecked(isChecked);

            // Simpan semua perubahan ke Firestore
            updateAllSettings(isChecked, isChecked, isChecked);

            // Pasang lagi listener anak
            addIndividualListeners();
        });

        // Pasang listener untuk setiap switch anak
        addIndividualListeners();
    }

    /**
     * Fungsi untuk memasang listener pada switch anak.
     */
    private void addIndividualListeners() {
        binding.switchPesanBaru.setOnCheckedChangeListener(individualListener);
        binding.switchTawaranBantuan.setOnCheckedChangeListener(individualListener);
        binding.switchPermintaanSelesai.setOnCheckedChangeListener(individualListener);
    }

    /**
     * Fungsi untuk melepas listener dari switch anak.
     */
    private void removeIndividualListeners() {
        binding.switchPesanBaru.setOnCheckedChangeListener(null);
        binding.switchTawaranBantuan.setOnCheckedChangeListener(null);
        binding.switchPermintaanSelesai.setOnCheckedChangeListener(null);
    }

    /**
     * Satu listener yang digunakan oleh semua switch anak.
     */
    private final CompoundButton.OnCheckedChangeListener individualListener = (buttonView, isChecked) -> {
        if (isInitialising) return; // Jangan jalankan jika masih proses inisialisasi

        // Cek kembali status switch utama
        checkAllNotificationsSwitchState();

        // Simpan perubahan spesifik ini ke Firestore
        int id = buttonView.getId();
        if (id == binding.switchPesanBaru.getId()) {
            updateSetting("notifPesanBaru", isChecked);
        } else if (id == binding.switchTawaranBantuan.getId()) {
            updateSetting("notifTawaranBantuan", isChecked);
        } else if (id == binding.switchPermintaanSelesai.getId()) {
            updateSetting("notifPermintaanSelesai", isChecked);
        }
    };

    /**
     * Fungsi untuk memeriksa apakah semua switch anak aktif, lalu mengatur switch utama.
     */
    private void checkAllNotificationsSwitchState() {
        boolean allEnabled = binding.switchPesanBaru.isChecked() &&
                binding.switchTawaranBantuan.isChecked() &&
                binding.switchPermintaanSelesai.isChecked();

        // Hapus listener sementara agar tidak memicu loop tak terbatas
        binding.switchAllNotifications.setOnCheckedChangeListener(null);
        binding.switchAllNotifications.setChecked(allEnabled);
        // Pasang kembali listenernya
        binding.switchAllNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isInitialising) return;
            removeIndividualListeners();
            binding.switchPesanBaru.setChecked(isChecked);
            binding.switchTawaranBantuan.setChecked(isChecked);
            binding.switchPermintaanSelesai.setChecked(isChecked);
            updateAllSettings(isChecked, isChecked, isChecked);
            addIndividualListeners();
        });
    }

    /**
     * Fungsi untuk menyimpan SATU pengaturan ke Firestore.
     */
    private void updateSetting(String field, boolean value) {
        if (currentUser == null) return;
        db.collection("users").document(currentUser.getUid())
                .update(field, value)
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Gagal menyimpan pengaturan.", Toast.LENGTH_SHORT).show());
    }

    /**
     * Fungsi untuk menyimpan SEMUA pengaturan sekaligus.
     */
    private void updateAllSettings(boolean pesan, boolean tawaran, boolean selesai) {
        if (currentUser == null) return;
        Map<String, Object> settings = new HashMap<>();
        settings.put("notifPesanBaru", pesan);
        settings.put("notifTawaranBantuan", tawaran);
        settings.put("notifPermintaanSelesai", selesai);

        db.collection("users").document(currentUser.getUid())
                .update(settings)
                .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "Pengaturan disimpan.", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Gagal menyimpan pengaturan.", Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}