package com.example.temudarah.fragment;

import android.Manifest;
import android.annotation.SuppressLint;
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

import com.example.temudarah.databinding.FragmentEditPermintaanBinding;
import com.example.temudarah.model.PermintaanDonor;
import com.firebase.geofire.GeoFireUtils;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class EditPermintaanFragment extends Fragment {

    private static final String TAG = "EditPermintaanFragment";
    private static final String ARG_REQUEST_ID = "request_id";

    private FragmentEditPermintaanBinding binding;
    private FirebaseFirestore db;
    private String requestId;
    private Location lastKnownLocation; // Menyimpan lokasi baru jika diubah
    private FusedLocationProviderClient fusedLocationClient;
    private ActivityResultLauncher<String> requestPermissionLauncher;

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
        // Inisialisasi launcher untuk izin lokasi
        requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) getCurrentUserLocation();
            else Toast.makeText(getContext(), "Izin lokasi diperlukan.", Toast.LENGTH_SHORT).show();
        });
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

        setupDropdowns();
        setupListeners();

        if (requestId != null) {
            loadRequestData();
        }
    }

    private void setupDropdowns() {
        String[] bloodTypes = {"A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"};
        ArrayAdapter<String> bloodAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, bloodTypes);
        binding.dropdownGolonganDarah.setAdapter(bloodAdapter);
        binding.dropdownGolonganDarah.setOnClickListener(v -> binding.dropdownGolonganDarah.showDropDown());

        String[] genderTypes = {"Laki-laki", "Perempuan"};
        ArrayAdapter<String> genderAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, genderTypes);
        binding.dropdownJenisKelamin.setAdapter(genderAdapter);
        binding.dropdownJenisKelamin.setOnClickListener(v -> binding.dropdownJenisKelamin.showDropDown());
    }

    private void setupListeners() {
        binding.btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());
        binding.btnSimpanPerubahan.setOnClickListener(v -> saveChanges());
        binding.btnGunakanLokasi.setOnClickListener(v -> checkLocationPermission());
    }

    private void loadRequestData() {
        binding.progressBar.setVisibility(View.VISIBLE);
        db.collection("donation_requests").document(requestId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    binding.progressBar.setVisibility(View.GONE);
                    if (isAdded() && documentSnapshot.exists()) {
                        PermintaanDonor permintaan = documentSnapshot.toObject(PermintaanDonor.class);
                        if (permintaan != null) {
                            populateForm(permintaan);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    if(isAdded()) binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Gagal memuat data.", Toast.LENGTH_SHORT).show();
                });
    }

    private void populateForm(PermintaanDonor permintaan) {
        binding.editNamaPasien.setText(permintaan.getNamaPasien());
        binding.dropdownGolonganDarah.setText(permintaan.getGolonganDarahDibutuhkan(), false);
        binding.dropdownJenisKelamin.setText(permintaan.getJenisKelamin(), false);
        binding.editJumlahKantong.setText(String.valueOf(permintaan.getJumlahKantong()));
        binding.editNamaRs.setText(permintaan.getNamaRumahSakit());
        binding.editCatatan.setText(permintaan.getCatatan());

        // Simpan lokasi awal untuk digunakan jika tidak ada perubahan lokasi
        if (permintaan.getLokasiRs() != null) {
            GeoPoint gp = permintaan.getLokasiRs();
            lastKnownLocation = new Location("");
            lastKnownLocation.setLatitude(gp.getLatitude());
            lastKnownLocation.setLongitude(gp.getLongitude());
            binding.tvLokasiTerpilih.setText("Lokasi tersimpan. Tekan tombol untuk mengubah.");
        }
    }

    private void checkLocationPermission() {
        if (getContext() != null && ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            getCurrentUserLocation();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    @SuppressLint("MissingPermission")
    private void getCurrentUserLocation() {
        binding.btnGunakanLokasi.setText("Mencari...");
        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (isAdded()) {
                binding.btnGunakanLokasi.setText("Gunakan Lokasi Saat Ini");
                if (location != null) {
                    lastKnownLocation = location; // Update lastKnownLocation dengan koordinat baru
                    getAddressFromLocation(location);
                }
            }
        });
    }

    private void getAddressFromLocation(Location location) {
        try {
            Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());

            // Langsung tampilkan pesan bahwa sistem sedang mencari alamat
            binding.tvLokasiTerpilih.setText(String.format("Lokasi: Lat %.4f, Lng %.4f\nMencari alamat lengkap...",
                    location.getLatitude(), location.getLongitude()));

            // Pendekatan yang berfungsi di semua versi Android
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                // Untuk Android 13 (API 33) ke atas menggunakan callback
                geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1, addresses -> {
                    if (getContext() != null) {  // Pastikan fragment masih attached
                        processAddressResult(addresses, location);
                    }
                });
            } else {
                // Untuk Android 12 (API 32) ke bawah menggunakan metode sinkronus
                List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                processAddressResult(addresses, location);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting address: ", e);
            // Tampilkan pesan error tetapi tetap pertahankan informasi koordinat yang sudah ada
            binding.tvLokasiTerpilih.setText(String.format("Lokasi: Lat %.4f, Lng %.4f\nGagal mendapatkan alamat lengkap",
                    location.getLatitude(), location.getLongitude()));
        }
    }

    private void processAddressResult(List<Address> addresses, Location location) {
        try {
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                StringBuilder addressText = new StringBuilder();

                // Get the complete address line
                if (address.getMaxAddressLineIndex() >= 0) {
                    for (int i = 0; i <= address.getMaxAddressLineIndex(); i++) {
                        String addressLine = address.getAddressLine(i);
                        if (addressLine != null) {
                            addressText.append(addressLine);
                            if (i < address.getMaxAddressLineIndex()) {
                                addressText.append(", ");
                            }
                        }
                    }
                } else {
                    // Jika tidak ada address line, coba dapatkan komponen alamat lainnya
                    if (address.getLocality() != null) addressText.append(address.getLocality()).append(", ");
                    if (address.getSubAdminArea() != null) addressText.append(address.getSubAdminArea()).append(", ");
                    if (address.getAdminArea() != null) addressText.append(address.getAdminArea()).append(", ");
                    if (address.getCountryName() != null) addressText.append(address.getCountryName());
                }

                // Display both the coordinates and the address
                final String finalAddressText = addressText.toString();
                if (!TextUtils.isEmpty(finalAddressText)) {
                    final String locationText = String.format("Lokasi: Lat %.4f, Lng %.4f\nAlamat: %s",
                            location.getLatitude(), location.getLongitude(), finalAddressText);

                    // Memastikan update UI dilakukan di thread utama
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            if (binding != null) {
                                binding.tvLokasiTerpilih.setText(locationText);
                            }
                        });
                    }

                    // Log untuk debugging
                    Log.d(TAG, "Address found: " + finalAddressText);
                } else {
                    final String locationText = String.format("Lokasi: Lat %.4f, Lng %.4f\nAlamat tidak tersedia",
                            location.getLatitude(), location.getLongitude());

                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            if (binding != null) {
                                binding.tvLokasiTerpilih.setText(locationText);
                            }
                        });
                    }
                }
            } else {
                final String locationText = String.format("Lokasi: Lat %.4f, Lng %.4f\nAlamat tidak ditemukan",
                        location.getLatitude(), location.getLongitude());

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (binding != null) {
                            binding.tvLokasiTerpilih.setText(locationText);
                        }
                    });
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error processing address: ", e);
            final String errorText = String.format("Lokasi: Lat %.4f, Lng %.4f\nError: %s",
                    location.getLatitude(), location.getLongitude(), e.getMessage());

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (binding != null) {
                        binding.tvLokasiTerpilih.setText(errorText);
                    }
                });
            }
        }
    }

    private void saveChanges() {
        if (TextUtils.isEmpty(binding.editNamaPasien.getText())) {
            // ... (Tambahkan validasi lain jika perlu) ...
            return;
        }

        binding.btnSimpanPerubahan.setEnabled(false);
        binding.btnSimpanPerubahan.setText("Menyimpan...");

        Map<String, Object> updates = new HashMap<>();
        updates.put("namaPasien", binding.editNamaPasien.getText().toString().trim());
        updates.put("golonganDarahDibutuhkan", binding.dropdownGolonganDarah.getText().toString().trim());
        updates.put("jenisKelamin", binding.dropdownJenisKelamin.getText().toString().trim());
        updates.put("jumlahKantong", Integer.parseInt(binding.editJumlahKantong.getText().toString().trim()));
        updates.put("namaRumahSakit", binding.editNamaRs.getText().toString().trim());
        updates.put("catatan", binding.editCatatan.getText().toString().trim());

        // --- LOGIKA BARU ---
        // Selalu update waktu ke saat ini setiap kali ada perubahan
        updates.put("waktuDibuat", Timestamp.now());

        if (lastKnownLocation != null) {
            GeoPoint lokasiBaru = new GeoPoint(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
            updates.put("lokasiRs", lokasiBaru);
            updates.put("geohash", GeoFireUtils.getGeoHashForLocation(new GeoLocation(lokasiBaru.getLatitude(), lokasiBaru.getLongitude())));
        }

        db.collection("donation_requests").document(requestId).update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Permintaan berhasil diperbarui.", Toast.LENGTH_SHORT).show();
                    getParentFragmentManager().popBackStack();
                })
                .addOnFailureListener(e -> {
                    if(isAdded()) {
                        Toast.makeText(getContext(), "Gagal memperbarui.", Toast.LENGTH_SHORT).show();
                        binding.btnSimpanPerubahan.setEnabled(true);
                        binding.btnSimpanPerubahan.setText("Simpan Perubahan");
                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}