package com.example.temudarah.service;

import android.util.Log;
import androidx.annotation.NonNull;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "MyFirebaseMsgService";

    /**
     * Dipanggil setiap kali Firebase membuat atau memperbarui FCM Token.
     * Ini adalah tempat paling penting untuk menyimpan token ke Firestore.
     */
    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "Token baru dibuat: " + token);

        // Kirim token ini ke profil pengguna jika pengguna sedang login
        sendRegistrationToServer(token);
    }

    /**
     * Dipanggil saat ada pesan notifikasi masuk KETIKA APLIKASI SEDANG DIBUKA (foreground).
     */
    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        // Untuk saat ini, kita hanya akan menampilkan log.
        // Nanti kita bisa membuat notifikasi custom di sini.
        Log.d(TAG, "From: " + remoteMessage.getFrom());
        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "Notification Message Title: " + remoteMessage.getNotification().getTitle());
            Log.d(TAG, "Notification Message Body: " + remoteMessage.getNotification().getBody());
        }
    }

    /**
     * Fungsi untuk menyimpan atau memperbarui token di Firestore.
     */
    private void sendRegistrationToServer(String token) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            // Jika ada pengguna yang login, update tokennya
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("users").document(currentUser.getUid())
                    .update("fcmToken", token)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "FCM Token berhasil diupdate di Firestore."))
                    .addOnFailureListener(e -> Log.w(TAG, "Gagal mengupdate FCM Token.", e));
        }
    }
}