package com.example.ui

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

enum class BubbleArrowDirection {
    LEFT, RIGHT, TOP, BOTTOM
}

/**
 * Speech Bubble Shape designed for celestial map info cards and popups.
 * Seamlessly integrates an arrow pointer directed towards the lesson/stage planet sphere.
 */
class SpeechBubbleShape(
    val direction: BubbleArrowDirection,
    val cornerRadius: Dp = 18.dp,
    val arrowWidth: Dp = 12.dp,
    val arrowHeight: Dp = 18.dp,
    val arrowCenterY: Dp = 48.dp,
    val arrowCenterX: Dp = 90.dp
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val cr = with(density) { cornerRadius.toPx() }
        val aw = with(density) { arrowWidth.toPx() }
        val ah = with(density) { arrowHeight.toPx() }
        val cy = with(density) { arrowCenterY.toPx() }
        val cx = with(density) { arrowCenterX.toPx() }

        val bodyLeft = if (direction == BubbleArrowDirection.LEFT) aw else 0f
        val bodyRight = if (direction == BubbleArrowDirection.RIGHT) size.width - aw else size.width
        val bodyTop = if (direction == BubbleArrowDirection.TOP) ah else 0f
        val bodyBottom = if (direction == BubbleArrowDirection.BOTTOM) size.height - ah else size.height

        val path = Path().apply {
            // Start at top-left corner
            moveTo(bodyLeft + cr, bodyTop)

            // Top edge (with arrow if pointing TOP)
            if (direction == BubbleArrowDirection.TOP) {
                val arrowLeft = (cx - aw / 2f).coerceIn(bodyLeft + cr, bodyRight - cr - aw)
                val arrowTipX = arrowLeft + aw / 2f
                val arrowRight = arrowLeft + aw

                lineTo(arrowLeft, bodyTop)
                lineTo(arrowTipX, 0f)
                lineTo(arrowRight, bodyTop)
            }
            lineTo(bodyRight - cr, bodyTop)

            // Top-right corner
            arcTo(
                rect = Rect(bodyRight - 2 * cr, bodyTop, bodyRight, bodyTop + 2 * cr),
                startAngleDegrees = -90f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )

            // Right edge (with arrow if pointing RIGHT)
            if (direction == BubbleArrowDirection.RIGHT) {
                val arrowTop = (cy - ah / 2f).coerceIn(cr, bodyBottom - cr - ah)
                val arrowTipY = arrowTop + ah / 2f
                val arrowBottom = arrowTop + ah

                lineTo(bodyRight, arrowTop)
                lineTo(size.width, arrowTipY)
                lineTo(bodyRight, arrowBottom)
            }
            lineTo(bodyRight, bodyBottom - cr)

            // Bottom-right corner
            arcTo(
                rect = Rect(bodyRight - 2 * cr, bodyBottom - 2 * cr, bodyRight, bodyBottom),
                startAngleDegrees = 0f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )

            // Bottom edge (with arrow if pointing BOTTOM)
            if (direction == BubbleArrowDirection.BOTTOM) {
                val arrowLeft = (cx - aw / 2f).coerceIn(bodyLeft + cr, bodyRight - cr - aw)
                val arrowTipX = arrowLeft + aw / 2f
                val arrowRight = arrowLeft + aw

                lineTo(arrowRight, bodyBottom)
                lineTo(arrowTipX, size.height)
                lineTo(arrowLeft, bodyBottom)
            }
            lineTo(bodyLeft + cr, bodyBottom)

            // Bottom-left corner
            arcTo(
                rect = Rect(bodyLeft, bodyBottom - 2 * cr, bodyLeft + 2 * cr, bodyBottom),
                startAngleDegrees = 90f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )

            // Left edge (with arrow if pointing LEFT)
            if (direction == BubbleArrowDirection.LEFT) {
                val arrowTop = (cy - ah / 2f).coerceIn(cr, bodyBottom - cr - ah)
                val arrowTipY = arrowTop + ah / 2f
                val arrowBottom = arrowTop + ah

                lineTo(bodyLeft, arrowBottom)
                lineTo(0f, arrowTipY)
                lineTo(bodyLeft, arrowTop)
            }
            lineTo(bodyLeft, bodyTop + cr)

            // Top-left corner
            arcTo(
                rect = Rect(bodyLeft, bodyTop, bodyLeft + 2 * cr, bodyTop + 2 * cr),
                startAngleDegrees = 180f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )

            close()
        }
        return Outline.Generic(path)
    }
}