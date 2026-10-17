package com.example.saogallery.data

import com.example.saogallery.R

object GalleryRepository {
    val samples: List<GalleryPhoto> = listOf(
        GalleryPhoto("valley", "Lembah Danau Terang", R.drawable.valley, folder = "Alam"),
        GalleryPhoto("japan", "Gadis Kota Jepang", R.drawable.japan_city, folder = "Kota"),
        GalleryPhoto("cyber", "Pasar Neon Cyberpunk", R.drawable.cyberpunk, folder = "Ilustrasi"),
        GalleryPhoto("sea", "Pantai Payung Kuning", R.drawable.seaside, folder = "Alam"),
        GalleryPhoto("cat", "Kucing di Taman Bunga", R.drawable.garden_cat, folder = "Alam"),
        GalleryPhoto("lantern", "Lentera Festival Siang", R.drawable.lanterns, folder = "Kota"),
    )
}
