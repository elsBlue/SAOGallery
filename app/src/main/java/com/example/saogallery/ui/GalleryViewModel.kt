package com.example.saogallery.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.saogallery.data.DeviceGallery
import com.example.saogallery.data.GalleryPhoto
import com.example.saogallery.data.GalleryRepository
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
import kotlin.math.abs
import kotlin.math.roundToInt

data class GalleryUiState(
    val photos: List<GalleryPhoto>,
    val rotation: Float,
    val activeIndex: Int,
    val fromDevice: Boolean,
)

class GalleryViewModel(app: Application) : AndroidViewModel(app) {
    private val photos = MutableStateFlow(GalleryRepository.samples)
    private val rotation = MutableStateFlow(0f)
    private val fromDevice = MutableStateFlow(false)
    private var settleJob: Job? = null

    val uiState: StateFlow<GalleryUiState> = combine(photos, rotation, fromDevice) { list, angle, device ->
        GalleryUiState(
            photos = list,
            rotation = angle,
            activeIndex = activeIndexFor(angle, list.size),
            fromDevice = device,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        GalleryUiState(GalleryRepository.samples, 0f, 0, false),
    )

    fun scanDevice() {
        viewModelScope.launch {
            val found = withContext(Dispatchers.IO) { DeviceGallery.load(getApplication()) }
            if (found.isEmpty()) return@launch
            photos.value = found
            fromDevice.value = true
            rotation.value = 0f
        }
    }

    private var dragOrigin = 0f
    private var dragDx = 0f

    fun onDragStart() {
        settleJob?.cancel()
        dragOrigin = rotation.value
        dragDx = 0f
    }

    fun onDrag(deltaPx: Float) {
        val count = photos.value.size
        if (count == 0) return
        settleJob?.cancel()
        dragDx += deltaPx
        val step = 360f / count
        val fraction = (-dragDx / PIXELS_PER_PHOTO).coerceIn(-1f, 1f)
        rotation.value = dragOrigin + fraction * step
    }

    fun onDragEnd(velocityPx: Float) {
        val count = photos.value.size
        if (count == 0) return
        val step = 360f / count
        val fraction = (-dragDx / PIXELS_PER_PHOTO).coerceIn(-1f, 1f)
        val nudged = abs(fraction) > 0.28f
        val flicked = abs(fraction) > 0.16f && abs(velocityPx) > 1400f
        val go = when {
            nudged -> if (fraction > 0f) 1 else -1
            flicked -> if (velocityPx < 0f) 1 else -1
            else -> 0
        }
        val startIndex = (dragOrigin / step).roundToInt()
        animateTo((startIndex + go) * step)
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
        const val PIXELS_PER_PHOTO = 130f
    }
}
