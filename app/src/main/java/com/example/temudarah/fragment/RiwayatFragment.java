package com.example.temudarah.fragment;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.temudarah.R;
import com.example.temudarah.adapter.RiwayatDonasiAdapter;
import com.example.temudarah.adapter.RiwayatKegiatanAdapter;
import com.example.temudarah.databinding.FragmentRiwayatBinding;
import com.example.temudarah.model.Kegiatan;
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
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class RiwayatFragment extends Fragment {

    private static final String TAG = "RiwayatFragment";
    private FragmentRiwayatBinding binding;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    private RiwayatKegiatanAdapter kegiatanAdapter;
    private List<Kegiatan> kegiatanList;

    private RiwayatDonasiAdapter donasiAdapter;
    private List<RiwayatDonasiTampil> riwayatDonasiTampilList;

    private enum ActiveTab { KEGIATAN, DONOR_DARAH }

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

        setupRecyclerViews();
        setupTabListeners();

        // Memulai dengan tab Kegiatan aktif
        selectTab(ActiveTab.KEGIATAN);
    }

    private void setupRecyclerViews() {
        binding.rvRiwayat.setLayoutManager(new LinearLayoutManager(getContext()));

        kegiatanList = new ArrayList<>();
        kegiatanAdapter = new RiwayatKegiatanAdapter(kegiatanList, kegiatan -> {});

        riwayatDonasiTampilList = new ArrayList<>();
        donasiAdapter = new RiwayatDonasiAdapter(riwayatDonasiTampilList, riwayat -> {});
    }

    private void setupTabListeners() {
        binding.tabKegiatan.setOnClickListener(v -> selectTab(ActiveTab.KEGIATAN));
        binding.tabDonorDarah.setOnClickListener(v -> selectTab(ActiveTab.DONOR_DARAH));

        // TODO: Tambahkan listener untuk sub-filter di sini
    }

    private void selectTab(ActiveTab tab) {
        if (getContext() == null) return;

        if (tab == ActiveTab.KEGIATAN) {
            // Mengatur tampilan tab aktif
            binding.layoutTabKegiatan.setBackgroundResource(R.drawable.border_card_utama);
            binding.textTabKegiatan.setTextColor(Color.WHITE);
            binding.iconTabKegiatan.setColorFilter(Color.WHITE);

            // Mengatur tampilan tab tidak aktif
            binding.layoutTabDonor.setBackgroundResource(R.drawable.border_card_white);
            binding.textTabDonor.setTextColor(ContextCompat.getColor(requireContext(), R.color.utama));
            binding.iconTabDonor.setColorFilter(ContextCompat.getColor(requireContext(), R.color.utama));

            // Mengatur visibilitas grup filter
            binding.filterGroupKegiatan.setVisibility(View.VISIBLE);
            binding.filterGroupDonorDarah.setVisibility(View.GONE);

            // Mengatur adapter dan memuat data
            binding.rvRiwayat.setAdapter(kegiatanAdapter);
            loadMyRegisteredKegiatan("Semua");

        } else { // DONOR_DARAH
            // Mengatur tampilan tab aktif
            binding.layoutTabDonor.setBackgroundResource(R.drawable.border_card_utama);
            binding.textTabDonor.setTextColor(Color.WHITE);
            binding.iconTabDonor.setColorFilter(Color.WHITE);

            // Mengatur tampilan tab tidak aktif
            binding.layoutTabKegiatan.setBackgroundResource(R.drawable.border_card_white);
            binding.textTabKegiatan.setTextColor(ContextCompat.getColor(requireContext(), R.color.utama));
            binding.iconTabKegiatan.setColorFilter(ContextCompat.getColor(requireContext(), R.color.utama));

            // Mengatur visibilitas grup filter
            binding.filterGroupKegiatan.setVisibility(View.GONE);
            binding.filterGroupDonorDarah.setVisibility(View.VISIBLE);

            // Mengatur adapter dan memuat data
            binding.rvRiwayat.setAdapter(donasiAdapter);
            loadMyDonationHistory("Semua");
        }
    }

    private void loadMyRegisteredKegiatan(String jenisFilter) {
        if (currentUser == null) return;
        binding.progressBar.setVisibility(View.VISIBLE);

        Query query = db.collection("kegiatan_acara")
                .whereArrayContains("pendaftar", currentUser.getUid());

        if (!"Semua".equals(jenisFilter)) {
            query = query.whereEqualTo("jenisKegiatan", jenisFilter);
        }

        query.orderBy("tanggalMulai", Query.Direction.DESCENDING).get().addOnSuccessListener(queryDocumentSnapshots -> {
            binding.progressBar.setVisibility(View.GONE);
            kegiatanList.clear();
            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                kegiatanList.add(doc.toObject(Kegiatan.class));
            }
            kegiatanAdapter.notifyDataSetChanged();
        }).addOnFailureListener(e -> {
            binding.progressBar.setVisibility(View.GONE);
            Log.w(TAG, "Error loading kegiatan history", e);
        });
    }

    private void loadMyDonationHistory(String peranFilter) {
        if (currentUser == null) return;
        binding.progressBar.setVisibility(View.VISIBLE);
        riwayatDonasiTampilList.clear();
        donasiAdapter.notifyDataSetChanged();

        db.collection("active_donations")
                .whereArrayContains("participants", currentUser.getUid())
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(prosesSnapshots -> {
                    if (prosesSnapshots.isEmpty()) {
                        binding.progressBar.setVisibility(View.GONE);
                        return;
                    }

                    List<Task<DocumentSnapshot>> tasks = new ArrayList<>();
                    List<ProsesDonor> processes = new ArrayList<>();

                    for(DocumentSnapshot prosesDoc : prosesSnapshots.getDocuments()){
                        ProsesDonor proses = prosesDoc.toObject(ProsesDonor.class);
                        processes.add(proses);

                        String otherUserId = Objects.equals(proses.getRequesterId(), currentUser.getUid()) ? proses.getDonorId() : proses.getRequesterId();
                        tasks.add(db.collection("users").document(otherUserId).get());
                        tasks.add(db.collection("donation_requests").document(proses.getRequestId()).get());
                    }

                    Tasks.whenAllSuccess(tasks).addOnSuccessListener(results -> {
                        List<RiwayatDonasiTampil> tempList = new ArrayList<>();
                        for(int i=0; i < processes.size(); i++){
                            ProsesDonor proses = processes.get(i);
                            User otherUser = ((DocumentSnapshot) results.get(i * 2)).toObject(User.class);
                            PermintaanDonor permintaan = ((DocumentSnapshot) results.get(i * 2 + 1)).toObject(PermintaanDonor.class);

                            if(otherUser != null && permintaan != null){
                                boolean sayaPendonor = currentUser.getUid().equals(proses.getDonorId());
                                if("Semua".equals(peranFilter) || ("Sebagai Pendonor".equals(peranFilter) && sayaPendonor) || ("Sebagai Penerima".equals(peranFilter) && !sayaPendonor)){
                                    RiwayatDonasiTampil item = new RiwayatDonasiTampil();
                                    item.setTanggal(proses.getTimestamp());
                                    item.setStatusProses(proses.getStatusProses());
                                    item.setNamaPasien(permintaan.getNamaPasien());
                                    item.setPeranSaya(sayaPendonor ? "Anda Membantu" : "Anda Dibantu oleh");
                                    item.setJudulTampilan(otherUser.getFullName());
                                    tempList.add(item);
                                }
                            }
                        }
                        riwayatDonasiTampilList.clear();
                        riwayatDonasiTampilList.addAll(tempList);
                        donasiAdapter.notifyDataSetChanged();
                        binding.progressBar.setVisibility(View.GONE);
                    });
                }).addOnFailureListener(e -> binding.progressBar.setVisibility(View.GONE));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}