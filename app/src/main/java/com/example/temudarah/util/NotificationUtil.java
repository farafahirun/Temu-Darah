package com.example.temudarah.util;

import android.util.Log;
import com.example.temudarah.model.Notifikasi;
import com.example.temudarah.model.User;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;

public class NotificationUtil {

    private static final String TAG = "NotificationUtil";

    /**
     * Fungsi terpusat untuk membuat dokumen notifikasi di Firestore.
     * @param db Instance dari FirebaseFirestore.
     * @param recipientId UID dari pengguna yang akan menerima notifikasi.
     * @param title Judul notifikasi.
     * @param message Isi pesan notifikasi.
     * @param type Tipe notifikasi ("bantuan", "selesai", dll.)
     * @param tujuanId ID dokumen tujuan (misal: requestId atau chatRoomId).
     */
    public static void createNotification(FirebaseFirestore db, String recipientId, String title, String message, String type, String tujuanId) {
        // Cek pengaturan notifikasi si penerima terlebih dahulu
        db.collection("users").document(recipientId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User recipient = documentSnapshot.toObject(User.class);

                        // TODO: Tambahkan pengecekan spesifik berdasarkan tipe notifikasi
                        // Contoh: if (recipient != null && recipient.isNotifTawaranBantuan()) { ... }
                        // Untuk saat ini, kita anggap semua notif diizinkan
                        if (recipient != null) {

                            Notifikasi notif = new Notifikasi();
                            notif.setJudul(title);
                            notif.setPesan(message);
                            notif.setWaktu(Timestamp.now());
                            notif.setSudahDibaca(false);
                            notif.setTipe(type);
                            notif.setTujuanId(tujuanId);

                            // Simpan notifikasi ke sub-koleksi milik si PENERIMA
                            db.collection("users").document(recipientId)
                                    .collection("notifikasi").add(notif)
                                    .addOnSuccessListener(docRef -> Log.d(TAG, "Notifikasi '" + type + "' berhasil dibuat untuk " + recipientId))
                                    .addOnFailureListener(e -> Log.e(TAG, "Gagal membuat notifikasi '" + type + "'", e));
                        }
                    }
                });
    }
}