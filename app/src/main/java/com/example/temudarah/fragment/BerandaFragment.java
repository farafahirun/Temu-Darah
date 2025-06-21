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
import com.firebase.geofire.GeoQueryBounds;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import java.util.ArrayList;
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

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) getCurrentUserLocation();
            else Toast.makeText(getContext(), "Izin lokasi sangat penting untuk fitur ini.", Toast.LENGTH_LONG).show();
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
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

        String[] goldarOptions = {"Semua Gol. Darah", "A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-", "Rh-null"};
        ArrayAdapter<String> goldarAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, goldarOptions);
        binding.autoCompleteGolonganDarah.setAdapter(goldarAdapter);
        binding.autoCompleteGolonganDarah.setText(goldarOptions[0], false);
    }

    private void setupListeners() {
        binding.ivNotification.setOnClickListener(v -> navigateTo(new NotifikasiFragment()));
        binding.btnBuatPermintaan.setOnClickListener(v -> navigateTo(new BuatPermintaanFragment()));
        binding.filterBeranda.setOnClickListener(v -> {
            if (lastKnownLocation != null) {
                loadDonationRequests(lastKnownLocation);
            } else {
                checkLocationPermissionAndLoadData();
                Toast.makeText(getContext(), "Mencari lokasi Anda terlebih dahulu...", Toast.LENGTH_SHORT).show();
            }
        });
        binding.btnPermintaanSaya.setOnClickListener(v ->
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new PermintaanSayaFragment())
                        .addToBackStack(null)
                        .commit()
        );
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
        showLoading(true);
        if (getActivity() == null) return;
        fusedLocationClient.getLastLocation().addOnSuccessListener(requireActivity(), location -> {
            if (location != null) {
                lastKnownLocation = location;
                adapter.updateUserLocation(location);
                Log.d(TAG, "Lokasi ditemukan. Memuat permintaan...");
                loadDonationRequests(location);
            } else {
                showLoading(false);
                Toast.makeText(getContext(), "Gagal mendapatkan lokasi. Pastikan GPS aktif.", Toast.LENGTH_LONG).show();
            }
        });
    }

    // Di dalam class BerandaFragment.java

    private void loadDonationRequests(final Location location) {
        showLoading(true);
        if (currentUser == null) {
            Log.e(TAG, "Tidak bisa memuat data, pengguna saat ini null.");
            showLoading(false);
            return;
        }

        String genderFilter = binding.autoCompleteGender.getText().toString();
        String bloodTypeFilter = binding.autoCompleteGolonganDarah.getText().toString();
        final GeoLocation center = new GeoLocation(location.getLatitude(), location.getLongitude());
        final double radiusInM = 50 * 1000;

        List<GeoQueryBounds> bounds = GeoFireUtils.getGeoHashQueryBounds(center, radiusInM);
        final List<Task<QuerySnapshot>> tasks = new ArrayList<>();

        for (GeoQueryBounds b : bounds) {
            Query q = db.collection("donation_requests")
                    .whereEqualTo("status", "Aktif")
                    // --- BARIS whereNotEqualTo DIHAPUS DARI SINI ---
                    .orderBy("geohash")
                    .startAt(b.startHash)
                    .endAt(b.endHash);

            if (!"Semua Gender".equals(genderFilter)) {
                // Asumsi field gender pasien ada di PermintaanDonor
                // q = q.whereEqualTo("genderPasien", genderFilter);
            }
            if (!"Semua Gol. Darah".equals(bloodTypeFilter)) {
                q = q.whereEqualTo("golonganDarahDibutuhkan", bloodTypeFilter);
            }
            tasks.add(q.get());
        }

        Tasks.whenAllComplete(tasks).addOnCompleteListener(t -> {
            List<PermintaanDonor> matchingDocs = new ArrayList<>();
            for (Task<QuerySnapshot> task : tasks) {
                if (task.isSuccessful()) {
                    for (DocumentSnapshot doc : task.getResult()) {
                        PermintaanDonor permintaan = doc.toObject(PermintaanDonor.class);
                        if (permintaan != null) {
                            // --- FILTER MANUAL DILAKUKAN DI SINI ---
                            // Cek apakah UID pembuat TIDAK SAMA dengan UID pengguna saat ini
                            if (!permintaan.getPembuatUid().equals(currentUser.getUid())) {
                                // Jika bukan postingan sendiri, baru lakukan pengecekan jarak
                                GeoPoint geoPoint = doc.getGeoPoint("lokasiRs");
                                if (geoPoint != null) {
                                    GeoLocation docLocation = new GeoLocation(geoPoint.getLatitude(), geoPoint.getLongitude());
                                    double distanceInM = GeoFireUtils.getDistanceBetween(docLocation, center);
                                    if (distanceInM <= radiusInM) {
                                        permintaan.setRequestId(doc.getId());
                                        matchingDocs.add(permintaan);
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Log.w(TAG, "Query gagal: ", task.getException());
                }
            }

            showLoading(false);
            permintaanList.clear();
            permintaanList.addAll(matchingDocs);
            adapter.notifyDataSetChanged();
            updateEmptyState();
        });
    }

    private void showLoading(boolean isLoading) {
        if (binding != null) {
            binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
    }

    private void updateEmptyState() {
        if (permintaanList.isEmpty()) {
            binding.tvNoData.setVisibility(View.VISIBLE);
            binding.rvPendonor.setVisibility(View.GONE);
        } else {
            binding.tvNoData.setVisibility(View.GONE);
            binding.rvPendonor.setVisibility(View.VISIBLE);
        }
    }

    private void navigateTo(Fragment fragment) {
        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}