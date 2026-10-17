package com.example.saogallery.data

import androidx.annotation.DrawableRes

data class GalleryPhoto(
    val id: String,
    val name: String,
    @DrawableRes val imageRes: Int = 0,
    val imageUri: String? = null,
    val folder: String = "Lainnya",
)
