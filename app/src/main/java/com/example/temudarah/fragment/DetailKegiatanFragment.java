package com.example.temudarah.fragment;

import android.os.Bundle;
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
import com.example.temudarah.databinding.FragmentDetailKegiatanBinding;
import com.example.temudarah.model.Kegiatan;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DetailKegiatanFragment extends Fragment {

    private static final String TAG = "DetailKegiatanFragment";
    private static final String ARG_KEGIATAN_ID = "kegiatan_id";

    private FragmentDetailKegiatanBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String kegiatanId;
    private Kegiatan currentKegiatan; // Untuk menyimpan data kegiatan yang sedang dilihat

    // Factory method untuk membuat instance fragment ini dengan membawa data ID.
    public static DetailKegiatanFragment newInstance(String kegiatanId) {
        DetailKegiatanFragment fragment = new DetailKegiatanFragment();
        Bundle args = new Bundle();
        args.putString(ARG_KEGIATAN_ID, kegiatanId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            kegiatanId = getArguments().getString(ARG_KEGIATAN_ID);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentDetailKegiatanBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        if (kegiatanId != null) {
            loadKegiatanDetails();
        } else {
            Toast.makeText(getContext(), "Error: Gagal memuat kegiatan.", Toast.LENGTH_SHORT).show();
            getParentFragmentManager().popBackStack();
        }

        setupListeners();
    }

    private void setupListeners() {
        // Menggunakan navigationIcon dari Toolbar untuk tombol kembali
        binding.toolbarDetailKegiatan.setNavigationOnClickListener(v -> getParentFragmentManager().popBackStack());

        binding.btnDaftar.setOnClickListener(v -> registerToKegiatan());

        binding.btnSimpan.setOnClickListener(v -> {
            // TODO: Logika untuk menyimpan ID kegiatan ke profil user
            Toast.makeText(getContext(), "Fitur 'Simpan' akan datang!", Toast.LENGTH_SHORT).show();
        });
    }

    private void loadKegiatanDetails() {
        binding.progressBar.setVisibility(View.VISIBLE);
        DocumentReference docRef = db.collection("kegiatan_acara").document(kegiatanId);

        docRef.get().addOnSuccessListener(documentSnapshot -> {
            binding.progressBar.setVisibility(View.GONE);
            if (documentSnapshot.exists()) {
                currentKegiatan = documentSnapshot.toObject(Kegiatan.class);
                if (currentKegiatan != null) {
                    currentKegiatan.setKegiatanId(documentSnapshot.getId());
                    populateUi(currentKegiatan);
                }
            } else {
                Toast.makeText(getContext(), "Kegiatan ini mungkin sudah tidak tersedia.", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e -> {
            binding.progressBar.setVisibility(View.GONE);
            Log.e(TAG, "Gagal memuat detail kegiatan", e);
        });
    }

    private void populateUi(Kegiatan kegiatan) {
        binding.tvDetailJudul.setText(kegiatan.getJudul());
        binding.chipDetailJenis.setText(kegiatan.getJenisKegiatan());
        binding.tvDetailPenyelenggara.setText("oleh " + kegiatan.getPenyelenggara());
        binding.tvDetailLokasi.setText(kegiatan.getLokasiDetail());
        binding.tvDetailDeskripsi.setText(kegiatan.getDeskripsi());

        // Format tanggal dan waktu
        if (kegiatan.getTanggalMulai() != null && kegiatan.getTanggalSelesai() != null) {
            SimpleDateFormat sdfDate = new SimpleDateFormat("dd MMMM yyyy", Locale.forLanguageTag("id-ID"));
            SimpleDateFormat sdfTime = new SimpleDateFormat("HH:mm", Locale.getDefault());
            String tanggal = sdfDate.format(kegiatan.getTanggalMulai().toDate());
            String waktuMulai = sdfTime.format(kegiatan.getTanggalMulai().toDate());
            String waktuSelesai = sdfTime.format(kegiatan.getTanggalSelesai().toDate());
            binding.tvDetailWaktu.setText(tanggal + ", " + waktuMulai + " - " + waktuSelesai + " WITA");
        }

        // Memuat gambar banner
        if (getContext() != null && kegiatan.getGambarKegiatanUrl() != null && !kegiatan.getGambarKegiatanUrl().isEmpty()) {
            Glide.with(requireContext()).load(kegiatan.getGambarKegiatanUrl()).placeholder(R.drawable.carousel).into(binding.ivDetailGambar);
        }

        // Atur kondisi tombol "Daftar" dan teks kuota
        updateRegisterButtonState();
    }

    private void registerToKegiatan() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(getContext(), "Anda harus login untuk mendaftar", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.btnDaftar.setEnabled(false);
        binding.btnDaftar.setText("Memproses...");

        // Menggunakan FieldValue.arrayUnion untuk menambahkan UID ke array pendaftar secara aman
        // Ini akan mencegah duplikasi jika user tidak sengaja menekan dua kali
        db.collection("kegiatan_acara").document(kegiatanId)
                .update("pendaftar", FieldValue.arrayUnion(user.getUid()))
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Anda berhasil terdaftar!", Toast.LENGTH_SHORT).show();
                    // Update data lokal dan kondisi tombol secara langsung
                    if (currentKegiatan.getPendaftar() == null) {
                        currentKegiatan.setPendaftar(new ArrayList<>());
                    }
                    currentKegiatan.getPendaftar().add(user.getUid());
                    updateRegisterButtonState(); // Panggil lagi untuk refresh kuota & tombol
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Gagal mendaftar.", Toast.LENGTH_SHORT).show();
                    binding.btnDaftar.setEnabled(true);
                    binding.btnDaftar.setText("Daftar Kegiatan");
                });
    }

    /**
     * Fungsi untuk mengatur tampilan tombol daftar dan teks kuota berdasarkan data terkini.
     */
    private void updateRegisterButtonState() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null || currentKegiatan == null) return;

        List<String> pendaftarList = currentKegiatan.getPendaftar();
        int pendaftarCount = (pendaftarList != null) ? pendaftarList.size() : 0;
        int kuota = currentKegiatan.getKuota();
        int sisaKuota = kuota - pendaftarCount;

        // Update teks kuota
        String teksKuota = String.format(Locale.getDefault(), "Pendaftar: %d / %d (Sisa: %d)", pendaftarCount, kuota, sisaKuota);
        binding.tvDetailKuota.setText(teksKuota);

        // Cek kondisi tombol
        if (pendaftarList != null && pendaftarList.contains(user.getUid())) {
            binding.btnDaftar.setText("Anda Sudah Terdaftar");
            binding.btnDaftar.setEnabled(false);
        } else if (sisaKuota <= 0) {
            binding.btnDaftar.setText("Kuota Penuh");
            binding.btnDaftar.setEnabled(false);
        } else {
            binding.btnDaftar.setText("Daftar Kegiatan");
            binding.btnDaftar.setEnabled(true);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}