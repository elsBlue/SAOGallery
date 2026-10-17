package com.example.saogallery.data

import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.ContextCompat

object DeviceGallery {
    fun permission(): String {
        return if (Build.VERSION.SDK_INT >= 33) {
            android.Manifest.permission.READ_MEDIA_IMAGES
        } else {
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        }
    }

    fun canRead(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= 34) {
            val selected = ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
            ) == PackageManager.PERMISSION_GRANTED
            if (selected) return true
        }
        return ContextCompat.checkSelfPermission(context, permission()) == PackageManager.PERMISSION_GRANTED
    }

    fun load(context: Context): List<GalleryPhoto> {
        if (!canRead(context)) return emptyList()
        val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
        )
        val photos = mutableListOf<GalleryPhoto>()
        context.contentResolver.query(
            uri,
            projection,
            null,
            null,
            "${MediaStore.Images.Media.DATE_ADDED} DESC",
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val folderCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                photos += GalleryPhoto(
                    id = id.toString(),
                    name = cursor.getString(nameCol) ?: "Foto",
                    imageUri = ContentUris.withAppendedId(uri, id).toString(),
                    folder = cursor.getString(folderCol)?.ifBlank { null } ?: "Lainnya",
                )
            }
        }
        return photos
    }
}
