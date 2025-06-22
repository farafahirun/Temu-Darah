package com.example.temudarah.fragment;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.temudarah.R;
import com.example.temudarah.adapter.RiwayatDonasiAdapter;
import com.example.temudarah.databinding.FragmentRiwayatBinding;
import com.example.temudarah.model.PermintaanDonor;
import com.example.temudarah.model.ProsesDonor;
import com.example.temudarah.model.RiwayatDonasiTampil;
import com.example.temudarah.model.User;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class RiwayatFragment extends Fragment {

    private static final String TAG = "RiwayatFragment";
    private FragmentRiwayatBinding binding;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    private RiwayatDonasiAdapter donasiAdapter;
    private List<RiwayatDonasiTampil> masterRiwayatList; // List utama untuk semua data mentah dari Firestore
    private String currentFilter = "Semua";

    private enum UiState { LOADING, HAS_DATA, EMPTY }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentRiwayatBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        setupRecyclerView();
        setupFilterListeners();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (currentUser != null) {
            updateButtonStyles(currentFilter);
            loadAllMyHistory();
        } else {
            updateUiState(UiState.EMPTY, "Anda perlu login untuk melihat riwayat.");
        }
    }

    private void setupRecyclerView() {
        binding.rvRiwayat.setLayoutManager(new LinearLayoutManager(getContext()));
        masterRiwayatList = new ArrayList<>();
        // Inisialisasi adapter dengan list kosong, akan diisi oleh filterAndDisplayList
        donasiAdapter = new RiwayatDonasiAdapter(new ArrayList<>(), riwayat -> {
            Toast.makeText(getContext(), "Membuka detail riwayat...", Toast.LENGTH_SHORT).show();
            // TODO: Buka halaman DetailRiwayatFragment jika diperlukan
        });
        binding.rvRiwayat.setAdapter(donasiAdapter);
    }

    private void setupFilterListeners() {
        binding.semua.setOnClickListener(v -> applyFilter("Semua"));
        binding.filterPendonor.setOnClickListener(v -> applyFilter("Sebagai Pendonor"));
        binding.filterPenerima.setOnClickListener(v -> applyFilter("Sebagai Penerima"));
    }

    private void applyFilter(String filter) {
        currentFilter = filter;
        updateButtonStyles(currentFilter);
        // Memfilter dari data yang sudah ada di memori, tidak perlu query ulang ke internet
        filterAndDisplayList();
    }

    private void updateButtonStyles(String activeFilter) {
        if (getContext() == null) return;
        int activeBgColor = ContextCompat.getColor(requireContext(), R.color.utama);
        int inactiveBgColor = ContextCompat.getColor(requireContext(), R.color.background);
        int activeTextColor = Color.WHITE;
        int inactiveTextColor = ContextCompat.getColor(requireContext(), R.color.utama);

        binding.semua.setBackgroundTintList(ColorStateList.valueOf(inactiveBgColor));
        binding.semua.setTextColor(inactiveTextColor);
        binding.filterPendonor.setBackgroundTintList(ColorStateList.valueOf(inactiveBgColor));
        binding.filterPendonor.setTextColor(inactiveTextColor);
        binding.filterPenerima.setBackgroundTintList(ColorStateList.valueOf(inactiveBgColor));
        binding.filterPenerima.setTextColor(inactiveTextColor);

        if ("Semua".equals(activeFilter)) {
            binding.semua.setBackgroundTintList(ColorStateList.valueOf(activeBgColor));
            binding.semua.setTextColor(activeTextColor);
        } else if ("Sebagai Pendonor".equals(activeFilter)) {
            binding.filterPendonor.setBackgroundTintList(ColorStateList.valueOf(activeBgColor));
            binding.filterPendonor.setTextColor(activeTextColor);
        } else if ("Sebagai Penerima".equals(activeFilter)) {
            binding.filterPenerima.setBackgroundTintList(ColorStateList.valueOf(activeBgColor));
            binding.filterPenerima.setTextColor(activeTextColor);
        }
    }

    private void loadAllMyHistory() {
        if (currentUser == null) return;
        updateUiState(UiState.LOADING, null);

        // Tugas 1: Ambil semua proses donasi yang melibatkan Anda
        Task<QuerySnapshot> activeDonationsTask = db.collection("active_donations")
                .whereArrayContains("participants", currentUser.getUid()).get();

        // Tugas 2: Ambil semua permintaan yang Anda buat & BATALKAN sendiri
        Task<QuerySnapshot> selfCanceledRequestsTask = db.collection("donation_requests")
                .whereEqualTo("pembuatUid", currentUser.getUid())
                .whereEqualTo("status", "Dibatalkan").get();

        // Jalankan kedua tugas utama secara bersamaan
        Tasks.whenAllComplete(activeDonationsTask, selfCanceledRequestsTask)
                .addOnCompleteListener(task -> {
                    if (!isAdded() || !task.isSuccessful()) {
                        if (isAdded()) updateUiState(UiState.EMPTY, "Gagal memuat riwayat.");
                        Log.e(TAG, "Gagal memuat riwayat awal", task.getException());
                        return;
                    }

                    masterRiwayatList.clear();
                    Set<String> processedRequestIds = new HashSet<>();

                    // --- Tahap 1: Proses hasil dari `active_donations` (interaksi) ---
                    Task<QuerySnapshot> activeDonationsResultTask = (Task<QuerySnapshot>) task.getResult().get(0);
                    if (activeDonationsResultTask.isSuccessful() && activeDonationsResultTask.getResult() != null) {
                        // Logika pengayaan data yang kompleks kita letakkan di sini nanti
                        // Untuk sekarang kita hanya akan merakit data yang bisa dirakit
                        // (Ini memerlukan query lanjutan untuk nama, akan kita sederhanakan untuk contoh ini)
                        for(DocumentSnapshot doc : activeDonationsResultTask.getResult()) {
                            ProsesDonor proses = doc.toObject(ProsesDonor.class);
                            if(proses != null && proses.getRequestId() != null){
                                processedRequestIds.add(proses.getRequestId());
                                // Di sini seharusnya ada logika untuk mengambil detail user dan permintaan
                                // Untuk sekarang kita buat placeholder
                                RiwayatDonasiTampil item = new RiwayatDonasiTampil();
                                item.setProsesId(doc.getId());
                                item.setTanggal(proses.getTimestamp());
                                item.setStatusProses(proses.getStatusProses());
                                item.setNamaPasien("Memuat...");
                                boolean sayaPendonor = currentUser.getUid().equals(proses.getDonorId());
                                item.setPeranSaya(sayaPendonor ? "Sebagai Pendonor" : "Sebagai Penerima");
                                item.setJudulTampilan("Interaksi...");
                                masterRiwayatList.add(item);
                            }
                        }
                    }

                    // --- Tahap 2: Proses hasil dari `donation_requests` (permintaan batal) ---
                    Task<QuerySnapshot> finalRequestsResultTask = (Task<QuerySnapshot>) task.getResult().get(1);
                    if (finalRequestsResultTask.isSuccessful() && finalRequestsResultTask.getResult() != null) {
                        for (DocumentSnapshot doc : finalRequestsResultTask.getResult()) {
                            if (!processedRequestIds.contains(doc.getId())) {
                                PermintaanDonor permintaan = doc.toObject(PermintaanDonor.class);
                                if (permintaan != null) {
                                    RiwayatDonasiTampil item = new RiwayatDonasiTampil();
                                    item.setProsesId(doc.getId());
                                    item.setTanggal(permintaan.getWaktuDibuat());
                                    item.setStatusProses(permintaan.getStatus());
                                    item.setNamaPasien(permintaan.getNamaPasien());
                                    item.setPeranSaya("Sebagai Penerima");
                                    item.setJudulTampilan("Permintaan Anda (dibatalkan)");
                                    masterRiwayatList.add(item);
                                }
                            }
                        }
                    }

                    // Setelah semua data mentah terkumpul, terapkan filter awal
                    filterAndDisplayList();
                });
    }

    private void filterAndDisplayList() {
        if (!isAdded()) return;

        List<RiwayatDonasiTampil> filteredList = new ArrayList<>();
        if ("Semua".equals(currentFilter)) {
            filteredList.addAll(masterRiwayatList);
        } else {
            for (RiwayatDonasiTampil item : masterRiwayatList) {
                if (currentFilter.equals(item.getPeranSaya())) {
                    filteredList.add(item);
                }
            }
        }

        Collections.sort(filteredList, (o1, o2) -> {
            if (o1.getTanggal() == null) return 1;
            if (o2.getTanggal() == null) return -1;
            return o2.getTanggal().compareTo(o1.getTanggal());
        });

        donasiAdapter.updateData(filteredList);
        updateUiState(filteredList.isEmpty() ? UiState.EMPTY : UiState.HAS_DATA, "Tidak ada riwayat untuk filter ini.");
    }

    private void updateUiState(UiState state, String emptyMessage) {
        if (binding == null) return;
        binding.progressBar.setVisibility(state == UiState.LOADING ? View.VISIBLE : View.GONE);
        binding.rvRiwayat.setVisibility(state == UiState.HAS_DATA ? View.VISIBLE : View.GONE);
        binding.stateContainer.setVisibility(state == UiState.EMPTY || state == UiState.LOADING ? View.VISIBLE : View.GONE);
        if (state == UiState.EMPTY) {
            binding.tvEmptyState.setVisibility(View.VISIBLE);
            binding.tvEmptyState.setText(emptyMessage);
        } else {
            binding.tvEmptyState.setVisibility(View.GONE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}