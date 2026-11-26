package com.example.englishapp.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val Blue = Color(0xFF14B1FF)
val Green = Color(0xFF16A870)
val Red = Color(0xFFFF6B6B)
val Amber = Color(0xFFFFB020)
val Pink = Color(0xFFFF7BAC)
val Panel = Color(0xFF1A1D20)
val Outline = Color(0xFF2B2F33)
val CardBorder = Color(0xFF7FA3BF)
val NavyBg = Color(0xFF071622)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Blue,
            onPrimary = Color.Black,
            background = Color.Black,
            surface = Panel,
            onBackground = Color.White,
            onSurface = Color.White,
        ),
        content = content,
    )
}

fun highlightStyledText(text: String, keywords: List<String> = emptyList()): AnnotatedString {
    val cleanKeywords = keywords.filter { it.isNotBlank() }.sortedByDescending { it.length }
    return buildAnnotatedString {
        var i = 0
        while (i < text.length) {
            if (text.startsWith("**", i)) {
                val end = text.indexOf("**", i + 2)
                if (end > i + 2) {
                    withStyle(SpanStyle(color = Blue, fontWeight = FontWeight.Bold)) {
                        append(text.substring(i + 2, end))
                    }
                    i = end + 2
                    continue
                }
            }
            val matchedKw = cleanKeywords.firstOrNull { kw ->
                text.regionMatches(i, kw, 0, kw.length, ignoreCase = false) &&
                    (i == 0 || !text[i - 1].isLetterOrDigit()) &&
                    (i + kw.length >= text.length || !text[i + kw.length].isLetterOrDigit())
            }
            if (matchedKw != null) {
                withStyle(SpanStyle(color = Blue, fontWeight = FontWeight.Bold)) {
                    append(text.substring(i, i + matchedKw.length))
                }
                i += matchedKw.length
            } else {
                append(text[i])
                i++
            }
        }
    }
}

@Composable
fun PrimaryButton(text: String, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Blue,
            contentColor = Color.Black,
            disabledContainerColor = Blue.copy(alpha = 0.4f),
            disabledContentColor = Color.Black.copy(alpha = 0.6f),
        ),
    ) {
        Text(text, fontSize = 19.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SecondaryButton(text: String, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.5.dp, Outline),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
    ) {
        Text(text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun HintButton(enabled: Boolean = true, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(shape)
            .background(if (enabled) Color(0xFF0A2433) else Panel)
            .border(2.dp, if (enabled) Blue else Outline, shape)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Text("💡", fontSize = 24.sp)
    }
}

@Composable
fun Chip(
    text: String,
    modifier: Modifier = Modifier,
    background: Color = Color.Transparent,
    border: Color = Outline,
    textColor: Color = Color.White,
    onClick: (() -> Unit)? = null,
) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(background)
            .border(2.dp, border, shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = textColor, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun DashedBox(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.drawBehind {
            drawRoundRect(
                color = Color(0xFF3A3F44),
                cornerRadius = CornerRadius(16.dp.toPx()),
                style = Stroke(width = 3.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 12f))),
            )
        }
    )
}

@Composable
fun EmptySlot() {
    DashedBox(Modifier.width(96.dp).height(52.dp))
}

@Composable
fun PictureCard(emoji: String, modifier: Modifier = Modifier, border: Color = Color.Transparent, onClick: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(Color(0xFF2A2218))
            .border(BorderStroke(3.dp, border), shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(emoji, fontSize = 44.sp)
    }
}
