package com.example.temudarah.fragment;

import android.annotation.SuppressLint;
import android.location.Location;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.temudarah.databinding.FragmentEditPermintaanBinding; // Gunakan binding untuk layout edit
import com.example.temudarah.model.PermintaanDonor;
import com.firebase.geofire.GeoFireUtils;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import java.util.HashMap;
import java.util.Map;

public class EditPermintaanFragment extends Fragment {

    private static final String TAG = "EditPermintaanFragment";
    private static final String ARG_REQUEST_ID = "request_id";

    private FragmentEditPermintaanBinding binding;
    private FirebaseFirestore db;
    private String requestId;
    private Location lastKnownLocation; // Untuk menyimpan lokasi jika diubah
    private FusedLocationProviderClient fusedLocationClient;
    // Launcher untuk izin bisa ditambahkan jika perlu, sama seperti di BuatPermintaanFragment

    public static EditPermintaanFragment newInstance(String requestId) {
        EditPermintaanFragment fragment = new EditPermintaanFragment();
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
        binding = FragmentEditPermintaanBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = FirebaseFirestore.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        setupDropdown();
        setupListeners();

        if (requestId != null) {
            loadRequestData();
        }
    }

    private void setupListeners() {
        binding.btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());
        // Tombol Simpan sekarang memanggil saveChanges
        binding.btnSimpanPerubahan.setOnClickListener(v -> saveChanges());
        // binding.btnGunakanLokasi.setOnClickListener(...) bisa ditambahkan jika ingin ada fitur ubah lokasi
    }

    private void setupDropdown() {
        String[] bloodTypes = {"A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, bloodTypes);
        binding.dropdownGolonganDarah.setAdapter(adapter);
    }

    /**
     * Memuat data permintaan yang ada dari Firestore.
     */
    private void loadRequestData() {
//        binding.progressBar.setVisibility(View.VISIBLE); // Asumsi ada ProgressBar di layout
        db.collection("donation_requests").document(requestId).get()
                .addOnSuccessListener(documentSnapshot -> {
//                    binding.progressBar.setVisibility(View.GONE);
                    if (documentSnapshot.exists()) {
                        PermintaanDonor permintaan = documentSnapshot.toObject(PermintaanDonor.class);
                        if (permintaan != null) {
                            populateForm(permintaan);
                        }
                    }
                })
                .addOnFailureListener(e -> {
//                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Gagal memuat data.", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Mengisi form dengan data yang sudah ada.
     */
    private void populateForm(PermintaanDonor permintaan) {
        binding.editNamaPasien.setText(permintaan.getNamaPasien());
        binding.dropdownGolonganDarah.setText(permintaan.getGolonganDarahDibutuhkan(), false);
        binding.editJumlahKantong.setText(String.valueOf(permintaan.getJumlahKantong()));
        binding.editNamaRs.setText(permintaan.getNamaRumahSakit());
        binding.editCatatan.setText(permintaan.getCatatan());

        if (permintaan.getLokasiRs() != null) {
            GeoPoint gp = permintaan.getLokasiRs();
            lastKnownLocation = new Location("");
            lastKnownLocation.setLatitude(gp.getLatitude());
            lastKnownLocation.setLongitude(gp.getLongitude());
            binding.tvLokasiTerpilih.setText(String.format("Lokasi tersimpan: Lat %.4f, Lng %.4f", gp.getLatitude(), gp.getLongitude()));
        }
    }

    /**
     * Menyimpan perubahan ke Firestore menggunakan .update()
     */
    private void saveChanges() {
        // Lakukan validasi input di sini jika perlu

        binding.btnSimpanPerubahan.setEnabled(false);
        binding.btnSimpanPerubahan.setText("Menyimpan...");

        String namaPasien = binding.editNamaPasien.getText().toString().trim();
        String golDarah = binding.dropdownGolonganDarah.getText().toString().trim();
        int jumlahKantong = Integer.parseInt(binding.editJumlahKantong.getText().toString().trim());
        String namaRs = binding.editNamaRs.getText().toString().trim();
        String catatan = binding.editCatatan.getText().toString().trim();

        DocumentReference requestRef = db.collection("donation_requests").document(requestId);

        Map<String, Object> updates = new HashMap<>();
        updates.put("namaPasien", namaPasien);
        updates.put("golonganDarahDibutuhkan", golDarah);
        updates.put("jumlahKantong", jumlahKantong);
        updates.put("namaRumahSakit", namaRs);
        updates.put("catatan", catatan);

        // Jika user memilih lokasi baru, update juga lokasi & geohash
        if (lastKnownLocation != null) {
            GeoPoint lokasiBaru = new GeoPoint(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
            String geohashBaru = GeoFireUtils.getGeoHashForLocation(new GeoLocation(lokasiBaru.getLatitude(), lokasiBaru.getLongitude()));
            updates.put("lokasiRs", lokasiBaru);
            updates.put("geohash", geohashBaru);
        }

        requestRef.update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Permintaan berhasil diperbarui.", Toast.LENGTH_SHORT).show();
                    getParentFragmentManager().popBackStack();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Gagal memperbarui permintaan.", Toast.LENGTH_SHORT).show();
                    binding.btnSimpanPerubahan.setEnabled(true);
                    binding.btnSimpanPerubahan.setText("Simpan Perubahan");
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}