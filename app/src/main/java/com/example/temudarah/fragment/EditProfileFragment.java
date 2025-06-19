package com.example.temudarah.fragment;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Base64;
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
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.temudarah.R;
import com.example.temudarah.databinding.FragmentEditProfileBinding;
import com.example.temudarah.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class EditProfileFragment extends Fragment {
    private static final String TAG = "EditProfileFragment";
    private FragmentEditProfileBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private ActivityResultLauncher<Intent> cameraLauncher;
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
                binding.profileImage.setImageURI(uri);
                processImageUri(uri);
            }
        });

        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Bundle extras = result.getData().getExtras();
                        if (extras != null && extras.get("data") != null) {
                            Bitmap imageBitmap = (Bitmap) extras.get("data");
                            binding.profileImage.setImageBitmap(imageBitmap);
                            processBitmap(imageBitmap);
                        }
                    }
                });

        requestCameraPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(), isGranted -> {
                    if (isGranted) openCamera();
                    else
                        Toast.makeText(getContext(), "Izin kamera diperlukan", Toast.LENGTH_SHORT).show();
                });

        requestGalleryPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(), isGranted -> {
                    if (isGranted) openGallery();
                    else
                        Toast.makeText(getContext(), "Izin galeri diperlukan", Toast.LENGTH_SHORT).show();
                });
    }

    private void setupListeners() {
        binding.btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());
        binding.btnSave.setOnClickListener(v -> saveUserData());
        binding.profileImage.setOnClickListener(v -> {
            if (binding.imageUploadProgressBar.getVisibility() == View.GONE) {
                showImagePickerOptions();
            } else {
                Toast.makeText(getContext(), "Harap tunggu proses selesai.", Toast.LENGTH_SHORT).show();
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
        binding.radioMale.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) binding.radioFemale.setChecked(false);
        });
        binding.radioFemale.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) binding.radioMale.setChecked(false);
        });
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
        String permission = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) ?
                Manifest.permission.READ_MEDIA_IMAGES : Manifest.permission.READ_EXTERNAL_STORAGE;
        if (ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED) {
            openGallery();
        } else {
            requestGalleryPermissionLauncher.launch(permission);
        }
    }

    private void openCamera() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraLauncher.launch(cameraIntent);
    }

    private void openGallery() {
        galleryLauncher.launch("image/*");
    }

    private void loadUserData() {
        if (currentUser == null) return;
        Toast.makeText(getContext(), "Memuat data...", Toast.LENGTH_SHORT).show();
        DocumentReference userRef = db.collection("users").document(currentUser.getUid());
        userRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                User user = documentSnapshot.toObject(User.class);
                if (user != null) {
                    populateUi(user);
                }
            } else {
                Toast.makeText(getContext(), "Data profil tidak ditemukan.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void populateUi(User user) {
        binding.editNamaPengguna.setText(user.getUsername() != null ? user.getUsername() : "");
        binding.editFullName.setText(user.getFullName() != null ? user.getFullName() : "");
        binding.editDateOfBirth.setText(user.getBirthDate() != null ? user.getBirthDate() : "");
        binding.editBloodType.setText(user.getBloodType() != null ? user.getBloodType() : "");
        binding.editWeight.setText(user.getWeight() > 0 ? String.valueOf(user.getWeight()) : "");
        binding.editHeight.setText(user.getHeight() > 0 ? String.valueOf(user.getHeight()) : "");
        binding.editNomorKTP.setText(user.getKtpNumber() != null ? user.getKtpNumber() : "");
        binding.editAlamatLengkap.setText(user.getAddress() != null ? user.getAddress() : "");
        if (user.getGender() != null) {
            if (user.getGender().equalsIgnoreCase("Laki-laki") || user.getGender().equalsIgnoreCase("Male")) {
                binding.radioMale.setChecked(true);
            } else if (user.getGender().equalsIgnoreCase("Perempuan") || user.getGender().equalsIgnoreCase("Female")) {
                binding.radioFemale.setChecked(true);
            }
        }

        if (user.getProfileImageBase64() != null && !user.getProfileImageBase64().isEmpty() && getContext() != null) {
            try {
                byte[] imageBytes = Base64.decode(user.getProfileImageBase64(), Base64.DEFAULT);
                Glide.with(requireContext()).asBitmap().load(imageBytes).placeholder(R.drawable.logo_merah).into(binding.profileImage);
            } catch (Exception e) {
                binding.profileImage.setImageResource(R.drawable.logo_merah);
            }
        } else {
            binding.profileImage.setImageResource(R.drawable.logo_merah);
        }
    }

    private void saveUserData() {
        if (currentUser == null) return;
        if (!validateInput()) {
            Toast.makeText(getContext(), "Silakan periksa kembali data Anda.", Toast.LENGTH_SHORT).show();
            return;
        }
        Toast.makeText(getContext(), "Menyimpan...", Toast.LENGTH_SHORT).show();

        String namaPengguna = binding.editNamaPengguna.getText().toString().trim();
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
        String address = binding.editAlamatLengkap.getText().toString().trim();
        String ktpNumber = binding.editNomorKTP.getText().toString().trim();

        Map<String, Object> updates = new HashMap<>();
        updates.put("username", namaPengguna);
        updates.put("fullName", fullName);
        updates.put("birthDate", dateOfBirth);
        updates.put("gender", gender);
        updates.put("bloodType", bloodType);
        updates.put("weight", weight);
        updates.put("height", height);
        updates.put("age", age);
        updates.put("address", address);
        updates.put("ktpNumber", ktpNumber);

        db.collection("users").document(currentUser.getUid()).update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Profil berhasil diperbarui", Toast.LENGTH_SHORT).show();
                    if (getParentFragmentManager() != null)
                        getParentFragmentManager().popBackStack();
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Gagal memperbarui profil.", Toast.LENGTH_SHORT).show());
    }

    private boolean validateInput() {
        if (TextUtils.isEmpty(binding.editNamaPengguna.getText().toString().trim())) {
            binding.editNamaPengguna.setError("Nama Pengguna tidak boleh kosong");
            binding.editNamaPengguna.requestFocus();
            return false;
        }
        if (TextUtils.isEmpty(binding.editFullName.getText().toString().trim())) {
            binding.editFullName.setError("Nama Lengkap tidak boleh kosong");
            binding.editFullName.requestFocus();
            return false;
        }
        if (TextUtils.isEmpty(binding.editAlamatLengkap.getText().toString().trim())) {
            binding.editAlamatLengkap.setError("Alamat tidak boleh kosong");
            binding.editAlamatLengkap.requestFocus();
            return false;
        }
        String ktpNumber = binding.editNomorKTP.getText().toString().trim();
        if (TextUtils.isEmpty(ktpNumber)) {
            binding.editNomorKTP.setError("Nomor KTP tidak boleh kosong");
            binding.editNomorKTP.requestFocus();
            return false;
        } else if (ktpNumber.length() != 16) {
            binding.editNomorKTP.setError("Nomor KTP harus 16 digit");
            binding.editNomorKTP.requestFocus();
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

    private void processImageUri(Uri imageUri) {
        binding.imageUploadProgressBar.setVisibility(View.VISIBLE);
        new Thread(() -> {
            String base64Image = convertImageUriToBase64(imageUri);
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (base64Image != null) {
                        updateProfileImageInFirestore(base64Image);
                    } else {
                        Toast.makeText(getContext(), "Gagal memproses gambar.", Toast.LENGTH_SHORT).show();
                        binding.imageUploadProgressBar.setVisibility(View.GONE);
                    }
                });
            }
        }).start();
    }

    private void processBitmap(Bitmap bitmap) {
        binding.imageUploadProgressBar.setVisibility(View.VISIBLE);
        new Thread(() -> {
            String base64Image = convertBitmapToBase64(bitmap);
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (base64Image != null) {
                        updateProfileImageInFirestore(base64Image);
                    } else {
                        Toast.makeText(getContext(), "Gagal memproses gambar.", Toast.LENGTH_SHORT).show();
                        binding.imageUploadProgressBar.setVisibility(View.GONE);
                    }
                });
            }
        }).start();
    }

    private String convertBitmapToBase64(Bitmap bitmap) {
        try {
            Bitmap resizedBitmap = resizeBitmap(bitmap, 400);
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 60, byteArrayOutputStream);
            byte[] byteArray = byteArrayOutputStream.toByteArray();
            return Base64.encodeToString(byteArray, Base64.DEFAULT);
        } catch (Exception e) {
            Log.e(TAG, "Gagal mengubah bitmap ke Base64", e);
            return null;
        }
    }

    private String convertImageUriToBase64(Uri imageUri) {
        try (InputStream inputStream = requireContext().getContentResolver().openInputStream(imageUri)) {
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            return convertBitmapToBase64(bitmap);
        } catch (Exception e) {
            Log.e(TAG, "Gagal mengubah Uri ke Base64", e);
            return null;
        }
    }

    private Bitmap resizeBitmap(Bitmap bitmap, int maxSize) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        if (width <= maxSize && height <= maxSize) return bitmap;
        float ratio = Math.min((float) maxSize / width, (float) maxSize / height);
        int newWidth = Math.round(width * ratio);
        int newHeight = Math.round(height * ratio);
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
    }

    private void updateProfileImageInFirestore(String base64Image) {
        if (currentUser == null) return;
        Toast.makeText(getContext(), "Menyimpan foto profil...", Toast.LENGTH_SHORT).show();
        db.collection("users").document(currentUser.getUid())
                .update("profileImageBase64", base64Image)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Foto profil berhasil diperbarui.", Toast.LENGTH_SHORT).show();
                    binding.imageUploadProgressBar.setVisibility(View.GONE);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Gagal menyimpan foto: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    binding.imageUploadProgressBar.setVisibility(View.GONE);
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}