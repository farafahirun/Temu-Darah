# TemuDarah - Aplikasi Sosial Donor Darah Berbasis Lokasi
**TemuDarah** adalah sebuah aplikasi Android native yang dirancang untuk menjembatani kesenjangan antara individu yang membutuhkan transfusi darah dengan para pahlawan kemanusiaan yang siap mendonorkan darahnya. Aplikasi ini diciptakan sebagai solusi modern untuk mempercepat proses pencarian pendonor darah yang relevan berdasarkan lokasi dan golongan darah secara *real-time*.

## Visi Proyek
Tujuan utama TemuDarah adalah menciptakan sebuah ekosistem digital yang responsif dan saling membantu. Dengan memanfaatkan teknologi geolokasi dan komunikasi real-time, aplikasi ini bertujuan untuk:
* **Mempercepat Waktu Respons**: Memungkinkan pencarian pendonor terdekat.
* **Meningkatkan Partisipasi**: Memberikan pengalaman pengguna yang mudah dan memotivasi melalui gamifikasi.
* **Membangun Komunitas**: Menghubungkan pendonor dan penerima dalam satu platform yang aman dan terpercaya.

## Fitur Utama
Aplikasi ini dibangun dengan alur kerja yang lengkap untuk kedua sisi pengguna (penerima dan pendonor).
#### 1. Alur Pengguna & Manajemen Profil
* **Otentikasi Aman**: Sistem pendaftaran dan login menggunakan Email/Password dengan Firebase Authentication.
* **Manajemen Sesi**: Pengguna tetap login hingga mereka logout secara manual.
* **Profil Pengguna Lengkap**: Pengguna dapat melihat dan **mengedit** semua data pribadi dan medis mereka.
* **Upload Foto Profil**: Fungsionalitas penuh untuk mengganti foto profil dari galeri atau kamera.
* **Ganti Password Aman**: Pengguna dapat mengubah password mereka dengan verifikasi password lama.

#### 2. Alur Permintaan & Bantuan (Penerima & Pendonor)
* **Buat Permintaan Donor**: Form canggih untuk membuat permintaan, lengkap dengan pengambilan alamat otomatis dari **GPS (Geocoder)**.
* **Pencarian Berbasis Lokasi**: Beranda utama menampilkan daftar permintaan dari pengguna lain dalam radius 50km menggunakan **Firebase GeoFire**.
* **Filter Pencarian**: Pengguna dapat memfilter permintaan berdasarkan Golongan Darah dan Jenis Kelamin.
* **"Beri Bantuan" (Handshake)**: Tombol aksi yang akan mengubah status permintaan menjadi "Dalam Proses" dan membuat catatan interaksi baru di `active_donations` secara atomik menggunakan **WriteBatch**.
* **Manajemen Permintaan Pribadi**: Halaman khusus "Permintaan Saya" di mana pengguna bisa mengelola postingannya (Edit, Batalkan, Tandai Selesai).
* **Logika Pembatalan Canggih**: Mendukung pembatalan oleh penerima (baik sebelum maupun sesudah ada yang membantu) dengan alur yang logis.

#### 3. Sistem Interaksi & Riwayat
* **Chat Real-time**: Ruang chat pribadi antara peminta dan pendonor setelah "jabat tangan" berhasil, didukung oleh **Firestore Real-time Listener**.
* **Daftar Chat (Inbox)**: Halaman yang menampilkan semua percakapan aktif, diurutkan berdasarkan pesan terakhir, lengkap dengan preview pesan.
* **Riwayat Komprehensif**: Halaman riwayat yang menampilkan **semua** aktivitas (Berlangsung, Selesai, Dibatalkan), dengan kemampuan filter berdasarkan peran ("Sebagai Pendonor" atau "Sebagai Penerima").
* **Detail Riwayat**: Setiap item di riwayat bisa diklik untuk melihat detail lengkap dari transaksi tersebut.

#### 4. Gamifikasi & Notifikasi
* **Sistem Penghargaan**: Pengguna mendapatkan poin dan lencana (Perunggu, Perak, Emas) berdasarkan jumlah donasi yang berhasil diselesaikan.
* **Halaman Lencana**: Halaman visual yang menampilkan progres dan pencapaian pengguna.
* **Notifikasi In-App**: Sistem notifikasi internal yang berjalan saat ada pesan baru, tawaran bantuan, atau saat proses donasi selesai. Notifikasi akan hilang secara otomatis jika konteksnya sudah dilihat (misal: membuka chat).
