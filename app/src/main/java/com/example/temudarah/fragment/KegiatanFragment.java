package com.example.temudarah.fragment;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.example.temudarah.R;

public class KegiatanFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_kegiatan, container, false);

        // Inisialisasi dan event klik pada ikon notifikasi
        ImageView ivNotification = view.findViewById(R.id.iv_notification);
        ivNotification.setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new NotifikasiFragment())
                    .addToBackStack(null)
                    .commit();
        });

        return view;
    }
}