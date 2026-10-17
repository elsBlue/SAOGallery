package com.example.saogallery.ui

import android.app.Application
import android.graphics.BitmapFactory
import android.net.Uri
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
import java.util.UUID
import kotlin.math.abs
import kotlin.math.roundToInt

data class GalleryUiState(
    val photos: List<GalleryPhoto>,
    val trash: List<GalleryPhoto>,
    val rotation: Float,
    val activeIndex: Int,
)

class GalleryViewModel(app: Application) : AndroidViewModel(app) {
    private val vault = VaultStore(app)
    private val all = MutableStateFlow(vault.load())
    private val rotation = MutableStateFlow(0f)
    private var settleJob: Job? = null
    private var dragOrigin = 0f

    init {
        viewModelScope.launch(Dispatchers.IO) {
            var changed = false
            val next = all.value.map { photo ->
                if (photo.takenAt > 0L || photo.vaultFile == null) photo
                else {
                    val millis = vault.read(photo)?.let { captureMillis(it) } ?: 0L
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

    fun addPicked(items: List<Pair<String, String>>) {
        if (items.isEmpty()) return
        val start = visible().size
        viewModelScope.launch {
            val added = withContext(Dispatchers.IO) {
                items.mapNotNull { (name, uri) ->
                    val bytes = getApplication<Application>().contentResolver
                        .openInputStream(Uri.parse(uri))
                        ?.use { it.readBytes() }
                        ?: return@mapNotNull null
                    val taken = captureMillis(bytes).takeIf { it > 0L } ?: System.currentTimeMillis()
                    vault.import(name, bytes, UUID.randomUUID().toString(), taken)
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
    }
}

fun decodeScaled(bytes: ByteArray, maxEdge: Int): android.graphics.Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    var sample = 1
    while (bounds.outWidth / sample > maxEdge || bounds.outHeight / sample > maxEdge) sample *= 2
    val options = BitmapFactory.Options().apply { inSampleSize = sample }
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
}
