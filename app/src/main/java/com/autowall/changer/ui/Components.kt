package com.autowall.changer.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.autowall.changer.ui.theme.Faint
import com.autowall.changer.ui.theme.Ink
import com.autowall.changer.ui.theme.Muted
import com.autowall.changer.ui.theme.Line
import com.autowall.changer.ui.theme.Surface2
import com.autowall.changer.ui.theme.Surface3
import kotlin.math.cos
import kotlin.math.sin

enum class Glyph { Plus, Refresh, Close }

@Composable
fun GlyphIcon(
    glyph: Glyph,
    tint: Color,
    modifier: Modifier = Modifier,
    stroke: Dp = 1.7.dp,
) {
    androidx.compose.foundation.Canvas(modifier) {
        val w = size.width
        val h = size.height
        val width = stroke.toPx()
        val cap = StrokeCap.Round
        when (glyph) {
            Glyph.Plus -> {
                drawLine(tint, Offset(w * 0.5f, h * 0.24f), Offset(w * 0.5f, h * 0.76f), width, cap)
                drawLine(tint, Offset(w * 0.24f, h * 0.5f), Offset(w * 0.76f, h * 0.5f), width, cap)
            }

            Glyph.Close -> {
                drawLine(tint, Offset(w * 0.28f, h * 0.28f), Offset(w * 0.72f, h * 0.72f), width, cap)
                drawLine(tint, Offset(w * 0.72f, h * 0.28f), Offset(w * 0.28f, h * 0.72f), width, cap)
            }

            Glyph.Refresh -> {
                val radius = w * 0.33f
                drawArc(
                    color = tint,
                    startAngle = -55f,
                    sweepAngle = 300f,
                    useCenter = false,
                    topLeft = Offset(w / 2f - radius, h / 2f - radius),
                    size = Size(radius * 2f, radius * 2f),
                    style = Stroke(width = width, cap = cap),
                )
                val angle = Math.toRadians(-55.0)
                val tip = Offset(
                    w / 2f + radius * cos(angle).toFloat(),
                    h / 2f + radius * sin(angle).toFloat(),
                )
                drawLine(tint, tip, Offset(tip.x - w * 0.16f, tip.y), width, cap)
                drawLine(tint, tip, Offset(tip.x, tip.y - h * 0.16f), width, cap)
            }
        }
    }
}

@Composable
fun AutoWallSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val progress by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = tween(durationMillis = 180),
        label = "switch",
    )
    val track = lerp(Surface3, Ink, progress)
    val thumb = lerp(Faint, Color(0xFF0A0A0C), progress)
    Box(
        modifier = modifier
            .size(width = 50.dp, height = 30.dp)
            .clip(RoundedCornerShape(50))
            .background(track)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { onCheckedChange(!checked) },
    ) {
        Box(
            modifier = Modifier
                .padding(3.dp)
                .size(24.dp)
                .graphicsLayer { translationX = progress * 20.dp.toPx() }
                .clip(CircleShape)
                .background(thumb),
        )
    }
}

@Composable
fun SelectableChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = if (selected) Ink else Surface2,
        contentColor = if (selected) Color(0xFF0B0B0C) else Muted,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
        )
    }
}

@Composable
fun SectionLabel(text: String, trailing: String? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = Muted,
        )
        if (trailing != null) {
            Text(
                text = trailing,
                style = MaterialTheme.typography.bodySmall,
                color = Faint,
            )
        }
    }
}

@Composable
fun DashedTile(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .drawBehind {
                drawRoundRect(
                    color = Line2,
                    style = Stroke(
                        width = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f),
                    ),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(18.dp.toPx()),
                )
            }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { content() }
}

private val Line2 = Color(0xFF2E2E36)

@Composable
fun ThinDivider(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Line),
    )
}
