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

    /**
     * Interface untuk menangani semua aksi klik di dalam item.
     */
    public interface OnItemActionClickListener {
        void onItemClick(PermintaanDonor permintaan); // Untuk klik seluruh kartu
        void onStatusChangeClick(PermintaanDonor permintaan); // Untuk tombol Ubah Status/Batalkan
        void onEditClick(PermintaanDonor permintaan); // Untuk tombol Edit
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
        // Memanggil fungsi bind dengan listener yang tipenya sudah benar
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

        /**
         * Fungsi bind dengan parameter listener yang sudah diperbaiki.
         */
        void bind(final PermintaanDonor permintaan, final OnItemActionClickListener listener) {
            // Mengisi data teks
            binding.tvItemNamaPasien.setText("Pasien: " + permintaan.getNamaPasien());
            binding.tvItemGolDarah.setText("Butuh Gol. " + permintaan.getGolonganDarahDibutuhkan());

            if (permintaan.getWaktuDibuat() != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale.forLanguageTag("id-ID"));
                binding.tvItemWaktu.setText("Dibuat: " + sdf.format(permintaan.getWaktuDibuat().toDate()));
            } else {
                binding.tvItemWaktu.setText("Dibuat: -");
            }

            // Mengatur tampilan tombol dan status berdasarkan status permintaan
            String status = permintaan.getStatus();
            binding.tvItemStatus.setText("Status: " + status);

            if ("Aktif".equals(status)) {
                binding.tvItemStatus.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.sukses));
                binding.btnUbahStatus.setText("Batalkan");
                binding.btnUbahStatus.setVisibility(View.VISIBLE);
                binding.btnEdit.setVisibility(View.VISIBLE);
            } else if ("Dalam Proses".equals(status)) {
                binding.tvItemStatus.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.penerima));
                binding.btnUbahStatus.setText("Tandai Selesai");
                binding.btnUbahStatus.setVisibility(View.VISIBLE);
                binding.btnEdit.setVisibility(View.GONE); // Tidak bisa edit saat sedang diproses
            } else { // Untuk status "Selesai" atau "Dibatalkan"
                binding.tvItemStatus.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.text_info));
                binding.btnUbahStatus.setVisibility(View.GONE);
                binding.btnEdit.setVisibility(View.GONE);
            }

            // Mendaftarkan semua listener ke tombol yang sesuai
            itemView.setOnClickListener(v -> listener.onItemClick(permintaan));
            binding.btnEdit.setOnClickListener(v -> listener.onEditClick(permintaan));
            binding.btnUbahStatus.setOnClickListener(v -> listener.onStatusChangeClick(permintaan));
        }
    }
}