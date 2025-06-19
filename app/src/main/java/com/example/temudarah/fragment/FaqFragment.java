package com.example.temudarah.fragment;

import android.os.Bundle;
import android.transition.AutoTransition;
import android.transition.TransitionManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.fragment.app.Fragment;

import com.example.temudarah.R;

public class FaqFragment extends Fragment {

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private String mParam1;
    private String mParam2;

    public FaqFragment() {}

    public static FaqFragment newInstance(String param1, String param2) {
        FaqFragment fragment = new FaqFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_faq, container, false);

        View btnBack = view.findViewById(R.id.iv_back);
        btnBack.setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack()
        );

        int[] arrowIds = {
                R.id.arrowItem1, R.id.arrowItem2, R.id.arrowItem3, R.id.arrowItem4,
                R.id.arrowItem5, R.id.arrowItem6, R.id.arrowItem7, R.id.arrowItem8
        };
        int[] answerIds = {
                R.id.answerItem1, R.id.answerItem2, R.id.answerItem3, R.id.answerItem4,
                R.id.answerItem5, R.id.answerItem6, R.id.answerItem7, R.id.answerItem8
        };

        for (int i = 0; i < arrowIds.length; i++) {
            final View answer = view.findViewById(answerIds[i]);
            final ImageView arrow = view.findViewById(arrowIds[i]);

            if (arrow != null && answer != null) {
                arrow.setOnClickListener(new View.OnClickListener() {
                    boolean isExpanded = false;

                    @Override
                    public void onClick(View v) {
                        ViewGroup parent = (ViewGroup) answer.getParent();
                        TransitionManager.beginDelayedTransition(parent, new AutoTransition());

                        if (isExpanded) {
                            answer.animate()
                                    .alpha(0f)
                                    .setDuration(200)
                                    .withEndAction(() -> answer.setVisibility(View.GONE))
                                    .start();
                            arrow.animate().rotation(0).setDuration(200).start();
                        } else {
                            answer.setAlpha(0f);
                            answer.setVisibility(View.VISIBLE);
                            answer.animate()
                                    .alpha(1f)
                                    .setDuration(200)
                                    .start();
                            arrow.animate().rotation(180).setDuration(200).start();
                        }

                        isExpanded = !isExpanded;
                    }
                });
            } else {
                Log.e("FaqFragment", "ID arrow/answer tidak ditemukan: arrow=" + arrowIds[i] + ", answer=" + answerIds[i]);
            }
        }

        return view;
    }
}
