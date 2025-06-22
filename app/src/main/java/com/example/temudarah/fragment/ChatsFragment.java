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
import com.example.temudarah.model.Pesan;
import com.example.temudarah.model.ProsesDonor;
import com.example.temudarah.model.User;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

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
        // Selalu muat ulang daftar chat saat halaman ini ditampilkan
        if (currentUser != null) {
            loadChatList();
        } else {
            updateUiState(UiState.EMPTY, "Anda perlu login untuk melihat pesan.");
        }
    }

    private void setupRecyclerView() {
        binding.rvChats.setLayoutManager(new LinearLayoutManager(getContext()));
        chatPreviewList = new ArrayList<>();
        adapter = new ChatsAdapter(chatPreviewList, chatPreview -> {
            // Aksi saat item chat diklik -> buka ChatRoomFragment
            Fragment chatRoomFragment = ChatRoomFragment.newInstance(chatPreview.getChatRoomId(), chatPreview.getOtherUserName());
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, chatRoomFragment)
                    .addToBackStack(null)
                    .commit();
        });
        binding.rvChats.setAdapter(adapter);
    }

    private void loadChatList() {
        updateUiState(UiState.LOADING, null);

        // 1. Ambil semua "jabat tangan" (active_donations) di mana kita terlibat
        db.collection("active_donations")
                .whereArrayContains("participants", currentUser.getUid())
                .get()
                .addOnSuccessListener(prosesSnapshots -> {
                    if (!isAdded() || getContext() == null) return;
                    if (prosesSnapshots.isEmpty()) {
                        updateUiState(UiState.EMPTY, "Belum ada percakapan.");
                        return;
                    }

                    List<Task<?>> detailTasks = new ArrayList<>();
                    List<ProsesDonor> processes = new ArrayList<>();

                    for (DocumentSnapshot prosesDoc : prosesSnapshots.getDocuments()) {
                        ProsesDonor proses = prosesDoc.toObject(ProsesDonor.class);
                        if (proses != null && proses.getParticipants() != null && proses.getRequestId() != null) {
                            processes.add(proses);

                            // Siapkan 2 tugas untuk setiap proses: ambil profil lawan bicara & ambil pesan terakhir
                            String otherUserId = Objects.equals(proses.getRequesterId(), currentUser.getUid()) ? proses.getDonorId() : proses.getRequesterId();
                            if (otherUserId != null && !otherUserId.isEmpty()) {
                                detailTasks.add(db.collection("users").document(otherUserId).get());
                                detailTasks.add(db.collection("chats").document(proses.getChatRoomId()).collection("messages")
                                        .orderBy("timestamp", Query.Direction.DESCENDING).limit(1).get());
                            }
                        }
                    }

                    if (detailTasks.isEmpty()) {
                        updateUiState(UiState.EMPTY, "Gagal memproses data chat.");
                        return;
                    }

                    // Jalankan semua tugas secara bersamaan
                    Tasks.whenAllComplete(detailTasks).addOnCompleteListener(task -> {
                        if (!isAdded()) return;
                        List<ChatPreview> tempList = new ArrayList<>();
                        List<Task<?>> completedTasks = task.getResult();

                        for (int i = 0; i < processes.size(); i++) {
                            ProsesDonor proses = processes.get(i);
                            int userTaskIndex = i * 2;
                            int messageTaskIndex = i * 2 + 1;
                            if (messageTaskIndex >= completedTasks.size()) continue;

                            Task<?> userTask = completedTasks.get(userTaskIndex);
                            Task<?> messageTask = completedTasks.get(messageTaskIndex);

                            if (userTask.isSuccessful() && messageTask.isSuccessful()) {
                                DocumentSnapshot userDoc = (DocumentSnapshot) userTask.getResult();
                                QuerySnapshot messageQuery = (QuerySnapshot) messageTask.getResult();

                                if (userDoc.exists()) {
                                    User otherUser = userDoc.toObject(User.class);
                                    if (otherUser != null) {
                                        ChatPreview preview = new ChatPreview();
                                        preview.setChatRoomId(proses.getChatRoomId());
                                        preview.setOtherUserId(otherUser.getUid());
                                        preview.setOtherUserName(otherUser.getFullName());
                                        preview.setOtherUserPhotoBase64(otherUser.getProfileImageBase64());

                                        // Isi pesan terakhir dan waktunya
                                        if (!messageQuery.isEmpty()) {
                                            Pesan lastPesan = messageQuery.getDocuments().get(0).toObject(Pesan.class);
                                            preview.setLastMessage(lastPesan.getText());
                                            preview.setLastMessageTimestamp(lastPesan.getTimestamp());
                                        } else {
                                            preview.setLastMessage("Ketuk untuk memulai percakapan...");
                                            preview.setLastMessageTimestamp(proses.getTimestamp());
                                        }
                                        tempList.add(preview);
                                    }
                                }
                            }
                        }

                        // Urutkan berdasarkan waktu pesan terakhir
                        Collections.sort(tempList, (o1, o2) -> {
                            if (o1.getLastMessageTimestamp() == null) return 1;
                            if (o2.getLastMessageTimestamp() == null) return -1;
                            return o2.getLastMessageTimestamp().compareTo(o1.getLastMessageTimestamp());
                        });

                        chatPreviewList.clear();
                        chatPreviewList.addAll(tempList);
                        adapter.notifyDataSetChanged();
                        updateUiState(chatPreviewList.isEmpty() ? UiState.EMPTY : UiState.HAS_DATA, "Belum ada percakapan.");
                    });
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    updateUiState(UiState.EMPTY, "Gagal memuat daftar chat.");
                    Log.e(TAG, "Error loading chat list", e);
                });
    }

    private void updateUiState(UiState state, String emptyMessage) {
        if (binding == null) return;
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