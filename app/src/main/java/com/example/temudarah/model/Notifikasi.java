package com.example.temudarah.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.Exclude;

public class Notifikasi {
    @Exclude
    private String notifId;
    private String judul;
    private String pesan;
    private Timestamp waktu;
    private boolean sudahDibaca;
    private String tipe; // "pesan", "bantuan", "selesai"
    private String tujuanId; // ID dari chat, permintaan, atau proses donasi

    public Notifikasi() {}

    public String getNotifId() {
        return notifId;
    }

    public void setNotifId(String notifId) {
        this.notifId = notifId;
    }

    public String getJudul() {
        return judul;
    }

    public void setJudul(String judul) {
        this.judul = judul;
    }

    public String getPesan() {
        return pesan;
    }

    public void setPesan(String pesan) {
        this.pesan = pesan;
    }

    public Timestamp getWaktu() {
        return waktu;
    }

    public void setWaktu(Timestamp waktu) {
        this.waktu = waktu;
    }

    public boolean isSudahDibaca() {
        return sudahDibaca;
    }

    public void setSudahDibaca(boolean sudahDibaca) {
        this.sudahDibaca = sudahDibaca;
    }

    public String getTipe() {
        return tipe;
    }

    public void setTipe(String tipe) {
        this.tipe = tipe;
    }

    public String getTujuanId() {
        return tujuanId;
    }

    public void setTujuanId(String tujuanId) {
        this.tujuanId = tujuanId;
    }
}