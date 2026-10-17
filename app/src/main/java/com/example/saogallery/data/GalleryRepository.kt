package com.example.saogallery.data

import com.example.saogallery.R

object GalleryRepository {
    val photos: List<GalleryPhoto> = listOf(
        GalleryPhoto("valley", "Lembah Danau Terang", R.drawable.valley),
        GalleryPhoto("japan", "Gadis Kota Jepang", R.drawable.japan_city),
        GalleryPhoto("cyber", "Pasar Neon Cyberpunk", R.drawable.cyberpunk),
        GalleryPhoto("sea", "Pantai Payung Kuning", R.drawable.seaside),
        GalleryPhoto("cat", "Kucing di Taman Bunga", R.drawable.garden_cat),
        GalleryPhoto("lantern", "Lentera Festival Siang", R.drawable.lanterns),
    )
}
