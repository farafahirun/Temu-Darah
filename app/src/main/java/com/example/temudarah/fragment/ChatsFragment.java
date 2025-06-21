package com.example.temudarah.fragment;

import android.os.Bundle;
import android.os.Handler; // Import Handler
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
import java.util.List;

public class ChatsFragment extends Fragment {

    private static final String TAG = "ChatsFragment";
    private FragmentChatsBinding binding;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private ChatsAdapter adapter;
    private List<ChatPreview> chatPreviewList;

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

        if (currentUser != null) {
            loadChatList();
        } else {
            // If user is not logged in, hide loading overlay and show empty chat message
            binding.loadingOverlay.setVisibility(View.GONE); // Hide overlay
            binding.tvEmptyChat.setText("Anda perlu login untuk melihat pesan.");
            binding.tvEmptyChat.setVisibility(View.VISIBLE);
        }
    }

    private void setupRecyclerView() {
        chatPreviewList = new ArrayList<>();
        adapter = new ChatsAdapter(chatPreviewList, chatPreview -> {
            // Aksi saat item chat di klik -> buka ChatRoomFragment
            Fragment chatRoomFragment = ChatRoomFragment.newInstance(chatPreview.getChatRoomId(), chatPreview.getOtherUserName());
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, chatRoomFragment)
                    .addToBackStack(null)
                    .commit();
        });
        binding.rvChats.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvChats.setAdapter(adapter);
    }

    private void loadChatList() {
        // Show the full-screen loading overlay and hide other content
        binding.loadingOverlay.setVisibility(View.VISIBLE); // Show the loading overlay
        binding.tvEmptyChat.setVisibility(View.GONE);
        binding.rvChats.setVisibility(View.GONE);

        // Simulate a background task for 1 second
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                // This code will run after 1 second
                if (currentUser == null) {
                    binding.loadingOverlay.setVisibility(View.GONE); // Hide overlay
                    binding.tvEmptyChat.setText("Anda perlu login untuk melihat pesan.");
                    binding.tvEmptyChat.setVisibility(View.VISIBLE);
                    return;
                }

                // 1. Ambil dulu daftar "jabat tangan" (active_donations) di mana kita terlibat
                db.collection("active_donations")
                        .whereArrayContains("participants", currentUser.getUid())
                        .get()
                        .addOnSuccessListener(queryDocumentSnapshots -> {
                            if (queryDocumentSnapshots.isEmpty()) {
                                binding.loadingOverlay.setVisibility(View.GONE); // Hide overlay
                                binding.tvEmptyChat.setText("Belum ada percakapan.");
                                binding.tvEmptyChat.setVisibility(View.VISIBLE);
                                return;
                            }

                            // Siapkan list untuk menampung semua 'tugas' (query) yang akan dijalankan
                            List<Task<?>> tasks = new ArrayList<>();
                            List<ProsesDonor> processes = new ArrayList<>();

                            for (DocumentSnapshot doc : queryDocumentSnapshots) {
                                ProsesDonor proses = doc.toObject(ProsesDonor.class);
                                if (proses == null || proses.getParticipants() == null) continue;

                                processes.add(proses);

                                // 2. Untuk setiap 'jabat tangan', buat DUA tugas:
                                //    a. Tugas untuk mengambil profil lawan bicara
                                String otherUserId = "";
                                for (String participantId : proses.getParticipants()) {
                                    if (!participantId.equals(currentUser.getUid())) {
                                        otherUserId = participantId;
                                        break;
                                    }
                                }
                                if (!otherUserId.isEmpty()) {
                                    tasks.add(db.collection("users").document(otherUserId).get());
                                } else {
                                    // Handle case where otherUserId is not found (e.g., self-chat or malformed data)
                                    Log.w(TAG, "Other user ID not found in participants for chatRoomId: " + proses.getChatRoomId());
                                    // Add a dummy task or handle this scenario appropriately
                                    // For now, we'll skip this process to avoid crashes if otherUser is null
                                    continue; // Skip processing this 'proses' if otherUser is not found
                                }


                                //    b. Tugas untuk mengambil SATU pesan terakhir dari sub-koleksi
                                tasks.add(db.collection("chats").document(proses.getChatRoomId()).collection("messages")
                                        .orderBy("timestamp", Query.Direction.DESCENDING)
                                        .limit(1)
                                        .get());
                            }

                            // If there are no valid tasks, just hide the progress bar and show empty text
                            if (tasks.isEmpty()) {
                                binding.loadingOverlay.setVisibility(View.GONE); // Hide overlay
                                binding.tvEmptyChat.setText("Belum ada percakapan.");
                                binding.tvEmptyChat.setVisibility(View.VISIBLE);
                                return;
                            }

                            // 3. Jalankan semua tugas (ambil profil & ambil pesan terakhir) secara bersamaan
                            Tasks.whenAllComplete(tasks).addOnCompleteListener(allTasks -> {
                                chatPreviewList.clear();
                                for (int i = 0; i < processes.size(); i++) {
                                    ProsesDonor proses = processes.get(i);

                                    // Ensure we don't go out of bounds if some tasks were skipped
                                    if ((i * 2 + 1) >= tasks.size()) {
                                        Log.w(TAG, "Task index out of bounds for processes. Skipping remaining.");
                                        continue;
                                    }

                                    // Ambil hasil dari tugas ambil profil
                                    Task<DocumentSnapshot> userTask = (Task<DocumentSnapshot>) tasks.get(i * 2);
                                    // Ambil hasil dari tugas ambil pesan terakhir
                                    Task<QuerySnapshot> messageTask = (Task<QuerySnapshot>) tasks.get(i * 2 + 1);

                                    if (userTask.isSuccessful() && messageTask.isSuccessful()) {
                                        DocumentSnapshot userDoc = userTask.getResult();
                                        QuerySnapshot messageQuery = messageTask.getResult();

                                        User otherUser = userDoc.toObject(User.class);

                                        if (otherUser != null) {
                                            // 4. Rakit semua data menjadi satu objek ChatPreview
                                            ChatPreview preview = new ChatPreview();
                                            preview.setChatRoomId(proses.getChatRoomId());
                                            preview.setOtherUserId(otherUser.getUid());
                                            preview.setOtherUserName(otherUser.getFullName());
                                            preview.setOtherUserPhotoBase64(otherUser.getProfileImageBase64());

                                            // Cek apakah ada pesan terakhir
                                            if (!messageQuery.isEmpty()) {
                                                Pesan lastPesan = messageQuery.getDocuments().get(0).toObject(Pesan.class);
                                                if (lastPesan != null) {
                                                    preview.setLastMessage(lastPesan.getText());
                                                    preview.setLastMessageTimestamp(lastPesan.getTimestamp());
                                                }
                                            } else {
                                                preview.setLastMessage("Ketuk untuk memulai percakapan...");
                                                preview.setLastMessageTimestamp(proses.getTimestamp()); // Use donation timestamp if no messages
                                            }

                                            chatPreviewList.add(preview);
                                        } else {
                                            Log.w(TAG, "Other user object is null for chatRoomId: " + proses.getChatRoomId());
                                        }
                                    } else {
                                        Log.e(TAG, "Failed to get user or message for chatRoomId: " + proses.getChatRoomId() +
                                                ", User task success: " + userTask.isSuccessful() + ", Message task success: " + messageTask.isSuccessful() +
                                                ", User exception: " + (userTask.getException() != null ? userTask.getException().getMessage() : "none") +
                                                ", Message exception: " + (messageTask.getException() != null ? messageTask.getException().getMessage() : "none"));
                                    }
                                }

                                // Sort the chat previews by timestamp of the last message (or donation if no messages)
                                chatPreviewList.sort((o1, o2) -> {
                                    if (o1.getLastMessageTimestamp() == null && o2.getLastMessageTimestamp() == null) return 0;
                                    if (o1.getLastMessageTimestamp() == null) return 1; // o1 comes after o2 if o1 has no timestamp
                                    if (o2.getLastMessageTimestamp() == null) return -1; // o2 comes after o1 if o2 has no timestamp
                                    return o2.getLastMessageTimestamp().compareTo(o1.getLastMessageTimestamp()); // Newest first
                                });


                                // 5. Setelah semua data dirakit, baru update RecyclerView
                                adapter.notifyDataSetChanged();
                                binding.loadingOverlay.setVisibility(View.GONE); // Hide overlay
                                if (chatPreviewList.isEmpty()) {
                                    binding.tvEmptyChat.setText("Belum ada percakapan.");
                                    binding.tvEmptyChat.setVisibility(View.VISIBLE);
                                } else {
                                    binding.rvChats.setVisibility(View.VISIBLE);
                                }
                            }).addOnFailureListener(e -> {
                                Log.e(TAG, "Error completing all chat tasks", e);
                                binding.loadingOverlay.setVisibility(View.GONE); // Hide overlay
                                binding.tvEmptyChat.setText("Gagal memuat percakapan.");
                                binding.tvEmptyChat.setVisibility(View.VISIBLE);
                            });

                        }).addOnFailureListener(e -> {
                            binding.loadingOverlay.setVisibility(View.GONE); // Hide overlay
                            binding.tvEmptyChat.setText("Gagal memuat daftar chat.");
                            binding.tvEmptyChat.setVisibility(View.VISIBLE);
                            Log.e(TAG, "Gagal memuat daftar chat", e);
                        });
            }
        }, 1000);
    }
}