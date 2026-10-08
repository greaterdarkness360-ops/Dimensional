package com.dimensional.app.ui

import android.bluetooth.BluetoothDevice
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
import androidx.compose.material3.*
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
    diagnosticText: String,
    pairedDevices: List<BluetoothDevice>,
    onReRegister: () -> Unit,
    onConnectDevice: (BluetoothDevice) -> Unit,
    onEvent: (TrackpadUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDeviceDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F0F))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // 1. Header Status Koneksi & "By Natanael"
        ConnectionHeader(
            status = state.connectionStatus,
            onConnectClick = { showDeviceDialog = true }
        )

        // 2. Banner Diagnostik
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp)
                .background(Color(0xFF1E232A), RoundedCornerShape(8.dp))
                .border(1.dp, Color(0xFF2C3E50), RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Diagnostik: $diagnosticText",
                    color = Color(0xFF64B5F6),
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Refresh",
                    color = Color(0xFFFFB74D),
                    fontSize = 11.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { onReRegister() }
                        .padding(4.dp)
                )
            }
        }

        // 3. Kanvas Trackpad
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .background(Color(0xFF191919), RoundedCornerShape(12.dp))
                .border(
                    width = 1.dp,
                    color = if (state.isDragLockActive) Color(0xFF4A90E2) else Color(0xFF282828),
                    shape = RoundedCornerShape(12.dp)
                )
                .trackpadTouchHandler(
                    onPointerMove = { dx, dy, dt -> onEvent(TrackpadUiEvent.PointerMoved(dx, dy, dt)) },
                    onTwoFingerScroll = { dy, dt -> onEvent(TrackpadUiEvent.TwoFingerScrolled(dy, dt)) },
                    onDragLockStart = { onEvent(TrackpadUiEvent.DragLockStarted) },
                    onDragLockEnd = { onEvent(TrackpadUiEvent.DragLockEnded) }
                )
        ) {
            UndoButton(
                onClick = { onEvent(TrackpadUiEvent.UndoTriggered) },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 12.dp, bottom = 12.dp)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))
        Divider(color = Color(0xFF262626), thickness = 1.dp)

        // 4. Tombol L dan R
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

    if (showDeviceDialog) {
        AlertDialog(
            onDismissRequest = { showDeviceDialog = false },
            title = { Text("Pilih Tablet / PC", color = Color.White) },
            text = {
                Column {
                    if (pairedDevices.isEmpty()) {
                        Text("Belum ada perangkat terpasang.", color = Color.Gray)
                    } else {
                        pairedDevices.forEach { device ->
                            @Suppress("MissingPermission")
                            val name = device.name ?: device.address
                            TextButton(
                                onClick = {
                                    onConnectDevice(device)
                                    showDeviceDialog = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(name, color = Color(0xFF4A90E2), fontSize = 16.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDeviceDialog = false }) {
                    Text("Tutup", color = Color.LightGray)
                }
            },
            containerColor = Color(0xFF222222)
        )
    }
}

@Composable
private fun ConnectionHeader(
    status: ConnectionStatus,
    onConnectClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val (indicatorColor, statusText) = when (status) {
                    is ConnectionStatus.Connected -> Color(0xFF4CAF50) to "Terhubung: ${status.deviceName}"
                    ConnectionStatus.Connecting -> Color(0xFFFFC107) to "Menghubungkan..."
                    ConnectionStatus.Disconnected -> Color(0xFF757575) to "Belum Terhubung"
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

            if (status !is ConnectionStatus.Connected) {
                Text(
                    text = "Sambungkan",
                    color = Color(0xFF4A90E2),
                    fontSize = 12.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { onConnectClick() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        // Teks "By Natanael" persis di bawah status koneksi
        Text(
            text = "By Natanael",
            color = Color(0xFF888888),
            fontSize = 10.sp,
            letterSpacing = 0.5.sp,
            modifier = Modifier.padding(start = 16.dp, top = 2.dp)
        )
    }
}

@Composable
private fun UndoButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
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
private fun MouseButton(label: String, onDown: () -> Unit, onUp: () -> Unit, modifier: Modifier = Modifier) {
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
        Text(text = label, color = if (isPressed) Color.White else Color(0xFF707070), fontSize = 32.sp)
    }
}
