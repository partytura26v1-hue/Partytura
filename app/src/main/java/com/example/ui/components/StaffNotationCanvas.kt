package com.example.ui.components

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.example.data.model.ClefType
import com.example.domain.MusicTheoryUtils
import com.example.domain.NoteDuration
import com.example.domain.StaffElement
import kotlin.math.max

@Composable
fun StaffNotationCanvas(
    staffNotes: String,
    clef: ClefType,
    transposeSemitones: Int,
    modifier: Modifier = Modifier
) {
    val elements = remember(staffNotes, transposeSemitones) {
        MusicTheoryUtils.parseStaffElements(staffNotes, transposeSemitones)
    }

    val surfaceColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val lineColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
    val inkColor = MaterialTheme.colorScheme.onSurface
    val accentColor = MaterialTheme.colorScheme.primary

    if (elements.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(140.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(surfaceColor)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Wpisz nuty (np. C4 D4 E4 G4 | A4 G4 E4 -), aby zobaczyć pięciolinię.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val canvasWidthDp = max(320, 96 + elements.size * 44).dp
    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(surfaceColor)
            .horizontalScroll(scrollState)
            .padding(vertical = 8.dp)
    ) {
        Canvas(
            modifier = Modifier
                .width(canvasWidthDp)
                .height(156.dp)
        ) {
            val lineSpacing = size.height / 11f
            val topStaffY = lineSpacing * 3.2f
            val bottomStaffY = topStaffY + lineSpacing * 4f
            val halfStepPx = lineSpacing / 2f
            val bottomLineStep = MusicTheoryUtils.bottomLineDiatonicStep(clef)

            // Draw 5 staff lines
            for (i in 0..4) {
                val y = topStaffY + i * lineSpacing
                drawLine(
                    color = lineColor,
                    start = Offset(18f, y),
                    end = Offset(size.width - 18f, y),
                    strokeWidth = 2.2f
                )
            }

            // Left vertical bar
            drawLine(
                color = inkColor.copy(alpha = 0.7f),
                start = Offset(18f, topStaffY),
                end = Offset(18f, bottomStaffY),
                strokeWidth = 3f
            )

            // Draw Clef symbol & note labels using Android nativeCanvas
            val clefPaint = Paint().apply {
                isAntiAlias = true
                color = accentColor.toArgb()
                textSize = lineSpacing * 3.4f
                typeface = Typeface.SERIF
            }
            val labelPaint = Paint().apply {
                isAntiAlias = true
                color = accentColor.toArgb()
                textSize = lineSpacing * 0.95f
                textAlign = Paint.Align.CENTER
                typeface = Typeface.MONOSPACE
            }
            val accidentalPaint = Paint().apply {
                isAntiAlias = true
                color = inkColor.toArgb()
                textSize = lineSpacing * 1.45f
                textAlign = Paint.Align.RIGHT
                typeface = Typeface.DEFAULT_BOLD
            }

            val clefGlyph = when (clef) {
                ClefType.BASS -> "F:"
                ClefType.PERCUSSION -> "||"
                else -> "G"
            }
            drawContext.canvas.nativeCanvas.drawText(
                clefGlyph,
                28f,
                topStaffY + lineSpacing * 3.1f,
                clefPaint
            )

            var xCursor = 110f
            val stepX = 76f

            elements.forEach { elem ->
                when (elem) {
                    is StaffElement.BarLine -> {
                        drawLine(
                            color = inkColor.copy(alpha = 0.65f),
                            start = Offset(xCursor - 12f, topStaffY),
                            end = Offset(xCursor - 12f, bottomStaffY),
                            strokeWidth = 2.5f
                        )
                        xCursor += stepX * 0.55f
                    }

                    is StaffElement.Rest -> {
                        val restY = topStaffY + lineSpacing * 2f
                        drawRect(
                            color = inkColor.copy(alpha = 0.75f),
                            topLeft = Offset(xCursor - 9f, restY - 4f),
                            size = Size(18f, 8f)
                        )
                        xCursor += stepX * 0.85f
                    }

                    is StaffElement.Note -> {
                        val stepsFromBottom = elem.diatonicStep - bottomLineStep
                        val noteY = bottomStaffY - stepsFromBottom * halfStepPx

                        // Ledger lines above or below staff
                        if (stepsFromBottom < -1) {
                            var s = -2
                            while (s >= stepsFromBottom) {
                                val ly = bottomStaffY - s * halfStepPx
                                drawLine(
                                    color = lineColor,
                                    start = Offset(xCursor - 18f, ly),
                                    end = Offset(xCursor + 18f, ly),
                                    strokeWidth = 2.2f
                                )
                                s -= 2
                            }
                        } else if (stepsFromBottom > 9) {
                            var s = 10
                            while (s <= stepsFromBottom) {
                                val ly = bottomStaffY - s * halfStepPx
                                drawLine(
                                    color = lineColor,
                                    start = Offset(xCursor - 18f, ly),
                                    end = Offset(xCursor + 18f, ly),
                                    strokeWidth = 2.2f
                                )
                                s += 2
                            }
                        }

                        // Accidental
                        if (elem.accidental.isNotEmpty()) {
                            val accSymbol = if (elem.accidental == "#") "♯" else "♭"
                            drawContext.canvas.nativeCanvas.drawText(
                                accSymbol,
                                xCursor - 15f,
                                noteY + halfStepPx * 0.6f,
                                accidentalPaint
                            )
                        }

                        // Notehead
                        val headW = lineSpacing * 1.18f
                        val headH = lineSpacing * 0.86f
                        rotate(degrees = -18f, pivot = Offset(xCursor, noteY)) {
                            if (elem.duration == NoteDuration.QUARTER) {
                                drawOval(
                                    color = inkColor,
                                    topLeft = Offset(xCursor - headW / 2f, noteY - headH / 2f),
                                    size = Size(headW, headH)
                                )
                            } else {
                                drawOval(
                                    color = inkColor,
                                    topLeft = Offset(xCursor - headW / 2f, noteY - headH / 2f),
                                    size = Size(headW, headH),
                                    style = Stroke(width = 3f)
                                )
                            }
                        }

                        // Stem for half and quarter notes
                        if (elem.duration != NoteDuration.WHOLE) {
                            val stemUp = stepsFromBottom < 4
                            val stemLen = lineSpacing * 3.1f
                            val stemX = if (stemUp) xCursor + headW * 0.44f else xCursor - headW * 0.44f
                            val stemEndY = if (stemUp) noteY - stemLen else noteY + stemLen
                            drawLine(
                                color = inkColor,
                                start = Offset(stemX, noteY),
                                end = Offset(stemX, stemEndY),
                                strokeWidth = 2.8f,
                                cap = StrokeCap.Round
                            )
                        }

                        // Note name below staff for fast band reading
                        drawContext.canvas.nativeCanvas.drawText(
                            elem.label,
                            xCursor,
                            size.height - 6f,
                            labelPaint
                        )

                        xCursor += stepX
                    }
                }
            }
        }
    }
}
