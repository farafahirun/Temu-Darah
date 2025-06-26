package com.example.temudarah.fragment;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
    private static final long MIN_LOADING_TIME = 1000; // 1 second in milliseconds

    private FragmentDetailRiwayatBinding binding;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private String prosesId;

    // Handler for managing loading delay
    private Handler delayHandler;
    private long startTime; // To track when loading started

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
        delayHandler = new Handler(Looper.getMainLooper()); // Initialize handler on the main looper
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
        binding.navbar.setNavigationOnClickListener(v -> getParentFragmentManager().popBackStack());
        binding.btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());

        if (prosesId != null) {
            // Record the start time of loading
            startTime = System.currentTimeMillis();
            binding.loadingOverlay.setVisibility(View.VISIBLE);
            loadRiwayatDetails();
        } else {
            handleFailure("ID Riwayat tidak valid.");
        }
    }

    private void loadRiwayatDetails() {
        // Try to load from active_donations first
        db.collection("active_donations").document(prosesId).get()
                .addOnSuccessListener(prosesDoc -> {
                    if (!isAdded()) {
                        hideLoadingOverlayDelayed(); // Ensure overlay is hidden even if fragment detached
                        return;
                    }

                    if (prosesDoc.exists()) {
                        // Data found in active_donations, proceed as usual
                        ProsesDonor proses = prosesDoc.toObject(ProsesDonor.class);
                        if (proses == null || proses.getRequestId() == null || proses.getRequesterId() == null || proses.getDonorId() == null) {
                            handleFailure("Data riwayat proses tidak lengkap.");
                            hideLoadingOverlayDelayed();
                            return;
                        }
                        fetchRequestAndUserForActiveDonation(proses);
                    } else {
                        // Data not found in active_donations, try loading directly from donation_requests
                        loadCanceledRequestDetails(prosesId);
                    }
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) {
                        hideLoadingOverlayDelayed(); // Ensure overlay is hidden even if fragment detached
                        return;
                    }
                    // If fetching from active_donations completely fails (e.g., network error),
                    // we can still try donation_requests as a fallback, or handle it as a failure.
                    loadCanceledRequestDetails(prosesId);
                });
    }

    private void fetchRequestAndUserForActiveDonation(ProsesDonor proses) {
        // Tahap 2: Setelah data proses didapat, ambil detail permintaannya
        db.collection("donation_requests").document(proses.getRequestId()).get()
                .addOnSuccessListener(requestDoc -> {
                    if (!isAdded() || !requestDoc.exists()) {
                        handleFailure("Detail permintaan tidak ditemukan.");
                        hideLoadingOverlayDelayed();
                        return;
                    }
                    PermintaanDonor permintaan = requestDoc.toObject(PermintaanDonor.class);

                    // Tahap 3: Ambil detail user peminta (requester)
                    db.collection("users").document(proses.getRequesterId()).get()
                            .addOnSuccessListener(requesterDoc -> {
                                if (!isAdded() || !requesterDoc.exists()) {
                                    handleFailure("Profil peminta tidak ditemukan.");
                                    hideLoadingOverlayDelayed();
                                    return;
                                }
                                User requester = requesterDoc.toObject(User.class);

                                // Tahap 4: Ambil detail user pendonor (donor)
                                db.collection("users").document(proses.getDonorId()).get()
                                        .addOnSuccessListener(donorDoc -> {
                                            if (!isAdded() || !donorDoc.exists()) {
                                                handleFailure("Profil pendonor tidak ditemukan.");
                                                hideLoadingOverlayDelayed();
                                                return;
                                            }
                                            User donor = donorDoc.toObject(User.class);

                                            // Tahap Final: Setelah all data is ready, hide loading overlay with delay
                                            populateUiForActiveDonation(proses, permintaan, requester, donor);
                                            hideLoadingOverlayDelayed(); // Call this here!

                                        }).addOnFailureListener(e -> {
                                            handleFailure("Gagal memuat profil pendonor.");
                                            hideLoadingOverlayDelayed();
                                        });
                            }).addOnFailureListener(e -> {
                                handleFailure("Gagal memuat profil peminta.");
                                hideLoadingOverlayDelayed();
                            });
                }).addOnFailureListener(e -> {
                    handleFailure("Gagal memuat detail permintaan.");
                    hideLoadingOverlayDelayed();
                });
    }

    private void loadCanceledRequestDetails(String requestId) {
        db.collection("donation_requests").document(requestId).get()
                .addOnSuccessListener(requestDoc -> {
                    if (!isAdded()) {
                        hideLoadingOverlayDelayed(); // Ensure overlay is hidden even if fragment detached
                        return;
                    }

                    if (requestDoc.exists()) {
                        PermintaanDonor permintaan = requestDoc.toObject(PermintaanDonor.class);
                        if (permintaan != null && "Dibatalkan".equals(permintaan.getStatus())) {
                            // This is a self-canceled request
                            // Fetch the requester's details
                            db.collection("users").document(Objects.requireNonNull(permintaan.getPembuatUid())).get()
                                    .addOnSuccessListener(requesterDoc -> {
                                        if (!isAdded() || !requesterDoc.exists()) {
                                            handleFailure("Profil pembuat permintaan tidak ditemukan.");
                                            hideLoadingOverlayDelayed();
                                            return;
                                        }
                                        User requester = requesterDoc.toObject(User.class);
                                        populateUiForCanceledRequest(permintaan, requester);
                                        hideLoadingOverlayDelayed(); // Call this here!
                                    })
                                    .addOnFailureListener(e -> {
                                        handleFailure("Gagal memuat profil pembuat permintaan.");
                                        hideLoadingOverlayDelayed();
                                    });
                        } else {
                            handleFailure("Detail riwayat tidak ditemukan.");
                            hideLoadingOverlayDelayed();
                        }
                    } else {
                        handleFailure("Detail riwayat tidak ditemukan.");
                        hideLoadingOverlayDelayed();
                    }
                })
                .addOnFailureListener(e -> {
                    handleFailure("Gagal memuat detail riwayat.");
                    hideLoadingOverlayDelayed();
                });
    }

    private void populateUiForActiveDonation(ProsesDonor proses, PermintaanDonor permintaan, User requester, User donor) {
        if (getContext() == null || binding == null) return;

        binding.tvDetailStatus.setText(proses.getStatusProses());

        String requesterName = (requester != null && requester.getFullName() != null) ? requester.getFullName() : "Data Dihapus";
        String donorName = (donor != null && donor.getFullName() != null) ? donor.getFullName() : "Data Dihapus";

        // Tambahkan label "(Anda)" untuk kejelasan
        if (currentUser != null) {
            if (currentUser.getUid().equals(proses.getRequesterId())) {
                requesterName += " (Anda)";
            }
            if (currentUser.getUid().equals(proses.getDonorId())) {
                donorName += " (Anda)";
            }
        }

        binding.tvNamaPihak1.setText(requesterName);
        binding.tvNamaPihak2.setText(donorName);

        binding.tvDetailPasien.setText(permintaan.getNamaPasien());
        binding.tvDetailGolDarah.setText(permintaan.getGolonganDarahDibutuhkan());
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

        // Set "Peminta Bantuan" and "Pendonor" labels based on the active donation context
        binding.tvPihak1.setText("Peminta Bantuan");
        binding.tvPihak2.setText("Pendonor");
    }

    private void populateUiForCanceledRequest(PermintaanDonor permintaan, User requester) {
        if (getContext() == null || binding == null) return;

        binding.tvDetailStatus.setText(permintaan.getStatus());

        String requesterName = (requester != null && requester.getFullName() != null) ? requester.getFullName() : "Data Dihapus";
        if (currentUser != null && currentUser.getUid().equals(permintaan.getPembuatUid())) {
            requesterName += " (Anda)";
        }

        // For canceled requests, the 'donor' section might not be relevant or populated differently.
        // We'll hide the donor section or set appropriate default text.
        binding.tvPihak1.setText("Pembuat Permintaan"); // More accurate for canceled requests
        binding.tvNamaPihak1.setText(requesterName);

        binding.tvPihak2.setText("Pendonor"); // Still show label, but the name is "Belum Ada"
        binding.tvNamaPihak2.setText("Belum Ada"); // Or hide this section if preferred
        // binding.tvPihak2.setVisibility(View.GONE);
        // binding.tvNamaPihak2.setVisibility(View.GONE);


        binding.tvDetailPasien.setText(permintaan.getNamaPasien());
        binding.tvDetailGolDarah.setText(permintaan.getGolonganDarahDibutuhkan());
        binding.tvDetailLokasi.setText(permintaan.getNamaRumahSakit());

        if (permintaan.getWaktuDibuat() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM yyyy, HH:mm", new Locale("id", "ID"));
            binding.tvDetailTanggal.setText("Pada " + sdf.format(permintaan.getWaktuDibuat().toDate()));
        }

        int colorRes = R.color.utama; // Default color for canceled
        binding.tvDetailStatus.setTextColor(ContextCompat.getColor(getContext(), colorRes));
    }


    private void handleFailure(String message) {
        if (isAdded() && getContext() != null) {
            binding.loadingOverlay.setVisibility(View.GONE); // Ensure it's hidden on failure
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
            // Optionally, navigate back if data isn't found
            // getParentFragmentManager().popBackStack();
        }
    }

    // --- New method to hide loading overlay with delay ---
    private void hideLoadingOverlayDelayed() {
        if (binding == null) return; // Defensive check

        long elapsedTime = System.currentTimeMillis() - startTime;
        long delayRemaining = MIN_LOADING_TIME - elapsedTime;

        if (delayRemaining > 0) {
            // If less than MIN_LOADING_TIME has passed, delay the hiding
            delayHandler.postDelayed(() -> {
                if (binding != null) { // Check binding again before hiding
                    binding.loadingOverlay.setVisibility(View.GONE);
                }
            }, delayRemaining);
        } else {
            // If MIN_LOADING_TIME or more has passed, hide immediately
            binding.loadingOverlay.setVisibility(View.GONE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Remove any pending callbacks to prevent memory leaks if fragment is destroyed
        if (delayHandler != null) {
            delayHandler.removeCallbacksAndMessages(null);
        }
        binding = null;
    }
}