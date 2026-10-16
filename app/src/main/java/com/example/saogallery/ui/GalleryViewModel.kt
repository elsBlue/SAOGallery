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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

data class GalleryUiState(
    val photos: List<GalleryPhoto>,
    val rotation: Float,
    val activeIndex: Int,
)

class GalleryViewModel : ViewModel() {
    private val photos = GalleryRepository.photos
    val stepDegrees: Float = 360f / photos.size

    private val rotation = MutableStateFlow(0f)
    private var settleJob: Job? = null

    val uiState: StateFlow<GalleryUiState> = rotation
        .map { angle ->
            GalleryUiState(
                photos = photos,
                rotation = angle,
                activeIndex = activeIndexFor(angle),
            )
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            GalleryUiState(photos, 0f, 0),
        )

    fun onDragStart() {
        settleJob?.cancel()
    }

    fun onDrag(deltaPx: Float) {
        settleJob?.cancel()
        rotation.value -= deltaPx * DEGREES_PER_PIXEL
    }

    fun onDragEnd(velocityPx: Float) {
        val projected = rotation.value - velocityPx * FLING_SCALE
        val target = (projected / stepDegrees).roundToInt() * stepDegrees
        settleJob?.cancel()
        settleJob = viewModelScope.launch {
            val start = rotation.value
            val durationMs = 420L
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

    private fun activeIndexFor(angle: Float): Int {
        val snapped = (angle / stepDegrees).roundToInt()
        return Math.floorMod(snapped, photos.size)
    }

    private companion object {
        const val DEGREES_PER_PIXEL = 0.22f
        const val FLING_SCALE = 0.045f
    }
}
