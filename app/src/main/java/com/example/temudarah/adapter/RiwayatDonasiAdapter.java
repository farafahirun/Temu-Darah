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

    /**
     * FUNGSI BARU YANG HILANG SEBELUMNYA
     * Tugasnya untuk membersihkan list lama dan mengisi dengan data baru.
     */
    public void updateData(List<RiwayatDonasiTampil> newList) {
        this.riwayatList.clear();
        this.riwayatList.addAll(newList);
        notifyDataSetChanged();
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
            // (Isi dari fungsi bind ini tidak berubah, sudah benar dari sebelumnya)
            binding.tvPeran.setText(riwayat.getPeranSaya());
            binding.tvJudulTampilan.setText(riwayat.getJudulTampilan());
            binding.tvNamaPasien.setText("Untuk pasien: " + riwayat.getNamaPasien());
            binding.tvStatusProses.setText("Status: " + riwayat.getStatusProses());

            if (riwayat.getTanggal() != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM yyyy", new Locale("id", "ID"));
                binding.tvTanggal.setText(sdf.format(riwayat.getTanggal().toDate()));
            } else {
                binding.tvTanggal.setText("-");
            }

            int statusColorRes = R.color.text_info;
            if ("Selesai".equals(riwayat.getStatusProses())) statusColorRes = R.color.sukses;
            else if ("Dibatalkan".equals(riwayat.getStatusProses())) statusColorRes = R.color.utama;
            else if ("Berlangsung".equals(riwayat.getStatusProses())) statusColorRes = R.color.penerima;

            binding.tvStatusProses.setTextColor(ContextCompat.getColor(itemView.getContext(), statusColorRes));
            binding.tvPeran.setTextColor(ContextCompat.getColor(itemView.getContext(), statusColorRes));

            itemView.setOnClickListener(v -> listener.onItemClick(riwayat));
        }
    }
}