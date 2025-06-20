package com.example.temudarah.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.temudarah.R;
import com.example.temudarah.adapter.KegiatanAdapter;
import com.example.temudarah.databinding.FragmentKegiatanBinding;
import com.example.temudarah.model.Kegiatan;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration; // <-- IMPORT BARU
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class KegiatanFragment extends Fragment {

    private static final String TAG = "KegiatanFragment";
    private FragmentKegiatanBinding binding;
    private FirebaseFirestore db;
    private KegiatanAdapter adapter;
    private List<Kegiatan> kegiatanList;

    // --- VARIABEL BARU UNTUK MENGELOLA LISTENER REAL-TIME ---
    private ListenerRegistration kegiatanListener;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentKegiatanBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = FirebaseFirestore.getInstance();
        setupRecyclerView();
        // Fungsi load data sekarang akan dijalankan di onStart
    }

    @Override
    public void onStart() {
        super.onStart();
        // Mulai mendengarkan perubahan data saat fragment terlihat oleh pengguna
        listenToKegiatanData();
    }

    @Override
    public void onStop() {
        super.onStop();
        // Hentikan listener saat pengguna meninggalkan fragment untuk hemat baterai & data
        if (kegiatanListener != null) {
            kegiatanListener.remove();
        }
    }

    private void setupRecyclerView() {
        kegiatanList = new ArrayList<>();
        adapter = new KegiatanAdapter(kegiatanList, kegiatan -> {
            if (kegiatan.getKegiatanId() != null) {
                Fragment detailFragment = DetailKegiatanFragment.newInstance(kegiatan.getKegiatanId());
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, detailFragment)
                        .addToBackStack(null)
                        .commit();
            }
        });
        binding.rvKegiatan.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvKegiatan.setAdapter(adapter);
    }

    /**
     * FUNGSI YANG DIMODIFIKASI: Sekarang menggunakan listener real-time.
     */
    // Di dalam KegiatanFragment.java

    private void listenToKegiatanData() {
        binding.progressBar.setVisibility(View.VISIBLE);
        Log.d(TAG, "Mulai mendengarkan data kegiatan...");

        Query query = db.collection("kegiatan_acara")
                .orderBy("tanggalMulai", Query.Direction.ASCENDING);

        kegiatanListener = query.addSnapshotListener((queryDocumentSnapshots, error) -> {
            binding.progressBar.setVisibility(View.GONE);

            // ---- BAGIAN PENTING UNTUK DEBUGGING ----
            // Cek apakah ada error dari Firestore
            if (error != null) {
                Log.e(TAG, "Listen failed:", error);
                Toast.makeText(getContext(), "Error: " + error.getMessage(), Toast.LENGTH_LONG).show();
                return;
            }

            // Cek apakah data yang diterima tidak null
            if (queryDocumentSnapshots != null) {
                Log.d(TAG, "Data diterima, jumlah dokumen: " + queryDocumentSnapshots.size());
                kegiatanList.clear();
                for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                    Kegiatan kegiatan = doc.toObject(Kegiatan.class);
                    kegiatan.setKegiatanId(doc.getId());
                    kegiatanList.add(kegiatan);
                    Log.d(TAG, "Menambahkan kegiatan: " + kegiatan.getJudul());
                }
                adapter.notifyDataSetChanged();

                if (kegiatanList.isEmpty()) {
                    Log.d(TAG, "List kegiatan kosong setelah diproses.");
                    binding.tvEmptyState.setVisibility(View.VISIBLE);
                } else {
                    binding.tvEmptyState.setVisibility(View.GONE);
                }
            } else {
                Log.w(TAG, "Data diterima, tapi snapshot-nya null.");
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}