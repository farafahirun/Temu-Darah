package com.example.temudarah.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.temudarah.R;
import com.example.temudarah.model.Pesan;
import com.google.firebase.auth.FirebaseAuth;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class PesanAdapter extends RecyclerView.Adapter<PesanAdapter.ViewHolder> {

    // Konstanta untuk membedakan tipe view
    public static final int VIEW_TYPE_DIKIRIM = 1;
    public static final int VIEW_TYPE_DITERIMA = 2;

    private List<Pesan> pesanList;
    private String currentUserId;

    public PesanAdapter(List<Pesan> pesanList) {
        this.pesanList = pesanList;
        // Dapatkan ID user yang sedang login saat ini
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            this.currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }
    }

    /**
     * Metode kunci untuk RecyclerView dengan banyak tampilan.
     * Ia akan memeriksa siapa pengirim pesan dan mengembalikan tipe view yang sesuai.
     */
    @Override
    public int getItemViewType(int position) {
        Pesan pesan = pesanList.get(position);
        if (pesan.getSenderId() != null && pesan.getSenderId().equals(currentUserId)) {
            return VIEW_TYPE_DIKIRIM;
        } else {
            return VIEW_TYPE_DITERIMA;
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        // Pilih layout XML yang sesuai berdasarkan viewType yang dikembalikan oleh getItemViewType()
        if (viewType == VIEW_TYPE_DIKIRIM) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_pesan_dikirim, parent, false);
        } else {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_pesan_diterima, parent, false);
        }
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Pesan pesan = pesanList.get(position);
        holder.tvMessage.setText(pesan.getText());

        // Format dan tampilkan timestamp
        if (pesan.getTimestamp() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
            holder.tvTimestamp.setText(sdf.format(pesan.getTimestamp().toDate()));
        }
    }

    @Override
    public int getItemCount() {
        return pesanList.size();
    }

    /**
     * ViewHolder ini cukup satu, karena kedua layout item kita
     * memiliki ID komponen yang sama (tvMessage dan tvTimestamp).
     */
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage;
        TextView tvTimestamp;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
        }
    }
}