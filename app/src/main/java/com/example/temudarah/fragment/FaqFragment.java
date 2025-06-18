package com.example.temudarah.fragment;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.temudarah.R;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link FaqFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class FaqFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public FaqFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment FaqFragment.
     */
    // TODO: Rename and change types and number of parameters
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
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_faq, container, false);

        // Setup toggle for all FAQ items (1-8) with error log if id not found
        int[] arrowIds = {R.id.arrowItem1, R.id.arrowItem2, R.id.arrowItem3, R.id.arrowItem4, R.id.arrowItem5, R.id.arrowItem6, R.id.arrowItem7, R.id.arrowItem8};
        int[] answerIds = {R.id.answerItem1, R.id.answerItem2, R.id.answerItem3, R.id.answerItem4, R.id.answerItem5, R.id.answerItem6, R.id.answerItem7, R.id.answerItem8};
        for (int i = 0; i < arrowIds.length; i++) {
            final View answer = view.findViewById(answerIds[i]);
            View arrow = view.findViewById(arrowIds[i]);
            if (arrow != null && answer != null) {
                arrow.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (answer.getVisibility() == View.VISIBLE) {
                            answer.setVisibility(View.GONE);
                        } else {
                            answer.setVisibility(View.VISIBLE);
                        }
                    }
                });
            } else {
                // Log error jika id tidak ditemukan
                android.util.Log.e("FaqFragment", "FAQ arrow or answer id not found: arrow=" + arrowIds[i] + ", answer=" + answerIds[i]);
            }
        }

        return view;
    }
}