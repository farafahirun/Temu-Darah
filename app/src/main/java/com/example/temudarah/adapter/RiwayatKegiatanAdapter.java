package com.example.temudarah.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.temudarah.databinding.ItemRiwayatKegiatanBinding;
import com.example.temudarah.model.Kegiatan;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class RiwayatKegiatanAdapter extends RecyclerView.Adapter<RiwayatKegiatanAdapter.ViewHolder> {

    private List<Kegiatan> kegiatanList;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Kegiatan kegiatan);
    }

    public RiwayatKegiatanAdapter(List<Kegiatan> kegiatanList, OnItemClickListener listener) {
        this.kegiatanList = kegiatanList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemRiwayatKegiatanBinding binding = ItemRiwayatKegiatanBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
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
        private final ItemRiwayatKegiatanBinding binding;
        ViewHolder(ItemRiwayatKegiatanBinding binding) { super(binding.getRoot()); this.binding = binding; }
        void bind(final Kegiatan kegiatan, final OnItemClickListener listener) {
            binding.tvJudulKegiatan.setText(kegiatan.getJudul());
            binding.tvPenyelenggaraKegiatan.setText("oleh " + kegiatan.getPenyelenggara());
            if (kegiatan.getTanggalMulai() != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM yyyy", Locale.forLanguageTag("id-ID"));
                binding.tvTanggalKegiatan.setText(sdf.format(kegiatan.getTanggalMulai().toDate()));
            }
            itemView.setOnClickListener(v -> listener.onItemClick(kegiatan));
        }
    }
}