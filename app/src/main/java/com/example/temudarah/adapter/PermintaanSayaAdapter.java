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

            if (permintaan.getWaktuDibuat() != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM yyyy", Locale.getDefault());
                binding.tvItemWaktu.setText("Dibuat: " + sdf.format(permintaan.getWaktuDibuat().toDate()));
            }

            // Mengatur tampilan berdasarkan status
            if ("Aktif".equals(permintaan.getStatus())) {
                binding.tvItemStatus.setText("Status: Aktif");
                binding.tvItemStatus.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.sukses)); // Anda perlu warna green
                binding.btnUbahStatus.setText("Tandai Selesai");
                binding.btnUbahStatus.setVisibility(View.VISIBLE);
                binding.btnEdit.setVisibility(View.VISIBLE);
            } else {
                binding.tvItemStatus.setText("Status: " + permintaan.getStatus());
                binding.tvItemStatus.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.text_info));
                binding.btnUbahStatus.setVisibility(View.GONE);
                binding.btnEdit.setVisibility(View.GONE); // Tombol edit juga hilang jika sudah tidak aktif
            }

            binding.btnEdit.setOnClickListener(v -> listener.onEditClick(permintaan));
            binding.btnUbahStatus.setOnClickListener(v -> listener.onStatusChangeClick(permintaan));
        }
    }
}