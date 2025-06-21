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
import java.util.Date;
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

        // Set nama pengirim
        if (getItemViewType(position) == VIEW_TYPE_DITERIMA) {
            // Jika ini pesan yang diterima, tampilkan nama pengirimnya
            if (pesan.getSenderName() != null && !pesan.getSenderName().isEmpty()) {
                holder.tvSenderName.setText(pesan.getSenderName());
            } else {
                holder.tvSenderName.setText("Pengirim");
            }
        } else {
            // Jika ini pesan yang dikirim oleh user, bisa tetap "Anda" seperti di layout
            // atau bisa kosongkan jika tidak ingin menampilkan teks "Anda"
            // holder.tvSenderName.setVisibility(View.GONE); // Jika tidak ingin menampilkan "Anda"
        }

        // Format dan tampilkan timestamp dengan lengkap (jam dan tanggal)
        if (pesan.getTimestamp() != null) {
            Date messageDate = pesan.getTimestamp().toDate();

            // Format waktu (jam:menit)
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
            String timeStr = timeFormat.format(messageDate);

            // Format tanggal (jika perlu)
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yy", Locale.getDefault());
            String dateStr = dateFormat.format(messageDate);

            // Bandingkan tanggal pesan dengan hari ini
            Date today = new Date();
            SimpleDateFormat compareFormat = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
            boolean isToday = compareFormat.format(messageDate).equals(compareFormat.format(today));

            // Tampilkan waktu saja jika hari ini, atau tanggal + waktu jika bukan hari ini
            if (isToday) {
                holder.tvTimestamp.setText(timeStr);
            } else {
                holder.tvTimestamp.setText(dateStr + " " + timeStr);
            }
        }
    }

    @Override
    public int getItemCount() {
        return pesanList.size();
    }

    /**
     * ViewHolder ini untuk kedua layout, karena memiliki ID komponen yang sama.
     */
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage;
        TextView tvTimestamp;
        TextView tvSenderName;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
            tvSenderName = itemView.findViewById(R.id.tvSenderName);
        }
    }
}