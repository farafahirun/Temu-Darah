package com.example.temudarah.fragment;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
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

import com.example.temudarah.databinding.FragmentBuatPermintaanBinding;
import com.example.temudarah.model.PermintaanDonor;
import com.example.temudarah.model.User;
import com.example.temudarah.util.AlertUtil;
import com.firebase.geofire.GeoFireUtils;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class BuatPermintaanFragment extends Fragment {

    private static final String TAG = "BuatPermintaanFragment";
    private FragmentBuatPermintaanBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private FusedLocationProviderClient fusedLocationClient;
    private ActivityResultLauncher<String> requestPermissionLauncher;
    private Location lastKnownLocation;

    // Add a Calendar instance
    private Calendar calendar;

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

        calendar = Calendar.getInstance(); // Initialize calendar here

        setupDropdowns();
        setupListeners();

        // ✅ Tambahan ini untuk mengisi tanggal otomatis saat halaman dibuka
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        String currentDate = sdf.format(Calendar.getInstance().getTime());
        binding.editTanggalPenguguman.setText(currentDate);
    }

    private void setupDropdowns() {
        // Dropdown Golongan Darah
        String[] bloodTypes = {"A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"};
        ArrayAdapter<String> bloodAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, bloodTypes);
        binding.dropdownGolonganDarah.setAdapter(bloodAdapter);
        // Trik agar dropdown muncul saat diklik
        binding.dropdownGolonganDarah.setOnClickListener(v -> binding.dropdownGolonganDarah.showDropDown());

        // Dropdown Jenis Kelamin
        String[] genderTypes = {"Laki-laki", "Perempuan"};
        ArrayAdapter<String> genderAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, genderTypes);
        binding.dropdownJenisKelamin.setAdapter(genderAdapter);
        binding.dropdownJenisKelamin.setOnClickListener(v -> binding.dropdownJenisKelamin.showDropDown());
    }

    private void setupListeners() {
        binding.btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());
        binding.btnGunakanLokasi.setOnClickListener(v -> checkLocationPermissionAndGetData());
        binding.btnKirimPermintaan.setOnClickListener(v -> showConfirmationDialog());

        // --- ADD THIS BLOCK FOR DATE PICKER ---
        binding.editTanggalPenguguman.setOnClickListener(v -> showDatePickerDialog());
        // --- END ADDITION ---
    }

    // --- ADD THIS METHOD FOR DATE PICKER ---
    private void showDatePickerDialog() {
        DatePickerDialog.OnDateSetListener dateSetListener = (view, year, monthOfYear, dayOfMonth) -> {
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, monthOfYear);
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            updateDateInView();
        };

        new DatePickerDialog(requireContext(), dateSetListener,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void updateDateInView() {
        String myFormat = "dd/MM/yyyy"; // Or "yyyy-MM-dd", whatever format you prefer
        SimpleDateFormat sdf = new SimpleDateFormat(myFormat, Locale.US); // Use Locale.US for consistent date format
        binding.editTanggalPenguguman.setText(sdf.format(calendar.getTime()));
    }
    // --- END ADDITION ---

    @SuppressLint("MissingPermission")
    private void getCurrentUserLocation() {
        binding.btnGunakanLokasi.setEnabled(false);
        binding.btnGunakanLokasi.setText("Mencari GPS...");
        fusedLocationClient.getLastLocation().addOnSuccessListener(requireActivity(), location -> {
            binding.btnGunakanLokasi.setEnabled(true);
            binding.btnGunakanLokasi.setText("Gunakan Lokasi Saat Ini");
            if (location != null) {
                lastKnownLocation = location;
                getAddressFromLocation(location); // Panggil fungsi baru untuk dapatkan alamat
            } else {
                Toast.makeText(getContext(), "Gagal mendapatkan lokasi. Pastikan GPS aktif.", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void getAddressFromLocation(Location location) {
        binding.tvLokasiTerpilih.setText(String.format(Locale.getDefault(), "Mencari alamat untuk Lat: %.4f, Lng: %.4f...", location.getLatitude(), location.getLongitude()));

        // Geocoder perlu dijalankan di background thread agar UI tidak macet
        new Thread(() -> {
            if (getContext() == null) return;
            try {
                Geocoder geocoder = new Geocoder(getContext(), Locale.getDefault());
                List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (addresses != null && !addresses.isEmpty()) {
                            Address address = addresses.get(0);
                            String addressLine = address.getAddressLine(0);
                            binding.editNamaRs.setText(addressLine); // Otomatis isi ke kolom RS
                            binding.tvLokasiTerpilih.setText("Lokasi ditemukan: " + addressLine);
                            Toast.makeText(getContext(), "Alamat berhasil ditemukan!", Toast.LENGTH_SHORT).show();
                        } else {
                            binding.tvLokasiTerpilih.setText("Alamat tidak ditemukan.");
                        }
                    });
                }
            } catch (IOException e) {
                Log.e(TAG, "Geocoder error", e);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> binding.tvLokasiTerpilih.setText("Gagal mendapatkan alamat dari koordinat."));
                }
            }
        }).start();
    }

    private void checkLocationPermissionAndGetData() {
        if (getContext() != null && ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            getCurrentUserLocation();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    private void showConfirmationDialog() {
        if (getContext() != null) {
            // Validasi input terlebih dahulu
            if (!validateInput() || currentUser == null) {
                if (currentUser == null) Toast.makeText(getContext(), "Anda harus login.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Jika valid, tampilkan konfirmasi
            AlertUtil.showAlert(getContext(),
                    "Konfirmasi Permintaan",
                    "Apakah Anda yakin ingin mengirim permintaan donor darah ini?",
                    "Kirim", "Batal",
                    v -> saveDonationRequest(),
                    null);
        }
    }

    private void saveDonationRequest() {
        binding.btnKirimPermintaan.setEnabled(false);
        binding.btnKirimPermintaan.setText("Mengirim...");

        db.collection("users").document(currentUser.getUid()).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                User user = documentSnapshot.toObject(User.class);
                if (user != null) {
                    createRequestObject(user);
                } else {
                    handleSaveFailure("Gagal memuat data profil Anda.");
                }
            } else {
                handleSaveFailure("Data profil Anda tidak ditemukan.");
            }
        }).addOnFailureListener(e -> handleSaveFailure("Gagal memuat profil."));
    }

    private void createRequestObject(User pembuat) {
        String namaPasien = binding.editNamaPasien.getText().toString().trim();
        String golDarah = binding.dropdownGolonganDarah.getText().toString().trim();
        String jenisKelamin = binding.dropdownJenisKelamin.getText().toString().trim(); // Ambil data gender
        int jumlahKantong = Integer.parseInt(binding.editJumlahKantong.getText().toString().trim());
        String namaRs = binding.editNamaRs.getText().toString().trim();
        String catatan = binding.editCatatan.getText().toString().trim();
        String tanggalPengumuman = binding.editTanggalPenguguman.getText().toString().trim(); // Get the selected date

        PermintaanDonor permintaan = new PermintaanDonor();
        // Mengisi semua field, termasuk yang baru
        permintaan.setNamaPasien(namaPasien);
        permintaan.setGolonganDarahDibutuhkan(golDarah);
        permintaan.setJenisKelamin(jenisKelamin);
        permintaan.setJumlahKantong(jumlahKantong);
        permintaan.setNamaRumahSakit(namaRs);
        permintaan.setCatatan(catatan);
        permintaan.setTanggalPenguguman(tanggalPengumuman); // Set the date
        permintaan.setPembuatUid(pembuat.getUid());
        permintaan.setNamaPembuat(pembuat.getFullName());
        permintaan.setFotoPembuatBase64(pembuat.getProfileImageBase64());
        GeoPoint lokasi = new GeoPoint(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
        permintaan.setLokasiRs(lokasi);
        permintaan.setGeohash(GeoFireUtils.getGeoHashForLocation(new GeoLocation(lokasi.getLatitude(), lokasi.getLongitude())));
        permintaan.setStatus("Aktif");
        permintaan.setWaktuDibuat(Timestamp.now());

        db.collection("donation_requests").add(permintaan)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(getContext(), "Permintaan donor berhasil dibuat!", Toast.LENGTH_SHORT).show();
                    getParentFragmentManager().popBackStack();
                })
                .addOnFailureListener(e -> handleSaveFailure("Gagal membuat permintaan."));
    }

    private boolean validateInput() {
        if (TextUtils.isEmpty(binding.editNamaPasien.getText())) {
            binding.editNamaPasien.setError("Nama Pasien wajib diisi"); return false;
        }
        if (TextUtils.isEmpty(binding.dropdownGolonganDarah.getText())) {
            binding.dropdownGolonganDarah.setError("Pilih Golongan Darah"); return false;
        }
        if (TextUtils.isEmpty(binding.dropdownJenisKelamin.getText())) {
            binding.dropdownJenisKelamin.setError("Pilih Jenis Kelamin"); return false;
        }
        if (TextUtils.isEmpty(binding.editJumlahKantong.getText())) {
            binding.editJumlahKantong.setError("Jumlah kantong wajib diisi"); return false;
        }
        if (TextUtils.isEmpty(binding.editTanggalPenguguman.getText())) { // Add validation for the date field
            binding.editTanggalPenguguman.setError("Tanggal Pengumuman wajib diisi"); return false;
        }
        if (lastKnownLocation == null) {
            Toast.makeText(getContext(), "Harap tentukan lokasi dengan menekan tombol GPS.", Toast.LENGTH_LONG).show();
            return false;
        }
        return true;
    }

    private void handleSaveFailure(String message) {
        if (getContext() == null) return;
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        binding.btnKirimPermintaan.setEnabled(true);
        binding.btnKirimPermintaan.setText("Kirim Permintaan");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}