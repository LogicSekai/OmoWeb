# Kontribusi ke OmoWeb

Terima kasih sudah mau ikut mengembangkan OmoWeb! Proyek ini hidup dari kontribusi komunitas
**Logic Sekai**. Prinsipnya: **kalau kamu memperbaiki atau menambah sesuatu, kirim balik ke sini.**

*English speakers are welcome. Issues and pull requests may be written in Indonesian or English.*

## Sebelum mulai

1. Baca [Kode Etik](CODE_OF_CONDUCT.md).
2. Pahami lisensinya: **AGPL-3.0-or-later + Ketentuan Tambahan** ([`NOTICE`](NOTICE)) dan lisensi ganda
   dengan [lisensi komersial](COMMERCIAL-LICENSE.md).
3. Kontribusi pertama wajib menyetujui [CLA](CLA.md) (centang di template Pull Request) dan setiap
   commit wajib *sign-off* (`git commit -s`).
4. Untuk perubahan besar, **buka issue dulu** agar arahnya disepakati.

## Cara berkontribusi

- **Lapor bug**: pakai template *Laporan bug*. Sebutkan model HP, versi Android, versi Android Auto,
  versi OmoAuto, dan halaman/video yang bermasalah.
- **Usul fitur**: pakai template *Usulan fitur*.
- **Terjemahan**: `app/src/main/res/values/strings.xml` (Inggris) dan `values-in/strings.xml` (Indonesia).
- **Kode & dokumentasi**: lewat Pull Request.
- **Celah keamanan**: **jangan** lewat issue publik; ikuti [SECURITY.md](SECURITY.md).

## Menyiapkan lingkungan

JDK 17–23 dan Android SDK 36 (Android Studio versi stabil terbaru sudah cukup).

```bash
git clone https://github.com/LogicSekai/OmoWeb.git
cd OmoWeb
./gradlew :app:assembleDebug        # Windows: scripts\build-debug.bat atau omoweb.bat
./gradlew :app:testDebugUnitTest    # Windows: scripts\test.bat
```

Untuk menguji di motor, pasang juga [OmoAuto](https://github.com/LogicSekai/OmoAuto). Kontrak
broadcast OmoAuto ↔ OmoWeb dijelaskan di `docs/OMOWEB-LINK.md` repo OmoAuto; perubahan di sana harus
dilakukan di kedua repo.

## Alur kerja

1. **Fork**, lalu buat branch dari `main`: `feature/<singkat>`, `fix/<singkat>`, atau `docs/<singkat>`.
2. Commit kecil dan jelas, format `<area>: <ringkasan>`, mis. `ultrawide: skala lewat stylesheet`.
   Selalu `git commit -s`.
3. Jalankan unit test dan pastikan build debug berhasil.
4. Tambahkan catatan di [`CHANGELOG.md`](CHANGELOG.md) bila perubahannya terasa oleh pengguna.
5. Buka Pull Request ke `main` dan isi template-nya. CI menjalankan unit test dan build APK debug.

## Konvensi kode

- Kotlin official style (`.editorconfig`). Komentar dan nama simbol dalam **bahasa Inggris**.
- UI dibangun dengan View biasa (tanpa Compose) agar ringan di layar mobil. Teks UI selalu lewat
  `strings.xml` (EN + ID), jangan di-hard-code.
- Logika yang bisa diuji tanpa Android (pemetaan tombol, URL) masuk ke kelas murni seperti
  `RemoteInput.kt` / `WebUrls.kt`, beserta unit test-nya.
- JavaScript yang disuntikkan ke halaman ada di `WebScripts.kt`. Jangan bergantung pada inline `style`
  elemen YouTube (YouTube menulis ulang atribut itu); pakai stylesheet seperti Ultrawide.
- Dependency baru: versinya di `gradle/libs.versions.toml`, lisensinya kompatibel AGPL-3.0, dan
  kreditnya ditambahkan ke [`THIRD-PARTY-NOTICES.md`](THIRD-PARTY-NOTICES.md) dan teks kredit di
  `about_credits` (`strings.xml`).
- File Kotlin baru diawali header:

  ```kotlin
  // SPDX-License-Identifier: AGPL-3.0-or-later
  // Copyright (C) 2026 Logic Sekai and OmoWeb contributors. See NOTICE.
  ```

## Memakai kode orang lain

- Jangan menyalin kode dari proyek lain tanpa menyebut sumber dan lisensinya di PR.
- Kode hasil dekompilasi aplikasi pihak ketiga **tidak** diterima.
- Jangan commit kunci, APK, atau isi `_reference/`.

## Rilis (maintainer)

1. Naikkan `omo.versionCode` dan `omo.versionName` di `gradle.properties`.
2. **GitHub:** `scripts\build-release.bat` → buat GitHub Release dengan tag `v<versionName>`, lampirkan
   `dist\OmoWeb-<versi>-release.apk`. APK **wajib** ditandatangani kunci rilis yang sama
   (`keystore.properties`); aplikasi memverifikasi SHA-256, nama paket, dan versionCode sebelum memasang.
3. **Google Play:** `scripts\bundle-beta.bat` / `bundle-release.bat` (memakai `-Pomo.distribution=play`:
   tanpa pembaruan mandiri, izin `REQUEST_INSTALL_PACKAGES`, dan tautan donasi).

## Dukung tanpa menulis kode

Uji versi baru, laporkan bug, bantu jawab pertanyaan di komunitas, atau jadi sponsor lewat
[GitHub Sponsors](https://github.com/sponsors/LogicSekai), [Ko-fi](https://ko-fi.com/logicsekai), atau
[Trakteer](https://trakteer.id/kwaii).
