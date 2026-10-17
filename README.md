# SAOGallery

Galeri foto Android (Kotlin + Jetpack Compose) dengan jendela mengambang. Foto di ponsel terbaca lewat MediaStore, dikelompokkan per folder, lalu bisa diputar seperti kartu.

## Fitur

- Saat dibuka, aplikasi memindai gambar di perangkat dan foldernya.
- Daftar bawah punya tombol **Semua** dan **Folder**.
- Layar terbelah 61,8% kartu dan 38,2% daftar.
- Mode terang/gelap di kanan atas, tombol galeri senada di kanan bawah.
- Nama panjang dipotong dengan `...`.
- Enam gambar contoh tetap ada bila perangkat belum punya foto atau izin ditolak.

Package: `com.example.saogallery`.

## Cara menjalankan

```bash
chmod +x ./gradlew
./gradlew assembleDebug
```

Butuh JDK 17 dan Android SDK 35. APK debug ada di `app/build/outputs/apk/debug/`.

## GitHub Actions

[`.github/workflows/android-build.yml`](.github/workflows/android-build.yml) berjalan pada push ke `main` dan pull request, lalu mengunggah artifact `saogallery-debug-apk`.
