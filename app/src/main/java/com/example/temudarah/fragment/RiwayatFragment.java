package com.example.temudarah.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button; // Import Button
import android.widget.LinearLayout; // Import LinearLayout
import android.widget.TextView; // Import TextView
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
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class RiwayatFragment extends Fragment {

    private static final String TAG = "RiwayatFragment";
    private FragmentRiwayatBinding binding;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    private RiwayatDonasiAdapter donasiAdapter;
    private List<RiwayatDonasiTampil> riwayatDonasiTampilList;

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

        // Tangani pemilihan filter untuk Donor Darah
        // Karena hanya ada satu tab, kita langsung atur listenernya
        binding.filterBtnSemuaDonor.setOnClickListener(v -> {
            loadMyDonationHistory("Semua");
//            updateFilterButtonStates(binding.filterBtnSemuaDonor);
        });
        binding.filterBtnSebagaiPendonor.setOnClickListener(v -> {
            loadMyDonationHistory("Sebagai Pendonor");
//            updateFilterButtonStates(binding.filterBtnSebagaiPendonor);
        });
        binding.filterBtnSebagaiPenerima.setOnClickListener(v -> {
            loadMyDonationHistory("Sebagai Penerima");
//            updateFilterButtonStates(binding.filterBtnSebagaiPenerima);
        });

        // Muat riwayat donasi darah secara default (filter "Semua")
        loadMyDonationHistory("Semua");
//        updateFilterButtonStates(binding.filterBtnSemuaDonor); // Atur tombol filter default
    }

    private void setupRecyclerView() {
        binding.rvRiwayat.setLayoutManager(new LinearLayoutManager(getContext()));
        riwayatDonasiTampilList = new ArrayList<>();
        donasiAdapter = new RiwayatDonasiAdapter(riwayatDonasiTampilList, riwayat -> {});
        binding.rvRiwayat.setAdapter(donasiAdapter);
    }

//    private void updateFilterButtonStates(View selectedButton) {
//        // Reset semua tombol filter Donor Darah ke gaya default (outline)
//        binding.filterBtnSemuaDonor.setBackgroundResource(R.drawable.rounded_button_outline);
//        binding.filterBtnSemuaDonor.setTextColor(ContextCompat.getColor(requireContext(), R.color.utama_red));
//        binding.filterBtnSebagaiPendonor.setBackgroundResource(R.drawable.rounded_button_outline);
//        binding.filterBtnSebagaiPendonor.setTextColor(ContextCompat.getColor(requireContext(), R.color.utama_red));
//        binding.filterBtnSebagaiPenerima.setBackgroundResource(R.drawable.rounded_button_outline);
//        binding.filterBtnSebagaiPenerima.setTextColor(ContextCompat.getColor(requireContext(), R.color.utama_red));
//
//        // Set tombol yang dipilih ke gaya terpilih (filled)
//        selectedButton.setBackgroundResource(R.drawable.rounded_button_filled);
//        if (selectedButton instanceof Button) { // Gunakan Button, bukan android.widget.Button
//            ((Button) selectedButton).setTextColor(ContextCompat.getColor(requireContext(), R.color.text_white));
//        }
//    }

    private void loadMyDonationHistory(String peranFilter) {
        if (currentUser == null) {
            binding.progressBar.setVisibility(View.GONE);
            binding.tvEmptyStateRiwayat.setText("Anda perlu login untuk melihat riwayat donasi.");
            binding.tvEmptyStateRiwayat.setVisibility(View.VISIBLE);
            return;
        }

        binding.progressBar.setVisibility(View.VISIBLE);
        binding.tvEmptyStateRiwayat.setVisibility(View.GONE);
        riwayatDonasiTampilList.clear();
        donasiAdapter.notifyDataSetChanged();

        String currentUserId = currentUser.getUid();

        // Task 1: Ambil permintaan donor di mana pengguna saat ini adalah "pembuatUid"
        Task<QuerySnapshot> requestsAsRequesterTask = db.collection("donation_requests")
                .whereEqualTo("pembuatUid", currentUserId)
                .whereIn("status", Arrays.asList("Selesai", "Dibatalkan"))
                .get();

        // Task 2: Ambil proses donasi yang melibatkan pengguna saat ini (sebagai pembuat atau pendonor)
        Task<QuerySnapshot> activeDonationsTask = db.collection("active_donations")
                .whereArrayContains("participants", currentUserId)
                .get();

        Tasks.whenAllSuccess(requestsAsRequesterTask, activeDonationsTask)
                .addOnSuccessListener(results -> {
                    List<RiwayatDonasiTampil> tempList = new ArrayList<>();
                    List<String> processedRequestIds = new ArrayList<>();

                    // Proses permintaan di mana pengguna saat ini adalah pembuat permintaan (penerima)
                    QuerySnapshot requestsAsRequesterSnapshot = (QuerySnapshot) results.get(0);
                    for (DocumentSnapshot doc : requestsAsRequesterSnapshot.getDocuments()) {
                        PermintaanDonor permintaan = doc.toObject(PermintaanDonor.class);
                        if (permintaan != null) {
                            permintaan.setRequestId(doc.getId());
                            if ("Selesai".equals(permintaan.getStatus()) || "Dibatalkan".equals(permintaan.getStatus())) {
                                if ("Semua".equals(peranFilter) || "Sebagai Penerima".equals(peranFilter)) {
                                    RiwayatDonasiTampil item = new RiwayatDonasiTampil();
                                    item.setTanggal(permintaan.getWaktuDibuat());
                                    item.setStatusProses(permintaan.getStatus());
                                    item.setNamaPasien(permintaan.getNamaPasien());
                                    item.setPeranSaya("Anda Penerima");
                                    item.setJudulTampilan("Permintaan Anda untuk " + permintaan.getNamaPasien());
                                    tempList.add(item);
                                    processedRequestIds.add(permintaan.getRequestId());
                                }
                            }
                        }
                    }

                    // Proses active_donations untuk menemukan peran pendonor dan potensi permintaan "Selesai"
                    QuerySnapshot activeDonationsSnapshot = (QuerySnapshot) results.get(1);
                    List<Task<DocumentSnapshot>> userAndRequestTasks = new ArrayList<>();
                    List<ProsesDonor> processesToFetchDetailsFor = new ArrayList<>();

                    for (DocumentSnapshot prosesDoc : activeDonationsSnapshot.getDocuments()) {
                        ProsesDonor proses = prosesDoc.toObject(ProsesDonor.class);
                        if (proses != null && proses.getParticipants() != null && proses.getRequestId() != null) {
                            if (!processedRequestIds.contains(proses.getRequestId()) || currentUserId.equals(proses.getDonorId())) {
                                String otherUserId = null;
                                // Identifikasi UID pengguna lain
                                for (String participantId : proses.getParticipants()) {
                                    if (!participantId.equals(currentUserId)) {
                                        otherUserId = participantId;
                                        break;
                                    }
                                }

                                if (otherUserId != null) {
                                    processesToFetchDetailsFor.add(proses);
                                    userAndRequestTasks.add(db.collection("users").document(otherUserId).get());
                                    userAndRequestTasks.add(db.collection("donation_requests").document(proses.getRequestId()).get());
                                }
                            }
                        }
                    }

                    Tasks.whenAllSuccess(userAndRequestTasks).addOnSuccessListener(innerResults -> {
                        for (int i = 0; i < processesToFetchDetailsFor.size(); i++) {
                            ProsesDonor proses = processesToFetchDetailsFor.get(i);
                            DocumentSnapshot userDoc = (DocumentSnapshot) innerResults.get(i * 2);
                            DocumentSnapshot requestDoc = (DocumentSnapshot) innerResults.get(i * 2 + 1);

                            User otherUser = userDoc.toObject(User.class);
                            PermintaanDonor permintaan = requestDoc.toObject(PermintaanDonor.class);

                            if (otherUser != null && permintaan != null) {
                                boolean sayaPendonor = currentUserId.equals(proses.getDonorId());
                                boolean sayaPenerima = currentUserId.equals(proses.getRequesterId());

                                if ("Selesai".equals(permintaan.getStatus()) || "Dibatalkan".equals(permintaan.getStatus())) {
                                    if ("Semua".equals(peranFilter) ||
                                            ("Sebagai Pendonor".equals(peranFilter) && sayaPendonor) ||
                                            ("Sebagai Penerima".equals(peranFilter) && sayaPenerima)) {

                                        if (sayaPenerima && processedRequestIds.contains(permintaan.getRequestId())) {
                                            continue;
                                        }

                                        RiwayatDonasiTampil item = new RiwayatDonasiTampil();
                                        item.setTanggal(proses.getTimestamp());
                                        item.setStatusProses(permintaan.getStatus());
                                        item.setNamaPasien(permintaan.getNamaPasien());
                                        item.setPeranSaya(sayaPendonor ? "Anda Membantu" : "Anda Dibantu oleh");
                                        item.setJudulTampilan(sayaPendonor ? "Donasi untuk " + otherUser.getFullName() : "Bantuan dari " + otherUser.getFullName());
                                        tempList.add(item);
                                    }
                                }
                            }
                        }
                        Collections.sort(tempList, (o1, o2) -> {
                            if (o1.getTanggal() == null || o2.getTanggal() == null) return 0;
                            return o2.getTanggal().compareTo(o1.getTanggal());
                        });

                        riwayatDonasiTampilList.clear();
                        riwayatDonasiTampilList.addAll(tempList);
                        donasiAdapter.notifyDataSetChanged();
                        binding.progressBar.setVisibility(View.GONE);
                        updateEmptyStateDonasi();
                    }).addOnFailureListener(e -> {
                        Log.e(TAG, "Error fetching user/request details for active donations: ", e);
                        binding.progressBar.setVisibility(View.GONE);
                        updateEmptyStateDonasi();
                    });

                })
                .addOnFailureListener(e -> {
                    binding.progressBar.setVisibility(View.GONE);
                    Log.e(TAG, "Error loading donation history: ", e);
                    updateEmptyStateDonasi();
                });
    }

    private void updateEmptyStateDonasi() {
        binding.tvEmptyStateRiwayat.setVisibility(riwayatDonasiTampilList.isEmpty() ? View.VISIBLE : View.GONE);
        binding.rvRiwayat.setVisibility(riwayatDonasiTampilList.isEmpty() ? View.GONE : View.VISIBLE);
        if (riwayatDonasiTampilList.isEmpty()) {
            binding.tvEmptyStateRiwayat.setText("Belum ada riwayat donasi.");
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}