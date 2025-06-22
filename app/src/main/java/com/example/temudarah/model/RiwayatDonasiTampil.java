package com.example.temudarah.model;

import com.google.firebase.Timestamp;

public class RiwayatDonasiTampil {

    private String prosesId;
    private String peranSaya;          // "Anda Membantu" atau "Anda Dibantu oleh"
    private String judulTampilan;      // "Permintaan oleh Budi" atau "Bantuan dari Siti"
    private String namaPasien;
    private String statusProses;       // "Berlangsung", "Selesai", "Dibatalkan"
    private Timestamp tanggal;

    public RiwayatDonasiTampil() {}

    public String getProsesId() {
        return prosesId;
    }

    public void setProsesId(String prosesId) {
        this.prosesId = prosesId;
    }

    public String getPeranSaya() {
        return peranSaya;
    }

    public void setPeranSaya(String peranSaya) {
        this.peranSaya = peranSaya;
    }

    public String getJudulTampilan() {
        return judulTampilan;
    }

    public void setJudulTampilan(String judulTampilan) {
        this.judulTampilan = judulTampilan;
    }

    public String getNamaPasien() {
        return namaPasien;
    }

    public void setNamaPasien(String namaPasien) {
        this.namaPasien = namaPasien;
    }

    public String getStatusProses() {
        return statusProses;
    }

    public void setStatusProses(String statusProses) {
        this.statusProses = statusProses;
    }

    public Timestamp getTanggal() {
        return tanggal;
    }

    public void setTanggal(Timestamp tanggal) {
        this.tanggal = tanggal;
    }
}