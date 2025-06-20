package com.example.temudarah.adapter;

import android.util.Base64;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.temudarah.R;
import com.example.temudarah.databinding.ItemChatBinding;
import com.example.temudarah.model.ChatPreview;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class ChatsAdapter extends RecyclerView.Adapter<ChatsAdapter.ViewHolder> {

    private List<ChatPreview> chatPreviewList;
    private OnChatClickListener listener;

    // Interface untuk menangani klik pada setiap item
    public interface OnChatClickListener {
        void onChatClick(ChatPreview chatPreview);
    }

    public ChatsAdapter(List<ChatPreview> chatPreviewList, OnChatClickListener listener) {
        this.chatPreviewList = chatPreviewList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemChatBinding binding = ItemChatBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(chatPreviewList.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return chatPreviewList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemChatBinding binding;

        ViewHolder(ItemChatBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(final ChatPreview chatPreview, final OnChatClickListener listener) {
            // Set nama lawan bicara
            binding.tvChatName.setText(chatPreview.getOtherUserName());

            // Set pesan terakhir
            if (chatPreview.getLastMessage() != null && !chatPreview.getLastMessage().isEmpty()) {
                binding.tvLastMessage.setText(chatPreview.getLastMessage());
            } else {
                binding.tvLastMessage.setText("Ketuk untuk memulai percakapan...");
            }

            // Set waktu pesan terakhir dengan format yang bagus
            if (chatPreview.getLastMessageTimestamp() != null) {
                binding.tvChatTimestamp.setText(formatTimestamp(chatPreview.getLastMessageTimestamp()));
            } else {
                binding.tvChatTimestamp.setText("");
            }

            // Set foto profil lawan bicara
            if (chatPreview.getOtherUserPhotoBase64() != null && !chatPreview.getOtherUserPhotoBase64().isEmpty()) {
                try {
                    byte[] imageBytes = Base64.decode(chatPreview.getOtherUserPhotoBase64(), Base64.DEFAULT);
                    Glide.with(itemView.getContext())
                            .asBitmap()
                            .load(imageBytes)
                            .placeholder(R.drawable.logo_merah) // Placeholder
                            .into(binding.ivChatProfile);
                } catch (Exception e) {
                    binding.ivChatProfile.setImageResource(R.drawable.logo_merah);
                }
            } else {
                binding.ivChatProfile.setImageResource(R.drawable.logo_merah);
            }

            // Atur listener klik untuk seluruh item
            itemView.setOnClickListener(v -> listener.onChatClick(chatPreview));
        }

        /**
         * Fungsi helper untuk mengubah timestamp menjadi format waktu yang lebih ramah.
         * Contoh: "10:30", "Kemarin", atau "18/06/25"
         */
        private String formatTimestamp(Timestamp timestamp) {
            if (timestamp == null) return "";

            Calendar messageTime = Calendar.getInstance();
            messageTime.setTime(timestamp.toDate());

            Calendar now = Calendar.getInstance();

            if (now.get(Calendar.DATE) == messageTime.get(Calendar.DATE) &&
                    now.get(Calendar.YEAR) == messageTime.get(Calendar.YEAR)) {
                // Jika hari ini
                return new SimpleDateFormat("HH:mm", Locale.getDefault()).format(timestamp.toDate());
            } else if (now.get(Calendar.DATE) - messageTime.get(Calendar.DATE) == 1 &&
                    now.get(Calendar.YEAR) == messageTime.get(Calendar.YEAR)) {
                // Jika kemarin
                return "Kemarin";
            } else {
                // Jika sudah lama
                return new SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(timestamp.toDate());
            }
        }
    }
}