package com.example.temudarah.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.temudarah.R;
import com.example.temudarah.databinding.ItemKegiatanBinding;
import com.example.temudarah.model.Kegiatan;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class KegiatanAdapter extends RecyclerView.Adapter<KegiatanAdapter.ViewHolder> {

    private List<Kegiatan> kegiatanList;
    private OnItemClickListener listener;

    // Interface listener sekarang hanya butuh satu aksi: saat seluruh item diklik
    public interface OnItemClickListener {
        void onItemClick(Kegiatan kegiatan);
    }

    public KegiatanAdapter(List<Kegiatan> kegiatanList, OnItemClickListener listener) {
        this.kegiatanList = kegiatanList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Menggunakan View Binding untuk layout item_kegiatan.xml
        ItemKegiatanBinding binding = ItemKegiatanBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(kegiatanList.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return kegiatanList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemKegiatanBinding binding;

        ViewHolder(ItemKegiatanBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        /**
         * Fungsi bind yang sudah disederhanakan untuk menampilkan data ringkas.
         */
        void bind(final Kegiatan kegiatan, final OnItemClickListener listener) {
            binding.tvJudulKegiatan.setText(kegiatan.getJudul());
            binding.tvPenyelenggara.setText("oleh " + kegiatan.getPenyelenggara());

            // Mengisi tanggal dan waktu yang sudah dipisah
            if (kegiatan.getTanggalMulai() != null && kegiatan.getTanggalSelesai() != null) {
                SimpleDateFormat sdfDate = new SimpleDateFormat("dd MMMM yyyy", Locale.forLanguageTag("id-ID"));
                binding.tvTanggalKegiatan.setText(sdfDate.format(kegiatan.getTanggalMulai().toDate()));

                SimpleDateFormat sdfTime = new SimpleDateFormat("HH:mm", Locale.getDefault());
                String waktuMulai = sdfTime.format(kegiatan.getTanggalMulai().toDate());
                String waktuSelesai = sdfTime.format(kegiatan.getTanggalSelesai().toDate());
                String rentangWaktu = waktuMulai + " - " + waktuSelesai;
                binding.tvJamKegiatan.setText(rentangWaktu);
            } else {
                binding.tvTanggalKegiatan.setText("Tanggal belum ditentukan");
                binding.tvJamKegiatan.setText("");
            }

            // Memuat gambar banner kegiatan menggunakan Glide
            if (itemView.getContext() != null && kegiatan.getGambarKegiatanUrl() != null && !kegiatan.getGambarKegiatanUrl().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(kegiatan.getGambarKegiatanUrl())
                        .placeholder(R.drawable.carousel) // Gambar default saat loading
                        .error(R.drawable.carousel) // Gambar default jika ada error
                        .into(binding.ivGambarKegiatan);
            }

            // Listener untuk seluruh kartu item, akan membuka halaman detail
            itemView.setOnClickListener(v -> listener.onItemClick(kegiatan));
        }
    }
}