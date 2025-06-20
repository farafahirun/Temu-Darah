package com.example.temudarah.fragment;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.temudarah.activity.MainActivity;
import com.example.temudarah.databinding.FragmentHelpCenterBinding;
import java.util.ArrayList;
import java.util.List;

public class HelpCenterFragment extends Fragment {
    private FragmentHelpCenterBinding binding;
    private List<View> helpTopicViews;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHelpCenterBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).hideBottomNav();
        }
        initializeHelpTopicsList();
        setupListeners();
    }

    private void initializeHelpTopicsList() {
        helpTopicViews = new ArrayList<>();
        helpTopicViews.add(binding.getStarted);
        helpTopicViews.add(binding.chats);
        helpTopicViews.add(binding.userManual);
        helpTopicViews.add(binding.connectFriends);
        helpTopicViews.add(binding.voiceVideoCalls);
        helpTopicViews.add(binding.communities);
        helpTopicViews.add(binding.privacySafetySecurity);
        helpTopicViews.add(binding.accountBans);
        helpTopicViews.add(binding.tutorialVideo);
        helpTopicViews.add(binding.bugReport);
    }

    private void setupListeners() {
        binding.btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());
        binding.inputTelusuri.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                binding.btnContactUs.setVisibility(View.GONE);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        binding.telusuri.setOnClickListener(v -> {
            String query = binding.inputTelusuri.getText().toString();
            filterHelpTopics(query);
            binding.btnContactUs.setVisibility(View.VISIBLE);
            hideKeyboard();
        });

        binding.inputTelusuri.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                binding.telusuri.performClick();
                return true;
            }
            return false;
        });
    }
    private void filterHelpTopics(String query) {
        String lowerCaseQuery = query.toLowerCase().trim();
        if (lowerCaseQuery.isEmpty()) {
            for (View topicView : helpTopicViews) {
                topicView.setVisibility(View.VISIBLE);
            }
            return;
        }

        for (View topicView : helpTopicViews) {
            if (topicView instanceof LinearLayout && ((LinearLayout) topicView).getChildCount() > 1) {
                TextView textView = (TextView) ((LinearLayout) topicView).getChildAt(1);
                String topicText = textView.getText().toString().toLowerCase();

                if (topicText.contains(lowerCaseQuery)) {
                    topicView.setVisibility(View.VISIBLE);
                } else {
                    topicView.setVisibility(View.GONE);
                }
            }
        }
    }

    private void hideKeyboard() {
        if (getContext() == null || getView() == null) return;
        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(getView().getWindowToken(), 0);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}