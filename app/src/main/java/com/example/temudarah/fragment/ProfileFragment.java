package com.example.temudarah.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.temudarah.R;
import com.example.temudarah.activity.MasukActivity;
import com.example.temudarah.databinding.FragmentProfileBinding;
import com.example.temudarah.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProfileFragment extends Fragment {
    private static final String TAG = "ProfileFragment";
    private FragmentProfileBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private ExecutorService executorService;
    private Handler mainHandler;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();

        executorService = Executors.newFixedThreadPool(2);
        mainHandler = new Handler(Looper.getMainLooper());

        // Show loading for 2 seconds
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.scrollViewProfile.setVisibility(View.GONE);

        showLoading();

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (currentUser != null) {
                loadUserProfileData();
            } else {
                Toast.makeText(getContext(), "Pengguna tidak ditemukan, silakan login kembali.", Toast.LENGTH_LONG).show();
                logoutUser();
            }
            setupMenuListeners();
        }, 1000);

    }

    private void showLoading() {
        if (binding != null) {
            binding.loadingOverlay.setVisibility(View.VISIBLE);
            binding.scrollViewProfile.setVisibility(View.GONE);
        }
    }

    private void hideLoading() {
        if (binding != null) {
            binding.loadingOverlay.setVisibility(View.GONE);
            binding.scrollViewProfile.setVisibility(View.VISIBLE);
        }
    }

    private void loadUserProfileData() {
        String uid = currentUser.getUid();
        DocumentReference userDocRef = db.collection("users").document(uid);

        userDocRef.get().addOnSuccessListener(documentSnapshot -> {
            executorService.execute(() -> {
                User user = null;
                if (documentSnapshot.exists()) {
                    user = documentSnapshot.toObject(User.class);
                }

                User finalUser = user;
                mainHandler.post(() -> {
                    if (binding != null && getContext() != null) {
                        if (finalUser != null) {
                            populateUi(finalUser);
                        } else {
                            Toast.makeText(getContext(), "Data profil tidak dapat di-parse atau tidak ditemukan.", Toast.LENGTH_SHORT).show();
                            // Atur teks ke default jika gagal
                            binding.tvName.setText("Tidak tersedia");
                            binding.tvBloodType.setText("-");
                            binding.tvGender.setText("-");
                            binding.tvWeight.setText("-");
                            binding.tvHeight.setText("-");
                            binding.tvAge.setText("-");
                            binding.tvLastDonation.setText("Tidak tersedia");
                            binding.profileImage.setImageResource(R.drawable.logo_merah);
                        }
                        hideLoading(); // Sembunyikan ProgressBar setelah data dimuat (baik berhasil/gagal parse)
                    }
                });
            });
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Gagal memuat data profil", e);
            mainHandler.post(() -> {
                if (binding != null && getContext() != null) {
                    Toast.makeText(getContext(), "Gagal memuat profil. Silakan coba lagi.", Toast.LENGTH_SHORT).show();
                    // Atur teks ke default jika gagal
                    binding.tvName.setText("Error");
                    binding.tvBloodType.setText("-");
                    binding.tvGender.setText("-");
                    binding.tvWeight.setText("-");
                    binding.tvHeight.setText("-");
                    binding.tvAge.setText("-");
                    binding.tvLastDonation.setText("Tidak tersedia");
                    binding.profileImage.setImageResource(R.drawable.logo_merah);
                    hideLoading(); // Sembunyikan ProgressBar setelah gagal
                }
            });
        });
    }

    private void populateUi(User user) {
        if (binding == null) {
            return;
        }
        binding.tvName.setText(user.getFullName());
        binding.tvBloodType.setText(user.getBloodType());
        binding.tvGender.setText(user.getGender());
        binding.tvWeight.setText(String.valueOf(user.getWeight()));
        binding.tvHeight.setText(String.valueOf(user.getHeight()));

        if (user.getBirthDate() != null && !user.getBirthDate().isEmpty()) {
            binding.tvAge.setText(calculateAge(user.getBirthDate()));
        } else {
            binding.tvAge.setText("-");
        }

        if (user.getHasDonatedBefore() != null && user.getHasDonatedBefore().equals("Ya") &&
                user.getLastDonationDate() != null && !user.getLastDonationDate().isEmpty()) {
            binding.tvLastDonation.setText("Terakhir Donor: " + user.getLastDonationDate());
            binding.tvLastDonation.setVisibility(View.VISIBLE);
        } else {
            binding.tvLastDonation.setText("Belum Pernah Donor");
            binding.tvLastDonation.setVisibility(View.VISIBLE);
        }

        if (user.getProfileImageBase64() != null && !user.getProfileImageBase64().isEmpty() && getContext() != null) {
            try {
                byte[] imageBytes = Base64.decode(user.getProfileImageBase64(), Base64.DEFAULT);

                Glide.with(requireContext())
                        .asBitmap()
                        .load(imageBytes)
                        .placeholder(R.drawable.logo_merah)
                        .error(R.drawable.logo_merah)
                        .into(binding.profileImage);
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "Gagal decode Base64: String bukan format Base64 yang valid.", e);
                binding.profileImage.setImageResource(R.drawable.logo_merah);
            } catch (Exception e) {
                Log.e(TAG, "Kesalahan saat memuat gambar profil.", e);
                binding.profileImage.setImageResource(R.drawable.logo_merah);
            }
        } else {
            binding.profileImage.setImageResource(R.drawable.logo_merah);
        }
    }

    private void setupMenuListeners() {
        if (binding == null) {
            return;
        }
        binding.ivNotification.setOnClickListener(v -> navigateToFragment(new NotifikasiFragment()));
        binding.layoutEditProfile.setOnClickListener(v -> navigateToFragment(new EditProfileFragment()));
        binding.layoutNotifications.setOnClickListener(v -> navigateToFragment(new PengaturanNotifikasiFragment()));
        binding.layoutPrivacy.setOnClickListener(v -> navigateToFragment(new PrivasiFragment()));
        binding.layoutHelpCenter.setOnClickListener(v -> navigateToFragment(new HelpCenterFragment()));
        binding.layoutTerms.setOnClickListener(v -> navigateToFragment(new SyaratKetentuanFragment()));
        binding.layoutFAQs.setOnClickListener(v -> navigateToFragment(new FaqFragment()));
        binding.layoutLogout.setOnClickListener(v -> logoutUser());
    }

    private void navigateToFragment(Fragment fragment) {
        if (getParentFragmentManager() != null) {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(null)
                    .commit();
        }
    }

    private void logoutUser() {
        mAuth.signOut();
        if (getActivity() != null) {
            Toast.makeText(getContext(), "Anda telah logout.", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(getActivity(), MasukActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            getActivity().finish();
        }
    }

    private String calculateAge(String birthDateStr) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        try {
            Date birthDate = sdf.parse(birthDateStr);
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
        if (executorService != null) {
            executorService.shutdownNow();
        }
        binding = null;
    }
}