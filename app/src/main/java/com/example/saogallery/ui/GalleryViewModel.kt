package com.example.saogallery.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.saogallery.data.GalleryPhoto
import com.example.saogallery.data.GalleryRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.abs
import kotlin.math.roundToInt

data class GalleryUiState(
    val photos: List<GalleryPhoto>,
    val rotation: Float,
    val activeIndex: Int,
)

class GalleryViewModel : ViewModel() {
    private val photos = MutableStateFlow(GalleryRepository.photos)
    private val rotation = MutableStateFlow(0f)
    private var settleJob: Job? = null
    private var dragOrigin = 0f

    val uiState: StateFlow<GalleryUiState> = combine(photos, rotation) { list, angle ->
        GalleryUiState(
            photos = list,
            rotation = angle,
            activeIndex = activeIndexFor(angle, list.size),
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        GalleryUiState(GalleryRepository.photos, 0f, 0),
    )

    fun onDragStart() {
        settleJob?.cancel()
        dragOrigin = rotation.value
    }

    fun onDrag(deltaPx: Float) {
        val count = photos.value.size
        if (count == 0) return
        settleJob?.cancel()
        val step = 360f / count
        val next = rotation.value - deltaPx * DEGREES_PER_PIXEL
        rotation.value = next.coerceIn(dragOrigin - step, dragOrigin + step)
    }

    fun onDragEnd(velocityPx: Float) {
        val count = photos.value.size
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
        val count = photos.value.size
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
        if (clean.isEmpty() || index !in photos.value.indices) return
        photos.value = photos.value.mapIndexed { i, photo ->
            if (i == index) photo.copy(name = clean) else photo
        }
    }

    fun addPicked(items: List<Pair<String, String>>) {
        if (items.isEmpty()) return
        val start = photos.value.size
        val added = items.map { (name, uri) ->
            GalleryPhoto(id = UUID.randomUUID().toString(), name = name, imageUri = uri)
        }
        photos.value = photos.value + added
        showIndex(start)
    }

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
