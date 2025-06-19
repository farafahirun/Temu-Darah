package com.example.temudarah.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.temudarah.R;
import com.example.temudarah.databinding.FragmentEditProfileBinding;
import com.example.temudarah.model.User;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class EditProfileFragment extends Fragment {

    private static final int REQUEST_CAMERA = 1;
    private static final int REQUEST_GALLERY = 2;
    private static final String TAG = "EditProfileFragment";

    private FragmentEditProfileBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private String currentUserId;
    private Uri imageUri;

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

        if (currentUser != null) {
            currentUserId = currentUser.getUid();
            loadUserData();
        } else {
            Toast.makeText(getContext(), "Pengguna tidak terautentikasi.", Toast.LENGTH_SHORT).show();
        }

        setupListeners();
        setupDatePicker();
        setupGenderRadioButtons();

        // Set up profile image click listener
        binding.profileImage.setOnClickListener(v -> showImagePickerOptions());
    }

    private void setupListeners() {
        binding.btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());
        binding.btnCancel.setOnClickListener(v -> getParentFragmentManager().popBackStack());
        binding.btnSave.setOnClickListener(v -> saveUserData());
    }

    private void setupDatePicker() {
        binding.editDateOfBirth.setFocusable(false);
        binding.editDateOfBirth.setClickable(true);

        binding.editDateOfBirth.setOnClickListener(v -> {
            final Calendar c = Calendar.getInstance();
            int year = c.get(Calendar.YEAR);
            int month = c.get(Calendar.MONTH);
            int day = c.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    requireContext(),
                    (datePicker, selectedYear, selectedMonth, selectedDay) -> {
                        String selectedDate = selectedDay + "/" + (selectedMonth + 1) + "/" + selectedYear;
                        binding.editDateOfBirth.setText(selectedDate);
                    },
                    year, month, day);
            datePickerDialog.show();
        });
    }

    private void setupGenderRadioButtons() {
        binding.radioMale.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                binding.radioFemale.setChecked(false);
            }
        });

        binding.radioFemale.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                binding.radioMale.setChecked(false);
            }
        });
    }

    private void loadUserData() {
        if (currentUserId == null) return;
        Toast.makeText(getContext(), "Memuat data...", Toast.LENGTH_SHORT).show();

        DocumentReference userRef = db.collection("users").document(currentUserId);
        userRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                User user = documentSnapshot.toObject(User.class);
                if (user != null) {
                    populateUi(user);
                }
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(getContext(), "Gagal mengambil data profil.", Toast.LENGTH_SHORT).show();
        });
    }

    private void populateUi(User user) {
        binding.editFullName.setText(user.getFullName());
        binding.editDateOfBirth.setText(user.getBirthDate());
        binding.editBloodType.setText(user.getBloodType());

        if (user.getWeight() > 0) {
            binding.editWeight.setText(String.valueOf(user.getWeight()));
        }
        if (user.getHeight() > 0) {
            binding.editHeight.setText(String.valueOf(user.getHeight()));
        }

        if (user.getGender() != null) {
            if (user.getGender().equalsIgnoreCase("Laki-laki") || user.getGender().equalsIgnoreCase("Male")) {
                binding.radioMale.setChecked(true);
            } else if (user.getGender().equalsIgnoreCase("Perempuan") || user.getGender().equalsIgnoreCase("Female")) {
                binding.radioFemale.setChecked(true);
            }
        }

        // Display profile image if exists
        if (user.getProfileImageUrl() != null) {
            Glide.with(this)
                    .load(user.getProfileImageUrl())  // URL gambar profil dari Firestore
                    .into(binding.profileImage);  // Menampilkan gambar ke ImageView
        }
    }

    private void saveUserData() {
        if (currentUserId == null) return;

        String fullName = binding.editFullName.getText().toString().trim();
        String dateOfBirth = binding.editDateOfBirth.getText().toString().trim();
        String bloodType = binding.editBloodType.getText().toString().trim();

        String gender = "";
        if (binding.radioMale.isChecked()) {
            gender = "Laki-laki";
        } else if (binding.radioFemale.isChecked()) {
            gender = "Perempuan";
        }

        int weight = 0;
        if (!binding.editWeight.getText().toString().trim().isEmpty()) {
            try {
                weight = Integer.parseInt(binding.editWeight.getText().toString().trim());
            } catch (NumberFormatException e) {
                binding.editWeight.setError("Format angka salah");
                return;
            }
        }

        double height = 0.0;
        if (!binding.editHeight.getText().toString().trim().isEmpty()) {
            try {
                height = Double.parseDouble(binding.editHeight.getText().toString().trim());
            } catch (NumberFormatException e) {
                binding.editHeight.setError("Format angka salah");
                return;
            }
        }

        Toast.makeText(getContext(), "Menyimpan...", Toast.LENGTH_SHORT).show();
        DocumentReference userRef = db.collection("users").document(currentUserId);

        Map<String, Object> updates = new HashMap<>();
        updates.put("fullName", fullName);
        updates.put("birthDate", dateOfBirth);
        updates.put("gender", gender);
        updates.put("bloodType", bloodType);
        updates.put("weight", weight);
        updates.put("height", height);

        userRef.update(updates).addOnSuccessListener(aVoid -> {
            Toast.makeText(getContext(), "Data berhasil diperbarui", Toast.LENGTH_SHORT).show();
            getParentFragmentManager().popBackStack();
        }).addOnFailureListener(e -> {
            Toast.makeText(getContext(), "Gagal memperbarui data", Toast.LENGTH_SHORT).show();
        });
    }

    private void showImagePickerOptions() {
        String[] options = {"Kamera", "Galeri"};
        new AlertDialog.Builder(getContext())
                .setTitle("Pilih Sumber Gambar")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        openCamera();
                    } else {
                        openGallery();
                    }
                })
                .show();
    }

    private void openCamera() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (cameraIntent.resolveActivity(getActivity().getPackageManager()) != null) {
            startActivityForResult(cameraIntent, REQUEST_CAMERA);
        } else {
            Toast.makeText(getContext(), "Kamera tidak tersedia", Toast.LENGTH_SHORT).show();
        }
    }

    private void openGallery() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(galleryIntent, REQUEST_GALLERY);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_CAMERA) {
                // Menangani hasil dari kamera
                Bitmap photo = (Bitmap) data.getExtras().get("data");
                binding.profileImage.setImageBitmap(photo);

                imageUri = getImageUriFromBitmap(photo);
                uploadImageToFirebase(imageUri);  // Upload gambar ke Firebase
            } else if (requestCode == REQUEST_GALLERY) {
                // Menangani hasil dari galeri
                imageUri = data.getData();
                binding.profileImage.setImageURI(imageUri);

                uploadImageToFirebase(imageUri);  // Upload gambar ke Firebase
            }
        }
    }

    private Uri getImageUriFromBitmap(Bitmap bitmap) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes); // Pastikan mengonversi ke format JPEG
        String path = MediaStore.Images.Media.insertImage(getActivity().getContentResolver(), bitmap, "TempImage", null);
        return Uri.parse(path);
    }

    private void uploadImageToFirebase(Uri imageUri) {
        if (imageUri != null) {
            String fileExtension = getFileExtension(imageUri); // Menentukan ekstensi file
            StorageReference storageRef = FirebaseStorage.getInstance().getReference();
            StorageReference profileImageRef = storageRef.child("profileImages/" + currentUserId + "." + fileExtension);

            // Upload file
            profileImageRef.putFile(imageUri)
                    .addOnSuccessListener(taskSnapshot -> {
                        // Setelah upload sukses, dapatkan URL gambar yang baru
                        profileImageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                            // Update URL gambar di Firestore
                            updateProfileImageUrl(uri.toString());
                        }).addOnFailureListener(e -> {
                            Toast.makeText(getContext(), "Gagal mendapatkan URL gambar", Toast.LENGTH_SHORT).show();
                        });
                    })
                    .addOnFailureListener(e -> {
                        // Jika gagal upload
                        Toast.makeText(getContext(), "Gagal meng-upload gambar", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Upload gagal: " + e.getMessage());
                    });
        }
    }

    private String getFileExtension(Uri uri) {
        String extension = null;
        if (uri.getScheme().equals(ContentResolver.SCHEME_CONTENT)) {
            // Untuk file yang dipilih dari galeri
            MimeTypeMap mime = MimeTypeMap.getSingleton();
            extension = mime.getExtensionFromMimeType(getContext().getContentResolver().getType(uri));
        } else {
            // Untuk file yang dipilih dari kamera
            extension = MimeTypeMap.getFileExtensionFromUrl(uri.toString());
        }
        return extension != null ? extension : "jpg"; // default ke jpg jika tidak bisa mendapatkan ekstensi
    }

    private void updateProfileImageUrl(String imageUrl) {
        DocumentReference userRef = db.collection("users").document(currentUserId);

        Map<String, Object> updates = new HashMap<>();
        updates.put("profileImageUrl", imageUrl); // Menambahkan field profileImageUrl

        userRef.update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Gambar profil berhasil diperbarui", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Gagal memperbarui gambar profil", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
