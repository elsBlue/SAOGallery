package com.example.saogallery.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.saogallery.data.GalleryPhoto
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

private val Night = Color(0xFF14040A)
private val Paper = Color(0xFFF7F3EE)
private val Ink = Color(0xFF3A2C32)
private val XRed = Color(0xD9E10600)
private val DotA = Color(0xFFFF4D6A)
private val DotB = Color(0xFFFF8AA0)
private val DotC = Color(0xFFFFC0CC)

@Composable
fun GalleryScreen(
    state: GalleryUiState,
    onDragStart: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: (Float) -> Unit,
) {
    val active = state.photos[state.activeIndex]
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Night)
            .pointerInput(Unit) {
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
                    onDragEnd = {
                        onDragEnd(tracker.calculateVelocity().x)
                    },
                    onDragCancel = { onDragEnd(0f) },
                )
            },
    ) {
        CrimsonSky()
        BoxWithConstraints(Modifier.fillMaxSize()) {
            FolderWindow(
                title = "Case_files",
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(x = maxWidth * -0.34f, y = -maxHeight * 0.16f)
                    .graphicsLayer {
                        rotationZ = -8f
                        rotationY = 18f
                        alpha = 0.55f
                        cameraDistance = 18f * density
                    },
            )
            FolderWindow(
                title = "Archive",
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(x = maxWidth * 0.30f, y = -maxHeight * 0.22f)
                    .graphicsLayer {
                        rotationZ = 7f
                        rotationY = -16f
                        alpha = 0.42f
                        cameraDistance = 18f * density
                    },
            )

            val screenDensity = LocalDensity.current
            val cardWidth = maxWidth * 0.62f
            val cardHeight = cardWidth * 1.32f
            val radiusPx = with(screenDensity) { (maxWidth * 0.52f).toPx() }
            val step = 360f / state.photos.size

            state.photos.forEachIndexed { index, photo ->
                val rel = wrapDegrees(index * step - state.rotation)
                val rad = Math.toRadians(rel.toDouble())
                val depth = cos(rad).toFloat()
                val scale = 0.62f + ((depth + 1f) / 2f) * 0.5f
                val fade = (1.15f - abs(rel) / 130f).coerceIn(0.15f, 1f)
                val activeBoost = if (index == state.activeIndex) 1.04f else 1f
                PortraitWindow(
                    photo = photo,
                    focused = index == state.activeIndex,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .zIndex(depth)
                        .width(cardWidth)
                        .height(cardHeight)
                        .graphicsLayer {
                            translationX = sin(rad).toFloat() * radiusPx
                            translationY = (1f - depth) * 36.dp.toPx() - 12.dp.toPx()
                            rotationY = (-rel * 0.72f).coerceIn(-62f, 62f)
                            rotationZ = rel * 0.045f
                            scaleX = scale * activeBoost
                            scaleY = scale * activeBoost
                            alpha = fade
                            cameraDistance = 16.dp.toPx()
                            transformOrigin = TransformOrigin.Center
                        },
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(top = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "SAO GALLERY",
                color = Color(0xFFFFD7DE),
                fontSize = 13.sp,
                letterSpacing = 4.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "geser untuk memutar",
                color = Color.White.copy(alpha = 0.55f),
                fontSize = 12.sp,
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(start = 22.dp, end = 22.dp, bottom = 18.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = active.fileName,
                color = Color.White.copy(alpha = 0.7f),
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
            )
            Text(
                text = active.name,
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = if (active.crossedOut) "DICOROT" else "BELUM DITANDAI",
                color = if (active.crossedOut) Color(0xFFFF5A5A) else Color(0xFFFFC9D1),
                fontSize = 12.sp,
                letterSpacing = 2.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                state.photos.forEachIndexed { index, _ ->
                    val on = index == state.activeIndex
                    Box(
                        Modifier
                            .size(if (on) 8.dp else 6.dp)
                            .background(
                                if (on) Color(0xFFFF4D6A) else Color.White.copy(alpha = 0.35f),
                                CircleShape,
                            ),
                    )
                }
            }
        }
    }
}

@Composable
private fun CrimsonSky() {
    Canvas(Modifier.fillMaxSize()) {
        drawRect(
            brush = Brush.verticalGradient(
                listOf(Color(0xFF4A1020), Color(0xFF1A060C), Color(0xFF2C0812), Color(0xFF120308)),
            ),
        )
        drawCircle(
            brush = Brush.radialGradient(
                listOf(Color(0xAA7A2030), Color(0x007A2030)),
                center = Offset(size.width * 0.5f, size.height * 0.38f),
                radius = size.minDimension * 0.72f,
            ),
            radius = size.minDimension * 0.72f,
            center = Offset(size.width * 0.5f, size.height * 0.38f),
        )
        drawCircle(
            brush = Brush.radialGradient(
                listOf(Color(0x55FF2A3C), Color(0x00FF2A3C)),
                center = Offset(size.width * 0.5f, size.height * 0.48f),
                radius = size.minDimension * 0.34f,
            ),
            radius = size.minDimension * 0.34f,
            center = Offset(size.width * 0.5f, size.height * 0.48f),
        )
        drawRect(
            brush = Brush.radialGradient(
                listOf(Color.Transparent, Color(0xCC070104)),
                center = Offset(size.width / 2f, size.height / 2f),
                radius = size.maxDimension * 0.72f,
            ),
        )
    }
}

@Composable
private fun PortraitWindow(
    photo: GalleryPhoto,
    focused: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .shadow(28.dp, RoundedCornerShape(12.dp), ambientColor = Color(0x66FF2040), spotColor = Color.Black)
            .clip(RoundedCornerShape(12.dp))
            .background(Paper),
    ) {
        TitleBar(photo.fileName)
        Box(Modifier.weight(1f).fillMaxWidth()) {
            Image(
                painter = painterResource(photo.imageRes),
                contentDescription = photo.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            if (photo.crossedOut) {
                Box(Modifier.fillMaxSize().background(Color(0x33B00012)))
                Strike()
            }
            if (focused) {
                PlayBadge(Modifier.align(Alignment.Center))
            }
        }
    }
}

@Composable
private fun TitleBar(title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(34.dp)
            .background(Paper)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Dot(DotA)
        Spacer(Modifier.width(5.dp))
        Dot(DotB)
        Spacer(Modifier.width(5.dp))
        Dot(DotC)
        Text(
            text = title,
            modifier = Modifier.padding(start = 10.dp),
            color = Ink.copy(alpha = 0.72f),
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
        )
    }
}

@Composable
private fun Dot(color: Color) {
    Box(Modifier.size(9.dp).background(color, CircleShape))
}

@Composable
private fun Strike() {
    Canvas(Modifier.fillMaxSize().padding(10.dp)) {
        val stroke = size.minDimension * 0.085f
        drawLine(
            color = XRed,
            start = Offset(0f, 0f),
            end = Offset(size.width, size.height),
            strokeWidth = stroke,
            cap = StrokeCap.Square,
        )
        drawLine(
            color = XRed,
            start = Offset(size.width, 0f),
            end = Offset(0f, size.height),
            strokeWidth = stroke,
            cap = StrokeCap.Square,
        )
    }
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
private fun FolderWindow(title: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .width(168.dp)
            .height(132.dp)
            .shadow(12.dp, RoundedCornerShape(10.dp))
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFFF3EEE8)),
    ) {
        TitleBar(title)
        Row(
            Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            repeat(3) {
                Box(
                    Modifier
                        .size(width = 36.dp, height = 28.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            Brush.verticalGradient(listOf(Color(0xFFFF8AA0), Color(0xFFFF4D6A))),
                        ),
                )
            }
        }
    }
}

private fun wrapDegrees(angle: Float): Float {
    var value = angle % 360f
    if (value > 180f) value -= 360f
    if (value < -180f) value += 360f
    return value
}
