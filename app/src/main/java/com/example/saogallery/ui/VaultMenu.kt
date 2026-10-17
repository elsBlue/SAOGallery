package com.example.saogallery.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val MenuPink = Color(0xFFFF4D6A)

@Composable
fun MenuGlyph() {
    val color = androidx.compose.material3.LocalContentColor.current
    Canvas(Modifier.size(18.dp)) {
        val radius = 1.7.dp.toPx()
        val x = size.width / 2f
        drawCircle(color, radius, Offset(x, size.height * 0.22f))
        drawCircle(color, radius, Offset(x, size.height * 0.50f))
        drawCircle(color, radius, Offset(x, size.height * 0.78f))
    }
}

@Composable
fun MenuSidebar(
    light: Boolean,
    onClose: () -> Unit,
    onBackup: () -> Unit,
    onRestore: () -> Unit,
) {
    val panel = if (light) Color(0xFFF7F4EF) else Color(0xFF171920)
    val ink = if (light) Color(0xFF2A2428) else Color.White
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.46f)).clickable(onClick = onClose)) {
        Column(
            Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .width(292.dp)
                .background(panel)
                .windowInsetsPadding(WindowInsets.systemBars)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                .padding(horizontal = 22.dp, vertical = 22.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Menu", color = ink, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Text("Close", color = ink.copy(alpha = 0.7f), modifier = Modifier.clickable(onClick = onClose).padding(8.dp))
            }
            Spacer(Modifier.height(22.dp))
            MenuRow("Backup", "Save an encrypted copy you can keep", ink, onBackup)
            Spacer(Modifier.height(8.dp))
            MenuRow("Restore backup", "Bring photos back from a backup file", ink, onRestore)
            Spacer(Modifier.weight(1f))
            Text(
                "The phone key cannot move to a new device. A backup uses a password only you know.",
                color = ink.copy(alpha = 0.55f),
                fontSize = 12.sp,
                lineHeight = 17.sp,
            )
        }
    }
}

@Composable
private fun MenuRow(title: String, body: String, ink: Color, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 12.dp),
    ) {
        Text(title, color = ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        Text(body, color = ink.copy(alpha = 0.62f), fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
fun BackupPage(
    light: Boolean,
    busy: Boolean,
    error: String?,
    onClose: () -> Unit,
    onCreate: (String) -> Unit,
) {
    val ink = if (light) Color(0xFF2A2428) else Color.White
    val panel = if (light) Color(0xFFF7F4EF) else Color(0xFF12141A)
    val field = if (light) Color.White else Color(0xFF1C1E26)
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    val mismatch = confirm.isNotEmpty() && password != confirm
    val ready = password.length >= 8 && password == confirm && !busy
    Column(Modifier.fillMaxSize().background(panel).windowInsetsPadding(WindowInsets.systemBars).padding(horizontal = 22.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Backup", color = ink, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text("Close", color = ink.copy(alpha = 0.7f), modifier = Modifier.clickable(enabled = !busy, onClick = onClose).padding(8.dp))
        }
        Text(
            "Photos are unlocked on this phone, then sealed again with this password. Nyte cannot open the file without it. Save it somewhere else, such as Drive, a computer, or a memory card. Automatic phone backup stays off.",
            color = ink.copy(alpha = 0.72f),
            fontSize = 14.sp,
            lineHeight = 20.sp,
        )
        Spacer(Modifier.height(18.dp))
        Text("New backup password", color = ink.copy(alpha = 0.7f), fontSize = 13.sp)
        SecretField(password, field, ink, "At least 8 characters") { password = it }
        Spacer(Modifier.height(12.dp))
        Text("Confirm password", color = ink.copy(alpha = 0.7f), fontSize = 13.sp)
        SecretField(confirm, field, ink, "Repeat the password") { confirm = it }
        if (mismatch) {
            Text("Those passwords do not match", color = MenuPink, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
        }
        if (!error.isNullOrBlank()) {
            Text(error, color = MenuPink, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
        }
        Spacer(Modifier.weight(1f))
        Box(
            Modifier
                .padding(bottom = 18.dp)
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(if (ready) MenuPink else ink.copy(alpha = 0.12f))
                .clickable(enabled = ready) { onCreate(password) },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                if (busy) "Encrypting…" else "Create backup",
                color = if (ready || busy) Color.White else ink.copy(alpha = 0.4f),
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
fun RestoreBackupPage(
    light: Boolean,
    busy: Boolean,
    error: String?,
    onClose: () -> Unit,
    onPick: (String) -> Unit,
) {
    val ink = if (light) Color(0xFF2A2428) else Color.White
    val panel = if (light) Color(0xFFF7F4EF) else Color(0xFF12141A)
    val field = if (light) Color.White else Color(0xFF1C1E26)
    var password by remember { mutableStateOf("") }
    val ready = password.isNotEmpty() && !busy
    Column(Modifier.fillMaxSize().background(panel).windowInsetsPadding(WindowInsets.systemBars).padding(horizontal = 22.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Restore backup", color = ink, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text("Close", color = ink.copy(alpha = 0.7f), modifier = Modifier.clickable(enabled = !busy, onClick = onClose).padding(8.dp))
        }
        Text(
            "Choose the backup file and the password you set when it was created. Photos already in this gallery are left as they are.",
            color = ink.copy(alpha = 0.72f),
            fontSize = 14.sp,
            lineHeight = 20.sp,
        )
        Spacer(Modifier.height(18.dp))
        Text("Backup password", color = ink.copy(alpha = 0.7f), fontSize = 13.sp)
        SecretField(password, field, ink, "Password for this file") { password = it }
        if (!error.isNullOrBlank()) {
            Text(error, color = MenuPink, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
        }
        Spacer(Modifier.weight(1f))
        Box(
            Modifier
                .padding(bottom = 18.dp)
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(if (ready) MenuPink else ink.copy(alpha = 0.12f))
                .clickable(enabled = ready) { onPick(password) },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                if (busy) "Restoring…" else "Choose backup file",
                color = if (ready || busy) Color.White else ink.copy(alpha = 0.4f),
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun SecretField(value: String, field: Color, ink: Color, placeholder: String, onChange: (String) -> Unit) {
    var shown by remember { mutableStateOf(false) }
    Box(
        Modifier
            .padding(top = 6.dp)
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(field)
            .padding(start = 16.dp, end = 8.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (value.isEmpty()) Text(placeholder, color = ink.copy(alpha = 0.32f), fontSize = 15.sp)
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            textStyle = TextStyle(color = ink, fontSize = 16.sp),
            cursorBrush = SolidColor(MenuPink),
            visualTransformation = if (shown) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth().padding(end = 36.dp),
        )
        Box(
            Modifier.align(Alignment.CenterEnd).size(36.dp).clip(CircleShape).clickable { shown = !shown },
            contentAlignment = Alignment.Center,
        ) {
            SecretEye(ink.copy(alpha = 0.72f), concealed = !shown)
        }
    }
}

@Composable
private fun SecretEye(color: Color, concealed: Boolean) {
    Canvas(Modifier.size(20.dp)) {
        val stroke = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round)
        val eye = Path().apply {
            moveTo(size.width * 0.08f, size.height * 0.5f)
            quadraticTo(size.width * 0.5f, size.height * 0.12f, size.width * 0.92f, size.height * 0.5f)
            quadraticTo(size.width * 0.5f, size.height * 0.88f, size.width * 0.08f, size.height * 0.5f)
            close()
        }
        drawPath(eye, color, style = stroke)
        drawCircle(color, radius = size.width * 0.11f, center = center)
        if (concealed) {
            drawLine(color, Offset(size.width * 0.2f, size.height * 0.78f), Offset(size.width * 0.8f, size.height * 0.22f), stroke.width, StrokeCap.Round)
        }
    }
}
