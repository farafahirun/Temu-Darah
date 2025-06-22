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
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.temudarah.R;
import com.example.temudarah.adapter.NotifikasiAdapter;
import com.example.temudarah.databinding.FragmentNotifikasiBinding;
import com.example.temudarah.model.Notifikasi;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class NotifikasiFragment extends Fragment {

    private static final String TAG = "NotifikasiFragmentDebug";
    private FragmentNotifikasiBinding binding;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private NotifikasiAdapter adapter;
    private List<Notifikasi> notifikasiList;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentNotifikasiBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        setupRecyclerView();
        binding.btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());
    }

    @Override
    public void onResume() {
        super.onResume();
        if (currentUser != null) {
            loadNotifications();
        } else {
            // Tampilkan pesan jika belum login
        }
    }

    private void setupRecyclerView() {
        binding.rvNotifikasi.setLayoutManager(new LinearLayoutManager(getContext()));
        notifikasiList = new ArrayList<>();

        // Inisialisasi adapter dengan listener klik yang sudah berfungsi
        adapter = new NotifikasiAdapter(notifikasiList, notifikasi -> {
            handleNotificationClick(notifikasi);
        });
        binding.rvNotifikasi.setAdapter(adapter);
    }

    // Di NotifikasiFragment.java


    private void loadNotifications() {
        binding.progressBar.setVisibility(View.VISIBLE);
        db.collection("users").document(currentUser.getUid()).collection("notifikasi")
                .orderBy("waktu", Query.Direction.DESCENDING)
                .limit(50)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!isAdded()) return;
                    binding.progressBar.setVisibility(View.GONE);
                    notifikasiList.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Notifikasi notifikasi = doc.toObject(Notifikasi.class);
                        notifikasi.setNotifId(doc.getId());

                        // --- Tambahkan log ini ---
                        Log.d(TAG, "Notifikasi Loaded: " + notifikasi.getJudul());
                        Log.d(TAG, "Pesan: " + notifikasi.getPesan());
                        Log.d(TAG, "Sender Name (dari objek Notifikasi): " + notifikasi.getSenderName());
                        Log.d(TAG, "Tipe: " + notifikasi.getTipe());
                        // --- Akhir log ---

                        notifikasiList.add(notifikasi);
                    }
                    adapter.notifyDataSetChanged();
                    binding.tvEmptyState.setVisibility(notifikasiList.isEmpty() ? View.VISIBLE : View.GONE);
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    binding.progressBar.setVisibility(View.GONE);
                    Log.e(TAG, "Gagal memuat notifikasi", e);
                });
    }
    /**
     * Fungsi utama untuk menangani klik pada notifikasi.
     */
    private void handleNotificationClick(Notifikasi notifikasi) {
        if (notifikasi == null || notifikasi.getTipe() == null) return;

        // Hapus notifikasi dari database dulu, baru navigasi
        db.collection("users").document(currentUser.getUid()).collection("notifikasi").document(notifikasi.getNotifId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Notifikasi berhasil dihapus.");
                    // Setelah berhasil dihapus, tentukan mau navigasi ke mana
                    if ("pesan".equals(notifikasi.getTipe())) {
                        // Ekstrak nama pengirim dari judul notifikasi
                        String title = notifikasi.getJudul();
                        String otherUserName = title.replace("Pesan baru dari ", "");
                        navigateToChatRoom(notifikasi.getTujuanId(), otherUserName);
                    }
                    // TODO: Tambahkan else if untuk tipe notifikasi lain, misal "bantuan"
                    // else if ("bantuan".equals(notifikasi.getTipe())) { ... }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Gagal memproses notifikasi.", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Fungsi helper untuk navigasi ke ruang chat.
     */
    private void navigateToChatRoom(String chatRoomId, String otherUserName) {
        if (getParentFragmentManager() != null) {
            Fragment chatFragment = ChatRoomFragment.newInstance(chatRoomId, otherUserName);
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, chatFragment)
                    .addToBackStack(null)
                    .commit();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}