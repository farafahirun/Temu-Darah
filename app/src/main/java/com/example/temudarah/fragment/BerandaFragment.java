package com.example.temudarah.fragment;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.example.temudarah.R;
import com.example.temudarah.adapter.PendonorAdapter;
import com.example.temudarah.model.Pendonor;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;

public class BerandaFragment extends Fragment {
    
    private RecyclerView rvPendonor;
    private PendonorAdapter pendonorAdapter;
    private List<Pendonor> pendonorList;
    private AutoCompleteTextView autoCompleteGender, autoCompleteGolonganDarah;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_beranda, container, false);

        // Inisialisasi dan event klik pada ikon notifikasi
        ImageView ivNotification = view.findViewById(R.id.iv_notification);
        ivNotification.setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new NotifikasiFragment())
                    .addToBackStack(null)
                    .commit();
        });

        // Inisialisasi RecyclerView
        rvPendonor = view.findViewById(R.id.rv_pendonor);
        rvPendonor.setLayoutManager(new LinearLayoutManager(getContext()));

        // Inisialisasi data dummy
        loadDummyData();

        // Setup Adapter
        pendonorAdapter = new PendonorAdapter(getContext(), pendonorList, pendonor -> {
            // Handle klik pada item pendonor
            Toast.makeText(getContext(), "Pendonor dipilih: " + pendonor.getNama(), Toast.LENGTH_SHORT).show();
        });

        rvPendonor.setAdapter(pendonorAdapter);

        // Setup Filter Gender
        autoCompleteGender = view.findViewById(R.id.autoCompleteGender);
        String[] genderOptions = {"Laki-laki", "Perempuan"};
        ArrayAdapter<String> genderAdapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_dropdown_item_1line, genderOptions);
        autoCompleteGender.setAdapter(genderAdapter);

        // Setup Filter Golongan Darah
        autoCompleteGolonganDarah = view.findViewById(R.id.autoCompleteGolonganDarah);
        String[] goldarOptions = {"A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"};
        ArrayAdapter<String> goldarAdapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_dropdown_item_1line, goldarOptions);
        autoCompleteGolonganDarah.setAdapter(goldarAdapter);

        return view;
    }

    // ini datanyaa (input manual yaaa abang kuuuu)
    private void loadDummyData() {
        pendonorList = new ArrayList<>();

        // Tambahkan data dummy pendonor
        pendonorList.add(new Pendonor("1", "Budi Santoso", "A+", "Laki-laki",
                "PMI Kota Bandung, Jl. Aceh No.79", "25 tahun", R.drawable.kuronuma));

        pendonorList.add(new Pendonor("2", "Nina Agustina", "O+", "Perempuan",
                "RS Santo Borromeus, Jl. Ir. H. Juanda No.100", "28 tahun", R.drawable.kuronuma));

        pendonorList.add(new Pendonor("3", "Rudi Hermawan", "B-", "Laki-laki",
                "PMI Kota Bandung, Jl. Aceh No.79", "32 tahun", R.drawable.kuronuma));

        pendonorList.add(new Pendonor("4", "Dewi Lestari", "AB+", "Perempuan",
                "RS Hasan Sadikin, Jl. Pasteur No.38", "24 tahun", R.drawable.kuronuma));

        pendonorList.add(new Pendonor("5", "Andi Wijaya", "A-", "Laki-laki",
                "RS Advent Bandung, Jl. Cihampelas No.161", "30 tahun", R.drawable.kuronuma));

        pendonorList.add(new Pendonor("6", "Siti Nur", "O-", "Perempuan",
                "PMI Kota Bandung, Jl. Aceh No.79", "27 tahun", R.drawable.kuronuma));
    }
}