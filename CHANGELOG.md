# Changelog

## 1.0.0 — 2026-11-01

Rilis publik pertama OmoWeb.

### Layar motor
- Browser YouTube yang terdaftar di **Android Auto › Sesuaikan peluncur** (Android 15+). Di layar motor
  OmoWeb langsung membuka YouTube (halaman terakhir, atau beranda).
- **Kontrol dari OmoAuto**: tombol Kontrol dikirim langsung dari OmoAuto ke OmoWeb selama OmoWeb tampil
  di layar motor (Android Auto tidak meneruskan tombol ke aplikasi di layar mobil). Penunjuk di layar
  (atas/bawah/kiri/kanan, pilih = ketuk), kontrol video layar penuh (putar/jeda, mundur/maju 10 detik).
- Tombol OmoAuto **Layar penuh** dan **Ultrawide** (layar penuh + video di-zoom selebar layar,
  atas-bawah terpotong; tekan lagi untuk normal). Ultrawide stabil: zoom lewat stylesheet
  `!important` dengan skala di variabel CSS, sehingga tidak terhapus saat YouTube menata ulang
  pemutar (sebelumnya berkedip antara ultrawide dan normal).
- **Suara aktif otomatis**: video YouTube tidak lagi mulai dalam keadaan bisu; bisu yang dipilih
  pengguna tetap dihormati.
- Video menjadi **pemutar media Android** biasa (media session + notifikasi berjudul): tombol media
  OmoAuto, headset, dan kontrol media sistem bisa putar/jeda, berikutnya/sebelumnya, mundur/maju.
- Toolbar melayang dan sembunyi sendiri setelah 3,5 detik; muncul lagi saat bagian atas layar disentuh
  atau penunjuk dibawa ke atas.

### Layar HP
- **Desain baru bergaya OmoAuto** dengan **mode terang & gelap** (Tampilan › Tema: Sistem / Terang /
  Gelap): kartu hero oranye untuk Buka YouTube & Lanjutkan, langkah bernomor, pilihan bersegmen,
  baris aksen untuk tautan, tombol pil, dan font Poppins. Toolbar browser di HP ikut tema; layar motor
  tetap gelap.
- Layar awal: Buka YouTube, Lanjutkan halaman terakhir, panduan Unknown sources + Sesuaikan peluncur
  dengan tombol ke pengaturan Android Auto, akun Google, dan Bahasa (HP / Indonesia / English).
- **Login Google tersimpan** dan dipakai bersama HP & motor. Jika Google menolak "browser tidak aman",
  OmoWeb otomatis mencoba lagi sebagai Chrome desktop.
- **Pembaruan aplikasi**: memeriksa GitHub Releases (otomatis sekitar dua kali sehari, bisa dimatikan),
  memberi notifikasi, lalu mengunduh dan memasang APK langsung dari aplikasi. Unduhan dicek SHA-256,
  nama paket, dan versinya. Build Google Play tetap diperbarui lewat Play.
- Kartu **Dukung OmoWeb** (GitHub Sponsors, Ko-fi, Trakteer, kode sumber, lapor bug) dan
  **Tentang & lisensi** (AGPL-3.0, kredit, penafian tidak berafiliasi dengan OMOWAY/Google/YouTube).

### Proyek
- Open source di github.com/LogicSekai/OmoWeb. Lisensi ganda: GNU AGPL-3.0-or-later dengan ketentuan
  atribusi (`NOTICE`) atau lisensi komersial bagi sponsor repo (`COMMERCIAL-LICENSE.md`).
- Target Android 16 (SDK 36), minimal Android 15 (API 35).
