<div align="center">

# OmoWeb

**YouTube dan web di layar motor OMOWAY, lewat Android Auto + OmoAuto.**

[![Android CI](https://github.com/LogicSekai/OmoWeb/actions/workflows/android.yml/badge.svg)](https://github.com/LogicSekai/OmoWeb/actions/workflows/android.yml)
[![License: AGPL v3](https://img.shields.io/badge/license-AGPL--3.0--or--later-blue.svg)](LICENSE)
[![Sponsor](https://img.shields.io/badge/sponsor-LogicSekai-ea4aaa?logo=githubsponsors&logoColor=white)](https://github.com/sponsors/LogicSekai)
[![Commercial license](https://img.shields.io/badge/commercial-via%20sponsor-orange.svg)](COMMERCIAL-LICENSE.md)
![Android 15+](https://img.shields.io/badge/Android-15%2B%20(API%2035)-3DDC84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-2.1-7F52FF?logo=kotlin&logoColor=white)

Package `com.logicsekai.omoweb` · aplikasi pendamping [**OmoAuto**](https://github.com/LogicSekai/OmoAuto) · dibuat oleh komunitas [**Logic Sekai**](https://logicsekai.com)

</div>

> [!WARNING]
> **Hanya untuk dipakai saat motor berhenti atau parkir.** Jangan menonton video saat berkendara.
> OmoWeb adalah proyek independen, tidak berafiliasi dengan OMOWAY, Google, maupun YouTube.

---

OmoWeb adalah browser ringan (halaman awal YouTube) yang terdaftar di **Android Auto › Sesuaikan
peluncur**. Saat [OmoAuto](https://github.com/LogicSekai/OmoAuto) menjalankan Android Auto ke layar
motor, OmoWeb bisa dibuka dari peluncur Android Auto dan tampil di layar motor.

## Fitur

- **YouTube di layar motor**: halaman awal `m.youtube.com`, dibuka langsung saat OmoWeb dijalankan di layar motor.
- **Dikendalikan dari OmoAuto**: Android Auto tidak meneruskan tombol ke aplikasi di layar mobil, jadi
  OmoAuto mengirim tombol Kontrol langsung ke OmoWeb:

  | Tombol | Di halaman web | Saat video layar penuh |
  |---|---|---|
  | Sebelumnya–Berikutnya / Atas–Bawah | Penunjuk naik–turun (di tepi: halaman bergulir) | Mundur / maju 10 detik |
  | Kiri / Kanan | Penunjuk ke kiri / kanan | Mundur / maju 10 detik |
  | Pilih | Ketuk di posisi penunjuk | Putar / jeda |
  | Kembali | Halaman sebelumnya | Keluar layar penuh |

- **Layar penuh** dan **Ultrawide** dari OmoAuto (Kontrol › Media › OmoWeb). Ultrawide = layar penuh
  dengan video di-zoom selebar layar, atas-bawah terpotong; tekan lagi untuk normal.
- **Suara aktif otomatis**: video YouTube tidak lagi mulai dalam keadaan bisu. Tombol bisu yang kamu
  tekan sendiri tetap dihormati.
- **Pemutar media biasa**: video punya media session dan notifikasi berjudul, jadi tombol media OmoAuto,
  headset, dan kontrol media sistem bisa mengendalikannya.
- **Login Google tersimpan** dan dipakai bersama di HP dan motor. Kalau Google menolak "browser tidak
  aman", OmoWeb otomatis mencoba lagi sebagai Chrome desktop.
- **Lanjutkan halaman terakhir**: riwayat sama di HP dan motor.
- **Toolbar sembunyi sendiri** di layar motor; sentuh bagian atas layar untuk memunculkannya.
- **Pembaruan otomatis**: notifikasi saat ada versi baru di GitHub Releases, lalu unduh dan pasang
  langsung dari aplikasi (checksum, nama paket, dan versi diperiksa sebelum dipasang).
- **Layar HP**: panduan pemasangan di Android Auto, akun Google, bahasa (Indonesia/English),
  pembaruan, dukung & donasi, serta lisensi.

## Unduh

APK tersedia di halaman [**Releases**](https://github.com/LogicSekai/OmoWeb/releases).
Butuh **Android 15 (API 35) atau lebih baru**, Android Auto, dan [OmoAuto](https://github.com/LogicSekai/OmoAuto)
untuk menampilkannya di layar motor OMOWAY.

## Memasang di Android Auto

1. Pasang OmoWeb, lalu buka sekali di HP.
2. Aktifkan mode developer Android Auto: pengaturan Android Auto → gulir ke **Versi** → ketuk ±10 kali → **OK**.
3. Pengaturan Android Auto → ⋮ → **Developer settings** → centang **Unknown sources**.
4. Paksa berhenti Android Auto, lalu buka pengaturan Android Auto → **Sesuaikan peluncur** → aktifkan **OmoWeb**.
5. Mulai Android Auto dari OmoAuto → buka OmoWeb dari peluncur di layar motor.

Jika OmoWeb gagal terbuka di layar, buka dulu aplikasi navigasi non-Google (mis. Waze) di Android
Auto, lalu buka OmoWeb.

---

## Build dari kode sumber

| Kebutuhan | Versi |
|---|---|
| JDK | 17–23 (JBR bawaan Android Studio sudah cukup) |
| Android SDK | Platform 36 + Build-Tools + Platform-Tools |
| Gradle | Otomatis lewat Gradle Wrapper |

```bash
git clone https://github.com/LogicSekai/OmoWeb.git
cd OmoWeb
./gradlew :app:assembleDebug        # APK debug
./gradlew :app:testDebugUnitTest    # unit test
```

Di Windows, jalankan **`omoweb.bat`** untuk menu, atau langsung:

| Script | Fungsi |
|---|---|
| `scripts\build-debug.bat` | APK debug → `dist\` (package `.debug`) |
| `scripts\build-release.bat` | APK release → `dist\` (untuk GitHub Releases, dengan pembaruan otomatis) |
| `scripts\install-usb.bat [debug\|release]` | Build + install lewat USB |
| `scripts\install-wifi.bat [ip:port] [debug\|release]` | Build + install lewat ADB Wi-Fi |
| `scripts\bundle-release.bat` / `bundle-beta.bat` | AAB untuk Google Play (tanpa pembaruan mandiri & tautan donasi) |
| `scripts\signing-setup.bat` | Membuat keystore rilis |
| `scripts\test.bat` | Unit test |

Versi diatur di `gradle.properties` (`omo.versionCode`, `omo.versionName`).

## Struktur

```
OmoWeb/
├─ app/src/main/kotlin/com/logicsekai/omoweb/
│  ├─ MainActivity.kt     UI browser, WebView, penunjuk remote, video layar penuh, ultrawide
│  ├─ PhoneSetupView.kt   Layar HP: panduan, akun, bahasa, pembaruan, dukung, lisensi
│  ├─ OmoAutoLink.kt      Broadcast dua arah dengan OmoAuto (status aktif & tombol Kontrol)
│  ├─ RemoteInput.kt      Pemetaan tombol/rotary → aksi, gerak penunjuk (unit-test)
│  ├─ WebScripts.kt       JavaScript: suara aktif, kontrol video, ultrawide, gulir
│  ├─ WebMediaSession.kt  Media session + notifikasi video
│  ├─ GoogleLogin.kt      Deteksi login & penolakan Google
│  ├─ WebUrls.kt          Alamat vs pencarian
│  └─ update/             Cek GitHub Releases, unduh & pasang APK, notifikasi
├─ app/src/main/AndroidManifest.xml   Pendaftaran ke peluncur Android Auto (CAR_LAUNCHER)
├─ app/src/play/                      Manifest khusus build Google Play
├─ scripts/               Script batch build/install/bundle
└─ gradle/libs.versions.toml
```

---

## Kontribusi

Kontribusi sangat diharapkan, dan **setiap perbaikan atau fitur wajib dikirim balik** ke repo ini lewat
Pull Request. Baca [CONTRIBUTING.md](CONTRIBUTING.md), [CLA.md](CLA.md),
[CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md), dan [SECURITY.md](SECURITY.md).

## Dukung Logic Sekai

OmoWeb gratis dan dikembangkan oleh komunitas **Logic Sekai**. Bila bermanfaat, dukung pengembangannya:

| Cara | Tautan | Cocok untuk |
|---|---|---|
| 💛 **GitHub Sponsors** | [github.com/sponsors/LogicSekai](https://github.com/sponsors/LogicSekai) | Sponsor rutin, juga [lisensi komersial](COMMERCIAL-LICENSE.md) |
| ☕ **Ko-fi** | [ko-fi.com/logicsekai](https://ko-fi.com/logicsekai) | Donasi sekali, PayPal atau kartu |
| 🇮🇩 **Trakteer** | [trakteer.id/kwaii](https://trakteer.id/kwaii) | Donasi dari Indonesia: QRIS, e-wallet, bank |

Di aplikasi, tautan ini ada di layar HP OmoWeb (kartu **Dukung OmoWeb**). Cara lain: kirim Pull Request,
laporkan bug, dan beri ⭐ repo ini.

## Lisensi

Copyright © 2026 **Logic Sekai** (logicsekai@gmail.com)

OmoWeb memakai **lisensi ganda**:

1. **[GNU AGPL-3.0-or-later](LICENSE)** dengan **[Ketentuan Tambahan](NOTICE)**:
   - ✅ Boleh dipakai, dipelajari, dimodifikasi, dan dipakai untuk komersial.
   - 📢 Versi yang dibagikan **wajib membuka seluruh kode sumbernya** dengan lisensi yang sama.
   - 🏷️ **Wajib mencantumkan kredit** *"Based on OmoWeb by Logic Sekai – https://github.com/LogicSekai/OmoWeb"*
     serta menyertakan `LICENSE` dan `NOTICE`.
   - ✏️ Versi modifikasi wajib memakai nama aplikasi dan *application ID* sendiri.
2. **[Lisensi komersial lewat sponsor](COMMERCIAL-LICENSE.md)**: wajib untuk pemakaian kode tertutup,
   tanpa atribusi, atau memakai merek OmoWeb/Logic Sekai. Caranya: menjadi **sponsor aktif** di
   [GitHub Sponsors](https://github.com/sponsors/LogicSekai) pada tier Komersial, lalu konfirmasi ke
   **logicsekai@gmail.com**.

Lisensi tidak memberi hak apa pun atas merek, firmware, atau perangkat OMOWAY, Google, dan YouTube.
Kredit pihak ketiga ada di [THIRD-PARTY-NOTICES.md](THIRD-PARTY-NOTICES.md).

## Penafian

- OmoWeb adalah proyek independen. **Tidak berafiliasi** dengan, didukung, atau disponsori oleh OMOWAY,
  Google, YouTube, maupun Android Auto. Nama-nama tersebut adalah merek dagang pemiliknya masing-masing.
- Konten YouTube tunduk pada Ketentuan Layanan YouTube. OmoWeb hanya menampilkan situs web YouTube biasa.
- Disediakan **apa adanya, tanpa garansi**. Gunakan hanya saat berhenti atau parkir.

<div align="center">

Dibuat dengan ❤️ oleh komunitas [Logic Sekai](https://logicsekai.com)

</div>
