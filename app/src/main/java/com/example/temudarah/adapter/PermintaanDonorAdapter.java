package com.example.temudarah.adapter;

import android.location.Location;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.temudarah.R;
import com.example.temudarah.databinding.ItemPermintaanDonorBinding;
import com.example.temudarah.model.PermintaanDonor;
import com.google.firebase.firestore.GeoPoint;

import java.util.List;
import java.util.Locale;

public class PermintaanDonorAdapter extends RecyclerView.Adapter<PermintaanDonorAdapter.ViewHolder> {

    private final List<PermintaanDonor> permintaanList;
    private Location currentUserLocation;
    private final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(PermintaanDonor permintaan);
    }

    public PermintaanDonorAdapter(List<PermintaanDonor> permintaanList, Location currentUserLocation, OnItemClickListener listener) {
        this.permintaanList = permintaanList;
        this.currentUserLocation = currentUserLocation;
        this.listener = listener;
    }

    public void updateUserLocation(Location location) {
        this.currentUserLocation = location;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemPermintaanDonorBinding binding = ItemPermintaanDonorBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(permintaanList.get(position), currentUserLocation, listener);
    }

    @Override
    public int getItemCount() {
        return permintaanList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemPermintaanDonorBinding binding;

        ViewHolder(ItemPermintaanDonorBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(final PermintaanDonor permintaan, Location currentUserLocation, final OnItemClickListener listener) {
            binding.tvNamaPasien.setText(permintaan.getNamaPasien());
            binding.tvRumahSakit.setText(permintaan.getNamaRumahSakit());
            binding.tvJenisKelamin.setText(permintaan.getJenisKelamin());
            binding.tvGolonganDarah.setText("Butuh Gol. " + permintaan.getGolonganDarahDibutuhkan());
            binding.tvJumlahKantong.setText(String.format(Locale.getDefault(), "%d Kantong", permintaan.getJumlahKantong()));

            if (permintaan.getFotoPembuatBase64() != null && !permintaan.getFotoPembuatBase64().isEmpty()) {
                try {
                    byte[] imageBytes = Base64.decode(permintaan.getFotoPembuatBase64(), Base64.DEFAULT);
                    Glide.with(itemView.getContext()).asBitmap().load(imageBytes).placeholder(R.drawable.foto_profil).into(binding.fotoProfilePembuat);
                } catch (Exception e) {
                    binding.fotoProfilePembuat.setImageResource(R.drawable.foto_profil);
                }
            } else {
                binding.fotoProfilePembuat.setImageResource(R.drawable.foto_profil);
            }

            if (currentUserLocation != null && permintaan.getLokasiRs() != null) {
                float[] results = new float[1];
                GeoPoint rsLocation = permintaan.getLokasiRs();
                Location.distanceBetween(currentUserLocation.getLatitude(), currentUserLocation.getLongitude(),
                        rsLocation.getLatitude(), rsLocation.getLongitude(), results);
                float distanceInKm = results[0] / 1000;
                binding.tvJarak.setText(String.format(Locale.getDefault(), "~ %.1f km", distanceInKm));
            } else {
                binding.tvJarak.setText("- km");
            }

            itemView.setOnClickListener(v -> listener.onItemClick(permintaan));
        }
    }
}