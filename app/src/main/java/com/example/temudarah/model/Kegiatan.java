package com.example.temudarah.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.GeoPoint;
import java.util.List;

public class Kegiatan {

    @Exclude private String kegiatanId; // ID unik dari dokumen Firestore

    private String judul;
    private String deskripsi;
    private String penyelenggara; // Nama penyelenggara, misal: "PMI Kota Makassar"
    private String lokasiDetail; // Alamat lengkap lokasi
    private String jenisKegiatan; // Isinya "Edukasi" atau "Donor Darah"
    private String gambarKegiatanUrl; // Link ke poster/banner kegiatan

    private GeoPoint lokasiKoordinat;
    private Timestamp tanggalMulai; // Menyimpan tanggal & waktu mulai
    private Timestamp tanggalSelesai; // Menyimpan tanggal & waktu selesai

    private int kuota; // Total kapasitas acara
    private List<String> pendaftar; // List berisi UID dari semua yang sudah mendaftar

    // Constructor kosong wajib ada agar Firestore bisa membuat objek ini secara otomatis
    public Kegiatan() {}

    // --- Penjelasan Kuota & Pendaftar ---
    // Kita tidak perlu field "sisaKuota" atau "jumlahPendaftar" terpisah di database.
    // Jumlah Pendaftar bisa didapat dari -> pendaftar.size()
    // Sisa Kuota bisa dihitung dari -> kuota - pendaftar.size()
    // Ini lebih efisien dan datanya selalu akurat.

    // --- GETTER DAN SETTER ---
    // Anda bisa generate otomatis di Android Studio (Klik kanan -> Generate -> Getter and Setter)

    @Exclude
    public String getKegiatanId() {
        return kegiatanId;
    }

    public void setKegiatanId(String kegiatanId) {
        this.kegiatanId = kegiatanId;
    }

    public String getJudul() {
        return judul;
    }

    public void setJudul(String judul) {
        this.judul = judul;
    }

    public String getDeskripsi() {
        return deskripsi;
    }

    public void setDeskripsi(String deskripsi) {
        this.deskripsi = deskripsi;
    }

    public String getPenyelenggara() {
        return penyelenggara;
    }

    public void setPenyelenggara(String penyelenggara) {
        this.penyelenggara = penyelenggara;
    }

    public String getLokasiDetail() {
        return lokasiDetail;
    }

    public void setLokasiDetail(String lokasiDetail) {
        this.lokasiDetail = lokasiDetail;
    }

    public String getJenisKegiatan() {
        return jenisKegiatan;
    }

    public void setJenisKegiatan(String jenisKegiatan) {
        this.jenisKegiatan = jenisKegiatan;
    }

    public String getGambarKegiatanUrl() {
        return gambarKegiatanUrl;
    }

    public void setGambarKegiatanUrl(String gambarKegiatanUrl) {
        this.gambarKegiatanUrl = gambarKegiatanUrl;
    }

    public GeoPoint getLokasiKoordinat() {
        return lokasiKoordinat;
    }

    public void setLokasiKoordinat(GeoPoint lokasiKoordinat) {
        this.lokasiKoordinat = lokasiKoordinat;
    }

    public Timestamp getTanggalMulai() {
        return tanggalMulai;
    }

    public void setTanggalMulai(Timestamp tanggalMulai) {
        this.tanggalMulai = tanggalMulai;
    }

    public Timestamp getTanggalSelesai() {
        return tanggalSelesai;
    }

    public void setTanggalSelesai(Timestamp tanggalSelesai) {
        this.tanggalSelesai = tanggalSelesai;
    }

    public int getKuota() {
        return kuota;
    }

    public void setKuota(int kuota) {
        this.kuota = kuota;
    }

    public List<String> getPendaftar() {
        return pendaftar;
    }

    public void setPendaftar(List<String> pendaftar) {
        this.pendaftar = pendaftar;
    }
}