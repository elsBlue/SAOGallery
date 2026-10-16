package com.example.saogallery.data

import com.example.saogallery.R

object GalleryRepository {
    val photos: List<GalleryPhoto> = listOf(
        GalleryPhoto(
            id = 1,
            name = "Ren Kade",
            fileName = "REC_014",
            note = "Arsip tengah. Ditandai.",
            imageRes = R.drawable.portrait_ren,
            crossedOut = true,
        ),
        GalleryPhoto(
            id = 2,
            name = "Mira Ell",
            fileName = "REC_021",
            note = "Jendela mengambang, belum ditandai.",
            imageRes = R.drawable.portrait_mira,
            crossedOut = false,
        ),
        GalleryPhoto(
            id = 3,
            name = "Harun Voss",
            fileName = "REC_008",
            note = "Rekaman lama. Sudah dicoret.",
            imageRes = R.drawable.portrait_harun,
            crossedOut = true,
        ),
        GalleryPhoto(
            id = 4,
            name = "Sable Quinn",
            fileName = "REC_033",
            note = "Berkas aktif di tepi lingkaran.",
            imageRes = R.drawable.portrait_sable,
            crossedOut = false,
        ),
        GalleryPhoto(
            id = 5,
            name = "Ivo Pell",
            fileName = "REC_019",
            note = "Ditandai dari putaran sebelumnya.",
            imageRes = R.drawable.portrait_ivo,
            crossedOut = true,
        ),
        GalleryPhoto(
            id = 6,
            name = "Nera Sol",
            fileName = "REC_027",
            note = "Potret terakhir dalam dek.",
            imageRes = R.drawable.portrait_nera,
            crossedOut = false,
        ),
    )
}
