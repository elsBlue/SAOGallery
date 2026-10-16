package com.example.saogallery.data

import androidx.annotation.DrawableRes

data class GalleryPhoto(
    val id: Int,
    val name: String,
    val fileName: String,
    val note: String,
    @DrawableRes val imageRes: Int,
    val crossedOut: Boolean,
)
