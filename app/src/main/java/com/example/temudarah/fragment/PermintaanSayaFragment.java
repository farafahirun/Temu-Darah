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
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

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
        binding.btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());
    }

    @Override
    public void onResume() {
        super.onResume();
        // Muat ulang data setiap kali fragment ini ditampilkan
        if (currentUser != null) {
            loadMyRequests();
        } else {
            binding.tvEmptyState.setText("Harap login untuk melihat riwayat.");
            binding.tvEmptyState.setVisibility(View.VISIBLE);
        }
    }

    private void setupRecyclerView() {
        permintaanSayaList = new ArrayList<>();
        adapter = new PermintaanSayaAdapter(permintaanSayaList, new PermintaanSayaAdapter.OnItemActionClickListener() {
            @Override
            public void onItemClick(PermintaanDonor permintaan) {
                Fragment detailFragment = DetailPermintaanSayaFragment.newInstance(permintaan.getRequestId());
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, detailFragment)
                        .addToBackStack(null).commit();
            }
            @Override
            public void onStatusChangeClick(PermintaanDonor permintaan) {
                showStatusChangeDialog(permintaan);
            }
            @Override
            public void onEditClick(PermintaanDonor permintaan) {
                Fragment editFragment = EditPermintaanFragment.newInstance(permintaan.getRequestId());
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, editFragment)
                        .addToBackStack(null).commit();
            }
        });
        binding.rvPermintaanSaya.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvPermintaanSaya.setAdapter(adapter);
    }

    private void loadMyRequests() {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.tvEmptyState.setVisibility(View.GONE);

        // Query HANYA untuk permintaan yang statusnya masih relevan (Aktif / Dalam Proses)
        db.collection("donation_requests")
                .whereEqualTo("pembuatUid", currentUser.getUid())
                .whereIn("status", Arrays.asList("Aktif", "Dalam Proses"))
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
                    binding.tvEmptyState.setVisibility(permintaanSayaList.isEmpty() ? View.VISIBLE : View.GONE);
                }).addOnFailureListener(e -> {
                    binding.progressBar.setVisibility(View.GONE);
                    Log.w(TAG, "Error loading requests", e);
                });
    }

    private void showStatusChangeDialog(PermintaanDonor permintaan) {
        String currentStatus = permintaan.getStatus();

        // Opsi dialog akan berbeda tergantung status saat ini
        if ("Aktif".equals(currentStatus)) {
            // Jika belum ada yang bantu, hanya bisa dibatalkan
            new AlertDialog.Builder(requireContext())
                    .setTitle("Batalkan Permintaan?")
                    .setMessage("Permintaan ini akan dihapus dari daftar publik dan masuk ke riwayat sebagai 'Dibatalkan'.")
                    .setPositiveButton("Ya, Batalkan", (dialog, which) -> updateRequestStatus(permintaan, "Dibatalkan"))
                    .setNegativeButton("Kembali", null)
                    .show();
        } else if ("Dalam Proses".equals(currentStatus)) {
            // Jika sudah ada yang bantu, ada dua pilihan
            final CharSequence[] options = {"Tandai Selesai", "Batalkan Bantuan dari Pendonor Ini"};
            new AlertDialog.Builder(requireContext())
                    .setTitle("Pilih Aksi")
                    .setItems(options, (dialog, which) -> {
                        if (which == 0) { // Opsi "Tandai Selesai"
                            updateRequestStatus(permintaan, "Selesai");
                        } else if (which == 1) { // Opsi "Batalkan Bantuan"
                            cancelDonationProcess(permintaan);
                        }
                    })
                    .show();
        }
    }

    private void cancelDonationProcess(PermintaanDonor permintaan) {
        binding.progressBar.setVisibility(View.VISIBLE);

        // Cari dokumen "jabat tangan" yang terkait dengan permintaan ini
        db.collection("active_donations")
                .whereEqualTo("requestId", permintaan.getRequestId())
                .limit(1).get()
                .addOnSuccessListener(snapshots -> {
                    if (snapshots.isEmpty()) {
                        // Jika tidak ada, kemungkinan ada error data. Kembalikan ke Aktif saja.
                        db.collection("donation_requests").document(permintaan.getRequestId()).update("status", "Aktif");
                        return;
                    }

                    DocumentSnapshot prosesDoc = snapshots.getDocuments().get(0);

                    // Gunakan WriteBatch untuk keamanan
                    WriteBatch batch = db.batch();

                    // Operasi 1: Hapus dokumen "jabat tangan"
                    batch.delete(prosesDoc.getReference());

                    // Operasi 2: Kembalikan status permintaan menjadi "Aktif"
                    DocumentReference requestRef = db.collection("donation_requests").document(permintaan.getRequestId());
                    batch.update(requestRef, "status", "Aktif");

                    batch.commit().addOnSuccessListener(aVoid -> {
                        Toast.makeText(getContext(), "Bantuan telah dibatalkan. Permintaan Anda aktif kembali.", Toast.LENGTH_LONG).show();
                        loadMyRequests(); // Muat ulang halaman
                    }).addOnFailureListener(e -> {
                        binding.progressBar.setVisibility(View.GONE);
                        Toast.makeText(getContext(), "Gagal membatalkan.", Toast.LENGTH_SHORT).show();
                    });
                });
    }

    private void updateRequestStatus(PermintaanDonor permintaan, String newStatus) {
        binding.progressBar.setVisibility(View.VISIBLE);
        WriteBatch batch = db.batch();

        // Operasi 1: Update status di `donation_requests`
        DocumentReference requestRef = db.collection("donation_requests").document(permintaan.getRequestId());
        batch.update(requestRef, "status", newStatus);

        // Jika donasi selesai, update juga dokumen prosesnya dan tanggal donor terakhir si pendonor
        if ("Selesai".equals(newStatus)) {
            db.collection("active_donations").whereEqualTo("requestId", permintaan.getRequestId()).limit(1).get()
                    .addOnSuccessListener(snapshots -> {
                        if (!snapshots.isEmpty()) {
                            DocumentSnapshot prosesDoc = snapshots.getDocuments().get(0);
                            ProsesDonor proses = prosesDoc.toObject(ProsesDonor.class);

                            // Operasi 2: Update status di `active_donations`
                            batch.update(prosesDoc.getReference(), "statusProses", "Selesai");

                            if (proses != null) {
                                // Operasi 3: Update `lastDonationDate` di profil PENDONOR
                                DocumentReference donorRef = db.collection("users").document(proses.getDonorId());
                                batch.update(donorRef, "lastDonationDate", new SimpleDateFormat("dd MMMM yyyy", Locale.forLanguageTag("id-ID")).format(new Date()));
                            }
                        }
                        // Jalankan semua operasi
                        commitBatch(batch);
                    });
        } else {
            // Jika hanya dibatalkan, langsung jalankan operasinya
            commitBatch(batch);
        }
    }

    private void commitBatch(WriteBatch batch) {
        batch.commit().addOnSuccessListener(aVoid -> {
            Toast.makeText(getContext(), "Status berhasil diubah.", Toast.LENGTH_SHORT).show();
            loadMyRequests(); // Muat ulang daftar
        }).addOnFailureListener(e -> {
            binding.progressBar.setVisibility(View.GONE);
            Toast.makeText(getContext(), "Gagal mengubah status.", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}