package com.example.temudarah.util;

import android.util.Log;

import com.example.temudarah.model.Notifikasi;
import com.example.temudarah.model.User;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;

public class NotificationUtil {

    private static final String TAG = "NotificationUtil";

    /**
     * Centralized function to create a notification document in Firestore.
     * @param db Instance of FirebaseFirestore.
     * @param recipientId UID of the user who will receive the notification.
     * @param title Notification title.
     * @param message Notification message.
     * @param type Notification type ("bantuan", "selesai", etc.)
     * @param targetId Target document ID (e.g., requestId or chatRoomId).
     * @param senderId UID of the sender.
     * @param senderName Name of the sender.
     */
    public static void createNotification(FirebaseFirestore db, String recipientId, String title, String message, String type, String targetId, String senderId, String senderName) {
        db.collection("users").document(recipientId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User recipient = documentSnapshot.toObject(User.class);

                        // For now, assume all notifications are allowed
                        if (recipient != null) {
                            Notifikasi notif = new Notifikasi();
                            notif.setJudul(title);
                            notif.setPesan(message);
                            notif.setWaktu(Timestamp.now());
                            notif.setSudahDibaca(false);
                            notif.setTipe(type);
                            notif.setTujuanId(targetId);
                            notif.setSenderId(senderId);
                            notif.setSenderName(senderName);

                            db.collection("users").document(recipientId)
                                    .collection("notifikasi").add(notif)
                                    .addOnSuccessListener(docRef -> Log.d(TAG, "Notification '" + type + "' created for " + recipientId))
                                    .addOnFailureListener(e -> Log.e(TAG, "Failed to create notification '" + type + "'", e));
                        }
                    }
                });
    }
}