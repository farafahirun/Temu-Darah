package com.example.temudarah.fragment;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.example.temudarah.R;
import com.example.temudarah.databinding.FragmentBuatPermintaanBinding;
import com.example.temudarah.model.PermintaanDonor;
import com.example.temudarah.model.User;
import com.firebase.geofire.GeoFireUtils;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;

public class BuatPermintaanFragment extends Fragment {

    private static final String TAG = "BuatPermintaanFragment";
    private FragmentBuatPermintaanBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private FusedLocationProviderClient fusedLocationClient;
    private ActivityResultLauncher<String> requestPermissionLauncher;
    private Location lastKnownLocation;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) getCurrentUserLocation();
            else Toast.makeText(getContext(), "Izin lokasi diperlukan.", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentBuatPermintaanBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
        setupDropdown();
        setupListeners();
    }

    private void setupDropdown() {
        String[] bloodTypes = {"A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, bloodTypes);
        binding.dropdownGolonganDarah.setAdapter(adapter);
    }

    private void setupListeners() {
        binding.btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());
        binding.btnGunakanLokasi.setOnClickListener(v -> checkLocationPermissionAndGetData());
        binding.btnKirimPermintaan.setOnClickListener(v -> saveDonationRequest());
    }

    private void checkLocationPermissionAndGetData() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            getCurrentUserLocation();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    @SuppressLint("MissingPermission")
    private void getCurrentUserLocation() {
        binding.btnGunakanLokasi.setEnabled(false);
        binding.btnGunakanLokasi.setText("Mencari GPS...");
        fusedLocationClient.getLastLocation().addOnSuccessListener(requireActivity(), location -> {
            if (location != null) {
                lastKnownLocation = location;
                binding.tvLokasiTerpilih.setText(String.format("Lokasi didapat: Lat %.4f, Lng %.4f", location.getLatitude(), location.getLongitude()));
                Toast.makeText(getContext(), "Lokasi berhasil didapatkan!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "Gagal mendapatkan lokasi. Pastikan GPS aktif.", Toast.LENGTH_LONG).show();
            }
            binding.btnGunakanLokasi.setEnabled(true);
            binding.btnGunakanLokasi.setText("Gunakan Lokasi Saat Ini (GPS)");
        });
    }

    private void saveDonationRequest() {
        if (!validateInput()) return;
        if (currentUser == null) {
            Toast.makeText(getContext(), "Anda harus login untuk membuat permintaan.", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.btnKirimPermintaan.setEnabled(false);
        binding.btnKirimPermintaan.setText("Mengirim...");

        db.collection("users").document(currentUser.getUid()).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                User user = documentSnapshot.toObject(User.class);
                if (user != null) {
                    createRequestObject(user);
                } else {
                    Toast.makeText(getContext(), "Gagal memuat data profil Anda.", Toast.LENGTH_SHORT).show();
                    binding.btnKirimPermintaan.setEnabled(true);
                    binding.btnKirimPermintaan.setText("Kirim Permintaan");
                }
            }
        });
    }

    private void createRequestObject(User pembuat) {
        String namaPasien = binding.editNamaPasien.getText().toString().trim();
        String golDarah = binding.dropdownGolonganDarah.getText().toString().trim();
        int jumlahKantong = Integer.parseInt(binding.editJumlahKantong.getText().toString().trim());
        String namaRs = binding.editNamaRs.getText().toString().trim();
        String catatan = binding.editCatatan.getText().toString().trim();

        PermintaanDonor permintaan = new PermintaanDonor();
        permintaan.setNamaPasien(namaPasien);
        permintaan.setGolonganDarahDibutuhkan(golDarah);
        permintaan.setJumlahKantong(jumlahKantong);
        permintaan.setNamaRumahSakit(namaRs);
        permintaan.setCatatan(catatan);
        permintaan.setPembuatUid(pembuat.getUid());
        permintaan.setNamaPembuat(pembuat.getFullName());
        permintaan.setFotoPembuatBase64(pembuat.getProfileImageBase64());
        GeoPoint lokasi = new GeoPoint(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
        permintaan.setLokasiRs(lokasi);
        String geohash = GeoFireUtils.getGeoHashForLocation(new GeoLocation(lokasi.getLatitude(), lokasi.getLongitude()));
        permintaan.setGeohash(geohash);
        permintaan.setStatus("Aktif");
        permintaan.setWaktuDibuat(Timestamp.now());

        db.collection("donation_requests").add(permintaan)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(getContext(), "Permintaan donor berhasil dibuat!", Toast.LENGTH_SHORT).show();
                    getParentFragmentManager().popBackStack();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Gagal membuat permintaan.", Toast.LENGTH_SHORT).show();
                    binding.btnKirimPermintaan.setEnabled(true);
                    binding.btnKirimPermintaan.setText("Kirim Permintaan");
                });
    }

    private boolean validateInput() {
        if (TextUtils.isEmpty(binding.editNamaPasien.getText())) { /*...*/ return false; }
        if (TextUtils.isEmpty(binding.dropdownGolonganDarah.getText())) { /*...*/ return false; }
        if (TextUtils.isEmpty(binding.editJumlahKantong.getText())) { /*...*/ return false; }
        if (lastKnownLocation == null) {
            Toast.makeText(getContext(), "Harap tentukan lokasi RS dengan menekan tombol GPS.", Toast.LENGTH_LONG).show();
            return false;
        }
        return true;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}