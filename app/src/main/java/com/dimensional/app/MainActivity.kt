package com.dimensional.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import com.dimensional.app.bluetooth.HidDeviceManager
import com.dimensional.app.bluetooth.HidReportDescriptor
import com.dimensional.app.ui.TrackpadScreen
import com.dimensional.app.ui.contract.ConnectionStatus
import com.dimensional.app.ui.contract.TrackpadUiEvent
import com.dimensional.app.ui.contract.TrackpadUiState
import kotlin.math.hypot
import kotlin.math.pow

class MainActivity : ComponentActivity() {

    private lateinit var hidManager: HidDeviceManager
    private var currentButtonMask: Byte = HidReportDescriptor.MOUSE_BTN_NONE

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val isGranted = permissions.values.all { it }
        if (isGranted) {
            hidManager.init()
        } else {
            Toast.makeText(this, "Izin Bluetooth diperlukan untuk menghubungkan ke PC", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        hidManager = HidDeviceManager(this)
        checkAndRequestPermissions()

        setContent {
            val connectionStatus by hidManager.connectionStatus.collectAsState()
            var isDragLockActive by remember { mutableStateOf(false) }

            val uiState = TrackpadUiState(
                connectionStatus = connectionStatus,
                isDragLockActive = isDragLockActive
            )

            TrackpadScreen(
                state = uiState,
                onEvent = { event ->
                    when (event) {
                        is TrackpadUiEvent.PointerMoved -> {
                            val (scaledDx, scaledDy) = calculatePointerDelta(event.deltaX, event.deltaY, event.dtMillis)
                            hidManager.sendMouseInput(currentButtonMask, scaledDx, scaledDy)
                        }
                        is TrackpadUiEvent.DragLockStarted -> {
                            isDragLockActive = true
                            currentButtonMask = HidReportDescriptor.MOUSE_BTN_LEFT
                            hidManager.sendMouseInput(currentButtonMask, 0, 0)
                        }
                        is TrackpadUiEvent.DragLockEnded -> {
                            isDragLockActive = false
                            currentButtonMask = HidReportDescriptor.MOUSE_BTN_NONE
                            hidManager.sendMouseInput(currentButtonMask, 0, 0)
                        }
                        is TrackpadUiEvent.LeftButtonDown -> {
                            currentButtonMask = (currentButtonMask.toInt() or HidReportDescriptor.MOUSE_BTN_LEFT.toInt()).toByte()
                            hidManager.sendMouseInput(currentButtonMask, 0, 0)
                        }
                        is TrackpadUiEvent.LeftButtonUp -> {
                            currentButtonMask = (currentButtonMask.toInt() and HidReportDescriptor.MOUSE_BTN_LEFT.toInt().inv()).toByte()
                            hidManager.sendMouseInput(currentButtonMask, 0, 0)
                        }
                        is TrackpadUiEvent.RightButtonDown -> {
                            currentButtonMask = (currentButtonMask.toInt() or HidReportDescriptor.MOUSE_BTN_RIGHT.toInt()).toByte()
                            hidManager.sendMouseInput(currentButtonMask, 0, 0)
                        }
                        is TrackpadUiEvent.RightButtonUp -> {
                            currentButtonMask = (currentButtonMask.toInt() and HidReportDescriptor.MOUSE_BTN_RIGHT.toInt().inv()).toByte()
                            hidManager.sendMouseInput(currentButtonMask, 0, 0)
                        }
                        is TrackpadUiEvent.UndoTriggered -> {
                            hidManager.sendUndoMacro()
                        }
                    }
                }
            )
        }
    }

    private fun checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val permissions = arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_ADVERTISE
            )
            val allGranted = permissions.all {
                ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
            }
            if (allGranted) {
                hidManager.init()
            } else {
                permissionLauncher.launch(permissions)
            }
        } else {
            hidManager.init()
        }
    }

    private fun calculatePointerDelta(rawDx: Float, rawDy: Float, dtMillis: Long): Pair<Byte, Byte> {
        if (dtMillis <= 0L) return Pair(0, 0)
        val distance = hypot(rawDx, rawDy)
        val velocity = distance / dtMillis
        val accelFactor = 1.25f * (1f + velocity.pow(1.3f))
        
        val dx = (rawDx * accelFactor).coerceIn(-127f, 127f).toInt().toByte()
        val dy = (rawDy * accelFactor).coerceIn(-127f, 127f).toInt().toByte()
        return Pair(dx, dy)
    }

    override fun onDestroy() {
        super.onDestroy()
        hidManager.release()
    }
}
