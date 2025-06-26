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
        void onContactClick(PermintaanDonor permintaan); // Tambahkan ini untuk tombol "Hubungi"
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
            binding.tvItemKantongDarah.setText("Jumlah: " + permintaan.getJumlahKantong() + " kantong");
            binding.tvItemRumahSakit.setText("Rumah Sakit: " + permintaan.getNamaRumahSakit());
            binding.tvItemCatatan.setText("Catatan: " + (permintaan.getCatatan() != null ? permintaan.getCatatan() : "-"));
            // tvBloodType tidak ada di item_permintaan_saya.xml yang Anda berikan, jadi saya hapus baris ini
            // Jika ada di item_permintaan_saya.xml Anda yang sebenarnya, tambahkan kembali.

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
                binding.btnHubungi.setVisibility(View.GONE); // Sembunyikan Hubungi jika masih Aktif
                binding.btnUbahStatus.setVisibility(View.VISIBLE);
                binding.btnEdit.setVisibility(View.VISIBLE);
            } else if ("Dalam Proses".equals(status)) {
                binding.tvItemStatus.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.penerima));
                binding.btnUbahStatus.setText("Selesai");
                binding.btnUbahStatus.setVisibility(View.VISIBLE);
                binding.btnEdit.setVisibility(View.GONE); // Edit tidak bisa saat Dalam Proses
                binding.btnHubungi.setVisibility(View.VISIBLE); // Tampilkan Hubungi jika Dalam Proses
            } else { // Untuk status "Selesai" atau "Dibatalkan"
                binding.tvItemStatus.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.text_info));
                binding.btnUbahStatus.setVisibility(View.GONE);
                binding.btnEdit.setVisibility(View.GONE);
                binding.btnHubungi.setVisibility(View.GONE); // Sembunyikan Hubungi jika Selesai/Dibatalkan
            }

            // Mendaftarkan semua listener ke tombol yang sesuai
            if (listener != null) { // Selalu cek null untuk listener
                itemView.setOnClickListener(v -> listener.onItemClick(permintaan));
                binding.btnEdit.setOnClickListener(v -> listener.onEditClick(permintaan));
                binding.btnUbahStatus.setOnClickListener(v -> listener.onStatusChangeClick(permintaan));
                binding.btnHubungi.setOnClickListener(v -> listener.onContactClick(permintaan)); // Atur listener untuk tombol Hubungi
            }
        }
    }
}