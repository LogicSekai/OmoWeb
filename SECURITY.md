# Kebijakan Keamanan

## Versi yang didukung

| Versi | Didukung |
|---|---|
| 1.0.x | ✅ |
| < 1.0 | ❌ |

## Melaporkan celah keamanan

**Jangan** laporkan celah keamanan lewat issue publik. Gunakan salah satu jalur privat berikut:

1. **GitHub › tab Security › Report a vulnerability** (private vulnerability reporting), atau
2. email ke **logicsekai@gmail.com** dengan subjek `[SECURITY] OmoWeb`.

Sertakan versi OmoWeb, model HP dan versi Android, langkah reproduksi, serta dampaknya.
Kami akan membalas dalam **7 hari**, memberi kabar perkembangan, dan mencantumkan nama Anda di catatan
rilis (bila Anda mau) setelah perbaikan dirilis.

## Lingkup

Termasuk lingkup laporan, misalnya:
- pengaturan WebView dan JavaScript yang disuntikkan OmoWeb (`WebScripts.kt`, `@JavascriptInterface`);
- penyimpanan cookie / sesi login Google dan pembersihannya saat keluar;
- link broadcast OmoWeb ↔ OmoAuto (`OmoAutoLink.kt`) yang bisa dipalsukan aplikasi lain;
- mekanisme pembaruan (`update/`): unduhan APK, pemeriksaan checksum, nama paket, dan versi;
- penanganan tautan (`intent://`, skema aplikasi) yang bisa membuka aplikasi lain tanpa izin.

Di luar lingkup: celah pada situs web yang dibuka (mis. YouTube), WebView sistem Android, Android Auto,
atau firmware OMOWAY. Laporkan ke pemiliknya masing-masing.
