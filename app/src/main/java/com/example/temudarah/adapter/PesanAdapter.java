package com.example.temudarah.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.temudarah.R;
import com.example.temudarah.model.Pesan;
import com.google.firebase.auth.FirebaseAuth;

import java.text.SimpleDateFormat;
import java.util.Calendar;
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

        // Hide sender name for both sent and received messages
        if (holder.tvSenderName != null) {
            holder.tvSenderName.setVisibility(View.GONE);
        }

        // Format dan tampilkan timestamp seperti WhatsApp
        if (pesan.getTimestamp() != null) {
            Date messageDate = pesan.getTimestamp().toDate();
            formatTimestampWhatsAppStyle(holder.tvTimestamp, messageDate);

            // Tampilkan read receipt (hanya untuk pesan yang dikirim)
            if (getItemViewType(position) == VIEW_TYPE_DIKIRIM && holder.ivReadStatus != null) {
                if (pesan.isRead()) {
                    // Pesan sudah dibaca, tampilkan double check
                    holder.ivReadStatus.setImageResource(R.drawable.ic_check_double);
                } else {
                    // Pesan belum dibaca, tampilkan single check
                    holder.ivReadStatus.setImageResource(R.drawable.ic_check_single);
                }
                holder.ivReadStatus.setVisibility(View.VISIBLE);
            }
        }
    }

    /**
     * Format timestamp seperti di WhatsApp:
     * - Jika hari ini: hanya jam (10:30)
     * - Jika kemarin: "Kemarin 10:30"
     * - Jika dalam seminggu ini: nama hari (Senin 10:30)
     * - Jika lebih lama: tanggal (23/6/25 10:30)
     */
    private void formatTimestampWhatsAppStyle(TextView textView, Date messageDate) {
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        String timeStr = timeFormat.format(messageDate);

        Calendar messageCalendar = Calendar.getInstance();
        messageCalendar.setTime(messageDate);

        Calendar todayCalendar = Calendar.getInstance();

        // Reset waktu ke 00:00:00 untuk perbandingan tanggal
        Calendar messageMidnight = Calendar.getInstance();
        messageMidnight.setTime(messageDate);
        messageMidnight.set(Calendar.HOUR_OF_DAY, 0);
        messageMidnight.set(Calendar.MINUTE, 0);
        messageMidnight.set(Calendar.SECOND, 0);
        messageMidnight.set(Calendar.MILLISECOND, 0);

        Calendar todayMidnight = Calendar.getInstance();
        todayMidnight.set(Calendar.HOUR_OF_DAY, 0);
        todayMidnight.set(Calendar.MINUTE, 0);
        todayMidnight.set(Calendar.SECOND, 0);
        todayMidnight.set(Calendar.MILLISECOND, 0);

        // Perbedaan dalam hari
        long diffDays = (todayMidnight.getTimeInMillis() - messageMidnight.getTimeInMillis()) / (24 * 60 * 60 * 1000);

        if (diffDays == 0) {
            // Hari ini: tampilkan hanya waktu
            textView.setText(timeStr);
        } else if (diffDays == 1) {
            // Kemarin
            textView.setText("Kemarin " + timeStr);
        } else if (diffDays > 1 && diffDays < 7) {
            // Dalam seminggu: tampilkan nama hari
            SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE", new Locale("id", "ID"));
            String dayName = dayFormat.format(messageDate);
            textView.setText(dayName + " " + timeStr);
        } else {
            // Lebih dari seminggu: tampilkan tanggal penuh
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yy", Locale.getDefault());
            textView.setText(dateFormat.format(messageDate) + " " + timeStr);
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
        ImageView ivReadStatus;  // Added for read receipts

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
            tvSenderName = itemView.findViewById(R.id.tvSenderName);
        }
    }
}