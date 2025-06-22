package com.example.temudarah.model;

import com.google.firebase.Timestamp;

// Model ini HANYA untuk membantu tampilan di RecyclerView, tidak disimpan di Firestore.
public class ChatPreview {
    private String chatRoomId;
    private String otherUserId;
    private String otherUserName;
    private String otherUserPhotoBase64;
    private String lastMessage;
    private Timestamp lastMessageTimestamp;
    private int unreadCount; // Added new field for unread message count

    public ChatPreview() {
        this.unreadCount = 0; // Default value
    }

    public String getChatRoomId() {
        return chatRoomId;
    }

    public void setChatRoomId(String chatRoomId) {
        this.chatRoomId = chatRoomId;
    }

    public String getOtherUserId() {
        return otherUserId;
    }

    public void setOtherUserId(String otherUserId) {
        this.otherUserId = otherUserId;
    }

    public String getOtherUserName() {
        return otherUserName;
    }

    public void setOtherUserName(String otherUserName) {
        this.otherUserName = otherUserName;
    }

    public String getOtherUserPhotoBase64() {
        return otherUserPhotoBase64;
    }

    public void setOtherUserPhotoBase64(String otherUserPhotoBase64) {
        this.otherUserPhotoBase64 = otherUserPhotoBase64;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public Timestamp getLastMessageTimestamp() {
        return lastMessageTimestamp;
    }

    public void setLastMessageTimestamp(Timestamp lastMessageTimestamp) {
        this.lastMessageTimestamp = lastMessageTimestamp;
    }

    public int getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(int unreadCount) {
        this.unreadCount = unreadCount;
    }
}