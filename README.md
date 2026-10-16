# SAOGallery

Galeri foto Android (Kotlin + Jetpack Compose) dengan jendela mengambang bergaya 3D. Foto berjejer miring, saling overlap, dan bisa diputar dengan gestur geser. Foto yang sudah “dicoret” membawa tanda silang merah besar. Foto aktif di tengah tampil paling besar.

Potret di aplikasi ini adalah ilustrasi asli, bukan artwork dari serial manapun.

## Fitur

- Carousel melingkar dengan jendela mengambang (title bar, tiga titik, perspektif `rotationY` / skala berdasarkan kedalaman).
- Geser horizontal untuk memutar galeri. Lepas jari dan galeri mengunci ke foto terdekat, dengan sedikit inersia.
- Satu foto aktif di tengah, lebih besar, plus lencana putar di tengah jendela.
- Foto bertanda (`crossedOut`) mendapat wash merah dan overlay X besar.
- Latar crimson gelap dengan glow di belakang kartu aktif.
- Data dummy 6 potret di `res/drawable`.
- MVVM ringan: `GalleryRepository` → `GalleryViewModel` → `GalleryScreen`.

## Struktur

```text
app/src/main/java/com/example/saogallery/
  MainActivity.kt
  data/GalleryPhoto.kt
  data/GalleryRepository.kt
  ui/GalleryViewModel.kt
  ui/GalleryScreen.kt
  ui/theme/Theme.kt
```

Package aplikasi: `com.example.saogallery`.

## Cara menjalankan

Butuh Android Studio (Ladybug atau lebih baru), JDK 17, dan Android SDK 35.

1. Clone repositori ini.
2. Buka folder project di Android Studio.
3. Biarkan Gradle sync selesai (wrapper sudah ikut di repo).
4. Jalankan konfigurasi `app` pada emulator atau perangkat (minSdk 26).

Dari terminal:

```bash
chmod +x ./gradlew
./gradlew assembleDebug
```

APK debug ada di `app/build/outputs/apk/debug/`.

## GitHub Actions

Workflow [`.github/workflows/android-build.yml`](.github/workflows/android-build.yml) berjalan pada push ke `main` dan pada pull request:

- Ubuntu latest
- JDK 17 (Temurin) dengan cache Gradle
- `assembleDebug`
- APK diunggah sebagai artifact `saogallery-debug-apk`

## Mengganti foto

Tambah entri di `GalleryRepository` dan letakkan file gambar (huruf kecil, tanpa spasi) di `app/src/main/res/drawable/`. Set `crossedOut = true` supaya silang merah muncul.
