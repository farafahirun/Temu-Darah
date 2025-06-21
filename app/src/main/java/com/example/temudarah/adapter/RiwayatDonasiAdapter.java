package com.example.temudarah.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.example.temudarah.R;
import com.example.temudarah.databinding.ItemRiwayatDonasiBinding;
import com.example.temudarah.model.RiwayatDonasiTampil;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class RiwayatDonasiAdapter extends RecyclerView.Adapter<RiwayatDonasiAdapter.ViewHolder> {

    private List<RiwayatDonasiTampil> riwayatList;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(RiwayatDonasiTampil riwayat);
    }

    public RiwayatDonasiAdapter(List<RiwayatDonasiTampil> riwayatList, OnItemClickListener listener) {
        this.riwayatList = riwayatList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemRiwayatDonasiBinding binding = ItemRiwayatDonasiBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(riwayatList.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return riwayatList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemRiwayatDonasiBinding binding;
        ViewHolder(ItemRiwayatDonasiBinding binding) { super(binding.getRoot()); this.binding = binding; }
        void bind(final RiwayatDonasiTampil riwayat, final OnItemClickListener listener) {
            binding.tvPeran.setText(riwayat.getPeranSaya());
            binding.tvJudulTampilan.setText(riwayat.getJudulTampilan());
            binding.tvNamaPasien.setText("Untuk pasien: " + riwayat.getNamaPasien());
            binding.tvStatusProses.setText("Status: " + riwayat.getStatusProses()); // Tambah "Status: "

            if (riwayat.getTanggal() != null) {
                // Pastikan Locale yang digunakan benar untuk format tanggal yang diinginkan
                SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM yyyy", new Locale("id", "ID")); // Gunakan Locale Indonesia
                binding.tvTanggal.setText(sdf.format(riwayat.getTanggal().toDate()));
            } else {
                binding.tvTanggal.setText("-");
            }

            int statusColorRes = R.color.text_info; // Default abu-abu
            if ("Selesai".equals(riwayat.getStatusProses())) {
                statusColorRes = R.color.sukses; // Hijau
            } else if ("Dibatalkan".equals(riwayat.getStatusProses())) {
                statusColorRes = R.color.utama_dark; // Merah
            } else if ("Berlangsung".equals(riwayat.getStatusProses())) {
                statusColorRes = R.color.penerima; // Kuning/Oranye
            }

            binding.tvStatusProses.setTextColor(ContextCompat.getColor(itemView.getContext(), statusColorRes));
            binding.tvPeran.setTextColor(ContextCompat.getColor(itemView.getContext(), statusColorRes)); // Asumsi peran juga mengikuti warna status

            itemView.setOnClickListener(v -> listener.onItemClick(riwayat));
        }
    }
}
