package com.example.temudarah.fragment;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.temudarah.R;

public class PrivasiFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_privasi, container, false);

        // Fungsi tombol back
        View btnBack = view.findViewById(R.id.navBar).findViewById(R.id.btnBack) != null ? view.findViewById(R.id.navBar).findViewById(R.id.btnBack) : null;
        if (btnBack == null) {
            // fallback: cari ImageView pertama di navBar
            View navBar = view.findViewById(R.id.navBar);
            if (navBar instanceof ViewGroup) {
                ViewGroup navBarGroup = (ViewGroup) navBar;
                for (int i = 0; i < navBarGroup.getChildCount(); i++) {
                    View child = navBarGroup.getChildAt(i);
                    if (child instanceof android.widget.ImageView) {
                        btnBack = child;
                        break;
                    }
                }
            }
        }
        if (btnBack != null) {
            btnBack.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    requireActivity().getSupportFragmentManager().popBackStack();
                }
            });
        }
        return view;
    }
}