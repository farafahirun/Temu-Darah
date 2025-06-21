package com.example.temudarah.fragment;

import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.temudarah.R;
import com.example.temudarah.activity.MainActivity;
import com.example.temudarah.databinding.FragmentDetailPermintaanBinding;
import com.example.temudarah.model.PermintaanDonor;
import com.example.temudarah.model.ProsesDonor;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.Arrays;
import java.util.Locale;

public class DetailPermintaanFragment extends Fragment {

    private static final String TAG = "DetailPermintaan";
    private static final String ARG_REQUEST_ID = "request_id";

    private FragmentDetailPermintaanBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String requestId;
    private PermintaanDonor currentPermintaan;

    public static DetailPermintaanFragment newInstance(String requestId) {
        DetailPermintaanFragment fragment = new DetailPermintaanFragment();
        Bundle args = new Bundle();
        args.putString(ARG_REQUEST_ID, requestId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            requestId = getArguments().getString(ARG_REQUEST_ID);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentDetailPermintaanBinding.inflate(inflater, container, false);
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).hideBottomNav();
        }
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        setupListeners();

        if (requestId != null) {
            loadRequestDetails();
        } else {
            Toast.makeText(getContext(), "Error: ID Permintaan tidak valid.", Toast.LENGTH_SHORT).show();
            getParentFragmentManager().popBackStack();
        }
    }

    private void setupListeners() {
        binding.btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());
        binding.btnBeriBantuan.setOnClickListener(v -> offerHelp());
    }

    private void loadRequestDetails() {
        binding.progressBar.setVisibility(View.VISIBLE);
        DocumentReference docRef = db.collection("donation_requests").document(requestId);

        docRef.get().addOnSuccessListener(documentSnapshot -> {
            binding.progressBar.setVisibility(View.GONE);
            if (documentSnapshot.exists()) {
                currentPermintaan = documentSnapshot.toObject(PermintaanDonor.class);
                if (currentPermintaan != null) {
                    // Panggil fungsi untuk mengisi data ke UI
                    populateUi(currentPermintaan);
                }
            } else {
                Toast.makeText(getContext(), "Permintaan ini mungkin sudah tidak tersedia.", Toast.LENGTH_SHORT).show();
                getParentFragmentManager().popBackStack();
            }
        }).addOnFailureListener(e -> {
            binding.progressBar.setVisibility(View.GONE);
            Log.e(TAG, "Gagal memuat detail", e);
        });
    }

    /**
     * FUNGSI LENGKAP: Mengisi semua komponen UI dengan data dari objek PermintaanDonor.
     */
    private void populateUi(PermintaanDonor permintaan) {
        // Mengisi data pembuat permintaan
        binding.tvNamaPembuat.setText("Diposting oleh: " + permintaan.getNamaPembuat());
        if (permintaan.getFotoPembuatBase64() != null && !permintaan.getFotoPembuatBase64().isEmpty() && getContext() != null) {
            try {
                byte[] imageBytes = Base64.decode(permintaan.getFotoPembuatBase64(), Base64.DEFAULT);
                Glide.with(requireContext()).asBitmap().load(imageBytes).placeholder(R.drawable.logo_merah).into(binding.ivPembuatFoto);
            } catch (Exception e) {
                binding.ivPembuatFoto.setImageResource(R.drawable.logo_merah);
            }
        } else {
            binding.ivPembuatFoto.setImageResource(R.drawable.logo_merah);
        }

        // Mengisi detail permintaan
        binding.tvDetailNamaPasien.setText(permintaan.getNamaPasien());
        binding.tvDetailJenisKelamin.setText(permintaan.getJenisKelamin() != null ? permintaan.getJenisKelamin() : "-"); // Set Jenis Kelamin
        binding.tvDetailGolDarah.setText(permintaan.getGolonganDarahDibutuhkan());
        binding.tvDetailJumlah.setText(String.format(Locale.getDefault(), "%d Kantong", permintaan.getJumlahKantong()));
        binding.tvDetailNamaRs.setText(permintaan.getNamaRumahSakit());
        binding.tvDetailCatatan.setText(permintaan.getCatatan());
        binding.tvDetailTanggalPenguguman.setText(permintaan.getTanggalPenguguman() != null && !permintaan.getTanggalPenguguman().isEmpty() ? permintaan.getTanggalPenguguman() : "-");
    }

    /**
     * FUNGSI LENGKAP: Logika untuk tombol "Beri Bantuan".
     */
    // Di dalam class DetailPermintaanFragment.java

    private void offerHelp() {
        FirebaseUser donorUser = mAuth.getCurrentUser();

        // Pengecekan awal (tidak berubah)
        if (donorUser == null || currentPermintaan == null) {
            Toast.makeText(getContext(), "Gagal, data tidak lengkap atau Anda belum login.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (donorUser.getUid().equals(currentPermintaan.getPembuatUid())) {
            Toast.makeText(getContext(), "Anda tidak bisa membantu permintaan Anda sendiri.", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.btnBeriBantuan.setEnabled(false);
        binding.btnBeriBantuan.setText("Memproses...");

        // Buat ID ruang chat yang konsisten
        String chatRoomId = createChatRoomId(donorUser.getUid(), currentPermintaan.getPembuatUid());

        // --- PERBAIKAN UTAMA: Pastikan SEMUA data untuk ProsesDonor diisi ---
        ProsesDonor newProcess = new ProsesDonor();
        newProcess.setRequestId(requestId); // ID dari permintaan donor aslinya
        newProcess.setChatRoomId(chatRoomId);
        newProcess.setParticipants(Arrays.asList(currentPermintaan.getPembuatUid(), donorUser.getUid()));
        newProcess.setStatusProses("Berlangsung");
        newProcess.setTimestamp(Timestamp.now());
        // Tambahkan ID peminta dan pendonor secara eksplisit
        newProcess.setRequesterId(currentPermintaan.getPembuatUid());
        newProcess.setDonorId(donorUser.getUid());
        // Inisialisasi pesan terakhir agar tidak null
        newProcess.setLastMessage("Ketuk untuk memulai percakapan");
        newProcess.setLastMessageTimestamp(Timestamp.now());


        // --- Transaksi Aman dengan WriteBatch (Tidak berubah) ---
        WriteBatch batch = db.batch();

        // Operasi 1: Update status di 'donation_requests'
        DocumentReference requestRef = db.collection("donation_requests").document(requestId);
        batch.update(requestRef, "status", "Dalam Proses");

        // Operasi 2: Buat dokumen "jabat tangan" baru di 'active_donations'
        DocumentReference donationProcessRef = db.collection("active_donations").document(chatRoomId);
        batch.set(donationProcessRef, newProcess); // Gunakan objek newProcess yang sudah lengkap

        // Jalankan kedua operasi
        batch.commit().addOnSuccessListener(aVoid -> {
            Toast.makeText(getContext(), "Anda telah menawarkan bantuan! Silakan mulai percakapan.", Toast.LENGTH_LONG).show();
            navigateToChatRoom(chatRoomId, currentPermintaan.getNamaPembuat());
        }).addOnFailureListener(e -> {
            Toast.makeText(getContext(), "Gagal menawarkan bantuan: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            binding.btnBeriBantuan.setEnabled(true);
            binding.btnBeriBantuan.setText("Beri Bantuan");
        });
    }

    private void navigateToChatRoom(String chatRoomId, String otherUserName) {
        // Pengecekan keamanan untuk memastikan fragment masih "hidup"
        if (getParentFragmentManager() == null || getContext() == null) {
            return;
        }

        // Buat instance baru dari ChatRoomFragment dengan membawa data yang diperlukan
        Fragment chatFragment = ChatRoomFragment.newInstance(chatRoomId, otherUserName);

        // Lakukan transaksi untuk mengganti fragment saat ini dengan fragment chat
        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, chatFragment)
                .addToBackStack(null) // Penting! Agar pengguna bisa menekan tombol back untuk kembali ke halaman detail ini
                .commit();
    }

    private String createChatRoomId(String uid1, String uid2) {
        if (uid1.compareTo(uid2) > 0) {
            return uid1 + "_" + uid2;
        } else {
            return uid2 + "_" + uid1;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
