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

        return view;
    }
}