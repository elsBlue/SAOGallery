package com.example.saogallery.ui

import android.app.Application
import android.app.RecoverableSecurityException
import android.content.ContentValues
import android.content.IntentSender
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.saogallery.data.GalleryPhoto
import com.example.saogallery.data.VaultStore
import com.example.saogallery.data.captureMillis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.UUID
import kotlin.math.abs
import kotlin.math.roundToInt

data class GalleryUiState(
    val photos: List<GalleryPhoto>,
    val trash: List<GalleryPhoto>,
    val rotation: Float,
    val activeIndex: Int,
)

data class ImportOutcome(
    val consent: IntentSender?,
    val imported: Int,
    val alreadyRemoved: Boolean,
)

sealed class DeleteStep {
    data class Consent(val sender: IntentSender) : DeleteStep()
    data object Done : DeleteStep()
    data object Failed : DeleteStep()
}

class GalleryViewModel(app: Application) : AndroidViewModel(app) {
    private val vault = VaultStore(app)
    private val all = MutableStateFlow(vault.load())
    private val rotation = MutableStateFlow(0f)
    private var settleJob: Job? = null
    private var dragOrigin = 0f
    private var deleteQueue: List<List<Uri>> = emptyList()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            var changed = false
            val next = all.value.map { photo ->
                if (photo.takenAt > 0L || photo.vaultFile == null) photo
                else {
                    val millis = vault.openPlain(photo)?.use { captureMillis(it) } ?: 0L
                    if (millis > 0L) {
                        changed = true
                        photo.copy(takenAt = millis)
                    } else {
                        photo
                    }
                }
            }
            if (changed) {
                all.value = next
                vault.save(next)
            }
        }
    }

    val uiState: StateFlow<GalleryUiState> = combine(all, rotation) { list, angle ->
        val visible = list.filter { it.trashedAt == null }
        GalleryUiState(
            photos = visible,
            trash = list.filter { it.trashedAt != null },
            rotation = angle,
            activeIndex = activeIndexFor(angle, visible.size),
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        run {
            val visible = all.value.filter { it.trashedAt == null }
            GalleryUiState(visible, all.value.filter { it.trashedAt != null }, 0f, 0)
        },
    )

    fun onDragStart() {
        settleJob?.cancel()
        dragOrigin = rotation.value
    }

    fun onDrag(deltaPx: Float) {
        val count = visible().size
        if (count == 0) return
        settleJob?.cancel()
        val step = 360f / count
        val next = rotation.value - deltaPx * DEGREES_PER_PIXEL
        rotation.value = next.coerceIn(dragOrigin - step, dragOrigin + step)
    }

    fun onDragEnd(velocityPx: Float) {
        val count = visible().size
        if (count == 0) return
        val step = 360f / count
        val delta = rotation.value - dragOrigin
        val nudged = abs(delta) > step * 0.28f
        val flicked = abs(delta) > step * 0.12f && abs(velocityPx) > 900f
        val steps = when {
            nudged -> if (delta > 0f) 1 else -1
            flicked -> if (velocityPx < 0f) 1 else -1
            else -> 0
        }
        animateTo(dragOrigin + steps * step)
    }

    fun showIndex(index: Int) {
        val count = visible().size
        if (count == 0) return
        val step = 360f / count
        val currentIndex = activeIndexFor(rotation.value, count)
        var delta = index - currentIndex
        if (delta > count / 2) delta -= count
        if (delta < -count / 2) delta += count
        if (abs(delta) <= 1) animateTo(rotation.value + delta * step)
        else rotation.value = index * step
    }

    fun rename(index: Int, name: String) {
        val clean = name.trim()
        val photo = visible().getOrNull(index) ?: return
        if (clean.isEmpty()) return
        update(photo.id) { it.copy(name = clean) }
    }

    fun trashExplained(): Boolean = vault.trashExplained()

    fun markTrashExplained() = vault.markTrashExplained()

    fun moveToTrash(index: Int) {
        settleJob?.cancel()
        val visible = visible()
        val photo = visible.getOrNull(index) ?: return
        update(photo.id) { it.copy(trashedAt = System.currentTimeMillis()) }
        val left = visible.size - 1
        rotation.value = if (left <= 0) 0f else index.coerceAtMost(left - 1) * (360f / left)
    }

    fun restore(id: String) {
        settleJob?.cancel()
        update(id) { it.copy(trashedAt = null) }
        val after = visible()
        val index = after.indexOfFirst { it.id == id }
        if (index >= 0 && after.isNotEmpty()) {
            rotation.value = index * (360f / after.size)
        }
    }

    fun emptyTrash() {
        val trashed = all.value.filter { it.trashedAt != null }
        trashed.forEach { vault.deleteFile(it) }
        all.value = all.value.filter { it.trashedAt == null }
        vault.save(all.value)
    }

    fun exportToPublic(ids: Set<String>, onDone: (Int) -> Unit) {
        val before = visible()
        val targets = before.filter { it.id in ids }
        if (targets.isEmpty()) {
            onDone(0)
            return
        }
        val activeId = before.getOrNull(activeIndexFor(rotation.value, before.size))?.id
        viewModelScope.launch {
            val exported = withContext(Dispatchers.IO) {
                targets.mapNotNull { photo ->
                    if (writePublic(photo)) photo.id else null
                }.toSet()
            }
            if (exported.isNotEmpty()) {
                settleJob?.cancel()
                all.value.filter { it.id in exported }.forEach { vault.deleteFile(it) }
                val next = all.value.filter { it.id !in exported }
                all.value = next
                vault.save(next)
                val after = next.filter { it.trashedAt == null }
                val index = after.indexOfFirst { it.id == activeId }.let { if (it >= 0) it else 0 }
                rotation.value = if (after.isEmpty()) 0f else index * (360f / after.size)
            }
            onDone(exported.size)
        }
    }

    private fun writePublic(photo: GalleryPhoto): Boolean {
        val resolver = getApplication<Application>().contentResolver
        var uri: Uri? = null
        return try {
            val safe = photo.name.replace(Regex("[^A-Za-z0-9 _-]"), "").trim().take(40).ifBlank { "Nyte" }
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "$safe-${photo.id.take(6)}.jpg")
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= 29) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Nyte Gallery")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }
            val created = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return false
            uri = created
            val wrote = resolver.openOutputStream(created)?.use { out -> writePhoto(photo, out) } ?: false
            if (!wrote) {
                resolver.delete(created, null, null)
                return false
            }
            if (Build.VERSION.SDK_INT >= 29) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(created, values, null, null)
            }
            true
        } catch (_: Exception) {
            uri?.let { resolver.delete(it, null, null) }
            false
        }
    }

    private fun writePhoto(photo: GalleryPhoto, out: java.io.OutputStream): Boolean {
        if (photo.vaultFile != null) return vault.copyPlain(photo, out)
        val bytes = bundledBytes(photo) ?: return false
        out.write(bytes)
        return true
    }

    private fun bundledBytes(photo: GalleryPhoto): ByteArray? {
        if (photo.imageRes == 0) return null
        val bitmap = BitmapFactory.decodeResource(getApplication<Application>().resources, photo.imageRes) ?: return null
        return ByteArrayOutputStream().use { stream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 92, stream)
            if (!bitmap.isRecycled) bitmap.recycle()
            stream.toByteArray()
        }
    }

    fun importFromDevice(uriStrings: List<String>, onReady: (ImportOutcome) -> Unit) {
        if (uriStrings.isEmpty()) return
        val start = visible().size
        viewModelScope.launch {
            val uris = uriStrings.map { Uri.parse(it) }
            val imported = withContext(Dispatchers.IO) {
                val resolver = getApplication<Application>().contentResolver
                uris.mapNotNull { uri ->
                    val rawName = resolver.query(uri, arrayOf(MediaStore.MediaColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) cursor.getString(0) else null
                    } ?: "Photo"
                    val taken = takenMillis(resolver, uri).takeIf { it > 0L } ?: System.currentTimeMillis()
                    val photo = resolver.openInputStream(uri)?.use { input ->
                        vault.importStream(rawName.substringBeforeLast('.'), input, UUID.randomUUID().toString(), taken)
                    } ?: return@mapNotNull null
                    uri to photo
                }
            }
            if (imported.isEmpty()) {
                onReady(ImportOutcome(null, 0, false))
                return@launch
            }
            all.value = all.value + imported.map { it.second }
            vault.save(all.value)
            showIndex(start)
            val removal = withContext(Dispatchers.IO) { planRemoval(imported.map { it.first }) }
            deleteQueue = removal.rest
            onReady(ImportOutcome(removal.consent, imported.size, removal.alreadyRemoved))
        }
    }

    private fun takenMillis(resolver: android.content.ContentResolver, uri: Uri): Long {
        val stored = try {
            resolver.query(uri, arrayOf(MediaStore.Images.Media.DATE_TAKEN), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst() && !cursor.isNull(0)) cursor.getLong(0) else 0L
            } ?: 0L
        } catch (_: Exception) {
            0L
        }
        if (stored > 0L) return stored
        return try {
            resolver.openInputStream(uri)?.use { captureMillis(it) } ?: 0L
        } catch (_: Exception) {
            0L
        }
    }

    private fun planRemoval(uris: List<Uri>): Removal {
        if (uris.isEmpty()) return Removal(null, true)
        val resolver = getApplication<Application>().contentResolver
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val chunks = uris.chunked(DELETE_BATCH)
            return try {
                Removal(
                    MediaStore.createDeleteRequest(resolver, chunks.first()).intentSender,
                    false,
                    chunks.drop(1),
                )
            } catch (_: Exception) {
                Removal(null, false)
            }
        }
        var removed = true
        for (uri in uris) {
            try {
                if (resolver.delete(uri, null, null) <= 0) removed = false
            } catch (error: SecurityException) {
                if (Build.VERSION.SDK_INT >= 29) {
                    recoverableSender(error)?.let { return Removal(it, false) }
                }
                removed = false
            }
        }
        return Removal(null, removed)
    }

    fun nextDeleteStep(): DeleteStep {
        val queue = deleteQueue
        if (queue.isEmpty()) return DeleteStep.Done
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            deleteQueue = emptyList()
            return DeleteStep.Failed
        }
        return try {
            val sender = MediaStore.createDeleteRequest(
                getApplication<Application>().contentResolver,
                queue.first(),
            ).intentSender
            deleteQueue = queue.drop(1)
            DeleteStep.Consent(sender)
        } catch (_: Exception) {
            deleteQueue = emptyList()
            DeleteStep.Failed
        }
    }

    fun clearDeleteQueue() {
        deleteQueue = emptyList()
    }

    @RequiresApi(29)
    private fun recoverableSender(error: SecurityException): IntentSender? {
        return (error as? RecoverableSecurityException)?.userAction?.actionIntent?.intentSender
    }

    fun addPicked(items: List<Pair<String, String>>) {
        if (items.isEmpty()) return
        val start = visible().size
        viewModelScope.launch {
            val added = withContext(Dispatchers.IO) {
                val resolver = getApplication<Application>().contentResolver
                items.mapNotNull { (name, uriText) ->
                    val uri = Uri.parse(uriText)
                    val taken = takenMillis(resolver, uri).takeIf { it > 0L } ?: System.currentTimeMillis()
                    resolver.openInputStream(uri)?.use { input ->
                        vault.importStream(name, input, UUID.randomUUID().toString(), taken)
                    }
                }
            }
            if (added.isEmpty()) return@launch
            all.value = all.value + added
            vault.save(all.value)
            showIndex(start)
        }
    }

    fun openVault(photo: GalleryPhoto): ByteArray? = vault.read(photo)

    private fun update(id: String, change: (GalleryPhoto) -> GalleryPhoto) {
        all.value = all.value.map { if (it.id == id) change(it) else it }
        vault.save(all.value)
    }

    private fun visible() = all.value.filter { it.trashedAt == null }

    private fun animateTo(target: Float) {
        settleJob?.cancel()
        settleJob = viewModelScope.launch {
            val start = rotation.value
            val durationMs = 480L
            val started = System.nanoTime()
            while (isActive) {
                val t = ((System.nanoTime() - started) / 1_000_000f) / durationMs
                if (t >= 1f) {
                    rotation.value = target
                    break
                }
                val eased = 1f - (1f - t) * (1f - t) * (1f - t)
                rotation.value = start + (target - start) * eased
                delay(16)
            }
        }
    }

    private fun activeIndexFor(angle: Float, count: Int): Int {
        if (count == 0) return 0
        val step = 360f / count
        return Math.floorMod((angle / step).roundToInt(), count)
    }

    private companion object {
        const val DEGREES_PER_PIXEL = 0.22f
        const val DELETE_BATCH = 20
    }
}

private data class Removal(
    val consent: IntentSender?,
    val alreadyRemoved: Boolean,
    val rest: List<List<Uri>> = emptyList(),
)
