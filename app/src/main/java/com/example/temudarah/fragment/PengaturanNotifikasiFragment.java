package com.example.temudarah.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;
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

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentPengaturanNotifikasiBinding.inflate(inflater, container, false);
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
                    // Set kondisi switch berdasarkan data dari Firestore
                    binding.switchPesanBaru.setChecked(user.isNotifPesanBaru());
                    binding.switchTawaranBantuan.setChecked(user.isNotifTawaranBantuan());
                    binding.switchPermintaanSelesai.setChecked(user.isNotifPermintaanSelesai());

                    // Setelah kondisi awal di-set, baru pasang listener
                    setupSwitchListeners();
                }
            }
        });
    }

    private void setupSwitchListeners() {
        // Ganti ID switch ini sesuai dengan ID di layout Anda
        binding.switchPesanBaru.setOnCheckedChangeListener((buttonView, isChecked) -> updateSetting("notifPesanBaru", isChecked));
        binding.switchTawaranBantuan.setOnCheckedChangeListener((buttonView, isChecked) -> updateSetting("notifTawaranBantuan", isChecked));
        binding.switchPermintaanSelesai.setOnCheckedChangeListener((buttonView, isChecked) -> updateSetting("notifPermintaanSelesai", isChecked));
    }

    private void updateSetting(String field, boolean value) {
        if (currentUser == null) return;
        db.collection("users").document(currentUser.getUid())
                .update(field, value)
                .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "Pengaturan disimpan.", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Gagal menyimpan pengaturan.", Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}