package com.example.temudarah.fragment;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.temudarah.databinding.FragmentEditProfileBinding;
import com.example.temudarah.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class EditProfileFragment extends Fragment {
    private static final String TAG = "EditProfileFragment";
    private FragmentEditProfileBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private String currentUserId;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
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
            currentUserId = currentUser.getUid();
            loadUserData();
        } else {
            Toast.makeText(getContext(), "Pengguna tidak terautentikasi.", Toast.LENGTH_SHORT).show();
        }
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}