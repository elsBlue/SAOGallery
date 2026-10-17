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
    }

    fun onDrag(deltaPx: Float) {
        if (photos.value.isEmpty()) return
        settleJob?.cancel()
        rotation.value -= deltaPx * DEGREES_PER_PIXEL
    }

    fun onDragEnd(velocityPx: Float) {
        val count = photos.value.size
        if (count == 0) return
        val step = 360f / count
        val projected = rotation.value - velocityPx * FLING_SCALE
        animateTo((projected / step).roundToInt() * step)
    }

    fun showIndex(index: Int) {
        val count = photos.value.size
        if (count == 0) return
        val step = 360f / count
        val current = rotation.value
        val currentMod = ((current % 360f) + 360f) % 360f
        var delta = index * step - currentMod
        if (delta > 180f) delta -= 360f
        if (delta < -180f) delta += 360f
        animateTo(current + delta)
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
        const val FLING_SCALE = 0.045f
    }
}
