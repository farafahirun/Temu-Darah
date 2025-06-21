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
import com.example.temudarah.model.User; // Import model User
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot; // Import DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.text.SimpleDateFormat; // Import SimpleDateFormat
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays; // Import Arrays (still needed for some checks, but not for initial query)
import java.util.Locale; // Import Locale

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
        } else {
            binding.progressBar.setVisibility(View.GONE);
            binding.tvEmptyState.setText("Anda perlu login untuk melihat permintaan.");
            binding.tvEmptyState.setVisibility(View.VISIBLE);
        }
    }

    // Metode ini akan dipanggil setiap kali Fragment kembali ke tampilan utama
    @Override
    public void onResume() {
        super.onResume();
        if (currentUser != null) {
            loadMyRequests(); // Muat ulang permintaan saat kembali ke fragment
        }
    }


    private void setupRecyclerView() {
        permintaanSayaList = new ArrayList<>();
        // Inisialisasi adapter dengan OnItemActionClickListener
        adapter = new PermintaanSayaAdapter(permintaanSayaList, new PermintaanSayaAdapter.OnItemActionClickListener() {
            @Override
            public void onStatusChangeClick(PermintaanDonor permintaan) {
                showStatusChangeDialog(permintaan);
            }

            @Override
            public void onEditClick(PermintaanDonor permintaan) {
                // Pastikan permintaan memiliki RequestId sebelum membuka fragment edit
                if (permintaan.getRequestId() != null && !permintaan.getRequestId().isEmpty()) {
                    Fragment editFragment = EditPermintaanFragment.newInstance(permintaan.getRequestId());
                    getParentFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container, editFragment)
                            .addToBackStack(null)
                            .commit();
                } else {
                    Toast.makeText(getContext(), "ID permintaan tidak ditemukan.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onContactClick(PermintaanDonor permintaan) {
                // Implementasi logika saat tombol "Hubungi" diklik
                handleContactButtonClick(permintaan);
            }
        });
        binding.rvPermintaanSaya.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvPermintaanSaya.setAdapter(adapter);
    }

    private void loadMyRequests() {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.tvEmptyState.setVisibility(View.GONE);
        binding.rvPermintaanSaya.setVisibility(View.GONE);

        if (currentUser == null) {
            binding.progressBar.setVisibility(View.GONE);
            binding.tvEmptyState.setText("Anda perlu login untuk melihat permintaan.");
            binding.tvEmptyState.setVisibility(View.VISIBLE);
            return;
        }

        // Kueri Firestore TANPA FILTER STATUS di sisi Firestore
        // Ini akan mengambil SEMUA permintaan yang dibuat oleh pengguna saat ini
        db.collection("donation_requests")
                .whereEqualTo("pembuatUid", currentUser.getUid())
                .orderBy("waktuDibuat", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    binding.progressBar.setVisibility(View.GONE);
                    List<PermintaanDonor> filteredList = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        PermintaanDonor permintaan = doc.toObject(PermintaanDonor.class);
                        if (permintaan != null) {
                            // Filter status di sisi aplikasi (Client-Side Filtering)
                            if ("Aktif".equals(permintaan.getStatus()) || "Dalam Proses".equals(permintaan.getStatus())) {
                                permintaan.setRequestId(doc.getId()); // Set ID dokumen dari Firestore
                                filteredList.add(permintaan);
                            }
                        }
                    }
                    permintaanSayaList.clear();
                    permintaanSayaList.addAll(filteredList);
                    adapter.notifyDataSetChanged();
                    updateEmptyState();
                })
                .addOnFailureListener(e -> {
                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Gagal memuat permintaan saya.", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error loading my requests", e);
                    updateEmptyState(); // Perbarui status kosong meskipun ada error
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
            Toast.makeText(getContext(), "Status permintaan tidak dapat diubah.", Toast.LENGTH_SHORT).show();
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
                    Toast.makeText(getContext(), "Gagal mengubah status: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error updating request status", e);
                });
    }

    private void updateLastDonationDateForDonor(String requestId) {
        // Cari dokumen 'active_donations' untuk menemukan siapa pendonornya
        db.collection("active_donations")
                .whereEqualTo("requestId", requestId)
                .limit(1) // Hanya perlu satu proses untuk request ini
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot doc = queryDocumentSnapshots.getDocuments().get(0);
                        ProsesDonor proses = doc.toObject(ProsesDonor.class);
                        if (proses != null && proses.getParticipants() != null && proses.getParticipants().size() == 2) {
                            String donorId = null;
                            // Cari UID pendonor (yang bukan UID pembuat permintaan)
                            for (String participantUid : proses.getParticipants()) {
                                if (currentUser != null && !participantUid.equals(currentUser.getUid())) {
                                    donorId = participantUid;
                                    break;
                                }
                            }

                            if (donorId != null) {
                                final String finalDonorId = donorId; // Membuat salinan final dari donorId
                                // Update field 'lastDonationDate' di profil si pendonor
                                // Simpan tanggal donor terakhir sebagai String (format dd/MM/yyyy)
                                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                                String todayDate = sdf.format(Timestamp.now().toDate());

                                db.collection("users").document(finalDonorId) // Menggunakan finalDonorId
                                        .update("lastDonationDate", todayDate, "hasDonatedBefore", "Ya") // Update kedua field
                                        .addOnCompleteListener(task -> {
                                            if (task.isSuccessful()) {
                                                Log.d(TAG, "Tanggal donor terakhir dan status 'hasDonatedBefore' untuk " + finalDonorId + " telah diupdate."); // Menggunakan finalDonorId
                                            } else {
                                                Log.e(TAG, "Gagal update tanggal donor terakhir untuk " + finalDonorId, task.getException()); // Menggunakan finalDonorId
                                            }
                                            loadMyRequests(); // Muat ulang daftar setelah semua selesai
                                        });
                            } else {
                                Log.w(TAG, "Pendonor tidak ditemukan dalam proses donasi ini.");
                                loadMyRequests();
                            }
                        } else {
                            Log.w(TAG, "Proses donasi tidak valid atau tidak memiliki 2 partisipan.");
                            loadMyRequests();
                        }
                    } else {
                        Log.w(TAG, "Tidak ada proses donasi aktif ditemukan untuk requestId: " + requestId);
                        loadMyRequests(); // Tetap muat ulang meskipun tidak ditemukan
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error mencari proses donasi: ", e);
                    loadMyRequests();
                });
    }


    // Metode baru untuk menangani klik tombol "Hubungi"
    private void handleContactButtonClick(PermintaanDonor permintaan) {
        binding.progressBar.setVisibility(View.VISIBLE); // Tampilkan loading
        // Cari dokumen active_donations yang memiliki requestId ini dan melibatkan pengguna saat ini
        db.collection("active_donations")
                .whereEqualTo("requestId", permintaan.getRequestId())
                .limit(1) // Hanya perlu satu hasil
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    binding.progressBar.setVisibility(View.GONE);
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot doc = queryDocumentSnapshots.getDocuments().get(0);
                        ProsesDonor proses = doc.toObject(ProsesDonor.class);
                        if (proses != null && proses.getParticipants() != null && currentUser != null) {
                            String otherUserId = null;
                            // Temukan UID lawan bicara (yang bukan UID saya)
                            for (String participantId : proses.getParticipants()) {
                                if (!participantId.equals(currentUser.getUid())) {
                                    otherUserId = participantId;
                                    break;
                                }
                            }

                            if (otherUserId != null) {
                                // Ambil nama pengguna lawan bicara dari koleksi 'users'
                                String finalOtherUserId = otherUserId; // Variabel final untuk digunakan di dalam lambda
                                db.collection("users").document(otherUserId).get()
                                        .addOnSuccessListener(userDoc -> {
                                            if (userDoc.exists()) {
                                                User otherUser = userDoc.toObject(User.class);
                                                if (otherUser != null && otherUser.getFullName() != null) {
                                                    // Buka ChatRoomFragment
                                                    Fragment chatRoomFragment = ChatRoomFragment.newInstance(proses.getChatRoomId(), otherUser.getFullName());
                                                    if (getParentFragmentManager() != null) {
                                                        getParentFragmentManager().beginTransaction()
                                                                .replace(R.id.fragment_container, chatRoomFragment)
                                                                .addToBackStack(null)
                                                                .commit();
                                                    }
                                                } else {
                                                    Toast.makeText(getContext(), "Nama pendonor tidak ditemukan.", Toast.LENGTH_SHORT).show();
                                                }
                                            } else {
                                                Toast.makeText(getContext(), "Profil pendonor tidak ditemukan.", Toast.LENGTH_SHORT).show();
                                            }
                                        })
                                        .addOnFailureListener(e -> {
                                            Toast.makeText(getContext(), "Gagal memuat profil pendonor.", Toast.LENGTH_SHORT).show();
                                            Log.e(TAG, "Error fetching donor profile: ", e);
                                        });
                            } else {
                                Toast.makeText(getContext(), "Pendonor tidak ditemukan untuk permintaan ini.", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(getContext(), "Data proses donasi tidak lengkap.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(getContext(), "Belum ada pendonor yang aktif untuk permintaan ini.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Gagal memeriksa status donasi.", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error fetching active donation for contact: ", e);
                });
    }


    private void updateEmptyState() {
        binding.tvEmptyState.setVisibility(permintaanSayaList.isEmpty() ? View.VISIBLE : View.GONE);
        binding.rvPermintaanSaya.setVisibility(permintaanSayaList.isEmpty() ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}