package com.dimensional.app.ui.gesture

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import kotlin.math.hypot

private const val DOUBLE_TAP_TIMEOUT_MS = 280L
private const val TOUCH_SLOP = 6f

fun Modifier.trackpadTouchHandler(
    onPointerMove: (dx: Float, dy: Float, dt: Long) -> Unit,
    onDragLockStart: () -> Unit,
    onDragLockEnd: () -> Unit
): Modifier = pointerInput(Unit) {
    var lastTapUpTime = 0L
    var lastEventUptime = 0L

    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        val downTime = down.uptimeMillis
        val isDoubleTapHold = (downTime - lastTapUpTime) <= DOUBLE_TAP_TIMEOUT_MS

        var isDragLocking = false
        var hasMovedPastSlop = false
        var accumulatedMovement = 0f

        if (isDoubleTapHold) {
            isDragLocking = true
            onDragLockStart()
        }

        lastEventUptime = downTime
        val currentPointerId = down.id

        while (true) {
            val event = awaitPointerEvent()
            val change = event.changes.firstOrNull { it.id == currentPointerId } ?: break

            if (!change.pressed) {
                val upTime = change.uptimeMillis
                if (isDragLocking) {
                    onDragLockEnd()
                    lastTapUpTime = 0L
                } else if (!hasMovedPastSlop && (upTime - downTime) < DOUBLE_TAP_TIMEOUT_MS) {
                    lastTapUpTime = upTime
                }
                change.consume()
                break
            }

            val delta = change.positionChange()
            val dt = (change.uptimeMillis - lastEventUptime).coerceAtLeast(1L)
            lastEventUptime = change.uptimeMillis

            if (!hasMovedPastSlop) {
                accumulatedMovement += hypot(delta.x, delta.y)
                if (accumulatedMovement > TOUCH_SLOP) {
                    hasMovedPastSlop = true
                }
            }

            if (hasMovedPastSlop || isDragLocking) {
                onPointerMove(delta.x, delta.y, dt)
            }

            change.consume()
        }
    }
}
