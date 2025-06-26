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
import android.os.Handler;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Base64;
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

import com.bumptech.glide.Glide;
import com.example.temudarah.R;
import com.example.temudarah.activity.MainActivity;
import com.example.temudarah.databinding.FragmentEditProfileBinding;
import com.example.temudarah.model.User;
import com.example.temudarah.util.AlertUtil;
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

    private String selectedBloodType; // To store the selected blood type from AutoCompleteTextView
    private String[] bloodTypes = {"A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"}; // Array of blood types

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initializeLaunchers();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentEditProfileBinding.inflate(inflater, container, false);
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).hideBottomNav();
        }
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
        setupBloodTypeAutoComplete(); // Initialize the AutoCompleteTextView

        showScreenLoading();

        new Handler().postDelayed(() -> {
            if (currentUser != null) {
                loadUserData(); // load data after 2 seconds
            } else {
                hideScreenLoading();
                Toast.makeText(getContext(), "Pengguna tidak terautentikasi.", Toast.LENGTH_SHORT).show();
            }
        }, 2000);
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
        binding.btnSave.setOnClickListener(v -> showConfirmationDialog());

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
                binding.editDateOfBirth.setText(String.format(Locale.getDefault(), "%02d/%02d/%04d", day, month + 1, year));
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

    private void setupBloodTypeAutoComplete() {
        // Create an ArrayAdapter and set it to the AutoCompleteTextView
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line, // Layout for dropdown items
                bloodTypes
        );
        binding.editBloodType.setAdapter(adapter);

        // Set an item click listener to capture the selected value
        binding.editBloodType.setOnItemClickListener((parent, view, position, id) -> {
            selectedBloodType = (String) parent.getItemAtPosition(position);
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
            hideScreenLoading();
        }).addOnFailureListener(e -> {
            Toast.makeText(getContext(), "Gagal memuat data profil.", Toast.LENGTH_SHORT).show();
            hideScreenLoading();
        });
    }

    private void populateUi(User user) {
        binding.editNamaPengguna.setText(user.getUsername() != null ? user.getUsername() : "");
        binding.editFullName.setText(user.getFullName() != null ? user.getFullName() : "");
        binding.editDateOfBirth.setText(user.getBirthDate() != null ? user.getBirthDate() : "");

        // Set AutoCompleteTextView selection for blood type
        String userBloodType = user.getBloodType();
        if (userBloodType != null && !userBloodType.isEmpty()) {
            binding.editBloodType.setText(userBloodType, false); // Set text, don't show dropdown
            selectedBloodType = userBloodType; // Keep fragment's state updated
        } else {
            binding.editBloodType.setText(null); // Set text to null to prevent potential selection issues on empty string
            selectedBloodType = null; // Ensure the internal state is null
        }

        binding.editWeight.setText(user.getWeight() > 0 ? String.valueOf(user.getWeight()) : "");
        binding.editHeight.setText(user.getHeight() > 0 ? String.valueOf(user.getHeight()) : "");
        binding.editNomorKTP.setText(user.getKtpNumber() != null ? user.getKtpNumber() : "");
        binding.editAlamatLengkap.setText(user.getAddress() != null ? user.getAddress() : "");

        if (user.getGender() != null) {
            if (user.getGender().equalsIgnoreCase("Laki-laki")) binding.radioMale.setChecked(true);
            else if (user.getGender().equalsIgnoreCase("Perempuan")) binding.radioFemale.setChecked(true);
        }

        if (user.getProfileImageBase64() != null && !user.getProfileImageBase64().isEmpty()) {
            try {
                byte[] imageBytes = Base64.decode(user.getProfileImageBase64(), Base64.DEFAULT);
                Glide.with(requireContext()).asBitmap().load(imageBytes).into(binding.profileImage);
            } catch (Exception e) {
                binding.profileImage.setImageResource(R.drawable.foto_profil);
            }
        } else {
            binding.profileImage.setImageResource(R.drawable.foto_profil);
        }
    }

    private void showConfirmationDialog() {
        if (currentUser == null) return;
        if (!validateInput()) return;

        if (getContext() != null) {
            AlertUtil.showAlert(getContext(),
                    "Konfirmasi Perubahan",
                    "Apakah Anda yakin ingin menyimpan perubahan data profil?",
                    "Simpan", "Batal",
                    v -> saveUserData(),
                    null);
        }
    }

    private void saveUserData() {
        showScreenLoading();

        String username = binding.editNamaPengguna.getText().toString().trim();
        String fullName = binding.editFullName.getText().toString().trim();
        String birthDate = binding.editDateOfBirth.getText().toString().trim();
        String gender = binding.radioMale.isChecked() ? "Laki-laki" : "Perempuan";
        // Get blood type from the selectedSpinner. If not selected from dropdown, get from current text.
        // It's safer to always use selectedBloodType if it was set, otherwise fallback to text from ACTV.
        String bloodType = (selectedBloodType != null && !selectedBloodType.isEmpty()) ? selectedBloodType : binding.editBloodType.getText().toString().trim();
        if (bloodType.isEmpty()) { // Final check if still empty
            Toast.makeText(getContext(), "Silakan pilih golongan darah.", Toast.LENGTH_SHORT).show();
            hideScreenLoading();
            return;
        }


        String ktpNumber = binding.editNomorKTP.getText().toString().trim();
        String address = binding.editAlamatLengkap.getText().toString().trim();
        String age = calculateAge(birthDate);

        int weight = 0;
        try {
            weight = Integer.parseInt(binding.editWeight.getText().toString().trim());
        } catch (NumberFormatException e) {
            Log.e(TAG, "Invalid weight format", e);
            // Handle error or set a default value
        }

        int height = 0;
        try {
            height = Integer.parseInt(binding.editHeight.getText().toString().trim());
        } catch (NumberFormatException e) {
            Log.e(TAG, "Invalid height format", e);
            // Handle error or set a default value
        }


        Map<String, Object> updates = new HashMap<>();
        updates.put("username", username);
        updates.put("fullName", fullName);
        updates.put("birthDate", birthDate);
        updates.put("gender", gender);
        updates.put("bloodType", bloodType); // Save the selected blood type
        updates.put("weight", weight);
        updates.put("height", height);
        updates.put("age", age);
        updates.put("ktpNumber", ktpNumber);
        updates.put("address", address);

        db.collection("users").document(currentUser.getUid()).update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Profil berhasil diperbarui", Toast.LENGTH_SHORT).show();
                    hideScreenLoading();
                    getParentFragmentManager().popBackStack();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Gagal memperbarui profil.", Toast.LENGTH_SHORT).show();
                    hideScreenLoading();
                });
    }

    private boolean validateInput() {
        if (TextUtils.isEmpty(binding.editNamaPengguna.getText().toString().trim())) {
            binding.editNamaPengguna.setError("Nama Pengguna tidak boleh kosong");
            return false;
        }
        if (TextUtils.isEmpty(binding.editFullName.getText().toString().trim())) {
            binding.editFullName.setError("Nama Lengkap tidak boleh kosong");
            return false;
        }
        if (TextUtils.isEmpty(binding.editAlamatLengkap.getText().toString().trim())) {
            binding.editAlamatLengkap.setError("Alamat tidak boleh kosong");
            return false;
        }
        if (TextUtils.isEmpty(binding.editNomorKTP.getText().toString().trim())) {
            binding.editNomorKTP.setError("Nomor KTP tidak boleh kosong");
            return false;
        } else if (binding.editNomorKTP.getText().toString().length() != 16) {
            binding.editNomorKTP.setError("Nomor KTP harus 16 digit");
            return false;
        }
        if (!binding.radioMale.isChecked() && !binding.radioFemale.isChecked()) {
            Toast.makeText(getContext(), "Silakan pilih jenis kelamin", Toast.LENGTH_SHORT).show();
            return false;
        }
        // Validate AutoCompleteTextView selection
        if (selectedBloodType == null || selectedBloodType.isEmpty()) {
            // Check if the text in the AutoCompleteTextView is one of the valid blood types
            String enteredText = binding.editBloodType.getText().toString().trim();
            boolean isValidBloodType = false;
            for (String type : bloodTypes) {
                if (type.equalsIgnoreCase(enteredText)) {
                    isValidBloodType = true;
                    selectedBloodType = type; // Ensure selectedBloodType is set if typed correctly
                    break;
                }
            }
            if (!isValidBloodType) {
                binding.editBloodType.setError("Silakan pilih golongan darah yang valid");
                Toast.makeText(getContext(), "Silakan pilih golongan darah yang valid", Toast.LENGTH_SHORT).show();
                return false;
            }
        }
        return true;
    }

    private String calculateAge(String birthDateStr) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Date birthDate = sdf.parse(birthDateStr);
            if (birthDate == null) return "-";
            Calendar today = Calendar.getInstance();
            Calendar dob = Calendar.getInstance();
            dob.setTime(birthDate);
            int age = today.get(Calendar.YEAR) - dob.get(Calendar.YEAR);
            if (today.get(Calendar.DAY_OF_YEAR) < dob.get(Calendar.DAY_OF_YEAR)) age--;
            return String.valueOf(age);
        } catch (ParseException e) {
            return "-";
        }
    }

    private void processImageUri(Uri uri) {
        binding.imageUploadProgressBar.setVisibility(View.VISIBLE);
        new Thread(() -> {
            String base64 = convertImageUriToBase64(uri);
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (base64 != null) updateProfileImageInFirestore(base64);
                    else binding.imageUploadProgressBar.setVisibility(View.GONE);
                });
            }
        }).start();
    }

    private void processBitmap(Bitmap bitmap) {
        binding.imageUploadProgressBar.setVisibility(View.VISIBLE);
        new Thread(() -> {
            String base64 = convertBitmapToBase64(bitmap);
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (base64 != null) updateProfileImageInFirestore(base64);
                    else binding.imageUploadProgressBar.setVisibility(View.GONE);
                });
            }
        }).start();
    }

    private String convertBitmapToBase64(Bitmap bitmap) {
        Bitmap resized = Bitmap.createScaledBitmap(bitmap, 400, 400, true);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        resized.compress(Bitmap.CompressFormat.JPEG, 60, output);
        return Base64.encodeToString(output.toByteArray(), Base64.DEFAULT);
    }

    private String convertImageUriToBase64(Uri uri) {
        try (InputStream inputStream = requireContext().getContentResolver().openInputStream(uri)) {
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            return convertBitmapToBase64(bitmap);
        } catch (Exception e) {
            return null;
        }
    }

    private void updateProfileImageInFirestore(String base64Image) {
        db.collection("users").document(currentUser.getUid())
                .update("profileImageBase64", base64Image)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Foto profil berhasil diperbarui", Toast.LENGTH_SHORT).show();
                    binding.imageUploadProgressBar.setVisibility(View.GONE);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Gagal memperbarui foto", Toast.LENGTH_SHORT).show();
                    binding.imageUploadProgressBar.setVisibility(View.GONE);
                });
    }

    private void showScreenLoading() {
        binding.loadingOverlay.setVisibility(View.VISIBLE);
        binding.scrollView.setVisibility(View.GONE);
    }

    private void hideScreenLoading() {
        binding.loadingOverlay.setVisibility(View.GONE);
        binding.scrollView.setVisibility(View.VISIBLE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

