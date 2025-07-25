package com.example.temudarah.activity;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.ArrayAdapter;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.temudarah.databinding.ActivityDaftarBinding;
import com.example.temudarah.model.User;
import com.example.temudarah.util.AlertUtil;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.Objects;

public class DaftarActivity extends AppCompatActivity {
    private static final String TAG = "DaftarActivity";
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private ActivityDaftarBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityDaftarBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        setupListeners();
        setupDropdowns();
        setupDatePicker();
    }

    private void setupListeners() {
        binding.btnDaftar.setOnClickListener(v -> {
            if (validateInput()) {
                registerUser();
            } else {
                AlertUtil.showAlert(this,
                        "Data Tidak Lengkap",
                        "Silakan periksa kembali data yang Anda masukkan",
                        "Mengerti", null, null, null);
            }
        });

        binding.ivBack.setOnClickListener(v -> finish());
        binding.tvMasuk.setOnClickListener(v ->
                startActivity(new Intent(DaftarActivity.this, MasukActivity.class)));
    }

    private void setupDropdowns() {
        String[] genders = {"Laki-laki", "Perempuan"};
        ArrayAdapter<String> genderAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, genders);
        binding.spinnerGender.setAdapter(genderAdapter);

        String[] bloodTypes = {"A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-", "Rh-null"};
        ArrayAdapter<String> bloodTypeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, bloodTypes);
        binding.spinnerGolonganDarah.setAdapter(bloodTypeAdapter);

        String[] donorOptions = {"Ya", "Tidak"};
        ArrayAdapter<String> donorAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, donorOptions);
        binding.spinnerDonorSebelumnya.setAdapter(donorAdapter);

        binding.spinnerDonorSebelumnya.setOnItemClickListener((parent, view, position, id) -> {
            String selectedOption = (String) parent.getItemAtPosition(position);
            if ("Ya".equals(selectedOption)) {
                binding.tilLastDonationDate.setVisibility(View.VISIBLE);
                if (TextUtils.isEmpty(binding.etLastDonationDate.getText())) {
                    showLastDonationDatePicker();
                }
            } else {
                binding.tilLastDonationDate.setVisibility(View.GONE);
                binding.etLastDonationDate.setText("");
            }
        });
    }

    private void setupDatePicker() {
        binding.etBirthDate.setOnClickListener(v -> {
            final Calendar c = Calendar.getInstance();
            int year = c.get(Calendar.YEAR);
            int month = c.get(Calendar.MONTH);
            int day = c.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    DaftarActivity.this,
                    (view, year1, monthOfYear, dayOfMonth) ->
                            binding.etBirthDate.setText(dayOfMonth + "/" + (monthOfYear + 1) + "/" + year1),
                    year, month, day);
            datePickerDialog.show();
        });
        binding.etLastDonationDate.setOnClickListener(v -> showLastDonationDatePicker());
    }

    private void showLastDonationDatePicker() {
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                DaftarActivity.this,
                (view, year1, monthOfYear, dayOfMonth) ->
                        binding.etLastDonationDate.setText(dayOfMonth + "/" + (monthOfYear + 1) + "/" + year1),
                year, month, day);
        datePickerDialog.show();
    }

    private boolean validateInput() {
        String fullName = binding.etFullName.getText().toString().trim();
        String username = binding.etUsername.getText().toString().trim();
        String email = binding.etPhoneEmail.getText().toString().trim();
        String address = binding.etAddress.getText().toString().trim();
        String birthDate = binding.etBirthDate.getText().toString().trim();
        String weight = binding.etWeight.getText().toString().trim();
        String height = binding.etHeight.getText().toString().trim();
        String ktpNumber = binding.etKtpNumber.getText().toString().trim();
        String gender = binding.spinnerGender.getText().toString();
        String bloodType = binding.spinnerGolonganDarah.getText().toString();
        String previousDonor = binding.spinnerDonorSebelumnya.getText().toString();
        String lastDonationDate = binding.etLastDonationDate.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();
        String confirmPassword = binding.etKonfirmasiPassword.getText().toString().trim();

        if (TextUtils.isEmpty(fullName)) {
            binding.etFullName.setError("Nama Lengkap tidak boleh kosong");
            binding.etFullName.requestFocus();
            return false;
        }
        if (TextUtils.isEmpty(username)) {
            binding.etUsername.setError("Username tidak boleh kosong");
            binding.etUsername.requestFocus();
            return false;
        }
        if (TextUtils.isEmpty(email)) {
            binding.etPhoneEmail.setError("Email tidak boleh kosong");
            binding.etPhoneEmail.requestFocus();
            return false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etPhoneEmail.setError("Format Email tidak valid");
            binding.etPhoneEmail.requestFocus();
            return false;
        }
        if (TextUtils.isEmpty(address)) {
            binding.etAddress.setError("Alamat tidak boleh kosong");
            binding.etAddress.requestFocus();
            return false;
        }
        if (TextUtils.isEmpty(birthDate)) {
            binding.etBirthDate.setError("Tanggal Lahir harus diisi");
            return false;
        }
        if (TextUtils.isEmpty(weight)) {
            binding.etWeight.setError("Berat Badan tidak boleh kosong");
            binding.etWeight.requestFocus();
            return false;
        }
        if (TextUtils.isEmpty(height)) {
            binding.etHeight.setError("Tinggi Badan tidak boleh kosong");
            binding.etHeight.requestFocus();
            return false;
        }
        if (TextUtils.isEmpty(ktpNumber)) {
            binding.etKtpNumber.setError("Nomor KTP tidak boleh kosong");
            binding.etKtpNumber.requestFocus();
            return false;
        } else if (ktpNumber.length() != 16) {
            binding.etKtpNumber.setError("Nomor KTP harus 16 digit");
            binding.etKtpNumber.requestFocus();
            return false;
        }
        if (TextUtils.isEmpty(gender)) {
            binding.spinnerGender.setError("Jenis Kelamin harus dipilih");
            binding.spinnerGender.requestFocus();
            return false;
        }
        if (TextUtils.isEmpty(bloodType)) {
            binding.spinnerGolonganDarah.setError("Golongan Darah harus dipilih");
            binding.spinnerGolonganDarah.requestFocus();
            return false;
        }
        if (TextUtils.isEmpty(previousDonor)) {
            binding.spinnerDonorSebelumnya.setError("Opsi ini harus dipilih");
            binding.spinnerDonorSebelumnya.requestFocus();
            return false;
        }
        if ("Ya".equals(previousDonor) && TextUtils.isEmpty(lastDonationDate)) {
            binding.etLastDonationDate.setError("Tanggal donor terakhir harus diisi jika sudah pernah donor");
            binding.etLastDonationDate.requestFocus();
            return false;
        }
        if (TextUtils.isEmpty(password)) {
            binding.etPassword.setError("Password tidak boleh kosong");
            binding.etPassword.requestFocus();
            return false;
        } else if (password.length() < 6) {
            binding.etPassword.setError("Password minimal 6 karakter");
            binding.etPassword.requestFocus();
            return false;
        }
        if (TextUtils.isEmpty(confirmPassword)) {
            binding.etKonfirmasiPassword.setError("Konfirmasi Password tidak boleh kosong");
            binding.etKonfirmasiPassword.requestFocus();
            return false;
        }
        if (!password.equals(confirmPassword)) {
            binding.etKonfirmasiPassword.setError("Password tidak cocok");
            binding.etPassword.setError("Password tidak cocok");
            binding.etKonfirmasiPassword.requestFocus();
            return false;
        } else {
            binding.etPassword.setError(null);
        }
        return true;
    }

    private void registerUser() {
        String email = binding.etPhoneEmail.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();

        Log.d("DAFTAR_DEBUG", "Fungsi registerUser() dipanggil.");

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d("DAFTAR_DEBUG", "createUserWithEmail: SUKSES. Memanggil saveAdditionalUserData...");
                        saveAdditionalUserData(task.getResult().getUser());
                    } else {
                        Log.w("DAFTAR_DEBUG", "createUserWithEmail: GAGAL", task.getException());
                        AlertUtil.showAlert(DaftarActivity.this, "Pendaftaran Gagal", Objects.requireNonNull(task.getException()).getMessage(), "OK", null, null, null);
                    }
                });
    }

    private void saveAdditionalUserData(FirebaseUser firebaseUser) {
        Log.d("DAFTAR_DEBUG", "Fungsi saveAdditionalUserData() dipanggil untuk UID: " + firebaseUser.getUid());

        String fullName = binding.etFullName.getText().toString().trim();
        String username = binding.etUsername.getText().toString().trim();
        String ktpNumber = binding.etKtpNumber.getText().toString().trim();
        String address = binding.etAddress.getText().toString().trim();
        String birthDate = binding.etBirthDate.getText().toString().trim();
        String gender = binding.spinnerGender.getText().toString();
        String bloodType = binding.spinnerGolonganDarah.getText().toString();
        String hasDonated = binding.spinnerDonorSebelumnya.getText().toString();
        String lastDonationDate = binding.etLastDonationDate.getText().toString().trim();
        int weight = TextUtils.isEmpty(binding.etWeight.getText().toString()) ? 0 : Integer.parseInt(binding.etWeight.getText().toString());
        int height = TextUtils.isEmpty(binding.etHeight.getText().toString()) ? 0 : Integer.parseInt(binding.etHeight.getText().toString());
        String profileImageUrl = "";


        if (firebaseUser != null) {
            User newUser = new User();
            newUser.setUid(firebaseUser.getUid());
            newUser.setEmail(firebaseUser.getEmail());
            newUser.setUsername(username);
            newUser.setFullName(fullName);
            newUser.setKtpNumber(ktpNumber);
            newUser.setAddress(address);
            newUser.setBirthDate(birthDate);
            newUser.setGender(gender);
            newUser.setBloodType(bloodType);
            newUser.setHasDonatedBefore(hasDonated);
            newUser.setWeight(weight);
            newUser.setHeight(height);
            newUser.setLastDonationDate(lastDonationDate);

            Log.d("DAFTAR_DEBUG", "Mencoba menyimpan data ke Firestore...");
            db.collection("users").document(firebaseUser.getUid())
                    .set(newUser)
                    .addOnSuccessListener(aVoid -> {
                        Log.d("DAFTAR_DEBUG", "Penyimpanan ke Firestore SUKSES.");
                        AlertUtil.showAlert(DaftarActivity.this, "Pendaftaran Berhasil", "Selamat, pendaftaran Anda berhasil!", "OK", null, null, null);

                        Intent intent = new Intent(DaftarActivity.this, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Log.w("DAFTAR_DEBUG", "Penyimpanan ke Firestore GAGAL", e);
                        AlertUtil.showAlert(DaftarActivity.this, "Pendaftaran Gagal", "Gagal menyimpan data profil.", "OK", null, null, null);
                    });
        } else {
            Log.w("DAFTAR_DEBUG", "firebaseUser bernilai null di saveAdditionalUserData.");
        }
    }
}