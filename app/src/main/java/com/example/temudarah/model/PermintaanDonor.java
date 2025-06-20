package com.example.temudarah.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.GeoPoint;

public class PermintaanDonor {

    private String namaPasien;
    private String golonganDarahDibutuhkan;
    private int jumlahKantong;
    private String catatan;

    private String namaRumahSakit;
    private GeoPoint lokasiRs;
    private String geohash;

    private String pembuatUid;
    private String namaPembuat;
    private String fotoPembuatBase64;

    private Timestamp waktuDibuat;
    private String status; // SANGAT PENTING: "Aktif" atau "Selesai"
    @Exclude
    private String requestId;

    public PermintaanDonor() {}

    public String getNamaPasien() {
        return namaPasien;
    }

    public void setNamaPasien(String namaPasien) {
        this.namaPasien = namaPasien;
    }

    public String getGolonganDarahDibutuhkan() {
        return golonganDarahDibutuhkan;
    }

    public void setGolonganDarahDibutuhkan(String golonganDarahDibutuhkan) {
        this.golonganDarahDibutuhkan = golonganDarahDibutuhkan;
    }

    public int getJumlahKantong() {
        return jumlahKantong;
    }

    public void setJumlahKantong(int jumlahKantong) {
        this.jumlahKantong = jumlahKantong;
    }

    public String getCatatan() {
        return catatan;
    }

    public void setCatatan(String catatan) {
        this.catatan = catatan;
    }

    public String getNamaRumahSakit() {
        return namaRumahSakit;
    }

    public void setNamaRumahSakit(String namaRumahSakit) {
        this.namaRumahSakit = namaRumahSakit;
    }

    public GeoPoint getLokasiRs() {
        return lokasiRs;
    }

    public void setLokasiRs(GeoPoint lokasiRs) {
        this.lokasiRs = lokasiRs;
    }

    public String getGeohash() {
        return geohash;
    }

    public void setGeohash(String geohash) {
        this.geohash = geohash;
    }

    public String getPembuatUid() {
        return pembuatUid;
    }

    public void setPembuatUid(String pembuatUid) {
        this.pembuatUid = pembuatUid;
    }

    public String getNamaPembuat() {
        return namaPembuat;
    }

    public void setNamaPembuat(String namaPembuat) {
        this.namaPembuat = namaPembuat;
    }

    public String getFotoPembuatBase64() {
        return fotoPembuatBase64;
    }

    public void setFotoPembuatBase64(String fotoPembuatBase64) {
        this.fotoPembuatBase64 = fotoPembuatBase64;
    }

    public Timestamp getWaktuDibuat() {
        return waktuDibuat;
    }

    public void setWaktuDibuat(Timestamp waktuDibuat) {
        this.waktuDibuat = waktuDibuat;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }
}