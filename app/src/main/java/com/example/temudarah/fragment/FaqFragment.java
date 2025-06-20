package com.example.temudarah.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.transition.AutoTransition;
import androidx.transition.TransitionManager;

import com.example.temudarah.R;
import com.example.temudarah.activity.MainActivity;
import com.example.temudarah.databinding.FragmentFaqBinding;

public class FaqFragment extends Fragment {
    private FragmentFaqBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentFaqBinding.inflate(inflater, container, false);
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).hideBottomNav();
        }
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());
        setupFaqItem(binding.arrowItem1, binding.answerItem1, binding.dividerItem1);
        setupFaqItem(binding.arrowItem2, binding.answerItem2, binding.dividerItem2);
        setupFaqItem(binding.arrowItem3, binding.answerItem3, binding.dividerItem3);
        setupFaqItem(binding.arrowItem4, binding.answerItem4, binding.dividerItem4);
        setupFaqItem(binding.arrowItem5, binding.answerItem5, binding.dividerItem5);
        setupFaqItem(binding.arrowItem6, binding.answerItem6, binding.dividerItem6);
        setupFaqItem(binding.arrowItem7, binding.answerItem7, binding.dividerItem7);
        setupFaqItem(binding.arrowItem8, binding.answerItem8, binding.dividerItem8);
    }

    private void setupFaqItem(ImageView arrowImageView, TextView answerTextView, View divider) {
        arrowImageView.setOnClickListener(v -> {
            TransitionManager.beginDelayedTransition((ViewGroup) binding.getRoot(), new AutoTransition());
            boolean isGone = answerTextView.getVisibility() == View.GONE;

            if (isGone) {
                answerTextView.setVisibility(View.VISIBLE);
                divider.setVisibility(View.VISIBLE);
                arrowImageView.setImageResource(R.drawable.panah_atas);
            } else {
                answerTextView.setVisibility(View.GONE);
                divider.setVisibility(View.GONE);
                arrowImageView.setImageResource(R.drawable.panah_bawah);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}