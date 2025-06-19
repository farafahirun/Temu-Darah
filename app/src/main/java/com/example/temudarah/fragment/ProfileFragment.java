package com.example.temudarah.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
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

public class ProfileFragment extends Fragment {
    private static final String TAG = "ProfileFragment";
    private FragmentProfileBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

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

        if (currentUser != null) {
            loadUserProfileData();
        } else {
            Toast.makeText(getContext(), "Pengguna tidak ditemukan, silakan login kembali.", Toast.LENGTH_LONG).show();
            logoutUser();
        }
        setupMenuListeners();
    }

    private void loadUserProfileData() {
        binding.tvName.setText("Memuat...");
        String uid = currentUser.getUid();
        DocumentReference userDocRef = db.collection("users").document(uid);

        userDocRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                User user = documentSnapshot.toObject(User.class);
                if (user != null) {
                    populateUi(user);
                }
            } else {
                Toast.makeText(getContext(), "Data profil tidak ditemukan.", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Gagal memuat data profil", e);
            Toast.makeText(getContext(), "Gagal memuat profil.", Toast.LENGTH_SHORT).show();
        });
    }

    private void populateUi(User user) {
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
    }

    private void setupMenuListeners() {
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
        binding = null;
    }
}