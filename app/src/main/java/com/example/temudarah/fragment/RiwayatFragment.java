package com.example.temudarah.fragment;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class RiwayatFragment extends Fragment {

    private static final String TAG = "RiwayatFragment";
    private FragmentRiwayatBinding binding;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    private RiwayatDonasiAdapter donasiAdapter;
    private List<RiwayatDonasiTampil> masterRiwayatList; // List utama untuk semua data mentah
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
        binding.cardPenghargaan.setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new PenghargaanFragment())
                    .addToBackStack(null)
                    .commit();
        });
        setupListeners();
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
        donasiAdapter = new RiwayatDonasiAdapter(new ArrayList<>(), proses -> {
            if (proses.getProsesId() != null && !proses.getProsesId().isEmpty()) {
                // Buat instance fragment detail dengan mengirim ID prosesnya
                Fragment detailRiwayatFragment = DetailRiwayatFragment.newInstance(proses.getProsesId());
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, detailRiwayatFragment)
                        .addToBackStack(null)
                        .commit();
            } else {
                Toast.makeText(getContext(), "Tidak bisa membuka detail, ID tidak ditemukan.", Toast.LENGTH_SHORT).show();
            }
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
        updateButtonStyles(filter);
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

        // Tahap 1: Ambil semua riwayat interaksi dari 'active_donations'
        db.collection("active_donations")
                .whereArrayContains("participants", currentUser.getUid())
                .get()
                .addOnCompleteListener(task -> {
                    if (!isAdded()) return; // Pastikan fragment masih aktif
                    if (!task.isSuccessful()) {
                        updateUiState(UiState.EMPTY, "Gagal memuat riwayat.");
                        return;
                    }

                    List<RiwayatDonasiTampil> interactionHistory = new ArrayList<>();
                    Set<String> processedRequestIds = new HashSet<>(); // Untuk mencegah duplikasi
                    QuerySnapshot prosesSnapshots = task.getResult();

                    if (prosesSnapshots.isEmpty()) {
                        // Jika tidak ada interaksi, langsung lanjut ke tahap 2
                        loadSelfCanceledRequests(interactionHistory, processedRequestIds);
                        return;
                    }

                    AtomicInteger counter = new AtomicInteger(0);
                    int totalItems = prosesSnapshots.size();

                    for (DocumentSnapshot doc : prosesSnapshots) {
                        ProsesDonor proses = doc.toObject(ProsesDonor.class);
                        enrichInteractionData(proses, doc.getId(), (item) -> {
                            if (item != null) {
                                interactionHistory.add(item);
                                processedRequestIds.add(proses.getRequestId());
                            }
                            // Cek apakah ini item terakhir yang diproses
                            if (counter.incrementAndGet() == totalItems) {
                                // Lanjut ke tahap 2 dengan membawa hasil tahap 1
                                loadSelfCanceledRequests(interactionHistory, processedRequestIds);
                            }
                        });
                    }
                });
    }

    private void setupListeners() {
        binding.ivNotification.setOnClickListener(v -> {
            navigateTo(new NotifikasiFragment());
        });
    }
    private void navigateTo(Fragment fragment) {
        if (getParentFragmentManager() != null) {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(null)
                    .commit();
        }
    }

    private void enrichInteractionData(ProsesDonor proses, String prosesId, final OnEnrichmentComplete callback) {
        if (proses == null || proses.getRequestId() == null) {
            callback.onComplete(null);
            return;
        }

        String otherUserId = Objects.equals(proses.getRequesterId(), currentUser.getUid()) ? proses.getDonorId() : proses.getRequesterId();
        db.collection("users").document(otherUserId).get().addOnSuccessListener(userDoc -> {
            db.collection("donation_requests").document(proses.getRequestId()).get().addOnSuccessListener(requestDoc -> {
                if (userDoc.exists() && requestDoc.exists()) {
                    User otherUser = userDoc.toObject(User.class);
                    PermintaanDonor permintaan = requestDoc.toObject(PermintaanDonor.class);
                    if (otherUser != null && permintaan != null) {
                        RiwayatDonasiTampil item = new RiwayatDonasiTampil();
                        item.setProsesId(prosesId);
                        item.setTanggal(proses.getTimestamp());
                        item.setStatusProses(proses.getStatusProses());
                        item.setNamaPasien(permintaan.getNamaPasien());
                        item.setGolonganDarah(permintaan.getGolonganDarahDibutuhkan());
                        boolean sayaPendonor = currentUser.getUid().equals(proses.getDonorId());
                        item.setPeranSaya(sayaPendonor ? "Sebagai Pendonor" : "Sebagai Penerima");
                        item.setJudulTampilan(sayaPendonor ? "Permintaan dari " + otherUser.getFullName() : "Bantuan dari " + otherUser.getFullName());
                        callback.onComplete(item);
                    } else {
                        callback.onComplete(null);
                    }
                } else {
                    callback.onComplete(null);
                }
            }).addOnFailureListener(e -> callback.onComplete(null));
        }).addOnFailureListener(e -> callback.onComplete(null));
    }

    private void loadSelfCanceledRequests(List<RiwayatDonasiTampil> currentHistory, Set<String> processedRequestIds) {
        db.collection("donation_requests")
                .whereEqualTo("pembuatUid", currentUser.getUid())
                .whereEqualTo("status", "Dibatalkan")
                .get()
                .addOnSuccessListener(canceledSnapshots -> {
                    if (!isAdded()) return;
                    for (DocumentSnapshot doc : canceledSnapshots) {
                        if (!processedRequestIds.contains(doc.getId())) {
                            PermintaanDonor permintaan = doc.toObject(PermintaanDonor.class);
                            if (permintaan != null) {
                                RiwayatDonasiTampil item = new RiwayatDonasiTampil();
                                item.setProsesId(doc.getId());
                                item.setTanggal(permintaan.getWaktuDibuat());
                                item.setStatusProses(permintaan.getStatus());
                                item.setNamaPasien(permintaan.getNamaPasien());
                                item.setGolonganDarah(permintaan.getGolonganDarahDibutuhkan());
                                item.setPeranSaya("Sebagai Penerima");
                                item.setJudulTampilan("Permintaan Anda (dibatalkan)");
                                currentHistory.add(item);
                            }
                        }
                    }
                    masterRiwayatList.clear();
                    masterRiwayatList.addAll(currentHistory);
                    filterAndDisplayList();
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    // Jika query kedua gagal, setidaknya tampilkan hasil dari query pertama
                    masterRiwayatList.clear();
                    masterRiwayatList.addAll(currentHistory);
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
            if (o1.getTanggal() == null || o2.getTanggal() == null) return 0;
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

    // Interface helper untuk callback
    interface OnEnrichmentComplete {
        void onComplete(RiwayatDonasiTampil item);
    }
}