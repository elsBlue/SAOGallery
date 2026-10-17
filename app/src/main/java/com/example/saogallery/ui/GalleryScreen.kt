package com.example.saogallery.ui

import android.app.Activity
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.view.WindowCompat
import com.example.saogallery.data.DeviceGallery
import com.example.saogallery.data.GalleryPhoto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

private val Paper = Color(0xFFF7F3EE)
private val Ink = Color(0xFF3A2C32)
private val DotA = Color(0xFFFF4D6A)
private val DotB = Color(0xFFFF8AA0)
private val DotC = Color(0xFFFFC0CC)

private const val CARD_LIMIT = 16
private const val LABEL_LIMIT = 22
private enum class ListMode { All, Folder }

@Composable
fun GalleryScreen(
    state: GalleryUiState,
    onDragStart: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: (Float) -> Unit,
    onShowIndex: (Int) -> Unit,
    onScan: () -> Unit,
) {
    val context = LocalContext.current
    var light by remember { mutableStateOf(false) }
    var galleryOpen by remember { mutableStateOf(false) }
    var granted by remember { mutableStateOf(DeviceGallery.canRead(context)) }
    val view = LocalView.current
    SideEffect {
        val window = (view.context as Activity).window
        val controller = WindowCompat.getInsetsController(window, view)
        controller.isAppearanceLightStatusBars = light
        controller.isAppearanceLightNavigationBars = light
    }

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        granted = DeviceGallery.canRead(context)
        if (granted) onScan()
    }
    LaunchedEffect(Unit) {
        if (DeviceGallery.canRead(context)) onScan() else permission.launch(DeviceGallery.permission())
    }

    val bg = if (light) {
        Brush.verticalGradient(listOf(Color(0xFFFBF9F6), Color(0xFFE7E1D8)))
    } else {
        Brush.verticalGradient(listOf(Color(0xFF23262E), Color(0xFF12141A), Color(0xFF0D0E12)))
    }
    val ink = if (light) Color(0xFF2A2428) else Color.White
    val chrome = if (light) Color.White else Color(0xFF1C1E26)
    val chromeInk = if (light) Color(0xFF2A2428) else Color.White

    Box(Modifier.fillMaxSize().background(bg)) {
        Column(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .weight(if (galleryOpen) 0.618f else 1f)
                    .fillMaxWidth()
                    .pointerInput(state.photos.size) {
                        val tracker = VelocityTracker()
                        detectHorizontalDragGestures(
                            onDragStart = {
                                tracker.resetTracking()
                                onDragStart()
                            },
                            onHorizontalDrag = { change, dragAmount ->
                                tracker.addPosition(change.uptimeMillis, change.position)
                                onDrag(dragAmount)
                            },
                            onDragEnd = { onDragEnd(tracker.calculateVelocity().x) },
                            onDragCancel = { onDragEnd(0f) },
                        )
                    },
            ) {
                Stage(state = state, light = light, compact = galleryOpen, ink = ink)
            }
            if (galleryOpen) {
                PhotoGrid(
                    photos = state.photos,
                    activeIndex = state.activeIndex,
                    light = light,
                    granted = granted,
                    onAllow = { permission.launch(DeviceGallery.permission()) },
                    onPick = onShowIndex,
                    modifier = Modifier.weight(0.382f).fillMaxWidth(),
                )
            }
        }

        ChromeButton(
            container = chrome,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(top = 12.dp, end = 12.dp),
            onClick = { light = !light },
        ) { ThemeGlyph(showSun = !light, color = chromeInk) }

        ChromeButton(
            container = chrome,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(bottom = 12.dp, end = 12.dp),
            onClick = { galleryOpen = !galleryOpen },
        ) { GalleryGlyph(chromeInk) }
    }
}

@Composable
private fun Stage(
    state: GalleryUiState,
    light: Boolean,
    compact: Boolean,
    ink: Color,
) {
    val active = state.photos.getOrNull(state.activeIndex)
    Box(Modifier.fillMaxSize()) {
        Fan(compact = compact, photos = state.photos, rotation = state.rotation, activeIndex = state.activeIndex)
        Text(
            text = "SAO GALLERY",
            color = if (light) Color(0xFF5C463F) else Color(0xFFF3ECE8),
            fontSize = 13.sp,
            letterSpacing = 4.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(top = 18.dp),
        )
        if (active != null) {
            Text(
                text = clipName(active.name, LABEL_LIMIT),
                color = ink,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = if (compact) 8.dp else 88.dp, start = 72.dp, end = 72.dp),
            )
        }
    }
}

@Composable
private fun Fan(
    compact: Boolean,
    photos: List<GalleryPhoto>,
    rotation: Float,
    activeIndex: Int,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val cardWidth = maxWidth * if (compact) 0.42f else 0.62f
        val cardHeight = cardWidth * 1.32f
        val radiusPx = with(density) { (maxWidth * if (compact) 0.34f else 0.52f).toPx() }
        val count = photos.size
        val step = if (count == 0) 360f else 360f / count
        val center = if (count == 0) 0f else rotation / step
        photos.forEachIndexed { index, photo ->
            var delta = index - center
            if (count > 0) {
                delta %= count
                if (delta > count / 2f) delta -= count
                if (delta < -count / 2f) delta += count
            }
            if (count > 8 && abs(delta) > 2.4f) return@forEachIndexed
            val rel = if (count <= 8) wrapDegrees(index * step - rotation) else delta * 32f
            if (count <= 8 && abs(rel) > 130f) return@forEachIndexed
            val rad = Math.toRadians(rel.toDouble())
            val depth = cos(rad).toFloat()
            val scale = 0.62f + ((depth + 1f) / 2f) * 0.5f
            val fade = (1.15f - abs(rel) / 130f).coerceIn(0.15f, 1f)
            val boost = if (index == activeIndex) 1.04f else 1f
            PortraitWindow(
                photo = photo,
                focused = index == activeIndex,
                modifier = Modifier
                    .align(Alignment.Center)
                    .zIndex(depth)
                    .width(cardWidth)
                    .height(cardHeight)
                    .graphicsLayer {
                        translationX = sin(rad).toFloat() * radiusPx
                        translationY = (1f - depth) * 28.dp.toPx() - 8.dp.toPx()
                        rotationY = (-rel * 0.72f).coerceIn(-62f, 62f)
                        rotationZ = rel * 0.045f
                        scaleX = scale * boost
                        scaleY = scale * boost
                        alpha = fade
                        cameraDistance = 16.dp.toPx()
                        transformOrigin = TransformOrigin.Center
                    },
            )
        }
    }
}

@Composable
private fun PortraitWindow(
    photo: GalleryPhoto,
    focused: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .shadow(24.dp, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .background(Paper),
    ) {
        Row(
            Modifier.fillMaxWidth().height(34.dp).padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Dot(DotA)
            Spacer(Modifier.width(5.dp))
            Dot(DotB)
            Spacer(Modifier.width(5.dp))
            Dot(DotC)
            Text(
                text = clipName(photo.name, CARD_LIMIT),
                modifier = Modifier.padding(start = 8.dp),
                color = Ink.copy(alpha = 0.72f),
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Clip,
            )
        }
        Box(Modifier.weight(1f).fillMaxWidth().background(Color(0xFFD9D3CC))) {
            PhotoImage(photo, Modifier.fillMaxSize(), 720)
            if (focused) PlayBadge(Modifier.align(Alignment.Center))
        }
    }
}

@Composable
private fun PhotoImage(photo: GalleryPhoto, modifier: Modifier, edge: Int) {
    if (photo.imageUri != null) {
        val context = LocalContext.current
        var bitmap by remember(photo.imageUri, edge) { mutableStateOf<ImageBitmap?>(null) }
        LaunchedEffect(photo.imageUri, edge) {
            bitmap = withContext(Dispatchers.IO) { decodeThumbnail(context, Uri.parse(photo.imageUri), edge) }
        }
        bitmap?.let {
            Image(it, contentDescription = photo.name, modifier = modifier, contentScale = ContentScale.Crop)
        }
    } else if (photo.imageRes != 0) {
        Image(
            painter = painterResource(photo.imageRes),
            contentDescription = photo.name,
            modifier = modifier,
            contentScale = ContentScale.Crop,
        )
    }
}

@Composable
private fun PhotoGrid(
    photos: List<GalleryPhoto>,
    activeIndex: Int,
    light: Boolean,
    granted: Boolean,
    onAllow: () -> Unit,
    onPick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var mode by remember { mutableStateOf(ListMode.All) }
    var openFolder by remember { mutableStateOf<String?>(null) }
    val folders = remember(photos) {
        photos.groupBy { it.folder }.entries.sortedBy { it.key.lowercase() }
    }
    val shown = if (mode == ListMode.Folder && openFolder != null) {
        photos.filter { it.folder == openFolder }
    } else {
        photos
    }
    val ink = if (light) Color(0xFF2A2428) else Color.White
    val idle = if (light) Color(0x14000000) else Color(0x1AFFFFFF)
    val idleInk = if (light) Color(0xFF5C463F) else Color.White.copy(alpha = 0.8f)

    Column(modifier.background(if (light) Color(0xFFF7F4EF) else Color(0xFF0C0D11))) {
        Row(
            Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ModeChip("Semua", mode == ListMode.All, light) {
                mode = ListMode.All
                openFolder = null
            }
            Spacer(Modifier.width(6.dp))
            ModeChip("Folder", mode == ListMode.Folder, light) { mode = ListMode.Folder }
            if (mode == ListMode.Folder && openFolder != null) {
                Text(
                    text = clipName(openFolder ?: "", 16),
                    color = idleInk,
                    fontSize = 13.sp,
                    maxLines = 1,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .clickable { openFolder = null },
                )
            }
        }
        if (!granted) {
            Text(
                text = "Izinkan akses foto",
                color = ink,
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp).clickable(onClick = onAllow),
            )
        }
        if (mode == ListMode.Folder && openFolder == null) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(8.dp, 8.dp, 8.dp, 72.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(folders, key = { it.key }) { entry ->
                    Column(Modifier.clickable { openFolder = entry.key }) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .aspectRatio(4f / 3f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(idle),
                        ) {
                            PhotoImage(entry.value.first(), Modifier.fillMaxSize(), 320)
                        }
                        Text(
                            text = "${clipName(entry.key, 18)} · ${entry.value.size}",
                            color = ink,
                            fontSize = 13.sp,
                            maxLines = 1,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(6.dp, 6.dp, 6.dp, 72.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(shown, key = { it.id }) { photo ->
                    val index = photos.indexOfFirst { it.id == photo.id }
                    Box(
                        Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFD9D3CC))
                            .clickable { if (index >= 0) onPick(index) },
                    ) {
                        PhotoImage(photo, Modifier.fillMaxSize(), 240)
                        if (index == activeIndex) Box(Modifier.fillMaxSize().background(Color(0x33FF4D6A)))
                    }
                }
            }
        }
    }
}

@Composable
private fun ModeChip(label: String, active: Boolean, light: Boolean, onClick: () -> Unit) {
    val bg = if (active) {
        if (light) Color(0xFF2A2428) else Color.White
    } else if (light) {
        Color(0x14000000)
    } else {
        Color(0x1AFFFFFF)
    }
    val fg = if (active) {
        if (light) Color.White else Color(0xFF1C1E26)
    } else if (light) {
        Color(0xFF5C463F)
    } else {
        Color.White.copy(alpha = 0.8f)
    }
    Text(
        text = label,
        color = fg,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier
            .clip(CircleShape)
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

@Composable
private fun ChromeButton(
    container: Color,
    modifier: Modifier,
    onClick: () -> Unit,
    glyph: @Composable () -> Unit,
) {
    Box(
        modifier
            .size(44.dp)
            .shadow(8.dp, CircleShape)
            .clip(CircleShape)
            .background(container)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { glyph() }
}

@Composable
private fun Dot(color: Color) {
    Box(Modifier.size(9.dp).background(color, CircleShape))
}

@Composable
private fun PlayBadge(modifier: Modifier = Modifier) {
    Canvas(modifier.size(54.dp)) {
        drawCircle(color = Color.White.copy(alpha = 0.92f), style = Stroke(width = 3.dp.toPx()))
        val path = Path().apply {
            val inset = size.minDimension * 0.34f
            moveTo(size.width * 0.4f, inset)
            lineTo(size.width * 0.4f, size.height - inset)
            lineTo(size.width - inset * 0.7f, size.height / 2f)
            close()
        }
        drawPath(path, Color.White.copy(alpha = 0.95f))
    }
}

@Composable
private fun ThemeGlyph(showSun: Boolean, color: Color) {
    Canvas(Modifier.size(22.dp)) {
        if (showSun) {
            drawCircle(color, radius = size.minDimension * 0.18f)
            val r = size.minDimension / 2f
            repeat(8) { i ->
                val a = Math.toRadians(i * 45.0)
                drawLine(
                    color,
                    center + Offset((cos(a) * r * 0.42f).toFloat(), (sin(a) * r * 0.42f).toFloat()),
                    center + Offset((cos(a) * r * 0.84f).toFloat(), (sin(a) * r * 0.84f).toFloat()),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }
        } else {
            val moon = PathParser().parsePathString("M12 3a6 6 0 0 0 9 9 9 9 0 1 1-9-9Z").toPath()
            scale(size.minDimension / 24f, size.minDimension / 24f) {
                drawPath(moon, color)
            }
        }
    }
}

@Composable
private fun GalleryGlyph(color: Color) {
    Canvas(Modifier.size(22.dp)) {
        drawRoundRect(
            color = color,
            topLeft = Offset(size.width * 0.08f, size.height * 0.16f),
            size = androidx.compose.ui.geometry.Size(size.width * 0.84f, size.height * 0.68f),
            cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx()),
            style = Stroke(width = 1.8.dp.toPx()),
        )
        drawCircle(color, radius = 2.dp.toPx(), center = Offset(size.width * 0.32f, size.height * 0.38f))
        val hill = Path().apply {
            moveTo(size.width * 0.14f, size.height * 0.72f)
            lineTo(size.width * 0.38f, size.height * 0.52f)
            lineTo(size.width * 0.52f, size.height * 0.64f)
            lineTo(size.width * 0.66f, size.height * 0.54f)
            lineTo(size.width * 0.86f, size.height * 0.74f)
        }
        drawPath(hill, color, style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round))
    }
}

private fun decodeThumbnail(context: android.content.Context, uri: Uri, edge: Int): ImageBitmap? {
    return try {
        if (Build.VERSION.SDK_INT >= 29) {
            context.contentResolver.loadThumbnail(uri, Size(edge, edge), null).asImageBitmap()
        } else {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, BitmapFactory.Options().apply { inSampleSize = 4 })?.asImageBitmap()
            }
        }
    } catch (_: Exception) {
        null
    }
}

private fun clipName(text: String, max: Int): String {
    val clean = text.replace(Regex("\\.[A-Za-z0-9]+$"), "")
    if (clean.length <= max) return clean
    return clean.take(max).trimEnd() + "..."
}

private fun wrapDegrees(angle: Float): Float {
    var value = angle % 360f
    if (value > 180f) value -= 360f
    if (value < -180f) value += 360f
    return value
}
