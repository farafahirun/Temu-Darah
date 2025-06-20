package com.example.temudarah.fragment;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.temudarah.activity.MainActivity;
import com.example.temudarah.databinding.FragmentPrivasiBinding;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class PrivasiFragment extends Fragment {
    private static final String TAG = "PrivasiFragment";
    private FragmentPrivasiBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentPrivasiBinding.inflate(inflater, container, false);

        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).hideBottomNav();
        }

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(getContext(), "Aksi ini memerlukan login.", Toast.LENGTH_LONG).show();
            if (getParentFragmentManager() != null) getParentFragmentManager().popBackStack();
            return;
        }

        setupListeners();
    }

    private void setupListeners() {
        binding.btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());
        binding.btnSave.setOnClickListener(v -> {
            if (validateInput()) {
                attemptPasswordChange();
            }
        });
    }

    private boolean validateInput() {
        binding.editPasswordLama.setError(null);
        binding.editPasswordBaru.setError(null);
        binding.editKonfirmasiPasswordBaru.setError(null);

        String oldPassword = binding.editPasswordLama.getText().toString().trim();
        String newPassword = binding.editPasswordBaru.getText().toString().trim();
        String confirmPassword = binding.editKonfirmasiPasswordBaru.getText().toString().trim();

        if (TextUtils.isEmpty(oldPassword)) {
            binding.editPasswordLama.setError("Password lama tidak boleh kosong");
            binding.editPasswordLama.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(newPassword)) {
            binding.editPasswordBaru.setError("Password baru tidak boleh kosong");
            binding.editPasswordBaru.requestFocus();
            return false;
        }

        if (newPassword.length() < 6) {
            binding.editPasswordBaru.setError("Password minimal 6 karakter");
            binding.editPasswordBaru.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(confirmPassword)) {
            binding.editKonfirmasiPasswordBaru.setError("Konfirmasi password tidak boleh kosong");
            binding.editKonfirmasiPasswordBaru.requestFocus();
            return false;
        }

        if (!newPassword.equals(confirmPassword)) {
            binding.editKonfirmasiPasswordBaru.setError("Password tidak cocok");
            binding.editKonfirmasiPasswordBaru.requestFocus();
            return false;
        }

        if (oldPassword.equals(newPassword)) {
            binding.editPasswordBaru.setError("Password baru tidak boleh sama dengan password lama");
            binding.editPasswordBaru.requestFocus();
            return false;
        }

        return true;
    }

    private void attemptPasswordChange() {
        if (currentUser == null || currentUser.getEmail() == null) return;

        String oldPassword = binding.editPasswordLama.getText().toString().trim();
        String newPassword = binding.editPasswordBaru.getText().toString().trim();

        binding.btnSave.setEnabled(false);
        binding.btnSave.setText("Memproses...");

        AuthCredential credential = EmailAuthProvider.getCredential(currentUser.getEmail(), oldPassword);
        currentUser.reauthenticate(credential)
                .addOnCompleteListener(reauthTask -> {
                    if (reauthTask.isSuccessful()) {
                        Log.d(TAG, "User re-authenticated.");

                        currentUser.updatePassword(newPassword).addOnCompleteListener(updateTask -> {
                            if (updateTask.isSuccessful()) {
                                Log.d(TAG, "User password updated.");
                                Toast.makeText(getContext(), "Password berhasil diperbarui.", Toast.LENGTH_SHORT).show();
                                if (getParentFragmentManager() != null) getParentFragmentManager().popBackStack();
                            } else {
                                Log.w(TAG, "Error updating password", updateTask.getException());
                                Toast.makeText(getContext(), "Gagal memperbarui password.", Toast.LENGTH_SHORT).show();
                            }
                        });

                    } else {
                        Log.w(TAG, "Re-authentication failed.", reauthTask.getException());
                        binding.editPasswordLama.setError("Password lama yang Anda masukkan salah");
                        binding.editPasswordLama.requestFocus();
                    }
                    binding.btnSave.setEnabled(true);
                    binding.btnSave.setText("Simpan Perubahan");
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}