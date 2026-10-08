package com.dimensional.app.ui.contract

import androidx.compose.runtime.Immutable

@Immutable
data class TrackpadUiState(
    val connectionStatus: ConnectionStatus = ConnectionStatus.Disconnected,
    val isDragLockActive: Boolean = false
)

sealed interface ConnectionStatus {
    object Disconnected : ConnectionStatus
    object Connecting : ConnectionStatus
    data class Connected(val deviceName: String) : ConnectionStatus
}

sealed interface TrackpadUiEvent {
    data class PointerMoved(val deltaX: Float, val deltaY: Float, val dtMillis: Long) : TrackpadUiEvent
    object DragLockStarted : TrackpadUiEvent
    object DragLockEnded : TrackpadUiEvent
    object LeftButtonDown : TrackpadUiEvent
    object LeftButtonUp : TrackpadUiEvent
    object RightButtonDown : TrackpadUiEvent
    object RightButtonUp : TrackpadUiEvent
    object UndoTriggered : TrackpadUiEvent
}
