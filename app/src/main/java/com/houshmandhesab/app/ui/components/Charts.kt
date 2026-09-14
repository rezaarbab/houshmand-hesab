package com.houshmandhesab.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.houshmandhesab.app.util.Format
import com.houshmandhesab.app.util.toColor

data class PieSlice(val label: String, val value: Float, val color: Long)

@Composable
fun DonutChart(
    slices: List<PieSlice>,
    modifier: Modifier = Modifier,
    strokeWidth: Float = 60f,
    centerLabel: String? = null
) {
    val progress by animateFloatAsState(
        targetValue = if (slices.isEmpty()) 0f else 1f,
        animationSpec = tween(durationMillis = 700),
        label = "pie"
    )
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(190.dp)) {
            val total = slices.sumOf { it.value.toDouble() }.toFloat()
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(strokeWidth, cap = StrokeCap.Butt)
            )
            if (total > 0f) {
                var start = -90f
                slices.forEach { slice ->
                    val sweepAngle = (slice.value / total) * 360f
                    drawArc(
                        color = toColor(slice.color),
                        startAngle = start,
                        sweepAngle = sweepAngle * progress,
                        useCenter = false,
                        style = Stroke(strokeWidth, cap = StrokeCap.Butt)
                    )
                    start += sweepAngle
                }
            }
        }
        if (centerLabel != null) {
            Text(centerLabel, style = MaterialTheme.typography.titleSmall)
        }
    }
}

@Composable
fun PieLegend(slices: List<PieSlice>, persianDigits: Boolean, modifier: Modifier = Modifier) {
    val total = slices.sumOf { it.value.toDouble() }.toFloat()
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        slices.take(6).forEach { slice ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(12.dp)
                        .background(toColor(slice.color), CircleShape)
                )
                Text(
                    slice.label,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .weight(1f, fill = false)
                )
                Spacer(Modifier.weight(1f))
                val pct = if (total > 0) (slice.value / total * 100).toInt() else 0
                Text(
                    if (persianDigits) Format.toPersianDigits("$pct%") else "$pct%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

data class BarGroup(val label: String, val income: Float, val expense: Float)

@Composable
fun GroupedBarChart(
    groups: List<BarGroup>,
    incomeColor: Color,
    expenseColor: Color,
    modifier: Modifier = Modifier
) {
    val progress by animateFloatAsState(
        targetValue = if (groups.isEmpty()) 0f else 1f,
        animationSpec = tween(700),
        label = "bar"
    )
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
    ) {
        if (groups.isEmpty()) return@Canvas
        val maxValue = groups.maxOf { maxOf(it.income, it.expense) }.coerceAtLeast(1f)
        val groupSpace = size.width / groups.size
        val barWidth = groupSpace * 0.22f
        val chartHeight = size.height - 40f
        val paint = android.graphics.Paint().apply {
            color = labelColor.toArgb()
            textSize = 30f
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
        }
        groups.forEachIndexed { index, group ->
            val centerX = groupSpace * index + groupSpace / 2f
            val incomeH = (group.income / maxValue) * chartHeight * progress
            val expenseH = (group.expense / maxValue) * chartHeight * progress
            drawRoundRect(
                color = incomeColor,
                topLeft = Offset(centerX - barWidth - 5f, chartHeight - incomeH),
                size = Size(barWidth, incomeH),
                cornerRadius = CornerRadius(12f, 12f)
            )
            drawRoundRect(
                color = expenseColor,
                topLeft = Offset(centerX + 5f, chartHeight - expenseH),
                size = Size(barWidth, expenseH),
                cornerRadius = CornerRadius(12f, 12f)
            )
            drawContext.canvas.nativeCanvas.drawText(
                group.label,
                centerX,
                size.height - 6f,
                paint
            )
        }
    }
}

@Composable
fun MiniBars(
    values: List<Float>,
    color: Color,
    modifier: Modifier = Modifier
) {
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
    ) {
        if (values.isEmpty()) return@Canvas
        val max = values.max().coerceAtLeast(1f)
        val slot = size.width / values.size
        val barWidth = slot * 0.5f
        values.forEachIndexed { i, v ->
            val h = (v / max) * (size.height - 8f)
            drawRoundRect(
                color = if (v > 0f) color else trackColor,
                topLeft = Offset(slot * i + (slot - barWidth) / 2f, size.height - h),
                size = Size(barWidth, h.coerceAtLeast(6f)),
                cornerRadius = CornerRadius(12f, 12f)
            )
        }
    }
}

@Composable
fun ChartLegend(colors: List<Color>, labels: List<String>, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        colors.forEachIndexed { i, c ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(10.dp)
                        .background(c, RoundedCornerShape(3.dp))
                )
                Text(
                    labels[i],
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(start = 6.dp)
                )
            }
        }
    }
}
