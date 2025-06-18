package com.example.temudarah.fragment;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.fragment.app.Fragment;
import androidx.appcompat.widget.SwitchCompat;
import com.example.temudarah.databinding.FragmentPengaturanNotifikasiBinding;

public class PengaturanNotifikasiFragment extends Fragment {

    private FragmentPengaturanNotifikasiBinding binding;
    private SharedPreferences sharedPreferences;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        binding = FragmentPengaturanNotifikasiBinding.inflate(inflater, container, false);

        binding.btnBack.setOnClickListener(v -> {getParentFragmentManager().popBackStack(); });

        sharedPreferences = getActivity().getSharedPreferences("AppSettings", getContext().MODE_PRIVATE);
        // Mengakses SwitchCompat menggunakan binding
        SwitchCompat switchAllNotifications = binding.switchAllNotifications;
        SwitchCompat switchDonationSchedule = binding.switchDonationSchedule;
        SwitchCompat switch3DayReminder = binding.switch3DayReminder;
        SwitchCompat switchDonorPeriod = binding.switchDonorPeriod;
        SwitchCompat switchEmergencySearch = binding.switchEmergencySearch;
        SwitchCompat switchEmergencyDonation = binding.switchEmergencyDonation;
        SwitchCompat switchBloodAvailability = binding.switchBloodAvailability;

        // Menetapkan status awal switch berdasarkan data yang disimpan
        setSwitchInitialState(switchAllNotifications, "switchAllNotifications");
        setSwitchInitialState(switchDonationSchedule, "switchDonationSchedule");
        setSwitchInitialState(switch3DayReminder, "switch3DayReminder");
        setSwitchInitialState(switchDonorPeriod, "switchDonorPeriod");
        setSwitchInitialState(switchEmergencySearch, "switchEmergencySearch");
        setSwitchInitialState(switchEmergencyDonation, "switchEmergencyDonation");
        setSwitchInitialState(switchBloodAvailability, "switchBloodAvailability");

        // Set listener untuk perubahan status semua switch
        setSwitchChangeListener(switchAllNotifications, "switchAllNotifications");
        setSwitchChangeListener(switchDonationSchedule, "switchDonationSchedule");
        setSwitchChangeListener(switch3DayReminder, "switch3DayReminder");
        setSwitchChangeListener(switchDonorPeriod, "switchDonorPeriod");
        setSwitchChangeListener(switchEmergencySearch, "switchEmergencySearch");
        setSwitchChangeListener(switchEmergencyDonation, "switchEmergencyDonation");
        setSwitchChangeListener(switchBloodAvailability, "switchBloodAvailability");

        return binding.getRoot(); // Mengembalikan root view dari layout
    }

    // Method untuk mengatur status awal switch berdasarkan SharedPreferences
    private void setSwitchInitialState(SwitchCompat switchCompat, String switchKey) {
        boolean isChecked = sharedPreferences.getBoolean(switchKey, false); // Default false jika belum ada data
        switchCompat.setChecked(isChecked);
    }

    // Method untuk menyetel listener pada semua switch
    private void setSwitchChangeListener(SwitchCompat switchCompat, String switchKey) {
        switchCompat.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean(switchKey, isChecked);
            editor.apply();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
