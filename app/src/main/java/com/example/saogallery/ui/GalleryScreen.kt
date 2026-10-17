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
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.offset
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
import androidx.activity.compose.BackHandler
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.example.saogallery.data.GalleryPhoto
import com.example.saogallery.data.VaultStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.TextStyle
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
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
    onRename: (Int, String) -> Unit,
    onTrash: (Int) -> Unit,
    onRestore: (String) -> Unit,
    trashExplained: () -> Boolean,
    onTrashExplained: () -> Unit,
) {
    val context = LocalContext.current
    var light by remember { mutableStateOf(false) }
    var galleryOpen by remember { mutableStateOf(false) }
    var trashOpen by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf(false) }
    var explainingTrash by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf("") }
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
    val openRename = {
        val current = state.photos.getOrNull(state.activeIndex)
        if (current != null) {
            draft = current.name
            editing = true
        }
    }
    val requestTrash = {
        if (state.photos.isNotEmpty()) {
            if (trashExplained()) onTrash(state.activeIndex) else explainingTrash = true
        }
    }

    BackHandler(enabled = editing || explainingTrash || trashOpen || galleryOpen) {
        when {
            editing -> editing = false
            explainingTrash -> explainingTrash = false
            trashOpen -> trashOpen = false
            else -> galleryOpen = false
        }
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
                    compact = galleryOpen || trashOpen,
                    showPencil = galleryOpen,
                    showActions = !galleryOpen && !trashOpen,
                    ink = ink,
                    onEdit = openRename,
                    onDelete = requestTrash,
                )
            }
            if (trashOpen) {
                TrashGrid(
                    photos = state.trash,
                    light = light,
                    onRestore = onRestore,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                )
            } else if (galleryOpen) {
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

        Row(
            Modifier
                .align(Alignment.BottomEnd)
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(bottom = 12.dp, end = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (state.trash.isNotEmpty()) {
                ChromeButton(
                    container = chrome,
                    icon = chromeInk,
                    modifier = Modifier,
                    onClick = {
                        trashOpen = !trashOpen
                        if (trashOpen) galleryOpen = false
                    },
                ) { TrashGlyph() }
            }
            ChromeButton(
                container = chrome,
                icon = chromeInk,
                modifier = Modifier,
                onClick = {
                    trashOpen = false
                    if (!galleryOpen && state.photos.isEmpty()) openPicker()
                    galleryOpen = !galleryOpen
                },
            ) { GalleryGlyph() }
        }

        if (editing) {
            RenameOverlay(
                light = light,
                value = draft,
                onChange = { draft = it },
                onDismiss = { editing = false },
                onSave = {
                    onRename(state.activeIndex, draft)
                    editing = false
                },
            )
        }
        if (explainingTrash) {
            NoticeOverlay(
                light = light,
                title = "MASUK TRASH",
                body = "Gambar ini masuk ke Trash. Trash menghapusnya otomatis setelah 30 hari. Sebelum itu, gambar bisa dikembalikan.",
                onDismiss = { explainingTrash = false },
                onConfirm = {
                    onTrashExplained()
                    explainingTrash = false
                    onTrash(state.activeIndex)
                },
            )
        }
    }
}

@Composable
private fun Stage(
    state: GalleryUiState,
    light: Boolean,
    compact: Boolean,
    showPencil: Boolean,
    showActions: Boolean,
    ink: Color,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
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
            text = "NYTE GALLERY",
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
            Column(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = if (compact) 8.dp else 78.dp, start = 56.dp, end = 56.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = clipName(active.name, LABEL_LIMIT),
                        color = ink,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (showPencil) {
                        Spacer(Modifier.width(8.dp))
                        Box(
                            Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(ink.copy(alpha = 0.1f))
                                .clickable(onClick = onEdit),
                            contentAlignment = Alignment.Center,
                        ) {
                            PencilIcon(ink)
                        }
                    }
                }
            }
        }
        if (showActions && active != null) {
            androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxSize()) {
                val cardWidth = maxWidth * 0.62f
                val cardHeight = cardWidth * 1.32f
                Row(
                    Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = maxHeight / 2 + cardHeight / 2 * 1.04f - 8.dp + 14.dp)
                        .zIndex(30f),
                    horizontalArrangement = Arrangement.spacedBy(22.dp),
                ) {
                    Box(
                        Modifier
                            .height(42.dp)
                            .clip(RoundedCornerShape(21.dp))
                            .background(DotA)
                            .clickable(onClick = onDelete)
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("delete", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                    ActionChip(ink, onEdit) { PencilIcon(ink) }
                }
            }
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
        val count = photos.size
        if (count == 0) return@BoxWithConstraints
        val center = rotation / (360f / count)
        val focus = Math.floorMod(center.roundToInt(), count)
        val offsets = when (count) {
            1 -> listOf(0)
            2 -> listOf(-1, 0)
            else -> listOf(-1, 0, 1)
        }
        offsets.forEach { offset ->
            val index = Math.floorMod(focus + offset, count)
            val photo = photos[index]
            var delta = index - center
            delta %= count
            if (delta > count / 2f) delta -= count
            if (delta < -count / 2f) delta += count
            val rel = delta * 58f
            val rad = Math.toRadians(rel.toDouble())
            val depth = cos(rad).toFloat()
            val scale = 0.62f + ((depth + 1f) / 2f) * 0.5f
            val fade = (1.15f - abs(rel) / 130f).coerceIn(0.15f, 1f)
            val boost = if (index == activeIndex) 1.04f else 1f
            PortraitWindow(
                photo = photo,
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
        }
    }
}

@Composable
private fun PhotoImage(photo: GalleryPhoto, modifier: Modifier) {
    if (photo.vaultFile != null) {
        val context = LocalContext.current
        var bitmap by remember(photo.vaultFile) { mutableStateOf<ImageBitmap?>(null) }
        LaunchedEffect(photo.vaultFile) {
            bitmap = withContext(Dispatchers.IO) {
                val bytes = VaultStore(context).read(photo) ?: return@withContext null
                decodeScaled(bytes, 1080)?.asImageBitmap()
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
private fun TrashGrid(
    photos: List<GalleryPhoto>,
    light: Boolean,
    onRestore: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val ink = if (light) Color(0xFF2A2428) else Color.White
    Column(modifier.background(if (light) Color(0xFFF7F4EF) else Color(0xFF0C0D11))) {
        Text(
            "Trash · terhapus otomatis setelah 30 hari",
            color = ink.copy(alpha = 0.7f),
            fontSize = 12.sp,
            modifier = Modifier.padding(start = 14.dp, top = 10.dp, bottom = 4.dp),
        )
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier.fillMaxSize().padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            itemsIndexed(photos, key = { _, photo -> photo.id }) { _, photo ->
                Box(
                    Modifier
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFD9D3CC)),
                ) {
                    PhotoImage(photo, Modifier.fillMaxSize())
                    Box(
                        Modifier
                            .align(Alignment.BottomEnd)
                            .padding(4.dp)
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.55f))
                            .clickable { onRestore(photo.id) },
                        contentAlignment = Alignment.Center,
                    ) {
                        RestoreGlyph()
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionChip(ink: Color, onClick: () -> Unit, glyph: @Composable () -> Unit) {
    Box(
        Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(ink.copy(alpha = 0.1f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.material3.LocalContentColor provides ink,
        ) {
            glyph()
        }
    }
}

@Composable
private fun NoticeOverlay(
    light: Boolean,
    title: String,
    body: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val panel = if (light) Color(0xFFF7F3EE) else Color(0xFF1C1E26)
    val ink = if (light) Color(0xFF2A2428) else Color.White
    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.56f)).clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .padding(horizontal = 28.dp)
                .fillMaxWidth()
                .shadow(24.dp, RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp))
                .background(panel)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                .padding(horizontal = 20.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(title, color = ink, fontSize = 13.sp, letterSpacing = 2.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))
            Text(body, color = ink.copy(alpha = 0.82f), fontSize = 15.sp, lineHeight = 21.sp)
            Spacer(Modifier.height(18.dp))
            Box(
                Modifier
                    .height(44.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(DotA)
                    .clickable(onClick = onConfirm)
                    .padding(horizontal = 18.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("Masukkan ke Trash", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
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
private fun SaveIcon(color: Color) {
    Canvas(Modifier.size(22.dp)) {
        drawRoundRect(
            color,
            topLeft = Offset(size.width * 0.16f, size.height * 0.14f),
            size = Size(size.width * 0.68f, size.height * 0.72f),
            cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx()),
            style = Stroke(width = 1.8.dp.toPx()),
        )
        drawRect(
            color,
            topLeft = Offset(size.width * 0.34f, size.height * 0.14f),
            size = Size(size.width * 0.32f, size.height * 0.22f),
        )
        drawLine(
            color,
            Offset(size.width * 0.3f, size.height * 0.68f),
            Offset(size.width * 0.7f, size.height * 0.68f),
            strokeWidth = 1.8.dp.toPx(),
            cap = StrokeCap.Round,
        )
    }
}

@Composable
private fun RenameOverlay(
    light: Boolean,
    value: String,
    onChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
) {
    val focus = remember { FocusRequester() }
    var field by remember(value) {
        mutableStateOf(TextFieldValue(value, TextRange(value.length)))
    }
    LaunchedEffect(value) {
        field = TextFieldValue(value, TextRange(value.length))
        focus.requestFocus()
    }
    val panel = if (light) Color(0xFFF7F3EE) else Color(0xFF1C1E26)
    val ink = if (light) Color(0xFF2A2428) else Color.White
    val fieldColor = if (light) Color(0xFFFBF9F6) else Color(0xFF12141A)
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.56f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .padding(horizontal = 28.dp)
                .fillMaxWidth()
                .shadow(24.dp, RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp))
                .background(panel)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                )
                .padding(horizontal = 20.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Dot(DotA)
                Spacer(Modifier.width(5.dp))
                Dot(DotB)
                Spacer(Modifier.width(5.dp))
                Dot(DotC)
                Text(
                    "UBAH NAMA",
                    modifier = Modifier.padding(start = 10.dp),
                    color = ink.copy(alpha = 0.72f),
                    fontSize = 12.sp,
                    letterSpacing = 2.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(Modifier.height(16.dp))
            BasicTextField(
                value = field,
                onValueChange = {
                    field = it
                    onChange(it.text)
                },
                textStyle = TextStyle(color = ink, fontSize = 18.sp, fontWeight = FontWeight.Medium),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(DotA),
                modifier = Modifier
                    .focusRequester(focus)
                    .fillMaxWidth()
                    .height(112.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(fieldColor)
                    .padding(14.dp),
            )
            Spacer(Modifier.height(16.dp))
            Box(
                Modifier
                    .size(52.dp)
                    .shadow(8.dp, CircleShape)
                    .clip(CircleShape)
                    .background(DotA)
                    .clickable(onClick = onSave),
                contentAlignment = Alignment.Center,
            ) {
                SaveIcon(Color.White)
            }
        }
    }
}

@Composable
private fun Dot(color: Color) {
    Box(Modifier.size(9.dp).background(color, CircleShape))
}

@Composable
private fun PencilIcon(color: Color) {
    val path = remember {
        PathParser().parsePathString(
            "M3,17.25V21h3.75L17.81,9.94l-3.75,-3.75L3,17.25zM20.71,7.04c0.39,-0.39 0.39,-1.02 0,-1.41l-2.34,-2.34c-0.39,-0.39 -1.02,-0.39 -1.41,0l-1.83,1.83 3.75,3.75 1.83,-1.83z",
        ).toPath()
    }
    Canvas(Modifier.size(16.dp)) {
        withTransform({
            scale(size.minDimension / 24f, size.minDimension / 24f, Offset.Zero)
        }) {
            drawPath(path, color)
        }
    }
}

@Composable
private fun SunMoonIcon(light: Boolean) {
    val color = androidx.compose.material3.LocalContentColor.current
    Canvas(Modifier.size(22.dp)) {
        val stroke = Stroke(width = 1.7.dp.toPx(), cap = StrokeCap.Round)
        if (!light) {
            drawCircle(color, radius = size.minDimension * 0.18f, style = stroke)
            val inner = size.minDimension * 0.30f
            val outer = size.minDimension * 0.44f
            repeat(8) { i ->
                val a = Math.toRadians(i * 45.0)
                drawLine(
                    color,
                    center + Offset((cos(a) * inner).toFloat(), (sin(a) * inner).toFloat()),
                    center + Offset((cos(a) * outer).toFloat(), (sin(a) * outer).toFloat()),
                    stroke.width,
                    StrokeCap.Round,
                )
            }
        } else {
            val crescent = Path().apply {
                fillType = PathFillType.EvenOdd
                addOval(androidx.compose.ui.geometry.Rect(Offset(size.width * 0.18f, size.height * 0.12f), Size(size.width * 0.68f, size.height * 0.68f)))
                addOval(androidx.compose.ui.geometry.Rect(Offset(size.width * 0.38f, size.height * 0.04f), Size(size.width * 0.60f, size.height * 0.60f)))
            }
            drawPath(crescent, color)
        }
    }
}

@Composable
private fun TrashGlyph() {
    val color = androidx.compose.material3.LocalContentColor.current
    Canvas(Modifier.size(18.dp)) {
        val stroke = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round)
        drawLine(color, Offset(size.width * 0.16f, size.height * 0.24f), Offset(size.width * 0.84f, size.height * 0.24f), stroke.width, StrokeCap.Round)
        drawLine(color, Offset(size.width * 0.40f, size.height * 0.24f), Offset(size.width * 0.44f, size.height * 0.12f), stroke.width, StrokeCap.Round)
        drawLine(color, Offset(size.width * 0.60f, size.height * 0.24f), Offset(size.width * 0.56f, size.height * 0.12f), stroke.width, StrokeCap.Round)
        drawLine(color, Offset(size.width * 0.44f, size.height * 0.12f), Offset(size.width * 0.56f, size.height * 0.12f), stroke.width, StrokeCap.Round)
        drawRoundRect(
            color,
            topLeft = Offset(size.width * 0.24f, size.height * 0.30f),
            size = Size(size.width * 0.52f, size.height * 0.56f),
            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()),
            style = stroke,
        )
        drawLine(color, Offset(size.width * 0.40f, size.height * 0.40f), Offset(size.width * 0.40f, size.height * 0.74f), stroke.width, StrokeCap.Round)
        drawLine(color, Offset(size.width * 0.60f, size.height * 0.40f), Offset(size.width * 0.60f, size.height * 0.74f), stroke.width, StrokeCap.Round)
    }
}

@Composable
private fun RestoreGlyph() {
    Canvas(Modifier.size(16.dp)) {
        val color = Color.White
        val stroke = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round)
        drawLine(color, Offset(size.width * 0.22f, size.height * 0.72f), Offset(size.width * 0.78f, size.height * 0.72f), stroke.width, StrokeCap.Round)
        drawLine(color, Offset(size.width * 0.50f, size.height * 0.22f), Offset(size.width * 0.50f, size.height * 0.62f), stroke.width, StrokeCap.Round)
        drawLine(color, Offset(size.width * 0.34f, size.height * 0.38f), Offset(size.width * 0.50f, size.height * 0.22f), stroke.width, StrokeCap.Round)
        drawLine(color, Offset(size.width * 0.66f, size.height * 0.38f), Offset(size.width * 0.50f, size.height * 0.22f), stroke.width, StrokeCap.Round)
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

private fun displayName(context: android.content.Context, uri: Uri): String {
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0) return cursor.getString(index) ?: "Foto"
        }
    }
    return "Foto"
}
