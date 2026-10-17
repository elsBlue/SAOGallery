package com.example.saogallery.ui

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.saogallery.data.LockStore
import kotlin.math.min

private val LockPink = Color(0xFFFF4D6A)
private val LockGreen = Color(0xFF2F9B68)

fun fingerprintAvailable(context: Context): Boolean {
    val result = BiometricManager.from(context).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)
    return result == BiometricManager.BIOMETRIC_SUCCESS
}

fun promptFingerprint(context: Context, title: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
    val activity = context as? FragmentActivity ?: return onError("Fingerprint is not available")
    val prompt = BiometricPrompt(
        activity,
        ContextCompat.getMainExecutor(context),
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = onSuccess()
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                if (errorCode != BiometricPrompt.ERROR_USER_CANCELED && errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                    onError(errString.toString())
                }
            }
        },
    )
    prompt.authenticate(
        BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setNegativeButtonText("Cancel")
            .build(),
    )
}

@Composable
fun LockConfirm(light: Boolean, body: String, confirm: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
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
                .clip(RoundedCornerShape(24.dp))
                .background(panel)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                .padding(horizontal = 20.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("LOCK APP", color = ink, fontSize = 13.sp, letterSpacing = 2.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))
            Text(body, color = ink.copy(alpha = 0.82f), fontSize = 15.sp, lineHeight = 21.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(18.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                LockButton("Close", ink.copy(alpha = 0.12f), ink, Modifier.weight(1f), onDismiss)
                LockButton(confirm, LockPink, Color.White, Modifier.weight(1f), onConfirm)
            }
        }
    }
}

@Composable
fun LockSetupPage(
    light: Boolean,
    onClose: () -> Unit,
    onSave: (email: String, password: String?, pin: String?, pattern: String?, finger: Boolean) -> Unit,
) {
    val context = LocalContext.current
    val ink = if (light) Color(0xFF2A2428) else Color.White
    val panel = if (light) Color(0xFFF7F4EF) else Color(0xFF12141A)
    val field = if (light) Color.White else Color(0xFF1C1E26)
    val fingerOk = remember { fingerprintAvailable(context) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf<String?>(null) }
    var pin by remember { mutableStateOf<String?>(null) }
    var pattern by remember { mutableStateOf<String?>(null) }
    var finger by remember { mutableStateOf(false) }
    var open by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf("") }
    val readyCount = listOf(password, pin, pattern).count { it != null } + if (finger) 1 else 0
    val emailOk = email.trim().matches(Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$"))
    Column(
        Modifier.fillMaxSize().background(panel).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 28.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("LOCK", color = ink, fontSize = 13.sp, letterSpacing = 2.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text("Close", color = ink.copy(alpha = 0.7f), modifier = Modifier.clickable(onClick = onClose).padding(8.dp))
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "Set how Nyte Gallery locks. Choose at least two ways to get back in: password, PIN, pattern, or fingerprint. Add an email so a forgotten method can be recovered with a code.",
            color = ink.copy(alpha = 0.8f),
            fontSize = 15.sp,
            lineHeight = 21.sp,
        )
        Spacer(Modifier.height(16.dp))
        Text("Recovery email", color = ink.copy(alpha = 0.6f), fontSize = 12.sp)
        LockField(email, field, ink, KeyboardType.Email, false) { email = it }
        Spacer(Modifier.height(16.dp))
        MethodRow("Password", password != null, ink, field) { open = if (open == LockStore.PASSWORD) null else LockStore.PASSWORD }
        if (open == LockStore.PASSWORD) SecretPair(ink, field, KeyboardType.Password, "At least 6 characters") { first, second ->
            error = when {
                first.length < 6 -> "Password needs at least 6 characters"
                first != second -> "Passwords do not match"
                else -> {
                    password = first
                    open = null
                    ""
                }
            }
        }
        MethodRow("PIN", pin != null, ink, field) { open = if (open == LockStore.PIN) null else LockStore.PIN }
        if (open == LockStore.PIN) SecretPair(ink, field, KeyboardType.NumberPassword, "4 to 6 digits") { first, second ->
            error = when {
                !first.matches(Regex("\\d{4,6}")) -> "PIN must be 4 to 6 digits"
                first != second -> "PINs do not match"
                else -> {
                    pin = first
                    open = null
                    ""
                }
            }
        }
        MethodRow("Pattern", pattern != null, ink, field) { open = if (open == LockStore.PATTERN) null else LockStore.PATTERN }
        if (open == LockStore.PATTERN) {
            Text("Draw a pattern, then draw it again.", color = ink.copy(alpha = 0.65f), fontSize = 13.sp)
            PatternConfirm(ink) { value ->
                pattern = value
                open = null
                error = ""
            }
        }
        if (fingerOk) {
            MethodRow("Fingerprint", finger, ink, field) {
                promptFingerprint(context, "Confirm fingerprint", { finger = true }, { error = it })
            }
        } else {
            Text("Fingerprint is not available on this phone.", color = ink.copy(alpha = 0.45f), fontSize = 13.sp, modifier = Modifier.padding(vertical = 8.dp))
        }
        if (error.isNotEmpty()) {
            Text(error, color = LockPink, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
        }
        Text("$readyCount of 2 ways set", color = ink.copy(alpha = 0.55f), fontSize = 13.sp, modifier = Modifier.padding(top = 12.dp))
        Spacer(Modifier.height(16.dp))
        val canSave = emailOk && readyCount >= 2
        LockButton("Lock app", if (canSave) LockPink else ink.copy(alpha = 0.15f), if (canSave) Color.White else ink.copy(alpha = 0.4f), Modifier.fillMaxWidth()) {
            if (canSave) onSave(email.trim(), password, pin, pattern, finger)
            else error = if (!emailOk) "Enter a valid email" else "Set at least two unlock ways"
        }
    }
}

@Composable
fun LockGate(
    light: Boolean,
    store: LockStore,
    onUnlocked: () -> Unit,
    onSendCode: (onResult: (Boolean) -> Unit) -> Unit,
) {
    val context = LocalContext.current
    val ink = if (light) Color(0xFF2A2428) else Color.White
    val panel = if (light) Color(0xFFF7F4EF) else Color(0xFF12141A)
    val field = if (light) Color.White else Color(0xFF1C1E26)
    val methods = store.methods().toList()
    var method by remember { mutableStateOf(methods.firstOrNull { it != LockStore.FINGER } ?: methods.first()) }
    var value by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var emailMode by remember { mutableStateOf(false) }
    var sending by remember { mutableStateOf(false) }
    Column(
        Modifier.fillMaxSize().background(panel).padding(horizontal = 24.dp, vertical = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(28.dp))
        Text("NYTE GALLERY", color = ink, fontSize = 13.sp, letterSpacing = 4.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(18.dp))
        Text(
            if (emailMode) "Enter the code sent to ${store.email()}" else "This gallery is locked. Use one of the ways you set.",
            color = ink.copy(alpha = 0.75f),
            fontSize = 15.sp,
            lineHeight = 21.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(18.dp))
        if (!emailMode && methods.size > 1) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                methods.forEach { item ->
                    val on = item == method
                    Text(
                        item.replaceFirstChar { it.uppercase() },
                        color = if (on) Color.White else ink,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (on) LockPink else field)
                            .clickable { method = item; value = ""; error = "" }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }
        when {
            emailMode -> {
                LockField(value, field, ink, KeyboardType.Number, false) { if (it.length <= 6) value = it.filter { ch -> ch.isDigit() } }
                Spacer(Modifier.height(14.dp))
                LockButton("Unlock", LockPink, Color.White, Modifier.fillMaxWidth()) {
                    if (store.matchesCode(value)) onUnlocked() else error = "That code is wrong or expired"
                }
                Text(
                    if (sending) "Sending..." else "Send a new code",
                    color = ink.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 14.dp).clickable(enabled = !sending) {
                        sending = true
                        onSendCode { ok ->
                            sending = false
                            error = if (ok) "Code sent. The first time, confirm the email from Nyte Gallery, then send again." else "Couldn't send the code"
                        }
                    },
                )
                Text("Use another way", color = ink, modifier = Modifier.padding(top = 10.dp).clickable { emailMode = false; error = "" })
            }
            method == LockStore.FINGER -> {
                LockButton("Use fingerprint", LockPink, Color.White, Modifier.fillMaxWidth()) {
                    promptFingerprint(context, "Unlock Nyte Gallery", onUnlocked) { error = it }
                }
            }
            method == LockStore.PATTERN -> {
                PatternPad(ink) { drawn ->
                    if (store.matches(LockStore.PATTERN, drawn)) onUnlocked() else error = "Pattern does not match"
                }
            }
            else -> {
                val numeric = method == LockStore.PIN
                LockField(value, field, ink, if (numeric) KeyboardType.NumberPassword else KeyboardType.Password, true) {
                    value = if (numeric) it.filter { ch -> ch.isDigit() }.take(6) else it
                }
                Spacer(Modifier.height(14.dp))
                LockButton("Unlock", LockPink, Color.White, Modifier.fillMaxWidth()) {
                    if (store.matches(method, value)) onUnlocked() else error = "That ${method.replaceFirstChar { it.uppercase() }} is not right"
                }
            }
        }
        if (error.isNotEmpty()) Text(error, color = LockPink, fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 12.dp))
        if (!emailMode) {
            Spacer(Modifier.height(18.dp))
            Text(
                "Use email code",
                color = ink,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable {
                    emailMode = true
                    value = ""
                    error = ""
                    sending = true
                    onSendCode { ok ->
                        sending = false
                        error = if (ok) "Code sent to ${store.email()}" else "Couldn't send the code. Try another way."
                    }
                },
            )
        }
    }
}

@Composable
private fun MethodRow(label: String, set: Boolean, ink: Color, field: Color, onClick: () -> Unit) {
    Row(
        Modifier
            .padding(vertical = 6.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(field)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = ink, fontSize = 16.sp, modifier = Modifier.weight(1f))
        Text(if (set) "Set" else "Set up", color = if (set) LockGreen else ink.copy(alpha = 0.45f), fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SecretPair(
    ink: Color,
    field: Color,
    keyboard: KeyboardType,
    hint: String,
    onSubmit: (String, String) -> Unit,
) {
    var first by remember { mutableStateOf("") }
    var second by remember { mutableStateOf("") }
    Text(hint, color = ink.copy(alpha = 0.55f), fontSize = 12.sp)
    LockField(first, field, ink, keyboard, true) { first = it }
    LockField(second, field, ink, keyboard, true) { second = it }
    LockButton("Save this way", LockGreen, Color.White, Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 8.dp)) {
        onSubmit(first.trim(), second.trim())
    }
}

@Composable
private fun PatternConfirm(ink: Color, onConfirmed: (String) -> Unit) {
    var first by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf("Draw at least 4 dots") }
    PatternPad(ink) { drawn ->
        val saved = first
        if (saved == null) {
            if (drawn.split("-").size < 4) message = "Use at least 4 dots"
            else {
                first = drawn
                message = "Draw the same pattern again"
            }
        } else if (saved == drawn) onConfirmed(drawn) else message = "Patterns do not match. Draw it again."
    }
    Text(message, color = ink.copy(alpha = 0.65f), fontSize = 13.sp, modifier = Modifier.padding(bottom = 8.dp))
}

@Composable
private fun PatternPad(ink: Color, onComplete: (String) -> Unit) {
    var selected by remember { mutableStateOf(listOf<Int>()) }
    val complete = rememberUpdatedState(onComplete)
    Canvas(
        Modifier
            .padding(vertical = 12.dp)
            .size(240.dp)
            .pointerInput(Unit) {
                val current = mutableListOf<Int>()
                detectDragGestures(
                    onDragStart = { start ->
                        current.clear()
                        hit(start, size.width.toFloat())?.let { current += it }
                        selected = current.toList()
                    },
                    onDrag = { change, _ ->
                        val next = hit(change.position, size.width.toFloat())
                        if (next != null && next !in current) {
                            current += next
                            selected = current.toList()
                        }
                    },
                    onDragEnd = {
                        if (current.isNotEmpty()) complete.value(current.joinToString("-"))
                        current.clear()
                        selected = emptyList()
                    },
                    onDragCancel = {
                        current.clear()
                        selected = emptyList()
                    },
                )
            },
    ) {
        val cell = size.minDimension / 3f
        fun centerOf(index: Int) = Offset(cell * (index % 3 + 0.5f), cell * (index / 3 + 0.5f))
        for (i in 0 until selected.lastIndex) {
            drawLine(LockPink, centerOf(selected[i]), centerOf(selected[i + 1]), 4.dp.toPx(), StrokeCap.Round)
        }
        repeat(9) { index ->
            val on = index in selected
            drawCircle(if (on) LockPink else ink.copy(alpha = 0.35f), radius = if (on) 10.dp.toPx() else 7.dp.toPx(), center = centerOf(index))
        }
    }
}

private fun hit(position: Offset, width: Float): Int? {
    val cell = width / 3f
    val reach = cell * 0.34f
    for (index in 0..8) {
        val center = Offset(cell * (index % 3 + 0.5f), cell * (index / 3 + 0.5f))
        val dx = position.x - center.x
        val dy = position.y - center.y
        if (dx * dx + dy * dy <= reach * reach) return index
    }
    return null
}

@Composable
private fun LockField(
    value: String,
    field: Color,
    ink: Color,
    keyboard: KeyboardType,
    hidden: Boolean,
    onChange: (String) -> Unit,
) {
    BasicTextField(
        value = value,
        onValueChange = onChange,
        singleLine = true,
        textStyle = TextStyle(color = ink, fontSize = 16.sp),
        cursorBrush = SolidColor(LockPink),
        visualTransformation = if (hidden) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
        modifier = Modifier
            .padding(top = 8.dp)
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(field)
            .padding(horizontal = 14.dp, vertical = 14.dp),
    )
}

@Composable
private fun LockButton(label: String, background: Color, ink: Color, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier.height(44.dp).clip(RoundedCornerShape(22.dp)).background(background).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = ink, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }
}

@Composable
fun LockGlyph() {
    val color = androidx.compose.material3.LocalContentColor.current
    Canvas(Modifier.size(20.dp)) {
        val stroke = Stroke(width = 1.7.dp.toPx(), cap = StrokeCap.Round)
        val w = size.width
        val h = size.height
        drawRoundRect(
            color,
            topLeft = Offset(w * 0.18f, h * 0.44f),
            size = androidx.compose.ui.geometry.Size(w * 0.64f, h * 0.42f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx(), 3.dp.toPx()),
            style = stroke,
        )
        val path = Path().apply {
            arcTo(
                androidx.compose.ui.geometry.Rect(Offset(w * 0.30f, h * 0.16f), androidx.compose.ui.geometry.Size(w * 0.40f, h * 0.40f)),
                200f,
                140f,
                false,
            )
        }
        drawPath(path, color, style = stroke)
        drawCircle(color, radius = min(w, h) * 0.045f, center = Offset(w * 0.5f, h * 0.64f))
    }
}
