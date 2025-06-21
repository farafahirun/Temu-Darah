package com.example.temudarah.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.example.temudarah.R;
import com.example.temudarah.databinding.ItemPermintaanSayaBinding;
import com.example.temudarah.model.PermintaanDonor;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class PermintaanSayaAdapter extends RecyclerView.Adapter<PermintaanSayaAdapter.ViewHolder> {

    private List<PermintaanDonor> permintaanList;
    private OnItemActionClickListener listener;

    public interface OnItemActionClickListener {
        void onStatusChangeClick(PermintaanDonor permintaan);
        void onEditClick(PermintaanDonor permintaan);
        void onContactClick(PermintaanDonor permintaan); // Metode baru untuk tombol "Hubungi"
    }

    public PermintaanSayaAdapter(List<PermintaanDonor> permintaanList, OnItemActionClickListener listener) {
        this.permintaanList = permintaanList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemPermintaanSayaBinding binding = ItemPermintaanSayaBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(permintaanList.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return permintaanList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemPermintaanSayaBinding binding;

        ViewHolder(ItemPermintaanSayaBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(final PermintaanDonor permintaan, final OnItemActionClickListener listener) {
            binding.tvItemNamaPasien.setText("Pasien: " + permintaan.getNamaPasien());
            binding.tvItemGolDarah.setText("Butuh Gol. " + permintaan.getGolonganDarahDibutuhkan());
            binding.tvBloodType.setText(permintaan.getGolonganDarahDibutuhkan());
            binding.tvItemCatatan.setText("Catatan: " + (permintaan.getCatatan() != null ? permintaan.getCatatan() : "-"));

            if (permintaan.getWaktuDibuat() != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("dd MMMMyyyy, HH:mm", Locale.getDefault());
                binding.tvItemWaktu.setText("Dibuat: " + sdf.format(permintaan.getWaktuDibuat().toDate()));
            } else {
                binding.tvItemWaktu.setText("Dibuat: -");
            }

            // Mengatur tampilan berdasarkan status
            // Hanya mengubah warna teks untuk tvItemStatus
            if ("Aktif".equals(permintaan.getStatus())) {
                binding.tvItemStatus.setText("Status: Aktif");
                binding.tvItemStatus.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.sukses));
                binding.btnUbahStatus.setText("Batalkan");
                binding.btnUbahStatus.setVisibility(View.VISIBLE);
                binding.btnEdit.setVisibility(View.VISIBLE);
                binding.btnHubungi.setVisibility(View.GONE); // Sembunyikan Hubungi jika masih Aktif
            } else if ("Dalam Proses".equals(permintaan.getStatus())) {
                binding.tvItemStatus.setText("Status: Dalam Proses");
                binding.tvItemStatus.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.penerima));
                binding.btnUbahStatus.setText("Selesai");
                binding.btnUbahStatus.setVisibility(View.VISIBLE);
                binding.btnEdit.setVisibility(View.GONE); // Edit tidak bisa saat Dalam Proses
                binding.btnHubungi.setVisibility(View.VISIBLE); // Tampilkan Hubungi jika Dalam Proses
            } else if ("Selesai".equals(permintaan.getStatus()) || "Dibatalkan".equals(permintaan.getStatus())) {
                binding.tvItemStatus.setText("Status: " + permintaan.getStatus());
                binding.tvItemStatus.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.text_info));
                binding.btnUbahStatus.setVisibility(View.GONE);
                binding.btnEdit.setVisibility(View.GONE);
                binding.btnHubungi.setVisibility(View.GONE);
            } else {
                // Status tidak dikenal atau default
                binding.tvItemStatus.setText("Status: " + permintaan.getStatus());
                binding.tvItemStatus.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.text_info));
                binding.btnUbahStatus.setVisibility(View.GONE);
                binding.btnEdit.setVisibility(View.GONE);
                binding.btnHubungi.setVisibility(View.GONE);
            }

            // Set listeners untuk tombol aksi
            if (listener != null) {
                binding.btnEdit.setOnClickListener(v -> listener.onEditClick(permintaan));
                binding.btnUbahStatus.setOnClickListener(v -> listener.onStatusChangeClick(permintaan));
                binding.btnHubungi.setOnClickListener(v -> listener.onContactClick(permintaan)); // Atur listener untuk tombol Hubungi
            }
        }
    }
}
