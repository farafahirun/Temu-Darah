package com.example.temudarah.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.temudarah.R;
import com.example.temudarah.databinding.ItemNotifikasiBinding;
import com.example.temudarah.model.Notifikasi;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class NotifikasiAdapter extends RecyclerView.Adapter<NotifikasiAdapter.ViewHolder> {

    private List<Notifikasi> notifikasiList;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Notifikasi notifikasi);
    }

    // Constructor yang benar, menerima List dan Listener
    public NotifikasiAdapter(List<Notifikasi> notifikasiList, OnItemClickListener listener) {
        this.notifikasiList = notifikasiList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemNotifikasiBinding binding = ItemNotifikasiBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(notifikasiList.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return notifikasiList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemNotifikasiBinding binding;

        ViewHolder(ItemNotifikasiBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(final Notifikasi notifikasi, final OnItemClickListener listener) {
            binding.tvNotifJudul.setText(notifikasi.getJudul());
            binding.tvNotifSender.setText("From: " + (notifikasi.getSenderName() != null ? notifikasi.getSenderName() : "-"));
            binding.tvNotifPesan.setText(notifikasi.getPesan());

            if (notifikasi.getWaktu() != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
                binding.tvNotifWaktu.setText(sdf.format(notifikasi.getWaktu().toDate()));
            }

            // Ganti ikon berdasarkan tipe notifikasi
            if ("pesan".equals(notifikasi.getTipe())) {
                binding.ivNotifIcon.setImageResource(R.drawable.chats);
            } else if ("bantuan".equals(notifikasi.getTipe())) {
                binding.ivNotifIcon.setImageResource(R.drawable.call);
            } else {
                binding.ivNotifIcon.setImageResource(R.drawable.notifikasi_on);
            }

            itemView.setOnClickListener(v -> listener.onItemClick(notifikasi));
        }
    }
}