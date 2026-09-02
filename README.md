# DevId Changer

Aplikasi Android berbasis Root untuk mengganti dan menginjeksikan Device ID (`JsonDeviceID` & Guest ID) pada game Mobile Legends: Bang Bang (MLBB).

## Fitur Utama

- **Root Engine Support**: Mendukung KernelSU, APatch, Magisk, SuperSU, dan Lineage su dengan isolasi namespace (`su -mm`).
- **Versi MLBB**:
  - Global: `com.mobile.legends`
  - USA: `com.mobile.legends.us`
  - Vietnam: `com.vng.mlbbvn`
  - India: `com.mobile.legends.in`
  - HWAG: `com.mobile.legends.hwag`
  - LITE: `com.mobile.legends.lite`
- **Device ID Generator & Injeksi**:
  - Generator ID acak 40 karakter hex (Global) dan 84 karakter (USA).
  - Otomatis melakukan `force-stop` game dan membunuh PID game yang berjalan.
  - Backup otomatis file konfigurasi ke `/storage/emulated/0/Tyasimut/<package_name>/<timestamp>/`.
  - Injeksi aman ke file `playerprefs.xml`, `__SharedPreference__.xml`, dan `mlsdk_deviceinformation_us`.
  - Memperbaiki izin file (*chown* UID/GID dan *chmod 660/440*).
- **Cek Nickname & Region Akun**: Memeriksa nama player dan negara asal (countryOrigin) berdasarkan User ID dan Server ID via API GoPay.
- **Preset Manager**: Menyimpan daftar Device ID ke penyimpanan lokal.
- **Antarmuka**: Dibuat dengan Jetpack Compose & Material 3, mendukung Mode Gelap/Terang dan Bahasa Indonesia/Inggris.

## Alur Kerja Injeksi (How It Works)

1. **Deteksi Root**: Aplikasi mengeksekusi `su` untuk memastikan akses root dan mendeteksi tipe root aktif.
2. **Penghentian Game**: Menjalankan `am force-stop <package_name>` dan mematikan PID terkait.
3. **Backup Data**: Menyalin file `*.playerprefs.xml` dan `__SharedPreference__.xml` ke folder backup lokal.
4. **Modifikasi File**: Mengganti nilai `JsonDeviceID`, `__Java_JsonDeviceID__`, `__CachedRealAdvertisingID__`, dan `__GPSAdId__` menggunakan perbaikan string aman (allowlist sanitization).
5. **Koreksi Izin**: Menyesuaikan pemilik file (`chown`) dan izin akses (`chmod`) agar game dapat membaca file tanpa error permission denied.

## Struktur Proyek

```text
DEVID-CHANGER/
├── app/
│   ├── src/
│   │   └── main/
│   │       ├── java/com/warungerik/devidchanger/
│   │       │   ├── engine/       # Logika injeksi file XML & enkripsi string
│   │       │   ├── model/        # Data model & konfigurasi versi game
│   │       │   ├── network/      # API client untuk cek akun MLBB
│   │       │   ├── root/         # Exec & detector command shell root
│   │       │   ├── storage/      # Pengelola penyimpanan preset DevID
│   │       │   ├── ui/           # Komponen antarmuka Jetpack Compose
│   │       │   ├── DevidChangerApp.kt
│   │       │   ├── MainActivity.kt
│   │       │   └── MainViewModel.kt
│   │       └── AndroidManifest.xml
│   └── build.gradle.kts
├── build.gradle.kts
└── settings.gradle.kts
```

## Prasyarat & Kompilasi

### Prasyarat:
- Android Studio (Ladybug 2024.2.1 atau lebih baru)
- JDK 17
- Android SDK 36 (Min SDK 23 / Android 6.0)
- Perangkat ter-root (KernelSU / APatch / Magisk)

### Perintah Build:

1. Clone repositori:
   ```bash
   git clone https://github.com/warungerik/DEVID-CHANGER-MLBB.git
   cd DEVID-CHANGER-MLBB
   ```

2. Build Debug APK:
   ```bash
   ./gradlew assembleDebug
   ```

3. Build Release APK:
   ```bash
   ./gradlew assembleRelease
   ```

File APK hasil kompilasi berlokasi di `app/build/outputs/apk/`.

## Penafian (Disclaimer)

Aplikasi ini dibuat untuk tujuan edukasi, riset teknis, dan pengujian internal. Pengembang tidak bertanggung jawab atas penyalahgunaan atau kerusakan pada akun/perangkat yang disebabkan oleh penggunaan aplikasi ini.

## Lisensi

Lisensi [MIT](LICENSE).
