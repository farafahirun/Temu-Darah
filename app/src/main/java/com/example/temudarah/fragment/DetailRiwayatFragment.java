package com.example.temudarah.fragment;

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
import com.example.temudarah.R;
import com.example.temudarah.databinding.FragmentDetailRiwayatBinding;
import com.example.temudarah.model.PermintaanDonor;
import com.example.temudarah.model.ProsesDonor;
import com.example.temudarah.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Objects;

public class DetailRiwayatFragment extends Fragment {

    private static final String TAG = "DetailRiwayatFragment";
    private static final String ARG_PROSES_ID = "proses_id";

    private FragmentDetailRiwayatBinding binding;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private String prosesId;

    public static DetailRiwayatFragment newInstance(String prosesId) {
        DetailRiwayatFragment fragment = new DetailRiwayatFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PROSES_ID, prosesId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            prosesId = getArguments().getString(ARG_PROSES_ID);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentDetailRiwayatBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        binding.toolbar.setNavigationOnClickListener(v -> getParentFragmentManager().popBackStack());

        if (prosesId != null) {
            loadRiwayatDetails();
        } else {
            handleFailure("ID Riwayat tidak valid.");
        }
    }

    /**
     * FUNGSI YANG DIPERBAIKI TOTAL DENGAN LOGIKA BERANTAI YANG AMAN
     */
    private void loadRiwayatDetails() {
        binding.progressBar.setVisibility(View.VISIBLE);

        // Tahap 1: Ambil data utama dari 'active_donations'
        db.collection("active_donations").document(prosesId).get()
                .addOnSuccessListener(prosesDoc -> {
                    if (!isAdded() || !prosesDoc.exists()) {
                        handleFailure("Riwayat tidak ditemukan.");
                        return;
                    }
                    ProsesDonor proses = prosesDoc.toObject(ProsesDonor.class);
                    if (proses == null || proses.getRequestId() == null || proses.getRequesterId() == null || proses.getDonorId() == null) {
                        handleFailure("Data riwayat tidak lengkap.");
                        return;
                    }

                    // Tahap 2: Setelah data proses didapat, ambil detail permintaannya
                    db.collection("donation_requests").document(proses.getRequestId()).get()
                            .addOnSuccessListener(requestDoc -> {
                                if (!isAdded() || !requestDoc.exists()) {
                                    handleFailure("Detail permintaan tidak ditemukan.");
                                    return;
                                }
                                PermintaanDonor permintaan = requestDoc.toObject(PermintaanDonor.class);

                                // Tahap 3: Ambil detail user peminta (requester)
                                db.collection("users").document(proses.getRequesterId()).get()
                                        .addOnSuccessListener(requesterDoc -> {
                                            if (!isAdded() || !requesterDoc.exists()) {
                                                handleFailure("Profil peminta tidak ditemukan.");
                                                return;
                                            }
                                            User requester = requesterDoc.toObject(User.class);

                                            // Tahap 4: Ambil detail user pendonor (donor)
                                            db.collection("users").document(proses.getDonorId()).get()
                                                    .addOnSuccessListener(donorDoc -> {
                                                        if (!isAdded() || !donorDoc.exists()) {
                                                            handleFailure("Profil pendonor tidak ditemukan.");
                                                            return;
                                                        }
                                                        User donor = donorDoc.toObject(User.class);

                                                        // Tahap Final: Setelah semua data lengkap, baru tampilkan ke UI
                                                        binding.progressBar.setVisibility(View.GONE);
                                                        populateUi(proses, permintaan, requester, donor);

                                                    }).addOnFailureListener(e -> handleFailure("Gagal memuat profil pendonor."));
                                        }).addOnFailureListener(e -> handleFailure("Gagal memuat profil peminta."));
                            }).addOnFailureListener(e -> handleFailure("Gagal memuat detail permintaan."));
                })
                .addOnFailureListener(e -> handleFailure("Gagal memuat riwayat."));
    }

    private void populateUi(ProsesDonor proses, PermintaanDonor permintaan, User requester, User donor) {
        if (getContext() == null || binding == null) return;

        binding.tvDetailStatus.setText(proses.getStatusProses());

        String requesterName = (requester != null && requester.getFullName() != null) ? requester.getFullName() : "Data Dihapus";
        String donorName = (donor != null && donor.getFullName() != null) ? donor.getFullName() : "Data Dihapus";

        // Tambahkan label "(Anda)" untuk kejelasan
        if (currentUser.getUid().equals(requester.getUid())) {
            requesterName += " (Anda)";
        }
        if (currentUser.getUid().equals(donor.getUid())) {
            donorName += " (Anda)";
        }

        binding.tvNamaPihak1.setText(requesterName);
        binding.tvNamaPihak2.setText(donorName);

        binding.tvDetailPasien.setText(permintaan.getNamaPasien() + " (" + permintaan.getGolonganDarahDibutuhkan() + ")");
        binding.tvDetailLokasi.setText(permintaan.getNamaRumahSakit());

        if (proses.getTimestamp() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM yyyy, HH:mm", new Locale("id", "ID"));
            binding.tvDetailTanggal.setText("Pada " + sdf.format(proses.getTimestamp().toDate()));
        }

        int colorRes = R.color.text_info;
        if ("Selesai".equals(proses.getStatusProses())) {
            colorRes = R.color.sukses;
        } else if (proses.getStatusProses() != null && proses.getStatusProses().contains("Dibatalkan")) {
            colorRes = R.color.utama;
        }
        binding.tvDetailStatus.setTextColor(ContextCompat.getColor(getContext(), colorRes));
    }

    private void handleFailure(String message) {
        if (isAdded() && getContext() != null) {
            binding.progressBar.setVisibility(View.GONE);
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}