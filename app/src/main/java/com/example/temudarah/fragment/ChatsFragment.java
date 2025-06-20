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

    // Di dalam class ChatsFragment.java

    private void loadChatList() {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.tvEmptyChat.setVisibility(View.GONE);
        binding.rvChats.setVisibility(View.GONE);

        if (currentUser == null) {
            binding.progressBar.setVisibility(View.GONE);
            return;
        }

        // 1. Ambil dulu daftar "jabat tangan" (active_donations) di mana kita terlibat
        db.collection("active_donations")
                .whereArrayContains("participants", currentUser.getUid())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        binding.progressBar.setVisibility(View.GONE);
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
                        }

                        //    b. Tugas untuk mengambil SATU pesan terakhir dari sub-koleksi
                        tasks.add(db.collection("chats").document(proses.getChatRoomId()).collection("messages")
                                .orderBy("timestamp", Query.Direction.DESCENDING)
                                .limit(1)
                                .get());
                    }

                    // 3. Jalankan semua tugas (ambil profil & ambil pesan terakhir) secara bersamaan
                    Tasks.whenAllComplete(tasks).addOnCompleteListener(allTasks -> {
                        chatPreviewList.clear();
                        for (int i = 0; i < processes.size(); i++) {
                            ProsesDonor proses = processes.get(i);

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
                                    }

                                    chatPreviewList.add(preview);
                                }
                            }
                        }

                        // 5. Setelah semua data dirakit, baru update RecyclerView
                        adapter.notifyDataSetChanged();
                        binding.progressBar.setVisibility(View.GONE);
                        if (chatPreviewList.isEmpty()) {
                            binding.tvEmptyChat.setVisibility(View.VISIBLE);
                        } else {
                            binding.rvChats.setVisibility(View.VISIBLE);
                        }
                    });

                }).addOnFailureListener(e -> {
                    binding.progressBar.setVisibility(View.GONE);
                    Log.e(TAG, "Gagal memuat daftar chat", e);
                });
    }
}