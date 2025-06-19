package com.example.temudarah.fragment;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.temudarah.R;
import com.example.temudarah.databinding.FragmentEditProfileBinding;
import com.example.temudarah.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class EditProfileFragment extends Fragment {

    private static final String TAG = "EditProfileFragment";

    private FragmentEditProfileBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private Uri cameraImageUri;

    private ActivityResultLauncher<Uri> cameraLauncher;
    private ActivityResultLauncher<String> galleryLauncher;
    private ActivityResultLauncher<String> requestCameraPermissionLauncher;
    private ActivityResultLauncher<String> requestGalleryPermissionLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initializeLaunchers();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentEditProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();

        setupListeners();
        setupDatePicker();
        setupGenderRadioButtons();

        if (currentUser != null) {
            loadUserData();
        } else {
            Toast.makeText(getContext(), "Pengguna tidak terautentikasi.", Toast.LENGTH_SHORT).show();
        }
    }

    private void initializeLaunchers() {
        galleryLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                uploadImageToFirebase(uri);
            }
        });

        cameraLauncher = registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
            if (success && cameraImageUri != null) {
                uploadImageToFirebase(cameraImageUri);
            }
        });

        requestCameraPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(), isGranted -> {
                    if (isGranted) openCamera();
                    else Toast.makeText(getContext(), "Izin kamera diperlukan", Toast.LENGTH_SHORT).show();
                });

        requestGalleryPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(), isGranted -> {
                    if (isGranted) openGallery();
                    else Toast.makeText(getContext(), "Izin galeri diperlukan", Toast.LENGTH_SHORT).show();
                });
    }

    private void setupListeners() {
        binding.btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());
        binding.btnCancel.setOnClickListener(v -> getParentFragmentManager().popBackStack());
        binding.btnSave.setOnClickListener(v -> saveUserData());
        binding.profileImage.setOnClickListener(v -> {
            if (binding.imageUploadProgressBar.getVisibility() == View.GONE) {
                showImagePickerOptions();
            } else {
                Toast.makeText(getContext(), "Harap tunggu proses upload selesai.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupDatePicker() {
        binding.editDateOfBirth.setFocusable(false);
        binding.editDateOfBirth.setClickable(true);
        binding.editDateOfBirth.setOnClickListener(v -> {
            final Calendar c = Calendar.getInstance();
            new DatePickerDialog(requireContext(), (datePicker, year, month, day) -> {
                binding.editDateOfBirth.setText(day + "/" + (month + 1) + "/" + year);
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
        });
    }

    private void setupGenderRadioButtons() {
        binding.radioMale.setOnCheckedChangeListener((buttonView, isChecked) -> { if (isChecked) binding.radioFemale.setChecked(false); });
        binding.radioFemale.setOnCheckedChangeListener((buttonView, isChecked) -> { if (isChecked) binding.radioMale.setChecked(false); });
    }

    private void showImagePickerOptions() {
        String[] options = {"Buka Kamera", "Pilih dari Galeri"};
        new AlertDialog.Builder(requireContext())
                .setTitle("Ubah Foto Profil")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) checkCameraPermissionAndOpenCamera();
                    else checkGalleryPermissionAndOpenGallery();
                })
                .show();
    }

    private void checkCameraPermissionAndOpenCamera() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            openCamera();
        } else {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void checkGalleryPermissionAndOpenGallery() {
        String permission;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permission = Manifest.permission.READ_MEDIA_IMAGES;
        } else {
            permission = Manifest.permission.READ_EXTERNAL_STORAGE;
        }

        if (ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED) {
            openGallery();
        } else {
            requestGalleryPermissionLauncher.launch(permission);
        }
    }

    private void openCamera() {
        File imageFile;
        try {
            imageFile = File.createTempFile("temp_profile_image", ".jpg", requireContext().getExternalCacheDir());
        } catch (IOException e) {
            Log.e(TAG, "Gagal membuat file gambar sementara", e);
            Toast.makeText(getContext(), "Gagal menyiapkan kamera", Toast.LENGTH_SHORT).show();
            return;
        }

        cameraImageUri = FileProvider.getUriForFile(
                requireContext(),
                "com.example.temudarah.provider",
                imageFile
        );
        cameraLauncher.launch(cameraImageUri);
    }

    private void openGallery() {
        galleryLauncher.launch("image/*");
    }

    private void loadUserData() {
        if (currentUser == null) return;
        binding.profileImage.setAlpha(0.5f); // Menandakan sedang loading
        DocumentReference userRef = db.collection("users").document(currentUser.getUid());
        userRef.get().addOnSuccessListener(documentSnapshot -> {
            binding.profileImage.setAlpha(1.0f);
            if (documentSnapshot.exists()) {
                User user = documentSnapshot.toObject(User.class);
                if (user != null) {
                    populateUi(user);
                }
            }
        }).addOnFailureListener(e -> {
            binding.profileImage.setAlpha(1.0f);
            Toast.makeText(getContext(), "Gagal mengambil data profil.", Toast.LENGTH_SHORT).show();
        });
    }

    private void populateUi(User user) {
        binding.editFullName.setText(user.getFullName() != null ? user.getFullName() : "");
        binding.editDateOfBirth.setText(user.getBirthDate() != null ? user.getBirthDate() : "");
        binding.editBloodType.setText(user.getBloodType() != null ? user.getBloodType() : "");

        binding.editWeight.setText(user.getWeight() > 0 ? String.valueOf(user.getWeight()) : "");
        binding.editHeight.setText(user.getHeight() > 0 ? String.valueOf(user.getHeight()) : "");

        if (user.getGender() != null) {
            if (user.getGender().equalsIgnoreCase("Laki-laki") || user.getGender().equalsIgnoreCase("Male")) {
                binding.radioMale.setChecked(true);
            } else if (user.getGender().equalsIgnoreCase("Perempuan") || user.getGender().equalsIgnoreCase("Female")) {
                binding.radioFemale.setChecked(true);
            }
        }
        if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty() && getContext() != null) {
            Glide.with(requireContext())
                    .load(user.getProfileImageUrl())
                    .placeholder(R.drawable.logo_merah)
                    .error(R.drawable.logo_merah)
                    .into(binding.profileImage);
        }
    }

    private void saveUserData() {
        if (currentUser == null) return;
        if (!validateInput()) {
            Toast.makeText(getContext(), "Silakan periksa kembali data yang Anda masukkan.", Toast.LENGTH_SHORT).show();
            return;
        }
        Toast.makeText(getContext(), "Menyimpan...", Toast.LENGTH_SHORT).show();

        String fullName = binding.editFullName.getText().toString().trim();
        String dateOfBirth = binding.editDateOfBirth.getText().toString().trim();
        String bloodType = binding.editBloodType.getText().toString().trim();
        String gender = binding.radioMale.isChecked() ? "Laki-laki" : "Perempuan";
        String age = calculateAge(dateOfBirth);
        int weight = 0;
        if (!binding.editWeight.getText().toString().trim().isEmpty()) {
            weight = Integer.parseInt(binding.editWeight.getText().toString().trim());
        }
        int height = 0;
        if (!binding.editHeight.getText().toString().trim().isEmpty()) {
            height = Integer.parseInt(binding.editHeight.getText().toString().trim());
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("fullName", fullName);
        updates.put("birthDate", dateOfBirth);
        updates.put("gender", gender);
        updates.put("bloodType", bloodType);
        updates.put("weight", weight);
        updates.put("height", height);
        updates.put("age", age);

        db.collection("users").document(currentUser.getUid()).update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Profil berhasil diperbarui", Toast.LENGTH_SHORT).show();
                    if (getParentFragmentManager() != null) getParentFragmentManager().popBackStack();
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Gagal memperbarui profil.", Toast.LENGTH_SHORT).show());
    }

    private void uploadImageToFirebase(Uri imageUri) {
        if (imageUri == null || currentUser == null) return;
        binding.imageUploadProgressBar.setVisibility(View.VISIBLE);
        String fileName = "profile_images/" + currentUser.getUid();
        StorageReference profileImageRef = FirebaseStorage.getInstance().getReference(fileName);

        profileImageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> profileImageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    updateProfileImageUrlInFirestore(uri.toString());
                }))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Upload Gagal", e);
                    Toast.makeText(getContext(), "Upload Gagal: " + e.getMessage(), Toast.LENGTH_LONG).show();
                })
                .addOnCompleteListener(task -> binding.imageUploadProgressBar.setVisibility(View.GONE));
    }

    private void updateProfileImageUrlInFirestore(String imageUrl) {
        if (currentUser == null) return;
        db.collection("users").document(currentUser.getUid())
                .update("profileImageUrl", imageUrl)
                .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "Foto profil berhasil diperbarui.", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Gagal menyimpan link foto.", Toast.LENGTH_SHORT).show());
    }

    private boolean validateInput() {
        if (TextUtils.isEmpty(binding.editFullName.getText().toString().trim())) {
            binding.editFullName.setError("Nama tidak boleh kosong");
            binding.editFullName.requestFocus();
            return false;
        }
        if (!binding.radioMale.isChecked() && !binding.radioFemale.isChecked()) {
            Toast.makeText(getContext(), "Silakan pilih jenis kelamin", Toast.LENGTH_SHORT).show();
            return false;
        }
        try {
            if (!TextUtils.isEmpty(binding.editWeight.getText().toString())) {
                Integer.parseInt(binding.editWeight.getText().toString().trim());
            }
            if (!TextUtils.isEmpty(binding.editHeight.getText().toString())) {
                Integer.parseInt(binding.editHeight.getText().toString().trim());
            }
        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), "Format berat atau tinggi badan harus berupa angka", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private String calculateAge(String birthDateStr) {
        if (birthDateStr == null || birthDateStr.isEmpty()) return "-";
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        try {
            Date birthDate = sdf.parse(birthDateStr);
            if (birthDate == null) return "-";
            Calendar birthCal = Calendar.getInstance();
            birthCal.setTime(birthDate);
            Calendar todayCal = Calendar.getInstance();
            int age = todayCal.get(Calendar.YEAR) - birthCal.get(Calendar.YEAR);
            if (todayCal.get(Calendar.DAY_OF_YEAR) < birthCal.get(Calendar.DAY_OF_YEAR)) {
                age--;
            }
            return String.valueOf(age);
        } catch (ParseException e) {
            e.printStackTrace();
            return "-";
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}