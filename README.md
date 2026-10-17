# Nyte Gallery

Galeri foto Android (Kotlin + Jetpack Compose) bernama Nyte Gallery, dengan jendela mengambang. Enam foto terang sudah disertakan. Geser untuk memutar, kartu di tengah paling besar.

## Fitur

- Carousel jendela mengambang, geser horizontal, mengunci ke foto terdekat.
- Mode terang dan gelap. Tombolnya di kanan atas.
- Tombol galeri di kanan bawah, senada dengan tombol tema.
- Panel galeri membelah layar: kartu di atas, daftar 4 kolom di bawah. Ketuk foto untuk memindahkan kartu.
- Nama panjang dipotong dengan `...` di kartu dan di label bawah.
- Tombol `+` membuka pemilih foto perangkat.
- MVVM ringan: `GalleryRepository` → `GalleryViewModel` → `GalleryScreen`.

Package: `com.example.saogallery`.

## Cara menjalankan

Butuh Android Studio, JDK 17, dan Android SDK 35.

```bash
chmod +x ./gradlew
./gradlew assembleDebug
```

APK debug ada di `app/build/outputs/apk/debug/`.

## GitHub Actions

[`.github/workflows/android-build.yml`](.github/workflows/android-build.yml) berjalan pada push ke `main` dan pada pull request: Ubuntu latest, JDK 17, cache Gradle, lalu artifact `saogallery-debug-apk`.
