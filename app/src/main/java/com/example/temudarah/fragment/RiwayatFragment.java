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
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public class RiwayatFragment extends Fragment {

    private static final String TAG = "RiwayatFragment";
    private FragmentRiwayatBinding binding;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    private RiwayatDonasiAdapter donasiAdapter;
    private List<RiwayatDonasiTampil> riwayatDonasiTampilList;
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

        if (currentUser != null) {
            updateButtonStyles(currentFilter);
            loadMyDonationHistory(currentFilter);
        } else {
            updateUiState(UiState.EMPTY, "Anda perlu login untuk melihat riwayat.");
        }
    }

    private void setupRecyclerView() {
        binding.rvRiwayat.setLayoutManager(new LinearLayoutManager(getContext()));
        riwayatDonasiTampilList = new ArrayList<>();
        donasiAdapter = new RiwayatDonasiAdapter(riwayatDonasiTampilList, riwayat -> {
            // TODO: Aksi saat item riwayat diklik (misal: buka detail chat)
            Toast.makeText(getContext(), "Detail untuk: " + riwayat.getNamaPasien(), Toast.LENGTH_SHORT).show();
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
        loadMyDonationHistory(currentFilter);
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

    // Di dalam class RiwayatFragment.java

    private void loadMyDonationHistory(String peranFilter) {
        if (currentUser == null) return;
        updateUiState(UiState.LOADING, null);

        db.collection("active_donations")
                .whereArrayContains("participants", currentUser.getUid())
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(prosesSnapshots -> {
                    if (!isAdded() || getContext() == null) return;
                    if (prosesSnapshots.isEmpty()) {
                        updateUiState(UiState.EMPTY, "Belum ada riwayat donasi.");
                        return;
                    }

                    List<Task<?>> detailTasks = new ArrayList<>();
                    List<ProsesDonor> processes = new ArrayList<>();
                    for (DocumentSnapshot prosesDoc : prosesSnapshots.getDocuments()) {
                        ProsesDonor proses = prosesDoc.toObject(ProsesDonor.class);
                        if (proses != null && proses.getParticipants() != null && proses.getRequestId() != null) {
                            processes.add(proses);
                            String otherUserId = Objects.equals(proses.getRequesterId(), currentUser.getUid()) ? proses.getDonorId() : proses.getRequesterId();
                            if(otherUserId != null && !otherUserId.isEmpty()) {
                                detailTasks.add(db.collection("users").document(otherUserId).get());
                                detailTasks.add(db.collection("donation_requests").document(proses.getRequestId()).get());
                            }
                        }
                    }

                    if (detailTasks.isEmpty()) {
                        updateUiState(UiState.EMPTY, "Data riwayat tidak lengkap.");
                        return;
                    }

                    Tasks.whenAllComplete(detailTasks).addOnCompleteListener(task -> {
                        if (!isAdded() || getContext() == null) return;
                        List<RiwayatDonasiTampil> tempList = new ArrayList<>();
                        List<Task<?>> completedTasks = task.getResult();

                        for (int i = 0; i < processes.size(); i++) {
                            int userTaskIndex = i * 2;
                            int requestTaskIndex = i * 2 + 1;
                            if (requestTaskIndex >= completedTasks.size()) continue;

                            Task<?> userTask = completedTasks.get(userTaskIndex);
                            Task<?> requestTask = completedTasks.get(requestTaskIndex);

                            if (userTask.isSuccessful() && requestTask.isSuccessful()) {
                                DocumentSnapshot userDoc = (DocumentSnapshot) userTask.getResult();
                                DocumentSnapshot requestDoc = (DocumentSnapshot) requestTask.getResult();

                                if (userDoc != null && userDoc.exists() && requestDoc != null && requestDoc.exists()) {
                                    User otherUser = userDoc.toObject(User.class);
                                    PermintaanDonor permintaan = requestDoc.toObject(PermintaanDonor.class);

                                    if (otherUser != null && permintaan != null) {
                                        ProsesDonor proses = processes.get(i);
                                        boolean sayaPendonor = currentUser.getUid().equals(proses.getDonorId());

                                        if ("Semua".equals(peranFilter) ||
                                                ("Sebagai Pendonor".equals(peranFilter) && sayaPendonor) ||
                                                ("Sebagai Penerima".equals(peranFilter) && !sayaPendonor)) {

                                            // --- BLOK KODE YANG HILANG SEBELUMNYA ---
                                            // 1. Buat instance baru dari RiwayatDonasiTampil
                                            RiwayatDonasiTampil item = new RiwayatDonasiTampil();

                                            // 2. Isi semua datanya menggunakan setter
                                            item.setTanggal(proses.getTimestamp());
                                            item.setStatusProses(proses.getStatusProses());
                                            item.setNamaPasien(permintaan.getNamaPasien());
                                            item.setPeranSaya(sayaPendonor ? "Anda Membantu" : "Anda Dibantu oleh");
                                            item.setJudulTampilan(sayaPendonor ? "Permintaan dari " + otherUser.getFullName() : "Bantuan dari " + otherUser.getFullName());

                                            // 3. Baru tambahkan ke daftar sementara
                                            tempList.add(item);
                                            // --- AKHIR BLOK PERBAIKAN ---
                                        }
                                    }
                                }
                            }
                        }

                        updateFinalList(tempList); // Panggil fungsi helper baru
                    });
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    updateUiState(UiState.EMPTY, "Gagal memuat riwayat.");
                    Log.e(TAG, "Gagal memuat riwayat donasi", e);
                });
    }

    /**
     * Fungsi baru untuk mengurutkan dan mengupdate UI.
     * Dibuat terpisah agar lebih rapi.
     */
    private void updateFinalList(List<RiwayatDonasiTampil> listToShow) {
        // Urutkan berdasarkan tanggal, dari yang terbaru
        Collections.sort(listToShow, (o1, o2) -> {
            if (o1.getTanggal() == null || o2.getTanggal() == null) return 0;
            return o2.getTanggal().compareTo(o1.getTanggal());
        });

        riwayatDonasiTampilList.clear();
        riwayatDonasiTampilList.addAll(listToShow);
        donasiAdapter.notifyDataSetChanged();
        updateUiState(riwayatDonasiTampilList.isEmpty() ? UiState.EMPTY : UiState.HAS_DATA, "Tidak ada riwayat untuk filter ini.");
    }

    private void updateFinalList(List<RiwayatDonasiTampil> tempList, String peranFilter) {
        List<RiwayatDonasiTampil> filteredList = new ArrayList<>();
        if ("Semua".equals(peranFilter)) {
            filteredList.addAll(tempList);
        } else {
            for (RiwayatDonasiTampil item : tempList) {
                boolean sayaPendonor = "Anda Membantu".equals(item.getPeranSaya());
                if (("Sebagai Pendonor".equals(peranFilter) && sayaPendonor) ||
                        ("Sebagai Penerima".equals(peranFilter) && !sayaPendonor)) {
                    filteredList.add(item);
                }
            }
        }

        Collections.sort(filteredList, (o1, o2) -> o2.getTanggal().compareTo(o1.getTanggal()));

        riwayatDonasiTampilList.clear();
        riwayatDonasiTampilList.addAll(filteredList);
        donasiAdapter.notifyDataSetChanged();
        updateUiState(riwayatDonasiTampilList.isEmpty() ? UiState.EMPTY : UiState.HAS_DATA, "Tidak ada riwayat untuk filter ini.");
    }

    private void updateUiState(UiState state, String emptyMessage) {
        if (binding == null) return;
        binding.progressBar.setVisibility(state == UiState.LOADING ? View.VISIBLE : View.GONE);
        binding.rvRiwayat.setVisibility(state == UiState.HAS_DATA ? View.VISIBLE : View.GONE);
        binding.stateContainer.setVisibility(state == UiState.EMPTY || state == UiState.LOADING ? View.VISIBLE : View.GONE);
        binding.tvEmptyState.setVisibility(state == UiState.EMPTY ? View.VISIBLE : View.GONE);
        if (state == UiState.EMPTY && emptyMessage != null) {
            binding.tvEmptyState.setText(emptyMessage);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}