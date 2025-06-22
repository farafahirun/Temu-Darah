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
import com.example.temudarah.util.NotificationUtil;
import com.google.firebase.Timestamp; // Jika ini digunakan untuk Date()
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot; // Import DocumentSnapshot
import com.google.firebase.firestore.FieldValue;
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
        if (currentUser != null) {
            loadMyRequests();
        } else {
            binding.tvEmptyState.setText("Harap login untuk melihat riwayat.");
            binding.tvEmptyState.setVisibility(View.VISIBLE);
            binding.progressBar.setVisibility(View.GONE); // Pastikan progress bar mati
        }
    }

    private void setupRecyclerView() {
        permintaanSayaList = new ArrayList<>();
        adapter = new PermintaanSayaAdapter(permintaanSayaList, new PermintaanSayaAdapter.OnItemActionClickListener() {
            @Override
            public void onItemClick(PermintaanDonor permintaan) {
                // Logika untuk klik seluruh item (mungkin ke detail permintaan)
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
                if (permintaan.getRequestId() != null && !permintaan.getRequestId().isEmpty()) {
                    Fragment editFragment = EditPermintaanFragment.newInstance(permintaan.getRequestId());
                    getParentFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container, editFragment)
                            .addToBackStack(null).commit();
                } else {
                    Toast.makeText(getContext(), "ID permintaan tidak ditemukan.", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onContactClick(PermintaanDonor permintaan) {
                // Panggil metode baru untuk menangani klik tombol "Hubungi"
                handleContactButtonClick(permintaan);
            }
        });
        binding.rvPermintaanSaya.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvPermintaanSaya.setAdapter(adapter);
    }

    private void loadMyRequests() {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.tvEmptyState.setVisibility(View.GONE);
        binding.rvPermintaanSaya.setVisibility(View.GONE); // Sembunyikan RecyclerView saat loading

        if (currentUser == null) {
            binding.progressBar.setVisibility(View.GONE);
            binding.tvEmptyState.setText("Harap login untuk melihat permintaan.");
            binding.tvEmptyState.setVisibility(View.VISIBLE);
            return;
        }

        db.collection("donation_requests")
                .whereEqualTo("pembuatUid", currentUser.getUid())
                .whereIn("status", Arrays.asList("Aktif", "Dalam Proses"))
                .orderBy("waktuDibuat", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!isAdded()) return; // Pastikan fragment masih melekat

                    binding.progressBar.setVisibility(View.GONE);
                    permintaanSayaList.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        PermintaanDonor permintaan = doc.toObject(PermintaanDonor.class);
                        if (permintaan != null) {
                            permintaan.setRequestId(doc.getId()); // Set ID dokumen dari Firestore
                            permintaanSayaList.add(permintaan);
                        }
                    }
                    adapter.notifyDataSetChanged();
                    updateEmptyState(); // Panggil metode untuk mengupdate empty state
                }).addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Gagal memuat permintaan saya.", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error loading my requests", e);
                    updateEmptyState(); // Perbarui status kosong meskipun ada error
                });
    }

    private void updateEmptyState() {
        binding.tvEmptyState.setVisibility(permintaanSayaList.isEmpty() ? View.VISIBLE : View.GONE);
        binding.rvPermintaanSaya.setVisibility(permintaanSayaList.isEmpty() ? View.GONE : View.VISIBLE);
        if (permintaanSayaList.isEmpty()) {
            binding.tvEmptyState.setText("Belum ada permintaan donor yang aktif.");
        }
    }


    private void showStatusChangeDialog(PermintaanDonor permintaan) {
        String currentStatus = permintaan.getStatus();

        // Opsi dialog akan berbeda tergantung status saat ini
        if ("Aktif".equals(currentStatus)) {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Batalkan Permintaan?")
                    .setMessage("Anda yakin ingin membatalkan permintaan ini? Permintaan tidak akan bisa diaktifkan kembali.")
                    .setPositiveButton("Ya, Batalkan", (dialog, which) -> updateRequestStatus(permintaan, "Dibatalkan"))
                    .setNegativeButton("Kembali", null)
                    .show();
        } else if ("Dalam Proses".equals(currentStatus)) {
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

        db.collection("active_donations")
                .whereEqualTo("requestId", permintaan.getRequestId())
                .whereEqualTo("statusProses", "Berlangsung")
                .limit(1)
                .get()
                .addOnSuccessListener(snapshots -> {
                    if (!isAdded()) return;

                    if (snapshots.isEmpty()) {
                        Toast.makeText(getContext(), "Proses ini sudah tidak aktif.", Toast.LENGTH_SHORT).show();
                        binding.progressBar.setVisibility(View.GONE);
                        loadMyRequests();
                        return;
                    }

                    DocumentSnapshot prosesDoc = snapshots.getDocuments().get(0);
                    ProsesDonor proses = prosesDoc.toObject(ProsesDonor.class);
                    WriteBatch batch = db.batch();

                    // Operasi 1: UPDATE status proses donasi menjadi "Dibatalkan oleh Penerima"
                    batch.update(prosesDoc.getReference(), "statusProses", "Dibatalkan oleh Penerima");

                    // Operasi 2: Kembalikan status permintaan utama menjadi "Aktif" agar bisa dibantu orang lain
                    DocumentReference requestRef = db.collection("donation_requests").document(permintaan.getRequestId());
                    batch.update(requestRef, "status", "Aktif");

                    batch.commit().addOnSuccessListener(aVoid -> {
                        if (!isAdded()) return;
                        Toast.makeText(getContext(), "Bantuan telah dibatalkan. Permintaan Anda aktif kembali.", Toast.LENGTH_LONG).show();

                        // Kirim notifikasi ke pendonor bahwa permintaannya dibatalkan
                        if (proses != null && proses.getDonorId() != null) {
                            NotificationUtil.createNotification(
                                    db,
                                    proses.getDonorId(), // ID Penerima Notif (si pendonor)
                                    "Bantuan Dibatalkan",
                                    "Permintaan untuk " + permintaan.getNamaPasien() + " telah dibatalkan oleh penerima.",
                                    "dibatalkan", // Tipe notifikasi
                                    permintaan.getRequestId() // ID permintaan
                            );
                        }

                        loadMyRequests();
                    }).addOnFailureListener(e -> {
                        if (isAdded()) {
                            binding.progressBar.setVisibility(View.GONE);
                            Toast.makeText(getContext(), "Gagal membatalkan.", Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "Error batch commit cancel donation process", e);
                        }
                    });
                }).addOnFailureListener(e -> {
                    if (isAdded()) {
                        binding.progressBar.setVisibility(View.GONE);
                        Toast.makeText(getContext(), "Gagal mencari proses donasi aktif.", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Error searching active donations for cancel", e);
                    }
                });
    }


    private void updateRequestStatus(PermintaanDonor permintaan, String newStatus) {
        binding.progressBar.setVisibility(View.VISIBLE);
        WriteBatch batch = db.batch();

        DocumentReference requestRef = db.collection("donation_requests").document(permintaan.getRequestId());
        batch.update(requestRef, "status", newStatus);

        if ("Selesai".equals(newStatus)) {
            db.collection("active_donations").whereEqualTo("requestId", permintaan.getRequestId()).limit(1).get()
                    .addOnSuccessListener(snapshots -> {
                        if (!isAdded()) return; // Pastikan fragment masih melekat

                        if (snapshots.isEmpty()) {
                            commitBatch(batch); // Jika tidak ada proses aktif, hanya update status permintaan
                            return;
                        }

                        DocumentSnapshot prosesDoc = snapshots.getDocuments().get(0);
                        ProsesDonor proses = prosesDoc.toObject(ProsesDonor.class);

                        batch.update(prosesDoc.getReference(), "statusProses", "Selesai"); // Update status di active_donations

                        if (proses != null && proses.getDonorId() != null) {
                            DocumentReference donorRef = db.collection("users").document(proses.getDonorId());
                            String todayDate = new SimpleDateFormat("dd MMMM yyyy", new Locale("id", "ID")).format(new Date());

                            batch.update(donorRef, "lastDonationDate", todayDate);
                            batch.update(donorRef, "hasDonatedBefore", "Ya");
                            batch.update(donorRef, "donationCount", FieldValue.increment(1));

                            String notifTitle = "Proses Donasi Selesai";
                            String notifMessage = "Terima kasih! Permintaan untuk " + permintaan.getNamaPasien() + " telah ditandai selesai.";
                            NotificationUtil.createNotification(
                                    db,
                                    proses.getDonorId(),
                                    notifTitle,
                                    notifMessage,
                                    "selesai",
                                    proses.getRequestId()
                            );
                        }
                        commitBatch(batch); // Jalankan semua operasi dalam satu batch
                    }).addOnFailureListener(e -> {
                        if (!isAdded()) return;
                        commitBatch(batch); // Jika gagal mencari, tetap commit perubahan status permintaan awal
                        Log.e(TAG, "Gagal mencari proses donasi aktif saat tandai selesai", e);
                    });
        } else {
            commitBatch(batch); // Jika status hanya "Dibatalkan", langsung jalankan batch
        }
    }

    private void commitBatch(WriteBatch batch) {
        batch.commit().addOnSuccessListener(aVoid -> {
            if (!isAdded()) return;
            Toast.makeText(getContext(), "Status berhasil diubah.", Toast.LENGTH_SHORT).show();
            loadMyRequests(); // Muat ulang daftar untuk refresh tampilan
        }).addOnFailureListener(e -> {
            if(isAdded()) {
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Gagal mengubah status.", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Error batch commit update status", e);
            }
        });
    }

    // --- Metode baru untuk menangani klik tombol "Hubungi" ---
    private void handleContactButtonClick(PermintaanDonor permintaan) {
        binding.progressBar.setVisibility(View.VISIBLE); // Tampilkan loading

        // Cari dokumen 'active_donations' yang memiliki requestId ini dan melibatkan pengguna saat ini
        db.collection("active_donations")
                .whereEqualTo("requestId", permintaan.getRequestId())
                .limit(1) // Hanya perlu satu hasil
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!isAdded()) return;

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
                                String finalOtherUserId = otherUserId;
                                db.collection("users").document(otherUserId).get()
                                        .addOnSuccessListener(userDoc -> {
                                            if (!isAdded()) return;

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
                                            if (!isAdded()) return;
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
                    if (!isAdded()) return;
                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Gagal memeriksa status donasi.", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error fetching active donation for contact: ", e);
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}