package com.example.saogallery.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class VaultStore(context: Context) {
    private val dir = File(context.filesDir, "vault").apply {
        mkdirs()
        File(this, ".nomedia").takeIf { !it.exists() }?.createNewFile()
    }
    private val index = File(dir, "index.json")
    private val prefs = context.getSharedPreferences("nyte_gallery", Context.MODE_PRIVATE)

    fun load(): List<GalleryPhoto> {
        purgeExpired()
        if (!index.exists()) {
            if (!prefs.getBoolean(SEEDED, false)) {
                prefs.edit().putBoolean(SEEDED, true).apply()
                val seed = GalleryRepository.photos
                write(seed)
                return seed
            }
            return emptyList()
        }
        val saved = read()
        return saved.map { photo ->
            val bundled = GalleryRepository.photos.find { it.id == photo.id }
            if (bundled != null && photo.vaultFile == null) {
                val legacy = LEGACY_NAMES[photo.id]
                photo.copy(
                    imageRes = bundled.imageRes,
                    name = if (photo.name == legacy) bundled.name else photo.name,
                    takenAt = if (photo.takenAt > 0L) photo.takenAt else bundled.takenAt,
                )
            } else {
                photo
            }
        }
    }

    fun save(photos: List<GalleryPhoto>) {
        prefs.edit().putBoolean(SEEDED, true).apply()
        write(photos)
    }

    fun import(name: String, bytes: ByteArray, id: String, takenAt: Long): GalleryPhoto {
        val file = File(dir, "$id.nyt")
        file.writeBytes(encrypt(bytes))
        return GalleryPhoto(id = id, name = name, vaultFile = file.name, takenAt = takenAt)
    }

    fun read(photo: GalleryPhoto): ByteArray? {
        val name = photo.vaultFile ?: return null
        val file = File(dir, name)
        if (!file.exists()) return null
        return decrypt(file.readBytes())
    }

    fun deleteFile(photo: GalleryPhoto) {
        val name = photo.vaultFile ?: return
        File(dir, name).delete()
    }

    fun trashExplained(): Boolean = prefs.getBoolean(TRASH_EXPLAINED, false)

    fun markTrashExplained() {
        prefs.edit().putBoolean(TRASH_EXPLAINED, true).apply()
    }

    private fun purgeExpired() {
        if (!index.exists()) return
        val now = System.currentTimeMillis()
        val kept = read().filter { photo ->
            val trashed = photo.trashedAt
            val expired = trashed != null && now - trashed >= TRASH_MS
            if (expired) deleteFile(photo)
            !expired
        }
        if (kept.size != read().size) write(kept)
    }

    private fun read(): List<GalleryPhoto> {
        val array = JSONArray(index.readText())
        return List(array.length()) { i ->
            val item = array.getJSONObject(i)
            GalleryPhoto(
                id = item.getString("id"),
                name = item.getString("name"),
                vaultFile = item.optString("file").ifBlank { null },
                trashedAt = item.optLong("trashedAt").takeIf { item.has("trashedAt") && !item.isNull("trashedAt") && it > 0L },
                takenAt = item.optLong("takenAt"),
            )
        }
    }

    private fun write(photos: List<GalleryPhoto>) {
        val array = JSONArray()
        photos.forEach { photo ->
            array.put(
                JSONObject()
                    .put("id", photo.id)
                    .put("name", photo.name)
                    .put("file", photo.vaultFile ?: "")
                    .put("trashedAt", photo.trashedAt ?: JSONObject.NULL)
                    .put("takenAt", photo.takenAt),
            )
        }
        index.writeText(array.toString())
    }

    private fun encrypt(plain: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val iv = cipher.iv
        val body = cipher.doFinal(plain)
        return iv + body
    }

    private fun decrypt(blob: ByteArray): ByteArray {
        val iv = blob.copyOfRange(0, 12)
        val body = blob.copyOfRange(12, blob.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(128, iv))
        return cipher.doFinal(body)
    }

    private fun secretKey(): SecretKey {
        val store = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (store.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setRandomizedEncryptionRequired(true)
                .build(),
        )
        return generator.generateKey()
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "nyte_gallery_vault"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val SEEDED = "seeded"
        const val TRASH_EXPLAINED = "trash_explained"
        const val TRASH_MS = 30L * 24L * 60L * 60L * 1000L
        val LEGACY_NAMES = mapOf(
            "valley" to "Lembah Danau Terang",
            "japan" to "Gadis Kota Jepang",
            "cyber" to "Pasar Neon Cyberpunk",
            "sea" to "Pantai Payung Kuning",
            "cat" to "Kucing di Taman Bunga",
            "lantern" to "Lentera Festival Siang",
        )
    }
}
