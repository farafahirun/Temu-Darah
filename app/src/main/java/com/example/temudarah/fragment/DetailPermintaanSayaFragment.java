package com.example.temudarah.fragment;

import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.temudarah.R;
import com.example.temudarah.activity.MainActivity;
import com.example.temudarah.databinding.FragmentDetailPermintaanSayaBinding;
import com.example.temudarah.model.PermintaanDonor;
import com.example.temudarah.model.ProsesDonor;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DetailPermintaanSayaFragment extends Fragment {

    private static final String TAG = "DetailPermintaanSaya";
    private static final String ARG_REQUEST_ID = "request_id";

    private FragmentDetailPermintaanSayaBinding binding;
    private FirebaseFirestore db;
    private String requestId;
    private PermintaanDonor currentPermintaan;

    public static DetailPermintaanSayaFragment newInstance(String requestId) {
        DetailPermintaanSayaFragment fragment = new DetailPermintaanSayaFragment();
        Bundle args = new Bundle();
        args.putString(ARG_REQUEST_ID, requestId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            requestId = getArguments().getString(ARG_REQUEST_ID);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentDetailPermintaanSayaBinding.inflate(inflater, container, false);
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).hideBottomNav();
        }
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = FirebaseFirestore.getInstance();

        binding.toolbar.setNavigationOnClickListener(v -> getParentFragmentManager().popBackStack());
        binding.btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());

        if (requestId != null) {
            // ✅ Tampilkan hanya progress bar, sembunyikan semua lainnya
            binding.loadingOverlay.setVisibility(View.VISIBLE);
            binding.scrollView.setVisibility(View.GONE);
            binding.llButtons.setVisibility(View.GONE);
            binding.appBarLayout.setVisibility(View.GONE); // Sembunyikan AppBar juga

            new Handler().postDelayed(() -> {
                // ✅ Setelah 1 detik, tampilkan semua kembali
                binding.loadingOverlay.setVisibility(View.GONE);
                binding.scrollView.setVisibility(View.VISIBLE);
                binding.llButtons.setVisibility(View.VISIBLE);
                binding.appBarLayout.setVisibility(View.VISIBLE);

                loadRequestDetails(); // Pastikan ini tidak mengatur loadingOverlay lagi
            }, 1000);
        }
    }



    private void loadRequestDetails() {
        binding.progressBar.setVisibility(View.VISIBLE);
        db.collection("donation_requests").document(requestId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    binding.progressBar.setVisibility(View.GONE);
                    if (documentSnapshot.exists() && isAdded()) {
                        currentPermintaan = documentSnapshot.toObject(PermintaanDonor.class);
                        currentPermintaan.setRequestId(documentSnapshot.getId());
                        populateUi();
                    } else if (isAdded()) {
                        Toast.makeText(getContext(), "Permintaan ini tidak lagi tersedia.", Toast.LENGTH_SHORT).show();
                        getParentFragmentManager().popBackStack();
                    }
                }).addOnFailureListener(e -> {
                    if (isAdded()) {
                        binding.progressBar.setVisibility(View.GONE);
                        Toast.makeText(getContext(), "Gagal memuat detail.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void populateUi() {
        if (currentPermintaan == null || getContext() == null) return;

        // Mengisi data ke semua TextView
        binding.tvStatus.setText(currentPermintaan.getStatus());
        binding.tvNamaPasien.setText(currentPermintaan.getNamaPasien());
        binding.tvGolDarah.setText(currentPermintaan.getGolonganDarahDibutuhkan());
        binding.tvJumlahKantong.setText(String.format(Locale.getDefault(), "%d Kantong", currentPermintaan.getJumlahKantong()));
        binding.tvNamaRs.setText(currentPermintaan.getNamaRumahSakit());
        binding.tvCatatan.setText(currentPermintaan.getCatatan());

        // Mengatur tampilan (warna & tombol) berdasarkan status
        updateButtonsAndStatusView();
        setupActionListeners();
    }

    private void setupActionListeners() {
        binding.btnEdit.setOnClickListener(v -> {
            Fragment editFragment = EditPermintaanFragment.newInstance(currentPermintaan.getRequestId());
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, editFragment)
                    .addToBackStack(null).commit();
        });

        binding.btnUbahStatus.setOnClickListener(v -> showStatusChangeDialog());
    }

    private void updateButtonsAndStatusView() {
        if (currentPermintaan == null || getContext() == null) return;

        String status = currentPermintaan.getStatus();
        binding.tvStatus.setText(status);

        if ("Aktif".equals(status)) {
            binding.tvStatus.setTextColor(ContextCompat.getColor(getContext(), R.color.sukses));
            binding.btnUbahStatus.setText("Batalkan Permintaan");
            binding.btnEdit.setVisibility(View.VISIBLE);
            binding.btnUbahStatus.setVisibility(View.VISIBLE);
        } else if ("Dalam Proses".equals(status)) {
            binding.tvStatus.setTextColor(ContextCompat.getColor(getContext(), R.color.penerima));
            binding.btnUbahStatus.setText("Tandai Selesai");
            binding.btnEdit.setVisibility(View.GONE); // Tidak bisa edit saat sedang diproses
            binding.btnUbahStatus.setVisibility(View.VISIBLE);
        } else { // Selesai atau Dibatalkan
            binding.tvStatus.setTextColor(ContextCompat.getColor(getContext(), R.color.text_info));
            binding.btnEdit.setVisibility(View.GONE);
            binding.btnUbahStatus.setVisibility(View.GONE);
        }
    }

    private void showStatusChangeDialog() {
        String currentStatus = currentPermintaan.getStatus();
        final String newStatus;
        String message;
        String positiveButtonText;

        if ("Aktif".equals(currentStatus)) {
            newStatus = "Dibatalkan";
            message = "Anda yakin ingin membatalkan permintaan ini? Permintaan tidak akan bisa diaktifkan kembali.";
            positiveButtonText = "Ya, Batalkan";
        } else if ("Dalam Proses".equals(currentStatus)) {
            newStatus = "Selesai";
            message = "Dengan menandai selesai, Anda mengonfirmasi bahwa bantuan telah diterima. Aksi ini tidak bisa diubah.";
            positiveButtonText = "Ya, Tandai Selesai";
        } else {
            return;
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Konfirmasi Aksi")
                .setMessage(message)
                .setPositiveButton(positiveButtonText, (dialog, which) -> updateRequestStatus(newStatus))
                .setNegativeButton("Kembali", null)
                .show();
    }

    private void updateRequestStatus(String newStatus) {
        binding.progressBar.setVisibility(View.VISIBLE);
        WriteBatch batch = db.batch();

        DocumentReference requestRef = db.collection("donation_requests").document(requestId);
        batch.update(requestRef, "status", newStatus);

        if ("Selesai".equals(newStatus)) {
            db.collection("active_donations").whereEqualTo("requestId", requestId).limit(1).get()
                    .addOnSuccessListener(snapshots -> {
                        if (!snapshots.isEmpty()) {
                            DocumentSnapshot prosesDoc = snapshots.getDocuments().get(0);
                            batch.update(prosesDoc.getReference(), "statusProses", "Selesai");

                            ProsesDonor proses = prosesDoc.toObject(ProsesDonor.class);
                            if (proses != null && proses.getDonorId() != null) {
                                DocumentReference donorRef = db.collection("users").document(proses.getDonorId());
                                String todayDate = new SimpleDateFormat("dd MMMM yyyy", Locale.forLanguageTag("id-ID")).format(new Date());
                                batch.update(donorRef, "lastDonationDate", todayDate);
                            }
                        }
                        commitBatch(batch);
                    }).addOnFailureListener(e -> commitBatch(batch)); // Tetap commit walau gagal cari proses
        } else {
            commitBatch(batch);
        }
    }

    private void commitBatch(WriteBatch batch) {
        batch.commit().addOnSuccessListener(aVoid -> {
            Toast.makeText(getContext(), "Status berhasil diubah.", Toast.LENGTH_SHORT).show();
            // Kembali ke halaman sebelumnya untuk melihat daftar yang ter-update
            getParentFragmentManager().popBackStack();
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