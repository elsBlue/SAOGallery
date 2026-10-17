package com.example.saogallery.data

import com.example.saogallery.R

object GalleryRepository {
    val photos: List<GalleryPhoto> = listOf(
        GalleryPhoto("valley", "Lembah Danau Terang", R.drawable.valley, takenAt = stamp(2026, 11, 11, 11, 11)),
        GalleryPhoto("japan", "Gadis Kota Jepang", R.drawable.japan_city, takenAt = stamp(2026, 4, 3, 9, 24)),
        GalleryPhoto("cyber", "Pasar Neon Cyberpunk", R.drawable.cyberpunk, takenAt = stamp(2025, 8, 19, 8, 5)),
        GalleryPhoto("sea", "Pantai Payung Kuning", R.drawable.seaside, takenAt = stamp(2026, 6, 2, 4, 40)),
        GalleryPhoto("cat", "Kucing di Taman Bunga", R.drawable.garden_cat, takenAt = stamp(2024, 12, 28, 2, 15)),
        GalleryPhoto("lantern", "Lentera Festival Siang", R.drawable.lanterns, takenAt = stamp(2025, 1, 14, 10, 2)),
    )
}
