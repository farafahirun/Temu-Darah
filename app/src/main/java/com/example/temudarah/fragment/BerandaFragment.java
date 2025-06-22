package com.example.temudarah.fragment;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.temudarah.R;
import com.example.temudarah.adapter.PermintaanDonorAdapter;
import com.example.temudarah.databinding.FragmentBerandaBinding;
import com.example.temudarah.model.PermintaanDonor;
import com.firebase.geofire.GeoFireUtils;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BerandaFragment extends Fragment {

    private static final String TAG = "BerandaFragment";
    private FragmentBerandaBinding binding;
    private FirebaseFirestore db;
    private FusedLocationProviderClient fusedLocationClient;
    private ActivityResultLauncher<String> requestPermissionLauncher;
    private PermintaanDonorAdapter adapter;
    private List<PermintaanDonor> permintaanList;
    private Location lastKnownLocation;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;

    private enum UiState { LOADING, HAS_DATA, EMPTY }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) getCurrentUserLocation();
            else Toast.makeText(getContext(), "Izin lokasi sangat penting untuk fitur ini.", Toast.LENGTH_LONG).show();
        });
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentBerandaBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        db = FirebaseFirestore.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        setupRecyclerView();
        setupFilters();
        setupListeners();
    }

    @Override
    public void onResume() {
        super.onResume();
        checkLocationPermissionAndLoadData();
    }

    private void setupRecyclerView() {
        permintaanList = new ArrayList<>();
        adapter = new PermintaanDonorAdapter(permintaanList, null, permintaan -> {
            if (permintaan.getRequestId() != null && !permintaan.getRequestId().isEmpty()) {
                Fragment detailFragment = DetailPermintaanFragment.newInstance(permintaan.getRequestId());
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, detailFragment)
                        .addToBackStack(null)
                        .commit();
            }
        });
        binding.rvPendonor.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvPendonor.setAdapter(adapter);
    }

    private void setupFilters() {
        String[] genderOptions = {"Semua Gender", "Laki-laki", "Perempuan"};
        ArrayAdapter<String> genderAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, genderOptions);
        binding.autoCompleteGender.setAdapter(genderAdapter);
        binding.autoCompleteGender.setText(genderOptions[0], false);

        String[] goldarOptions = {"Semua Gol. Darah", "A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"};
        ArrayAdapter<String> goldarAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, goldarOptions);
        binding.autoCompleteGolonganDarah.setAdapter(goldarAdapter);
        binding.autoCompleteGolonganDarah.setText(goldarOptions[0], false);
    }

    private void setupListeners() {
        binding.ivNotification.setOnClickListener(v -> navigateTo(new NotifikasiFragment()));
        binding.btnBuatPermintaan.setOnClickListener(v -> navigateTo(new BuatPermintaanFragment()));
        binding.btnPermintaanSaya.setOnClickListener(v -> navigateTo(new PermintaanSayaFragment()));

        binding.filterBeranda.setOnClickListener(v -> { // Asumsi ID ikon search adalah filterBeranda
            if (lastKnownLocation != null) {
                loadDonationRequests(lastKnownLocation);
            } else {
                checkLocationPermissionAndLoadData();
            }
        });
    }

    private void checkLocationPermissionAndLoadData() {
        if (getContext() != null && ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            getCurrentUserLocation();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    @SuppressLint("MissingPermission")
    private void getCurrentUserLocation() {
        updateUiState(UiState.LOADING, "");
        if (getActivity() == null) return;
        fusedLocationClient.getLastLocation().addOnSuccessListener(requireActivity(), location -> {
            if (location != null) {
                lastKnownLocation = location;
                adapter.updateUserLocation(location);
                loadDonationRequests(location);
            } else {
                updateUiState(UiState.EMPTY, "Gagal mendapatkan lokasi. Pastikan GPS aktif.");
            }
        });
    }

    private void loadDonationRequests(final Location location) {
        if (currentUser == null) {
            updateUiState(UiState.EMPTY, "Anda perlu login.");
            return;
        }
        updateUiState(UiState.LOADING, "");

        String genderFilter = binding.autoCompleteGender.getText().toString();
        String bloodTypeFilter = binding.autoCompleteGolonganDarah.getText().toString();
        final GeoLocation center = new GeoLocation(location.getLatitude(), location.getLongitude());
        final double radiusInM = 50 * 1000; // 50 km

        // 1. Ambil SEMUA permintaan yang statusnya "Aktif"
        db.collection("donation_requests")
                .whereEqualTo("status", "Aktif")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!isAdded()) return;

                    List<PermintaanDonor> allActiveRequests = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        PermintaanDonor permintaan = doc.toObject(PermintaanDonor.class);
                        if (permintaan != null) {
                            permintaan.setRequestId(doc.getId());
                            allActiveRequests.add(permintaan);
                        }
                    }

                    // 2. Lakukan semua filter di sisi aplikasi
                    List<PermintaanDonor> filteredList = new ArrayList<>();
                    for (PermintaanDonor p : allActiveRequests) {
                        // Filter 1: Bukan postingan sendiri
                        if (p.getPembuatUid() != null && p.getPembuatUid().equals(currentUser.getUid())) {
                            continue;
                        }

                        // Filter 2: Jarak
                        GeoPoint rsLocation = p.getLokasiRs();
                        if (rsLocation != null) {
                            double distanceInM = GeoFireUtils.getDistanceBetween(new GeoLocation(rsLocation.getLatitude(), rsLocation.getLongitude()), center);
                            if (distanceInM > radiusInM) {
                                continue;
                            }
                        }

                        // Filter 3: Golongan Darah
                        if (!"Semua Gol. Darah".equals(bloodTypeFilter) && p.getGolonganDarahDibutuhkan() != null && !p.getGolonganDarahDibutuhkan().equalsIgnoreCase(bloodTypeFilter)) {
                            continue;
                        }

                        // Filter 4: Gender Pasien
                        if (!"Semua Gender".equals(genderFilter) && p.getJenisKelamin() != null && !p.getJenisKelamin().equalsIgnoreCase(genderFilter)) {
                            continue;
                        }

                        // Jika lolos semua filter, tambahkan ke daftar
                        filteredList.add(p);
                    }

                    // Urutkan berdasarkan jarak terdekat
                    Collections.sort(filteredList, (p1, p2) -> {
                        GeoPoint p1Loc = p1.getLokasiRs();
                        GeoPoint p2Loc = p2.getLokasiRs();
                        if (p1Loc == null || p2Loc == null) return 0;

                        double dist1 = GeoFireUtils.getDistanceBetween(new GeoLocation(p1Loc.getLatitude(), p1Loc.getLongitude()), center);
                        double dist2 = GeoFireUtils.getDistanceBetween(new GeoLocation(p2Loc.getLatitude(), p2Loc.getLongitude()), center);
                        return Double.compare(dist1, dist2);
                    });

                    permintaanList.clear();
                    permintaanList.addAll(filteredList);
                    adapter.notifyDataSetChanged();
                    updateUiState(permintaanList.isEmpty() ? UiState.EMPTY : UiState.HAS_DATA, "Tidak ada permintaan terdekat yang cocok.");
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    updateUiState(UiState.EMPTY, "Gagal memuat data.");
                    Log.e(TAG, "Error loading donation requests", e);
                });
    }

    private void navigateTo(Fragment fragment) {
        if (getParentFragmentManager() != null) {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(null)
                    .commit();
        }
    }

    private void updateUiState(UiState state, String message) {
        if (binding == null) return;
        binding.progressBar.setVisibility(state == UiState.LOADING ? View.VISIBLE : View.GONE);
        binding.rvPendonor.setVisibility(state == UiState.HAS_DATA ? View.VISIBLE : View.GONE);
        binding.tvNoData.setVisibility(state == UiState.EMPTY ? View.VISIBLE : View.GONE);
        if (state == UiState.EMPTY) {
            binding.tvNoData.setText(message);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}