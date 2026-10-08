package com.dimensional.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dimensional.app.ui.contract.ConnectionStatus
import com.dimensional.app.ui.contract.TrackpadUiEvent
import com.dimensional.app.ui.contract.TrackpadUiState
import com.dimensional.app.ui.gesture.trackpadTouchHandler

@Composable
fun TrackpadScreen(
    state: TrackpadUiState,
    onEvent: (TrackpadUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F0F))
            .statusBarsPadding()
            .navigationBarsPadding() // Safe area navigation bar Android
    ) {
        // 1. Header Status Koneksi
        ConnectionHeader(status = state.connectionStatus)

        // 2. Kanvas Trackpad Utama (Atas & Tengah)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 8.dp)
                .background(Color(0xFF191919), RoundedCornerShape(12.dp))
                .border(
                    width = 1.dp,
                    color = if (state.isDragLockActive) Color(0xFF4A90E2) else Color(0xFF282828),
                    shape = RoundedCornerShape(12.dp)
                )
                .trackpadTouchHandler(
                    onPointerMove = { dx, dy, dt -> onEvent(TrackpadUiEvent.PointerMoved(dx, dy, dt)) },
                    onDragLockStart = { onEvent(TrackpadUiEvent.DragLockStarted) },
                    onDragLockEnd = { onEvent(TrackpadUiEvent.DragLockEnded) }
                )
        ) {
            // Tombol "RE" di pojok kanan bawah kanvas
            UndoButton(
                onClick = { onEvent(TrackpadUiEvent.UndoTriggered) },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 12.dp, bottom = 12.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        Divider(color = Color(0xFF262626), thickness = 1.dp)

        // 3. Tombol Fisik Virtual "L" dan "R" (Terbagi 50:50)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .background(Color(0xFF141414))
        ) {
            MouseButton(
                label = "L",
                onDown = { onEvent(TrackpadUiEvent.LeftButtonDown) },
                onUp = { onEvent(TrackpadUiEvent.LeftButtonUp) },
                modifier = Modifier.weight(1f)
            )

            Divider(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(1.5.dp),
                color = Color(0xFF262626)
            )

            MouseButton(
                label = "R",
                onDown = { onEvent(TrackpadUiEvent.RightButtonDown) },
                onUp = { onEvent(TrackpadUiEvent.RightButtonUp) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ConnectionHeader(status: ConnectionStatus) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val (indicatorColor, statusText) = when (status) {
            is ConnectionStatus.Connected -> Color(0xFF4CAF50) to "Terhubung: ${status.deviceName}"
            ConnectionStatus.Connecting -> Color(0xFFFFC107) to "Menghubungkan..."
            ConnectionStatus.Disconnected -> Color(0xFF757575) to "Bluetooth Siap (Belum Terhubung)"
        }

        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(indicatorColor)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = statusText, color = Color.LightGray, fontSize = 12.sp)
    }
}

@Composable
private fun UndoButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    Box(
        modifier = modifier
            .size(width = 68.dp, height = 50.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF252525))
            .border(1.dp, Color(0xFF383838), RoundedCornerShape(8.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onClick()
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "RE", color = Color(0xFFECECEC), fontSize = 16.sp)
    }
}

@Composable
private fun MouseButton(
    label: String,
    onDown: () -> Unit,
    onUp: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    Box(
        modifier = modifier
            .fillMaxHeight()
            .background(if (isPressed) Color(0xFF2A2A2A) else Color(0xFF141414))
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown()
                    isPressed = true
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onDown()
                    waitForUpOrCancellation()
                    isPressed = false
                    onUp()
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isPressed) Color.White else Color(0xFF707070),
            fontSize = 32.sp
        )
    }
}
