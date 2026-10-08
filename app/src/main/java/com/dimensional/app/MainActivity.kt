package com.dimensional.app

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import com.dimensional.app.ui.TrackpadScreen
import com.dimensional.app.ui.contract.ConnectionStatus
import com.dimensional.app.ui.contract.TrackpadUiEvent
import com.dimensional.app.ui.contract.TrackpadUiState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            var uiState by remember {
                mutableStateOf(
                    TrackpadUiState(
                        connectionStatus = ConnectionStatus.Disconnected,
                        isDragLockActive = false
                    )
                )
            }

            TrackpadScreen(
                state = uiState,
                onEvent = { event ->
                    when (event) {
                        is TrackpadUiEvent.PointerMoved -> {
                            // Event kursor bergerak (siap dihubungkan ke Bluetooth HID)
                        }
                        is TrackpadUiEvent.DragLockStarted -> {
                            uiState = uiState.copy(isDragLockActive = true)
                        }
                        is TrackpadUiEvent.DragLockEnded -> {
                            uiState = uiState.copy(isDragLockActive = false)
                        }
                        is TrackpadUiEvent.LeftButtonDown -> {
                            // Left click ditekan
                        }
                        is TrackpadUiEvent.LeftButtonUp -> {
                            // Left click dilepas
                        }
                        is TrackpadUiEvent.RightButtonDown -> {
                            // Right click ditekan
                        }
                        is TrackpadUiEvent.RightButtonUp -> {
                            // Right click dilepas
                        }
                        is TrackpadUiEvent.UndoTriggered -> {
                            Toast.makeText(this, "Shortcut RE (Ctrl + Z) dipicu", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )
        }
    }
}
