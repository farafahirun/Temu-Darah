package com.example.temudarah.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.example.temudarah.R;
import com.example.temudarah.databinding.FragmentDetailRiwayatBinding;
import com.example.temudarah.model.PermintaanDonor;
import com.example.temudarah.model.ProsesDonor;
import com.example.temudarah.model.User;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class DetailRiwayatFragment extends Fragment {

    private static final String TAG = "DetailRiwayatFragment";
    private static final String ARG_PROSES_ID = "proses_id";

    private FragmentDetailRiwayatBinding binding;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private String prosesId;

    public static DetailRiwayatFragment newInstance(String prosesId) {
        DetailRiwayatFragment fragment = new DetailRiwayatFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PROSES_ID, prosesId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            prosesId = getArguments().getString(ARG_PROSES_ID);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentDetailRiwayatBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        binding.toolbar.setNavigationOnClickListener(v -> getParentFragmentManager().popBackStack());

        if (prosesId != null) {
            loadRiwayatDetails();
        }
    }

    // Di dalam class DetailRiwayatFragment.java

    private void loadRiwayatDetails() {
        binding.progressBar.setVisibility(View.VISIBLE);
        db.collection("active_donations").document(prosesId).get()
                .addOnSuccessListener(prosesDoc -> {
                    if (!isAdded() || !prosesDoc.exists()) {
                        if (isAdded()) Toast.makeText(getContext(), "Riwayat tidak ditemukan.", Toast.LENGTH_SHORT).show();
                        binding.progressBar.setVisibility(View.GONE);
                        return;
                    }

                    ProsesDonor proses = prosesDoc.toObject(ProsesDonor.class);
                    if (proses == null) return;

                    String otherUserId = Objects.equals(currentUser.getUid(), proses.getRequesterId()) ? proses.getDonorId() : proses.getRequesterId();

                    Task<DocumentSnapshot> userTask = db.collection("users").document(otherUserId).get();
                    Task<DocumentSnapshot> requestTask = db.collection("donation_requests").document(proses.getRequestId()).get();

                    // Kita gunakan List<Task<?>> agar lebih fleksibel
                    List<Task<?>> allTasks = new ArrayList<>();
                    allTasks.add(userTask);
                    allTasks.add(requestTask);

                    Tasks.whenAllSuccess(allTasks).addOnSuccessListener(results -> {
                        if (!isAdded()) return;

                        // --- PERBAIKAN UTAMA DI SINI (CASTING) ---
                        // Kita konfirmasi secara manual bahwa hasil ke-0 adalah DocumentSnapshot
                        DocumentSnapshot userResultDoc = (DocumentSnapshot) results.get(0);
                        // Kita konfirmasi juga hasil ke-1 adalah DocumentSnapshot
                        DocumentSnapshot requestResultDoc = (DocumentSnapshot) results.get(1);

                        if (userResultDoc.exists() && requestResultDoc.exists()) {
                            User otherUser = userResultDoc.toObject(User.class);
                            PermintaanDonor permintaan = requestResultDoc.toObject(PermintaanDonor.class);
                            populateUi(proses, permintaan, otherUser);
                        }
                        binding.progressBar.setVisibility(View.GONE);
                    }).addOnFailureListener(e -> {
                        if(isAdded()) binding.progressBar.setVisibility(View.GONE);
                        Log.e(TAG, "Gagal mengambil detail user/permintaan.", e);
                    });
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) binding.progressBar.setVisibility(View.GONE);
                    Log.e(TAG, "Gagal memuat detail riwayat", e);
                });
    }

    private void populateUi(ProsesDonor proses, PermintaanDonor permintaan, User otherUser) {
        if (getContext() == null) return;

        binding.tvDetailStatus.setText("Status: " + proses.getStatusProses());
        binding.tvDetailPasien.setText(permintaan.getNamaPasien() + " (" + permintaan.getGolonganDarahDibutuhkan() + ")");
        binding.tvDetailLokasi.setText(permintaan.getNamaRumahSakit());

        if (proses.getTimestamp() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale.forLanguageTag("id-ID"));
            binding.tvDetailTanggal.setText("Pada " + sdf.format(proses.getTimestamp().toDate()));
        }

        boolean sayaPendonor = currentUser.getUid().equals(proses.getDonorId());
        if (sayaPendonor) {
            binding.tvDetailPeran.setText("Anda Membantu");
            binding.tvDetailNamaLawan.setText("Permintaan dari " + otherUser.getFullName());
        } else {
            binding.tvDetailPeran.setText("Anda Dibantu oleh");
            binding.tvDetailNamaLawan.setText(otherUser.getFullName());
        }

        // Atur warna status
        int colorRes = R.color.text_info;
        if ("Selesai".equals(proses.getStatusProses())) colorRes = R.color.sukses;
        else if ("Dibatalkan".equals(proses.getStatusProses())) colorRes = R.color.utama;
        binding.tvDetailStatus.setTextColor(ContextCompat.getColor(getContext(), colorRes));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}