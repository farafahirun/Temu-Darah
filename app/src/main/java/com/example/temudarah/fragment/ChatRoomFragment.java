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
import com.example.temudarah.model.User;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

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

    // Add ListenerRegistration to properly manage the listener lifecycle
    private ListenerRegistration messagesListener;

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

        // Set click listener for the new back button
        binding.btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());

        // Set click listener for the menu button (optional)

        // Load other user's profile picture
        loadOtherUserProfile();

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

        // Remove any existing listener before setting a new one
        if (messagesListener != null) {
            messagesListener.remove();
        }

        // Ini adalah listener real-time. Kode ini akan otomatis berjalan setiap ada pesan baru.
        messagesListener = db.collection("chats").document(chatRoomId).collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.w(TAG, "Listen failed.", error);
                        return;
                    }

                    if (value != null && isAdded()) {
                        pesanList.clear();

                        // Batch update untuk mengubah status pesan menjadi "dibaca"
                        WriteBatch batch = db.batch();
                        boolean hasUnreadMessages = false;

                        for (QueryDocumentSnapshot doc : value) {
                            Pesan pesan = doc.toObject(Pesan.class);
                            pesanList.add(pesan);

                            // Jika pesan dikirim oleh pengguna lain dan belum dibaca
                            if (!currentUser.getUid().equals(pesan.getSenderId()) && !pesan.isRead()) {
                                hasUnreadMessages = true;
                                // Tandai pesan sebagai sudah dibaca
                                batch.update(doc.getReference(), "isRead", true);
                            }
                        }

                        // Commit batch update jika ada pesan yang belum dibaca
                        if (hasUnreadMessages) {
                            batch.commit()
                                .addOnSuccessListener(aVoid -> Log.d(TAG, "Messages marked as read"))
                                .addOnFailureListener(e -> Log.e(TAG, "Error marking messages as read", e));
                        }

                        adapter.notifyDataSetChanged();
                        // Scroll otomatis ke pesan paling bawah
                        binding.rvMessages.scrollToPosition(pesanList.size() - 1);
                    }
                });
    }

    private void sendMessage() {
        String messageText = binding.etMessage.getText().toString().trim();
        if (TextUtils.isEmpty(messageText) || currentUser == null || chatRoomId == null) return;

        // Membuat pesan tanpa nama pengirim karena sudah tampil di bagian atas chat
        Pesan pesan = new Pesan();
        pesan.setText(messageText);
        pesan.setSenderId(currentUser.getUid());
        // Set empty string for sender name since we don't need to display it in the bubbles
        pesan.setSenderName("");
        pesan.setTimestamp(Timestamp.now());

        binding.etMessage.setText(""); // Langsung kosongkan UI

        // Simpan pesan ke sub-koleksi 'messages'
        db.collection("chats").document(chatRoomId).collection("messages")
                .add(pesan)
                .addOnSuccessListener(documentReference -> {
                    // Chat room will be automatically updated via listener
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

    // Di dalam ChatRoomFragment.java
    private void createMessageNotification(Pesan pesan) {
        if (chatRoomId == null) return;

        String[] participants = chatRoomId.split("_");
        String recipientId = participants[0].equals(currentUser.getUid()) ? participants[1] : participants[0];

        db.collection("users").document(recipientId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User recipient = documentSnapshot.toObject(User.class);
                        if (recipient != null && recipient.isNotifPesanBaru()) {

                            Notifikasi notif = new Notifikasi();
                            notif.setJudul("Pesan baru"); // Judul umum
                            notif.setPesan(pesan.getText());
                            notif.setWaktu(pesan.getTimestamp());
                            notif.setSudahDibaca(false);
                            notif.setTipe("pesan");
                            notif.setTujuanId(chatRoomId);
                            notif.setSenderId(pesan.getSenderId());

                            // PENTING: Prioritaskan nama dari objek Pesan, lalu display name, lalu email, lalu "Pengguna Tak Dikenal"
                            String senderNameForNotification = pesan.getSenderName(); // Coba ambil dari objek Pesan dulu
                            if (senderNameForNotification == null || senderNameForNotification.trim().isEmpty()) {
                                senderNameForNotification = currentUser.getDisplayName(); // Coba ambil dari displayName pengguna saat ini
                                if (senderNameForNotification == null || senderNameForNotification.trim().isEmpty()) {
                                    senderNameForNotification = currentUser.getEmail(); // Fallback ke email
                                    if (senderNameForNotification == null || senderNameForNotification.trim().isEmpty()) {
                                        senderNameForNotification = "Pengguna Tak Dikenal"; // Fallback terakhir
                                    }
                                }
                            }
                            notif.setSenderName(senderNameForNotification);

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

    /**
     * Memuat data profil pengguna lawan chat untuk ditampilkan di toolbar
     */
    private void loadOtherUserProfile() {
    if (chatRoomId == null || currentUser == null) return;

    // Parse chatRoomId untuk mendapatkan ID lawan chat
    String[] userIds = chatRoomId.split("_");
    String otherUserId = userIds[0].equals(currentUser.getUid()) ? userIds[1] : userIds[0];

    // Ambil data pengguna dari Firestore
    db.collection("users").document(otherUserId)
        .get()
        .addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists() && isAdded()) {
                User otherUser = documentSnapshot.toObject(User.class);
                if (otherUser != null) {
                    // Set nama pengguna di toolbar - using fullName instead of nama
                    binding.tvToolbarTitle.setText(otherUser.getFullName() != null ?
                        otherUser.getFullName() :
                        (otherUser.getUsername() != null ? otherUser.getUsername() : "User"));

                    // Load profile picture if available - using profileImageBase64 instead of fotoUrl
                    String profileImage = otherUser.getProfileImageBase64();
                    if (profileImage != null && !profileImage.isEmpty()) {
                        try {
                            // If it's a Base64 string
                            if (profileImage.startsWith("data:image") || profileImage.startsWith("/9j")) {
                                // For Base64 encoded images
                                byte[] decodedString;
                                if (profileImage.contains(",")) {
                                    decodedString = android.util.Base64.decode(
                                            profileImage.split(",")[1],
                                            android.util.Base64.DEFAULT);
                                } else {
                                    decodedString = android.util.Base64.decode(
                                            profileImage,
                                            android.util.Base64.DEFAULT);
                                }

                                android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeByteArray(
                                        decodedString, 0, decodedString.length);
                                binding.ivToolbarProfile.setImageBitmap(bitmap);
                            } else {
                                // For URL images, use Glide
                                com.bumptech.glide.Glide.with(requireContext())
                                    .load(profileImage)
                                    .placeholder(com.example.temudarah.R.drawable.logo_merah)
                                    .error(com.example.temudarah.R.drawable.logo_merah)
                                    .into(binding.ivToolbarProfile);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error loading profile image", e);
                            // Set default image if loading fails
                            binding.ivToolbarProfile.setImageResource(
                                    com.example.temudarah.R.drawable.logo_merah);
                        }
                    }
                }
            }
        })
        .addOnFailureListener(e -> Log.e(TAG, "Error loading user data", e));
}

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Clean up listener when fragment is destroyed
        if (messagesListener != null) {
            messagesListener.remove();
        }
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).showBottomNav(); // Restore bottom nav if it was hidden
        }
        binding = null;
    }
}
