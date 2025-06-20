package com.example.temudarah.fragment;

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
import com.example.temudarah.model.Pesan;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
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

        setupRecyclerView();
        listenForMessages();

        binding.btnSendMessage.setOnClickListener(v -> sendMessage());
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

        // Buat objek Pesan baru
        Pesan pesan = new Pesan();
        pesan.setText(messageText);
        pesan.setSenderId(currentUser.getUid());
        pesan.setSenderName(currentUser.getDisplayName() != null ? currentUser.getDisplayName() : "User");
        pesan.setTimestamp(Timestamp.now());

        // 1. Simpan pesan ke sub-koleksi 'messages' (ini tidak berubah)
        db.collection("chats").document(chatRoomId).collection("messages")
                .add(pesan)
                .addOnSuccessListener(documentReference -> {
                    binding.etMessage.setText(""); // Kosongkan input setelah terkirim

                    // 2. SETELAH PESAN BERHASIL DISIMPAN, UPDATE DOKUMEN 'active_donations'
                    updateLastMessage(chatRoomId, messageText, pesan.getTimestamp());
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Gagal mengirim pesan.", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * FUNGSI BARU: Untuk mengupdate field pesan terakhir di koleksi active_donations.
     */
    private void updateLastMessage(String chatRoomId, String message, Timestamp timestamp) {
        db.collection("active_donations").document(chatRoomId)
                .update("lastMessage", message, "lastMessageTimestamp", timestamp)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Last message updated successfully."))
                .addOnFailureListener(e -> Log.e(TAG, "Error updating last message", e));
    }
}