package com.example.temudarah.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.temudarah.R;
import com.example.temudarah.adapter.PermintaanSayaAdapter;
import com.example.temudarah.databinding.FragmentPermintaanSayaBinding;
import com.example.temudarah.model.PermintaanDonor;
import com.example.temudarah.model.ProsesDonor;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class PermintaanSayaFragment extends Fragment {

    private static final String TAG = "PermintaanSayaFragment";
    private FragmentPermintaanSayaBinding binding;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private PermintaanSayaAdapter adapter;
    private List<PermintaanDonor> permintaanSayaList;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentPermintaanSayaBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        setupRecyclerView();
        if (currentUser != null) {
            loadMyRequests();
        }
    }

    private void setupRecyclerView() {
        permintaanSayaList = new ArrayList<>();
        adapter = new PermintaanSayaAdapter(permintaanSayaList, new PermintaanSayaAdapter.OnItemActionClickListener() {
            @Override
            public void onStatusChangeClick(PermintaanDonor permintaan) {
                showStatusChangeDialog(permintaan);
            }

            @Override
            public void onEditClick(PermintaanDonor permintaan) {
                Fragment editFragment = EditPermintaanFragment.newInstance(permintaan.getRequestId());
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, editFragment)
                        .addToBackStack(null)
                        .commit();
            }
        });
        binding.rvPermintaanSaya.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvPermintaanSaya.setAdapter(adapter);
    }

    private void loadMyRequests() {
        binding.progressBar.setVisibility(View.VISIBLE);
        db.collection("donation_requests")
                .whereEqualTo("pembuatUid", currentUser.getUid())
                .orderBy("waktuDibuat", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    binding.progressBar.setVisibility(View.GONE);
                    permintaanSayaList.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        PermintaanDonor permintaan = doc.toObject(PermintaanDonor.class);
                        permintaan.setRequestId(doc.getId());
                        permintaanSayaList.add(permintaan);
                    }
                    adapter.notifyDataSetChanged();
                    updateEmptyState();
                });
    }

    private void showStatusChangeDialog(PermintaanDonor permintaan) {
        String currentStatus = permintaan.getStatus();
        final String newStatus;
        String dialogMessage;
        String positiveButtonText;

        if ("Aktif".equals(currentStatus)) {
            newStatus = "Dibatalkan";
            dialogMessage = "Anda yakin ingin membatalkan permintaan ini? Permintaan tidak akan bisa diaktifkan kembali.";
            positiveButtonText = "Ya, Batalkan";
        } else if ("Dalam Proses".equals(currentStatus)) {
            newStatus = "Selesai";
            dialogMessage = "Dengan menandai selesai, Anda mengonfirmasi bahwa bantuan telah diterima. Aksi ini tidak bisa diubah.";
            positiveButtonText = "Ya, Tandai Selesai";
        } else {
            return; // Tidak ada aksi jika sudah Selesai atau Dibatalkan
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Konfirmasi Aksi")
                .setMessage(dialogMessage)
                .setPositiveButton(positiveButtonText, (dialog, which) -> updateRequestStatus(permintaan, newStatus))
                .setNegativeButton("Kembali", null)
                .show();
    }

    private void updateRequestStatus(PermintaanDonor permintaan, String newStatus) {
        binding.progressBar.setVisibility(View.VISIBLE);
        db.collection("donation_requests").document(permintaan.getRequestId())
                .update("status", newStatus)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Status berhasil diubah.", Toast.LENGTH_SHORT).show();
                    // Jika donasi selesai, update tanggal donor terakhir untuk si PENDONOR
                    if ("Selesai".equals(newStatus)) {
                        updateLastDonationDateForDonor(permintaan.getRequestId());
                    } else {
                        loadMyRequests(); // Muat ulang jika hanya dibatalkan
                    }
                })
                .addOnFailureListener(e -> {
                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Gagal mengubah status.", Toast.LENGTH_SHORT).show();
                });
    }

    private void updateLastDonationDateForDonor(String requestId) {
        // Cari dokumen 'active_donations' untuk menemukan siapa pendonornya
        db.collection("active_donations")
                .whereEqualTo("requestId", requestId)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        // Ambil dokumen pertama (seharusnya hanya ada satu)
                        DocumentSnapshot doc = queryDocumentSnapshots.getDocuments().get(0);
                        ProsesDonor proses = doc.toObject(ProsesDonor.class);
                        if (proses != null) {
                            String donorId = proses.getRequestId();
                            // Update field 'lastDonationDate' di profil si pendonor
                            db.collection("users").document(donorId)
                                    .update("lastDonationDate", Timestamp.now().toDate().toString()) // Simpan sebagai String
                                    .addOnCompleteListener(task -> {
                                        Log.d(TAG, "Tanggal donor terakhir untuk " + donorId + " telah diupdate.");
                                        loadMyRequests(); // Muat ulang daftar setelah semua selesai
                                    });
                        }
                    } else {
                        loadMyRequests(); // Tetap muat ulang meskipun tidak ditemukan
                    }
                });
    }

    private void updateEmptyState() {
        binding.tvEmptyState.setVisibility(permintaanSayaList.isEmpty() ? View.VISIBLE : View.GONE);
    }
}