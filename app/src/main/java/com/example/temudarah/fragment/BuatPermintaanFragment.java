package com.example.temudarah.fragment;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.DatePickerDialog; // Import DatePickerDialog
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

import java.util.Calendar; // Import Calendar
import java.util.Locale; // Import Locale
import java.text.SimpleDateFormat; // Import SimpleDateFormat

public class BuatPermintaanFragment extends Fragment {

    private static final String TAG = "BuatPermintaanFragment";
    private FragmentBuatPermintaanBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private FusedLocationProviderClient fusedLocationClient;
    private ActivityResultLauncher<String> requestPermissionLauncher;
    private Location lastKnownLocation;

    private String selectedBloodType; // To store selected blood type
    private String selectedGender; // To store selected gender

    private String[] bloodTypes = {"A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"};
    private String[] genderTypes = {"Laki-laki", "Perempuan"}; // Updated: Removed "Semua jenis"

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
        setupDropdowns();
        setupListeners();
        setupDatePicker(); // Call the new setupDatePicker method
    }

    private void setupDropdowns() {
        // Setup Blood Type Dropdown
        ArrayAdapter<String> bloodTypeAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, bloodTypes);
        binding.dropdownGolonganDarah.setAdapter(bloodTypeAdapter);
        binding.dropdownGolonganDarah.setOnItemClickListener((parent, view, position, id) -> {
            selectedBloodType = (String) parent.getItemAtPosition(position);
        });

        // Setup Gender Dropdown
        ArrayAdapter<String> genderAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, genderTypes);
        binding.dropdownJenisKelamin.setAdapter(genderAdapter);
        binding.dropdownJenisKelamin.setOnItemClickListener((parent, view, position, id) -> {
            selectedGender = (String) parent.getItemAtPosition(position);
        });
    }

    private void setupListeners() {
        binding.btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());
        binding.btnGunakanLokasi.setOnClickListener(v -> checkLocationPermissionAndGetData());
        binding.btnKirimPermintaan.setOnClickListener(v -> saveDonationRequest());
    }

    // New method to set up the DatePicker
    private void setupDatePicker() {
        // Ensure the TextInputEditText is clickable but not focusable for manual input
        binding.etTanggalPengumuman.setFocusable(false);
        binding.etTanggalPengumuman.setClickable(true);

        // Set OnClickListener to show DatePickerDialog
        binding.etTanggalPengumuman.setOnClickListener(v -> {
            final Calendar c = Calendar.getInstance();
            int year = c.get(Calendar.YEAR);
            int month = c.get(Calendar.MONTH);
            int day = c.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(requireContext(),
                    (view, selectedYear, selectedMonth, selectedDay) -> {
                        // Format the selected date and set it to the EditText
                        String formattedDate = String.format(Locale.getDefault(), "%02d/%02d/%04d", selectedDay, selectedMonth + 1, selectedYear);
                        binding.etTanggalPengumuman.setText(formattedDate);
                    }, year, month, day);
            datePickerDialog.show();
        });
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
            } else {
                Toast.makeText(getContext(), "Profil pengguna tidak ditemukan.", Toast.LENGTH_SHORT).show();
                binding.btnKirimPermintaan.setEnabled(true);
                binding.btnKirimPermintaan.setText("Kirim Permintaan");
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(getContext(), "Kesalahan saat memuat profil.", Toast.LENGTH_SHORT).show();
            binding.btnKirimPermintaan.setEnabled(true);
            binding.btnKirimPermintaan.setText("Kirim Permintaan");
            Log.e(TAG, "Error fetching user profile", e);
        });
    }

    private void createRequestObject(User pembuat) {
        String namaPasien = binding.editNamaPasien.getText().toString().trim();
        String jenisKelamin = (selectedGender != null && !selectedGender.isEmpty()) ? selectedGender : binding.dropdownJenisKelamin.getText().toString().trim();
        String golDarah = (selectedBloodType != null && !selectedBloodType.isEmpty()) ? selectedBloodType : binding.dropdownGolonganDarah.getText().toString().trim();
        int jumlahKantong = Integer.parseInt(binding.editJumlahKantong.getText().toString().trim());
        String namaRs = binding.editNamaRs.getText().toString().trim();
        String catatan = binding.editCatatan.getText().toString().trim();
        String tanggalPengumuman = binding.etTanggalPengumuman.getText().toString().trim(); // Get the announcement date

        PermintaanDonor permintaan = new PermintaanDonor();
        permintaan.setNamaPasien(namaPasien);
        permintaan.setJenisKelamin(jenisKelamin);
        permintaan.setGolonganDarahDibutuhkan(golDarah);
        permintaan.setJumlahKantong(jumlahKantong);
        permintaan.setNamaRumahSakit(namaRs);
        permintaan.setCatatan(catatan);
        permintaan.setPembuatUid(pembuat.getUid());
        permintaan.setNamaPembuat(pembuat.getFullName());
        permintaan.setFotoPembuatBase64(pembuat.getProfileImageBase64());
        permintaan.setTanggalPenguguman(tanggalPengumuman); // Set the announcement date
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
                    Log.e(TAG, "Error creating donation request", e);
                });
    }

    private boolean validateInput() {
        if (TextUtils.isEmpty(binding.editNamaPasien.getText().toString().trim())) {
            binding.editNamaPasien.setError("Nama pasien tidak boleh kosong");
            return false;
        }

        // Validate Jenis Kelamin
        String enteredGender = binding.dropdownJenisKelamin.getText().toString().trim();
        boolean isValidGender = false;
        if (!TextUtils.isEmpty(enteredGender)) {
            for (String type : genderTypes) { // Check against the updated genderTypes array
                if (type.equalsIgnoreCase(enteredGender)) {
                    isValidGender = true;
                    selectedGender = type;
                    break;
                }
            }
        }
        if (!isValidGender) {
            binding.dropdownJenisKelamin.setError("Silakan pilih jenis kelamin yang valid");
            Toast.makeText(getContext(), "Silakan pilih jenis kelamin yang valid.", Toast.LENGTH_SHORT).show();
            return false;
        }

        // Validate Golongan Darah
        String enteredBloodType = binding.dropdownGolonganDarah.getText().toString().trim();
        boolean isValidBloodType = false;
        if (!TextUtils.isEmpty(enteredBloodType)) {
            for (String type : bloodTypes) {
                if (type.equalsIgnoreCase(enteredBloodType)) {
                    isValidBloodType = true;
                    selectedBloodType = type;
                    break;
                }
            }
        }
        if (!isValidBloodType) {
            binding.dropdownGolonganDarah.setError("Silakan pilih golongan darah yang valid");
            Toast.makeText(getContext(), "Silakan pilih golongan darah yang valid.", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (TextUtils.isEmpty(binding.editJumlahKantong.getText().toString().trim())) {
            binding.editJumlahKantong.setError("Jumlah kantong tidak boleh kosong");
            return false;
        } else {
            try {
                int jumlah = Integer.parseInt(binding.editJumlahKantong.getText().toString().trim());
                if (jumlah <= 0) {
                    binding.editJumlahKantong.setError("Jumlah kantong harus lebih dari 0");
                    return false;
                }
            } catch (NumberFormatException e) {
                binding.editJumlahKantong.setError("Masukkan jumlah kantong yang valid");
                return false;
            }
        }

        if (TextUtils.isEmpty(binding.editNamaRs.getText().toString().trim())) {
            binding.editNamaRs.setError("Nama rumah sakit tidak boleh kosong");
            return false;
        }

        // Validate Tanggal Pengumuman
        if (TextUtils.isEmpty(binding.etTanggalPengumuman.getText().toString().trim())) {
            binding.etTanggalPengumuman.setError("Tanggal Pengumuman tidak boleh kosong");
            Toast.makeText(getContext(), "Silakan pilih tanggal pengumuman.", Toast.LENGTH_SHORT).show();
            return false;
        }


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
