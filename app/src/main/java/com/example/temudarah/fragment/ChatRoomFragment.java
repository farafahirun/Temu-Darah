package com.example.temudarah.fragment;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.temudarah.activity.MainActivity;
import com.example.temudarah.adapter.PesanAdapter;
import com.example.temudarah.databinding.FragmentChatRoomBinding;
import com.example.temudarah.model.Notifikasi;
import com.example.temudarah.model.Pesan;
import com.example.temudarah.model.ProsesDonor;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;
//import com.google.firebase.firestore.auth.User;
import com.example.temudarah.model.User;

import java.util.ArrayList;
import java.util.List;

public class ChatRoomFragment extends Fragment {

    private static final String TAG = "ChatRoomFragment";
    private static final String ARG_CHAT_ROOM_ID = "chat_room_id";
    private static final String ARG_OTHER_USER_NAME = "other_user_name";

    private FragmentChatRoomBinding binding;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    private PesanAdapter adapter;
    private List<Pesan> pesanList;
    private String chatRoomId;
    private String otherUserName;

    public static ChatRoomFragment newInstance(String chatRoomId, String otherUserName) {
        ChatRoomFragment fragment = new ChatRoomFragment();
        Bundle args = new Bundle();
        args.putString(ARG_CHAT_ROOM_ID, chatRoomId);
        args.putString(ARG_OTHER_USER_NAME, otherUserName);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            chatRoomId = getArguments().getString(ARG_CHAT_ROOM_ID);
            otherUserName = getArguments().getString(ARG_OTHER_USER_NAME);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentChatRoomBinding.inflate(inflater, container, false);
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).hideBottomNav();
        }
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        binding.tvToolbarTitle.setText(otherUserName);
        binding.toolbarChat.setNavigationOnClickListener(v -> getParentFragmentManager().popBackStack());

        // Add cancel button to the toolbar's menu
        binding.toolbarChat.inflateMenu(com.example.temudarah.R.menu.chat_menu);
        binding.toolbarChat.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == com.example.temudarah.R.id.action_cancel) {
                showCancellationConfirmationDialog();
                return true;
            }
            return false;
        });

        setupRecyclerView();
        listenForMessages();
        loadDonationProcess(); // Load process info to check who's the requester/donor

        binding.btnSendMessage.setOnClickListener(v -> sendMessage());
        markNotificationsAsRead();
    }

    private void setupRecyclerView() {
        pesanList = new ArrayList<>();
        adapter = new PesanAdapter(pesanList);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        layoutManager.setStackFromEnd(true); // Pesan baru selalu muncul di paling bawah
        binding.rvMessages.setLayoutManager(layoutManager);
        binding.rvMessages.setAdapter(adapter);
    }

    private void listenForMessages() {
        if (chatRoomId == null || currentUser == null) return;

        // Ini adalah listener real-time. Kode ini akan otomatis berjalan setiap ada pesan baru.
        db.collection("chats").document(chatRoomId).collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.w(TAG, "Listen failed.", error);
                        return;
                    }

                    if (value != null) {
                        pesanList.clear();
                        pesanList.addAll(value.toObjects(Pesan.class));
                        adapter.notifyDataSetChanged();
                        // Scroll otomatis ke pesan paling bawah
                        binding.rvMessages.scrollToPosition(pesanList.size() - 1);
                    }
                });
    }

// Di dalam class ChatRoomFragment.java

    private void sendMessage() {
        String messageText = binding.etMessage.getText().toString().trim();
        if (TextUtils.isEmpty(messageText) || currentUser == null || chatRoomId == null) return;

        String senderName = currentUser.getDisplayName() != null ? currentUser.getDisplayName() : "User";

        Pesan pesan = new Pesan();
        pesan.setText(messageText);
        pesan.setSenderId(currentUser.getUid());
        pesan.setSenderName(senderName);
        pesan.setTimestamp(Timestamp.now());

        binding.etMessage.setText(""); // Langsung kosongkan UI

        // Simpan pesan ke sub-koleksi 'messages'
        db.collection("chats").document(chatRoomId).collection("messages")
                .add(pesan)
                .addOnSuccessListener(documentReference -> {
                    binding.etMessage.setText("");
                    createMessageNotification(pesan);
                    // Panggil dengan 3 parameter yang benar
                    updateLastMessage(chatRoomId, messageText, pesan.getTimestamp());
                })
                .addOnFailureListener(e -> {
                    // Jika gagal, mungkin kembalikan teksnya atau tampilkan pesan error
                    binding.etMessage.setText(messageText);
                    Toast.makeText(getContext(), "Gagal mengirim pesan.", Toast.LENGTH_SHORT).show();
                });
    }

    private void createMessageNotification(Pesan pesan) {
        if (chatRoomId == null) return;

        // 1. Cari tahu siapa ID penerima
        String[] participants = chatRoomId.split("_");
        String recipientId = participants[0].equals(currentUser.getUid()) ? participants[1] : participants[0];

        // 2. Cek pengaturan notifikasi si penerima terlebih dahulu
        db.collection("users").document(recipientId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User recipient = documentSnapshot.toObject(User.class);
                        // Hanya kirim notifikasi jika user mengizinkannya
                        if (recipient != null && recipient.isNotifPesanBaru()) {

                            // 3. Buat objek notifikasi
                            Notifikasi notif = new Notifikasi();
                            notif.setJudul("Pesan baru dari " + pesan.getSenderName());
                            notif.setPesan(pesan.getText());
                            notif.setWaktu(pesan.getTimestamp());
                            notif.setSudahDibaca(false);
                            notif.setTipe("pesan"); // Tipe notifikasi
                            notif.setTujuanId(chatRoomId); // ID tujuan agar bisa diklik

                            // 4. Simpan notifikasi ke sub-koleksi milik si PENERIMA
                            db.collection("users").document(recipientId)
                                    .collection("notifikasi").add(notif)
                                    .addOnSuccessListener(docRef -> Log.d(TAG, "Notifikasi berhasil dibuat untuk " + recipientId))
                                    .addOnFailureListener(e -> Log.e(TAG, "Gagal membuat notifikasi", e));
                        } else {
                            Log.d(TAG, "User " + recipientId + " mematikan notifikasi pesan baru.");
                        }
                    }
                });
    }

    /**
     * FUNGSI BARU: Untuk mengupdate field pesan terakhir di koleksi active_donations.
     */
    private void updateLastMessage(String chatRoomId, String message, Timestamp timestamp) {
        db.collection("chat_rooms").document(chatRoomId)
                .update("lastMessage", message, "lastMessageTimestamp", timestamp)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Chat room last message updated."))
                .addOnFailureListener(e -> Log.e(TAG, "Error updating chat room", e));
    }

    private void showCancellationConfirmationDialog() {
        new AlertDialog.Builder(getContext())
                .setTitle("Konfirmasi Pembatalan")
                .setMessage("Apakah Anda yakin ingin membatalkan proses ini?")
                .setPositiveButton("Ya", (dialog, which) -> cancelDonationProcess())
                .setNegativeButton("Tidak", null)
                .show();
    }

    private void cancelDonationProcess() {
        if (currentUser == null || chatRoomId == null) return;

        // Hapus dokumen dari koleksi active_donations
        db.collection("active_donations").document(chatRoomId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Proses donor berhasil dibatalkan.");
                    Toast.makeText(getContext(), "Proses donor berhasil dibatalkan.", Toast.LENGTH_SHORT).show();
                    // Kembali ke halaman sebelumnya setelah membatalkan
                    getParentFragmentManager().popBackStack();
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error canceling donation process", e));
    }

    private void loadDonationProcess() {
        if (chatRoomId == null) return;

        db.collection("active_donations").document(chatRoomId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        ProsesDonor prosesDonor = documentSnapshot.toObject(ProsesDonor.class);
                        if (prosesDonor != null) {
                            // Cek apakah pengguna saat ini adalah requester atau donor
                            boolean isRequester = currentUser.getUid().equals(prosesDonor.getRequesterId());
                            boolean isDonor = currentUser.getUid().equals(prosesDonor.getDonorId());

                            // Tampilkan peran pengguna dalam proses donor (dihapus referensi ke tvRole)
                            if (isRequester) {
                                Log.d(TAG, "User is a Requester");
                                // Disini bisa ditambahkan logika khusus untuk requester jika dibutuhkan
                            } else if (isDonor) {
                                Log.d(TAG, "User is a Donor");
                                // Disini bisa ditambahkan logika khusus untuk donor jika dibutuhkan
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error loading donation process", e));
    }

    private void markNotificationsAsRead() {
        if (currentUser == null || chatRoomId == null) return;

        db.collection("users").document(currentUser.getUid()).collection("notifikasi")
                .whereEqualTo("tujuanId", chatRoomId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        // Tidak ada notifikasi untuk dihapus, tidak perlu melakukan apa-apa
                        return;
                    }

                    // Gunakan WriteBatch untuk menghapus semua notifikasi yang cocok dalam satu operasi
                    WriteBatch batch = db.batch();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        batch.delete(doc.getReference());
                    }

                    batch.commit()
                            .addOnSuccessListener(aVoid -> Log.d(TAG, "Notifikasi untuk chat " + chatRoomId + " berhasil dihapus."))
                            .addOnFailureListener(e -> Log.e(TAG, "Gagal menghapus notifikasi", e));
                });
    }
}