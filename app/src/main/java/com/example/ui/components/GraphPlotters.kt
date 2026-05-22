package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dsp.FilterResult
import com.example.dsp.FilterType
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

@Composable
fun MagnitudeResponsePlot(
    result: FilterResult,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val primaryColor = Color(0xFF55D6FF) // Actual sky-cyan
    val secondaryColor = Color(0xFFDDE2F9) // Interactive selector light indigo
    val tertiaryColor = Color(0xFFDDE2F9) // Guidelines target light indigo
    val onSurfaceVariant = Color(0xFFE2E4E9) // High-contrast Labels white-grey
    val gridColor = Color(0xFF43474E)
    val cardBg = Color(0xFF1A1C1E)

    // Touch inspection state
    var touchX by remember { mutableStateOf<Float?>(null) }

    val freqs = result.freqResponseFreqs
    val dbs = result.freqResponseMagDb

    Column(modifier = modifier) {
        // Dynamic Inspector Readout
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            if (touchX != null && freqs.isNotEmpty()) {
                val canvasWidth = 500f // arbitrary base, computed in Canvas, we'll map relative
                // Actually, let's show instructions if no touch, else active readout
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(cardBg)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(result) {
                        detectTapGestures(
                            onPress = { change ->
                                touchX = change.x
                                tryAwaitRelease()
                                touchX = null
                            }
                        )
                    }
                    .pointerInput(result) {
                        detectDragGestures(
                            onDragEnd = { touchX = null },
                            onDragCancel = { touchX = null },
                            onDrag = { change, _ ->
                                touchX = change.position.x
                            }
                        )
                    }
            ) {
                val width = size.width
                val height = size.height

                // Layout padding
                val paddingLeft = 65f
                val paddingRight = 40f
                val paddingTop = 30f
                val paddingBottom = 60f

                val chartWidth = width - paddingLeft - paddingRight
                val chartHeight = height - paddingTop - paddingBottom

                if (freqs.isEmpty() || dbs.isEmpty()) return@Canvas

                // 1. Draw Grid Lines (Y-Axis: dB, X-Axis: Hz)
                val dbTicks = listOf(0, -20, -40, -60, -80, -100)
                dbTicks.forEach { db ->
                    // Map dB to Y [0, -100] -> [0, chartHeight]
                    val yNorm = (db - 0.0) / -100.0
                    val y = paddingTop + (yNorm * chartHeight).toFloat()
                    
                    // Draw grid line
                    drawLine(
                        color = gridColor,
                        start = Offset(paddingLeft, y),
                        end = Offset(paddingLeft + chartWidth, y),
                        strokeWidth = 1f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    )

                    // Draw dB Label
                    drawText(
                        textMeasurer = textMeasurer,
                        text = "$db dB",
                        topLeft = Offset(10f, y - 20f),
                        style = TextStyle(
                            color = onSurfaceVariant,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }

                // X-Axis (Frequency) Ticks: 5 intervals
                val numXTicks = 5
                val maxFreq = result.sampleRate / 2.0
                for (i in 0 until numXTicks) {
                    val frac = i.toDouble() / (numXTicks - 1)
                    val fHz = frac * maxFreq
                    val x = paddingLeft + (frac * chartWidth).toFloat()

                    // Vertical grid line
                    drawLine(
                        color = gridColor,
                        start = Offset(x, paddingTop),
                        end = Offset(x, paddingTop + chartHeight),
                        strokeWidth = 1f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    )

                    // Label (kHz)
                    val label = if (fHz >= 1000.0) "%.1fkHz".format(fHz / 1000.0) else "%.0fHz".format(fHz)
                    drawText(
                        textMeasurer = textMeasurer,
                        text = label,
                        topLeft = Offset(x - 40f, paddingTop + chartHeight + 10f),
                        style = TextStyle(
                            color = onSurfaceVariant,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )
                    )
                }

                // 2. Plot Magnitude Response Curve
                val path = Path()
                for (i in freqs.indices) {
                    val f = freqs[i]
                    val db = dbs[i].coerceIn(-100.0, 0.0)

                    val xFrac = f / maxFreq
                    val yFrac = (db - 0.0) / -100.0

                    val px = paddingLeft + (xFrac * chartWidth).toFloat()
                    val py = paddingTop + (yFrac * chartHeight).toFloat()

                    if (i == 0) {
                        path.moveTo(px, py)
                    } else {
                        path.lineTo(px, py)
                    }
                }

                drawPath(
                    path = path,
                    color = primaryColor,
                    style = Stroke(width = 4f, join = StrokeJoin.Round)
                )

                // 3. Highlight Cutoff Frequencies as Orange vertical indicators
                val cutoffs = when (result.type) {
                    FilterType.LOWPASS, FilterType.HIGHPASS -> listOf(result.fCutoff1)
                    FilterType.BANDPASS, FilterType.BANDSTOP -> listOf(result.fCutoff1, result.fCutoff2)
                }

                cutoffs.forEach { fc ->
                    val xFrac = fc / maxFreq
                    val px = paddingLeft + (xFrac * chartWidth).toFloat()
                    
                    if (px in paddingLeft..(paddingLeft + chartWidth)) {
                        drawLine(
                            color = tertiaryColor,
                            start = Offset(px, paddingTop),
                            end = Offset(px, paddingTop + chartHeight),
                            strokeWidth = 3f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 10f), 0f)
                        )
                    }
                }

                // 4. Interactive Tracking Cursor details
                touchX?.let { tx ->
                    val constrainedTx = tx.coerceIn(paddingLeft, paddingLeft + chartWidth)
                    val frac = (constrainedTx - paddingLeft) / chartWidth
                    
                    // Match to find best index
                    val dataIndex = (frac * (freqs.size - 1)).toInt().coerceIn(freqs.indices)
                    val freqVal = freqs[dataIndex]
                    val dbVal = dbs[dataIndex]

                    val py = paddingTop + (((dbVal.coerceIn(-100.0, 0.0) - 0.0) / -100.0) * chartHeight).toFloat()

                    // Draw vertical tracking guide
                    drawLine(
                        color = secondaryColor.copy(alpha = 0.7f),
                        start = Offset(constrainedTx, paddingTop),
                        end = Offset(constrainedTx, paddingTop + chartHeight),
                        strokeWidth = 2f
                    )

                    // Target dot
                    drawCircle(
                        color = secondaryColor,
                        radius = 8f,
                        center = Offset(constrainedTx, py)
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 4f,
                        center = Offset(constrainedTx, py)
                    )

                    // Draw dynamic readout window
                    val inspectLabel = "%.1f Hz | %.1f dB".format(freqVal, dbVal)
                    val inspectText = textMeasurer.measure(
                        text = inspectLabel,
                        style = TextStyle(
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )

                    val boxW = inspectText.size.width + 24
                    val boxH = inspectText.size.height + 12
                    val bx = if (constrainedTx + boxW + 10f > paddingLeft + chartWidth) {
                        constrainedTx - boxW - 10f
                    } else {
                        constrainedTx + 10f
                    }
                    val by = (py - boxH / 2f).coerceIn(paddingTop, paddingTop + chartHeight - boxH)

                    drawRoundRect(
                        color = Color.Black.copy(alpha = 0.85f),
                        topLeft = Offset(bx, by),
                        size = Size(boxW.toFloat(), boxH.toFloat()),
                        cornerRadius = CornerRadius(12f, 12f)
                    )

                    drawText(
                        textMeasurer = textMeasurer,
                        text = inspectLabel,
                        topLeft = Offset(bx + 12f, by + 6f),
                        style = TextStyle(
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun PhaseResponsePlot(
    result: FilterResult,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val primaryColor = Color(0xFF55D6FF) // Actual sky-cyan
    val secondaryColor = Color(0xFFE2E4E9) // High-contrast Labels white-grey
    val interactiveColor = Color(0xFFDDE2F9) // Interactive selector light indigo
    val gridColor = Color(0xFF43474E)
    val cardBg = Color(0xFF1A1C1E)

    var touchX by remember { mutableStateOf<Float?>(null) }

    val freqs = result.freqResponseFreqs
    val phases = result.freqResponsePhaseDeg

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(260.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(cardBg)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(result) {
                    detectTapGestures(
                        onPress = { change ->
                            touchX = change.x
                            tryAwaitRelease()
                            touchX = null
                        }
                    )
                }
                .pointerInput(result) {
                    detectDragGestures(
                        onDragEnd = { touchX = null },
                        onDragCancel = { touchX = null },
                        onDrag = { change, _ ->
                            touchX = change.position.x
                        }
                    )
                }
        ) {
            val width = size.width
            val height = size.height

            val paddingLeft = 65f
            val paddingRight = 40f
            val paddingTop = 30f
            val paddingBottom = 60f

            val chartWidth = width - paddingLeft - paddingRight
            val chartHeight = height - paddingTop - paddingBottom

            if (freqs.isEmpty() || phases.isEmpty()) return@Canvas

            // 1. Draw Grid Lines (Y-Axis: phase +180 to -180 deg)
            val phaseTicks = listOf(180, 90, 0, -90, -180)
            phaseTicks.forEach { deg ->
                // Map +180..-180 -> 0..chartHeight
                val yNorm = (180.0 - deg) / 360.0
                val y = paddingTop + (yNorm * chartHeight).toFloat()

                drawLine(
                    color = gridColor,
                    start = Offset(paddingLeft, y),
                    end = Offset(paddingLeft + chartWidth, y),
                    strokeWidth = 1f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                )

                drawText(
                    textMeasurer = textMeasurer,
                    text = "${deg}°",
                    topLeft = Offset(10f, y - 20f),
                    style = TextStyle(
                        color = secondaryColor,
                        fontSize = 11.sp
                    )
                )
            }

            // X-Axis (Frequency) Ticks: 5 intervals
            val numXTicks = 5
            val maxFreq = result.sampleRate / 2.0
            for (i in 0 until numXTicks) {
                val frac = i.toDouble() / (numXTicks - 1)
                val fHz = frac * maxFreq
                val x = paddingLeft + (frac * chartWidth).toFloat()

                drawLine(
                    color = gridColor,
                    start = Offset(x, paddingTop),
                    end = Offset(x, paddingTop + chartHeight),
                    strokeWidth = 1f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                )

                val label = if (fHz >= 1000.0) "%.1fkHz".format(fHz / 1000.0) else "%.0fHz".format(fHz)
                drawText(
                    textMeasurer = textMeasurer,
                    text = label,
                    topLeft = Offset(x - 40f, paddingTop + chartHeight + 10f),
                    style = TextStyle(
                        color = secondaryColor,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )
                )
            }

            // 2. Plot Phase Response Curve
            val path = Path()
            for (i in freqs.indices) {
                val f = freqs[i]
                val ph = phases[i].coerceIn(-180.0, 180.0)

                val xFrac = f / maxFreq
                val yFrac = (180.0 - ph) / 360.0

                val px = paddingLeft + (xFrac * chartWidth).toFloat()
                val py = paddingTop + (yFrac * chartHeight).toFloat()

                if (i == 0) {
                    path.moveTo(px, py)
                } else {
                    // Avoid horizontal wraparound wrap-jump rendering artifacts if they wrapping at +-180
                    val prevPh = phases[i - 1]
                    if (abs(ph - prevPh) > 270.0) {
                        // Breakthrough jump: break line path and start new sub-path to look clean
                        path.moveTo(px, py)
                    } else {
                        path.lineTo(px, py)
                    }
                }
            }

            drawPath(
                path = path,
                color = primaryColor,
                style = Stroke(width = 3.5f, join = StrokeJoin.Round)
            )

            // 3. Hover inspections
            touchX?.let { tx ->
                val constrainedTx = tx.coerceIn(paddingLeft, paddingLeft + chartWidth)
                val frac = (constrainedTx - paddingLeft) / chartWidth

                val dataIndex = (frac * (freqs.size - 1)).toInt().coerceIn(freqs.indices)
                val freqVal = freqs[dataIndex]
                val phVal = phases[dataIndex]

                val py = paddingTop + (((180.0 - phVal) / 360.0) * chartHeight).toFloat()

                drawLine(
                    color = interactiveColor.copy(alpha = 0.7f),
                    start = Offset(constrainedTx, paddingTop),
                    end = Offset(constrainedTx, paddingTop + chartHeight),
                    strokeWidth = 2f
                )

                drawCircle(
                    color = interactiveColor,
                    radius = 8f,
                    center = Offset(constrainedTx, py)
                )

                val inspectLabel = "%.1f Hz | %.1f°".format(freqVal, phVal)
                val inspectText = textMeasurer.measure(
                    text = inspectLabel,
                    style = TextStyle(
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                )

                val boxW = inspectText.size.width + 24
                val boxH = inspectText.size.height + 12
                val bx = if (constrainedTx + boxW + 10f > paddingLeft + chartWidth) {
                    constrainedTx - boxW - 10f
                } else {
                    constrainedTx + 10f
                }
                val by = (py - boxH / 2f).coerceIn(paddingTop, paddingTop + chartHeight - boxH)

                drawRoundRect(
                    color = Color.Black.copy(alpha = 0.85f),
                    topLeft = Offset(bx, by),
                    size = Size(boxW.toFloat(), boxH.toFloat()),
                    cornerRadius = CornerRadius(12f, 12f)
                )

                drawText(
                    textMeasurer = textMeasurer,
                    text = inspectLabel,
                    topLeft = Offset(bx + 12f, by + 6f),
                    style = TextStyle(
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}

@Composable
fun ImpulseResponsePlot(
    result: FilterResult,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val stemColor = Color(0xFF55D6FF) // Actual / stem lines sky-cyan
    val nodeColor = Color(0xFFDDE2F9) // Target / nodes light indigo
    val secondaryColor = Color(0xFFE2E4E9) // High-contrast Labels white-grey
    val gridColor = Color(0xFF43474E)
    val cardBg = Color(0xFF1A1C1E)

    val response = result.impulseResponse

    // Display only first 50 stems for readability if taps > 50, or show 50 points by default
    val maxPoints = 50
    val impulsePointsToShow = remember(result) {
        val count = if (result.isFir) result.firCoefficients.size else maxPoints
        min(response.size, max(count, 35))
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(260.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(cardBg)
    ) {
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val width = size.width
            val height = size.height

            val paddingLeft = 65f
            val paddingRight = 40f
            val paddingTop = 30f
            val paddingBottom = 60f

            val chartWidth = width - paddingLeft - paddingRight
            val chartHeight = height - paddingTop - paddingBottom

            if (response.isEmpty()) return@Canvas

            // Find absolute max peak to dynamically scale Y-axis
            var absMax = 0.001
            for (i in 0 until impulsePointsToShow) {
                absMax = max(absMax, abs(response[i]))
            }
            val yMax = absMax * 1.2
            val yMin = -yMax

            // 1. Draw Grid Lines (Y-Axis bounds)
            val yTicks = listOf(yMax, yMax / 2, 0.0, yMin / 2, yMin)
            yTicks.forEach { valDb ->
                val yNorm = (yMax - valDb) / (yMax - yMin)
                val y = paddingTop + (yNorm * chartHeight).toFloat()

                drawLine(
                    color = gridColor,
                    start = Offset(paddingLeft, y),
                    end = Offset(paddingLeft + chartWidth, y),
                    strokeWidth = 1f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                )

                // Label format
                val label = "%.3f".format(valDb)
                drawText(
                    textMeasurer = textMeasurer,
                    text = label,
                    topLeft = Offset(10f, y - 20f),
                    style = TextStyle(
                        color = secondaryColor,
                        fontSize = 11.sp
                    )
                )
            }

            // Draw stem vertical dividers for time slots
            val numXTicks = 6
            for (i in 0 until numXTicks) {
                val frac = i.toDouble() / (numXTicks - 1)
                val sampleIdx = (frac * (impulsePointsToShow - 1)).toInt()
                val x = paddingLeft + (frac * chartWidth).toFloat()

                drawLine(
                    color = gridColor,
                    start = Offset(x, paddingTop),
                    end = Offset(x, paddingTop + chartHeight),
                    strokeWidth = 1f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                )

                drawText(
                    textMeasurer = textMeasurer,
                    text = "$sampleIdx",
                    topLeft = Offset(x - 10f, paddingTop + chartHeight + 10f),
                    style = TextStyle(
                        color = secondaryColor,
                        fontSize = 11.sp
                    )
                )
            }

            // Label for X-axis title
            val xTitle = textMeasurer.measure("Sample Index (n)")
            drawText(
                textMeasurer = textMeasurer,
                text = "Sample Index (n)",
                topLeft = Offset(paddingLeft + chartWidth/2 - xTitle.size.width/2, paddingTop + chartHeight + 35f),
                style = TextStyle(
                    color = secondaryColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            )

            // 2. Draw stems
            val zeroYNorm = yMax / (yMax - yMin)
            val centerY = paddingTop + (zeroYNorm * chartHeight).toFloat()

            for (i in 0 until impulsePointsToShow) {
                val valDb = response[i]
                val fracX = i.toDouble() / (impulsePointsToShow - 1)
                val rx = paddingLeft + (fracX * chartWidth).toFloat()

                val fracY = (yMax - valDb) / (yMax - yMin)
                val ry = paddingTop + (fracY * chartHeight).toFloat()

                // Draw vertical line from center zero baseline relative to peak
                drawLine(
                    color = stemColor.copy(alpha = 0.8f),
                    start = Offset(rx, centerY),
                    end = Offset(rx, ry),
                    strokeWidth = 3f
                )

                // Circle node
                drawCircle(
                    color = nodeColor,
                    radius = 6f,
                    center = Offset(rx, ry)
                )
                drawCircle(
                    color = Color.White,
                    radius = 2.5f,
                    center = Offset(rx, ry)
                )
            }
        }
    }
}
