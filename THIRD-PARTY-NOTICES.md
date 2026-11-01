# Kredit & pemberitahuan pihak ketiga

OmoWeb ditulis oleh Logic Sekai di atas pustaka open source berikut. Setiap komponen tetap berada di
bawah lisensinya sendiri; lisensi AGPL OmoWeb **tidak** mengubah lisensi mereka.

*Third-party credits and notices. Each component below keeps its own license.*

## Dibundel di dalam aplikasi

| Komponen | Pembuat | Lisensi | Dipakai untuk |
|---|---|---|---|
| [Kotlin standard library](https://github.com/JetBrains/kotlin) | JetBrains s.r.o. dan kontributor Kotlin | Apache-2.0 | Bahasa & runtime |
| [kotlinx.coroutines](https://github.com/Kotlin/kotlinx.coroutines) | JetBrains s.r.o. dan kontributor | Apache-2.0 | Pembaruan aplikasi (latar belakang) |
| [AndroidX Core KTX, Activity KTX, Lifecycle](https://developer.android.com/jetpack/androidx) | The Android Open Source Project | Apache-2.0 | Fondasi aplikasi Android |
| [AndroidX WebKit](https://developer.android.com/jetpack/androidx/releases/webkit) | The Android Open Source Project | Apache-2.0 | Fitur WebView tambahan |
| [Android for Cars App Library](https://developer.android.com/jetpack/androidx/releases/car-app) | The Android Open Source Project | Apache-2.0 | Entri manifest yang diperiksa Android Auto |
| [AndroidX WorkManager](https://developer.android.com/jetpack/androidx/releases/work) | The Android Open Source Project | Apache-2.0 | Pemeriksaan pembaruan berkala |
| [Poppins](https://github.com/itfoundry/Poppins) (Regular, Medium, SemiBold, Bold) | The Poppins Project Authors (Indian Type Foundry) | SIL OFL 1.1 ([teks lisensi](docs/licenses/Poppins-OFL.txt)) | Font layar HP (`app/src/main/res/font/`), sama dengan OmoAuto |

## Hanya untuk build & pengujian (tidak masuk APK)

| Komponen | Pembuat | Lisensi |
|---|---|---|
| [Gradle](https://gradle.org) & Gradle Wrapper | Gradle, Inc. | Apache-2.0 |
| [Android Gradle Plugin](https://developer.android.com/build) | Google LLC | Apache-2.0 |
| [Kotlin Gradle Plugin](https://github.com/JetBrains/kotlin) | JetBrains s.r.o. | Apache-2.0 |
| [JUnit 4](https://github.com/junit-team/junit4) | JUnit team | EPL-1.0 |
| [kotlin-test](https://github.com/JetBrains/kotlin) | JetBrains s.r.o. | Apache-2.0 |
| GitHub Actions: [checkout](https://github.com/actions/checkout), [setup-java](https://github.com/actions/setup-java), [upload-artifact](https://github.com/actions/upload-artifact), [gradle/actions](https://github.com/gradle/actions) | GitHub, Inc. / Gradle, Inc. | MIT |

## Konten pihak ketiga

OmoWeb menampilkan situs web (terutama YouTube) di dalam WebView sistem Android. Konten dan layanan
tersebut milik pemiliknya masing-masing dan tunduk pada ketentuan mereka; OmoWeb tidak menyalin,
mengunduh, atau mendistribusikan ulang konten itu.

## Merek dagang

OMOWAY, Android, Android Auto, Google Play, YouTube, dan nama terkait adalah merek dagang milik pemiliknya
masing-masing. OmoWeb adalah proyek independen dan **tidak berafiliasi** dengan, didukung, atau disponsori
oleh mereka.

## Teks lisensi

- Apache License 2.0: <https://www.apache.org/licenses/LICENSE-2.0>
- SIL Open Font License 1.1: [`docs/licenses/Poppins-OFL.txt`](docs/licenses/Poppins-OFL.txt)
- GNU AGPL v3.0: [`LICENSE`](LICENSE)
- Eclipse Public License 1.0: <https://www.eclipse.org/legal/epl-v10.html>
- MIT License: <https://opensource.org/license/mit>

Menemukan kredit yang kurang atau salah? Buka issue atau kirim email ke logicsekai@gmail.com.
