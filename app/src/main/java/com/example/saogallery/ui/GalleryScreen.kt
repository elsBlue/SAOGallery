package com.example.saogallery.ui

import android.app.Activity
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
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

@Composable
fun GalleryScreen(
    state: GalleryUiState,
    onDragStart: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: (Float) -> Unit,
    onShowIndex: (Int) -> Unit,
    onAdd: (List<Pair<String, String>>) -> Unit,
) {
    val context = LocalContext.current
    var light by remember { mutableStateOf(false) }
    var galleryOpen by remember { mutableStateOf(false) }
    val view = LocalView.current
    androidx.compose.runtime.SideEffect {
        val window = (view.context as Activity).window
        val controller = WindowCompat.getInsetsController(window, view)
        controller.isAppearanceLightStatusBars = light
        controller.isAppearanceLightNavigationBars = light
    }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(30),
    ) { uris ->
        val picked = uris.map { uri -> displayName(context, uri) to uri.toString() }
        if (picked.isNotEmpty()) {
            galleryOpen = true
            onAdd(picked)
        }
    }
    val openPicker = {
        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
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
                    .weight(1f)
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
                Stage(
                    state = state,
                    light = light,
                    compact = galleryOpen,
                    ink = ink,
                )
            }
            if (galleryOpen) {
                PhotoGrid(
                    photos = state.photos,
                    activeIndex = state.activeIndex,
                    light = light,
                    onAdd = openPicker,
                    onPick = onShowIndex,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                )
            }
        }

        ChromeButton(
            container = chrome,
            icon = chromeInk,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(top = 12.dp, end = 12.dp),
            onClick = { light = !light },
        ) { SunMoonIcon(light = light) }

        ChromeButton(
            container = chrome,
            icon = chromeInk,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(bottom = 12.dp, end = 12.dp),
            onClick = {
                if (!galleryOpen && state.photos.isEmpty()) openPicker()
                galleryOpen = !galleryOpen
            },
        ) { GalleryGlyph() }
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
        BoxWithConstraintsFill(
            compact = compact,
            photos = state.photos,
            rotation = state.rotation,
            activeIndex = state.activeIndex,
        )
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
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = if (compact) 8.dp else 88.dp, start = 72.dp, end = 72.dp),
            )
        }
    }
}

@Composable
private fun BoxWithConstraintsFill(
    compact: Boolean,
    photos: List<GalleryPhoto>,
    rotation: Float,
    activeIndex: Int,
) {
    androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val cardWidth = maxWidth * if (compact) 0.42f else 0.62f
        val cardHeight = cardWidth * 1.32f
        val radiusPx = with(density) { (maxWidth * if (compact) 0.34f else 0.52f).toPx() }
        val step = if (photos.isEmpty()) 360f else 360f / photos.size
        photos.forEachIndexed { index, photo ->
            val rel = wrapDegrees(index * step - rotation)
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
            PhotoImage(photo, Modifier.fillMaxSize())
            if (focused) {
                Box(
                    Modifier
                        .align(Alignment.Center)
                        .size(54.dp)
                        .graphicsLayer { }
                        .clip(CircleShape)
                        .background(Color.Transparent),
                ) {
                    PlayBadge()
                }
            }
        }
    }
}

@Composable
private fun PhotoImage(photo: GalleryPhoto, modifier: Modifier) {
    if (photo.imageUri != null) {
        val context = LocalContext.current
        var bitmap by remember(photo.imageUri) { mutableStateOf<ImageBitmap?>(null) }
        LaunchedEffect(photo.imageUri) {
            bitmap = withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(Uri.parse(photo.imageUri))?.use { stream ->
                    BitmapFactory.decodeStream(stream)?.asImageBitmap()
                }
            }
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
    onAdd: () -> Unit,
    onPick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.background(if (light) Color(0xFFF7F4EF) else Color(0xFF0C0D11)),
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier.fillMaxSize().padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            item {
                Box(
                    Modifier
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (light) Color(0x14000000) else Color(0x14FFFFFF))
                        .clickable(onClick = onAdd),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("+", color = if (light) Color(0xFF5C463F) else Color.White, fontSize = 28.sp)
                }
            }
            itemsIndexed(photos, key = { _, photo -> photo.id }) { index, photo ->
                Box(
                    Modifier
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFD9D3CC))
                        .clickable { onPick(index) },
                ) {
                    PhotoImage(photo, Modifier.fillMaxSize())
                    if (index == activeIndex) {
                        Box(Modifier.fillMaxSize().background(Color(0x33FF4D6A)))
                    }
                }
            }
        }
    }
}

@Composable
private fun ChromeButton(
    container: Color,
    icon: Color,
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
    ) {
        Box(Modifier.graphicsLayer { }) {
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.material3.LocalContentColor provides icon,
            ) {
                glyph()
            }
        }
    }
}

@Composable
private fun Dot(color: Color) {
    Box(Modifier.size(9.dp).background(color, CircleShape))
}

@Composable
private fun PlayBadge() {
    androidx.compose.foundation.Canvas(Modifier.size(54.dp)) {
        drawCircle(color = Color.White.copy(alpha = 0.92f), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx()))
        val path = androidx.compose.ui.graphics.Path().apply {
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
private fun SunMoonIcon(light: Boolean) {
    val color = androidx.compose.material3.LocalContentColor.current
    androidx.compose.foundation.Canvas(Modifier.size(22.dp)) {
        if (!light) {
            drawCircle(color, radius = size.minDimension * 0.22f, style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx()))
            val r = size.minDimension / 2f
            repeat(8) { i ->
                val a = Math.toRadians(i * 45.0)
                drawLine(
                    color,
                    start = center + androidx.compose.ui.geometry.Offset((cos(a) * r * 0.48f).toFloat(), (sin(a) * r * 0.48f).toFloat()),
                    end = center + androidx.compose.ui.geometry.Offset((cos(a) * r * 0.78f).toFloat(), (sin(a) * r * 0.78f).toFloat()),
                    strokeWidth = 2.dp.toPx(),
                )
            }
        } else {
            drawCircle(
                color,
                radius = size.minDimension * 0.32f,
                center = androidx.compose.ui.geometry.Offset(size.width * 0.42f, size.height * 0.5f),
            )
            drawCircle(
                Color.Transparent,
                radius = size.minDimension * 0.26f,
                center = androidx.compose.ui.geometry.Offset(size.width * 0.62f, size.height * 0.38f),
            )
        }
    }
}

@Composable
private fun GalleryGlyph() {
    val color = androidx.compose.material3.LocalContentColor.current
    androidx.compose.foundation.Canvas(Modifier.size(22.dp)) {
        drawRoundRect(
            color = color,
            topLeft = androidx.compose.ui.geometry.Offset(size.width * 0.08f, size.height * 0.16f),
            size = androidx.compose.ui.geometry.Size(size.width * 0.84f, size.height * 0.68f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx()),
        )
        drawCircle(color, radius = 2.2.dp.toPx(), center = androidx.compose.ui.geometry.Offset(size.width * 0.32f, size.height * 0.38f))
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

private fun displayName(context: android.content.Context, uri: Uri): String {
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0) return cursor.getString(index) ?: "Foto"
        }
    }
    return "Foto"
}
