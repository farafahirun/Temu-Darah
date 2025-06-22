package com.example.temudarah.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.temudarah.R;
import com.example.temudarah.adapter.ChatsAdapter;
import com.example.temudarah.databinding.FragmentChatsBinding;
import com.example.temudarah.model.ChatPreview;
import com.example.temudarah.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public class ChatsFragment extends Fragment {

    private static final String TAG = "ChatsFragment";
    private FragmentChatsBinding binding;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private ChatsAdapter adapter;
    private List<ChatPreview> chatPreviewList;

    private enum UiState { LOADING, HAS_DATA, EMPTY }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentChatsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        setupRecyclerView();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (currentUser != null) {
            setupChatListListener(); // Changed from loadChatList() to setupChatListListener()
        } else {
            updateUiState(UiState.EMPTY, "Anda perlu login untuk melihat pesan.");
        }
    }

    private void setupRecyclerView() {
        binding.rvChats.setLayoutManager(new LinearLayoutManager(getContext()));
        chatPreviewList = new ArrayList<>();
        adapter = new ChatsAdapter(chatPreviewList, chatPreview -> {
            Fragment chatRoomFragment = ChatRoomFragment.newInstance(chatPreview.getChatRoomId(), chatPreview.getOtherUserName());
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, chatRoomFragment)
                    .addToBackStack(null)
                    .commit();
        });
        binding.rvChats.setAdapter(adapter);
    }

    private void setupChatListListener() {
        if (currentUser == null) return;
        updateUiState(UiState.LOADING, null);

        // Use a real-time listener instead of get() to automatically update when changes occur
        db.collection("chat_rooms")
                .whereArrayContains("participants", currentUser.getUid())
                .orderBy("lastMessageTimestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((queryDocumentSnapshots, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Listen failed.", e);
                        if (isAdded()) {
                            updateUiState(UiState.EMPTY, "Gagal memuat daftar chat.");
                        }
                        return;
                    }

                    if (!isAdded() || queryDocumentSnapshots == null || queryDocumentSnapshots.isEmpty()) {
                        updateUiState(UiState.EMPTY, "Belum ada percakapan.");
                        return;
                    }

                    List<ChatPreview> tempList = new ArrayList<>();
                    int totalChats = queryDocumentSnapshots.size();
                    AtomicInteger counter = new AtomicInteger(0);

                    for (QueryDocumentSnapshot chatRoomDoc : queryDocumentSnapshots) {
                        List<String> participants = (List<String>) chatRoomDoc.get("participants");
                        if (participants == null) {
                            // Skip if there's weird data
                            if (counter.incrementAndGet() == totalChats) {
                                updateAdapterWithList(tempList);
                            }
                            continue;
                        }

                        // Find the other user's ID
                        String otherUserId = "";
                        for (String participantId : participants) {
                            if (!participantId.equals(currentUser.getUid())) {
                                otherUserId = participantId;
                                break;
                            }
                        }

                        if (otherUserId.isEmpty()) {
                            if (counter.incrementAndGet() == totalChats) {
                                updateAdapterWithList(tempList);
                            }
                            continue;
                        }

                        // Get the other user's profile
                        final String finalOtherUserId = otherUserId;
                        db.collection("users").document(otherUserId).get()
                                .addOnSuccessListener(userDoc -> {
                                    if (userDoc.exists()) {
                                        User otherUser = userDoc.toObject(User.class);
                                        if (otherUser != null) {
                                            // Create ChatPreview object
                                            ChatPreview preview = new ChatPreview();
                                            preview.setChatRoomId(chatRoomDoc.getId());
                                            preview.setOtherUserId(finalOtherUserId);
                                            preview.setOtherUserName(otherUser.getFullName());
                                            preview.setOtherUserPhotoBase64(otherUser.getProfileImageBase64());
                                            preview.setLastMessage(chatRoomDoc.getString("lastMessage"));
                                            preview.setLastMessageTimestamp(chatRoomDoc.getTimestamp("lastMessageTimestamp"));
                                            tempList.add(preview);
                                        }
                                    }
                                    // Check if this is the last processed item
                                    if (counter.incrementAndGet() == totalChats) {
                                        updateAdapterWithList(tempList);
                                    }
                                })
                                .addOnFailureListener(err -> {
                                    // Continue even if profile fetch fails
                                    if (counter.incrementAndGet() == totalChats) {
                                        updateAdapterWithList(tempList);
                                    }
                                });
                    }
                });
    }

    private void updateAdapterWithList(List<ChatPreview> list) {
        chatPreviewList.clear();
        chatPreviewList.addAll(list);

        // Urutkan berdasarkan waktu pesan terakhir
        Collections.sort(chatPreviewList, (o1, o2) -> {
            if (o1.getLastMessageTimestamp() == null) return 1;
            if (o2.getLastMessageTimestamp() == null) return -1;
            return o2.getLastMessageTimestamp().compareTo(o1.getLastMessageTimestamp());
        });

        adapter.notifyDataSetChanged();
        updateUiState(chatPreviewList.isEmpty() ? UiState.EMPTY : UiState.HAS_DATA, "Belum ada percakapan.");
    }

    private void updateUiState(UiState state, String emptyMessage) {
        if (binding == null || !isAdded()) return;
        binding.progressBar.setVisibility(state == UiState.LOADING ? View.VISIBLE : View.GONE);
        binding.rvChats.setVisibility(state == UiState.HAS_DATA ? View.VISIBLE : View.GONE);
        binding.tvEmptyChat.setVisibility(state == UiState.EMPTY ? View.VISIBLE : View.GONE);
        if (state == UiState.EMPTY) {
            binding.tvEmptyChat.setText(emptyMessage);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}