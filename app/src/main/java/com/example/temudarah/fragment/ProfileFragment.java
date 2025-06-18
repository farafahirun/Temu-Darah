package com.example.temudarah.fragment;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.temudarah.R;

public class ProfileFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // Navigasi ke Edit Profile saat layoutNotifikasi diklik
        View layoutEditProfile = view.findViewById(R.id.layoutEditProfile);
        layoutEditProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigasi ke FaqFragment
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new EditProfileFragment())
                        .addToBackStack(null)
                        .commit();
            }
        });

        // Navigasi ke Notifikasi saat layoutNotifikasi diklik
        View layoutNotifikasi = view.findViewById(R.id.layoutNotifications);
        layoutNotifikasi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigasi ke FaqFragment
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new PengaturanNotifikasiFragment())
                        .addToBackStack(null)
                        .commit();
            }
        });

        // Navigasi ke FAQ saat layoutFAQs diklik
        View layoutFAQs = view.findViewById(R.id.layoutFAQs);
        layoutFAQs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigasi ke FaqFragment
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new FaqFragment())
                        .addToBackStack(null)
                        .commit();
            }
        });

        // Navigasi ke Syarat & Ketentuan saat layoutTerms diklik
        View layoutTerms = view.findViewById(R.id.layoutTerms);
        layoutTerms.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigasi ke SyaratKetentuanFragment
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new SyaratKetentuanFragment())
                        .addToBackStack(null)
                        .commit();
            }
        });

        // Navigasi ke Privasi saat layoutPrivacy diklik
        View layoutPrivacy = view.findViewById(R.id.layoutPrivacy);
        layoutPrivacy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigasi ke PrivasiFragment
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new PrivasiFragment())
                        .addToBackStack(null)
                        .commit();
            }
        });

        // Navigasi ke Pusat Bantuan saat layoutHelpCenter diklik
        View layoutHelpCenter = view.findViewById(R.id.layoutHelpCenter);
        layoutHelpCenter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigasi ke HelpCenterFragment
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new HelpCenterFragment())
                        .addToBackStack(null)
                        .commit();
            }
        });

        return view;
    }
}