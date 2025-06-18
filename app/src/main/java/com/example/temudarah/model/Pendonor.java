package com.example.temudarah.model;

public class Pendonor {
    private String id;
    private String nama;
    private String golonganDarah;
    private String jenisKelamin;
    private String lokasi;
    private String usia;
    private int fotoResource;

    public Pendonor(String id, String nama, String golonganDarah, String jenisKelamin, String lokasi, String usia, int fotoResource) {
        this.id = id;
        this.nama = nama;
        this.golonganDarah = golonganDarah;
        this.jenisKelamin = jenisKelamin;
        this.lokasi = lokasi;
        this.usia = usia;
        this.fotoResource = fotoResource;
    }

    public String getId() {
        return id;
    }

    public String getNama() {
        return nama;
    }

    public String getGolonganDarah() {
        return golonganDarah;
    }

    public String getJenisKelamin() {
        return jenisKelamin;
    }

    public String getLokasi() {
        return lokasi;
    }

    public String getUsia() {
        return usia;
    }

    public int getFotoResource() {
        return fotoResource;
    }
}
