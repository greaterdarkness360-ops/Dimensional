package com.dimensional.app.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.Context
import com.dimensional.app.ui.contract.ConnectionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.Executors

@SuppressLint("MissingPermission")
class HidDeviceManager(private val context: Context) {

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
    private var hidDevice: BluetoothHidDevice? = null
    private var connectedHost: BluetoothDevice? = null

    private val _connectionStatus = MutableStateFlow<ConnectionStatus>(ConnectionStatus.Disconnected)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus

    private val executor = Executors.newSingleThreadExecutor()

    private val profileServiceListener = object : BluetoothProfile.ServiceListener {
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
            if (profile == BluetoothProfile.HID_DEVICE) {
                hidDevice = proxy as BluetoothHidDevice
                registerHidApp()
            }
        }

        override fun onServiceDisconnected(profile: Int) {
            if (profile == BluetoothProfile.HID_DEVICE) {
                hidDevice = null
                _connectionStatus.value = ConnectionStatus.Disconnected
            }
        }
    }

    private val hidCallback = object : BluetoothHidDevice.Callback() {
        override fun onConnectionStateChanged(device: BluetoothDevice, state: Int) {
            when (state) {
                BluetoothProfile.STATE_CONNECTED -> {
                    connectedHost = device
                    _connectionStatus.value = ConnectionStatus.Connected(device.name ?: device.address)
                }
                BluetoothProfile.STATE_CONNECTING -> {
                    _connectionStatus.value = ConnectionStatus.Connecting
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    connectedHost = null
                    _connectionStatus.value = ConnectionStatus.Disconnected
                }
            }
        }

        override fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, registered: Boolean) {
            if (registered) {
                if (pluggedDevice != null) {
                    hidDevice?.connect(pluggedDevice)
                }
            }
        }

        override fun onGetReport(device: BluetoothDevice?, type: Byte, id: Byte, bufferSize: Int) {
            if (device != null) {
                hidDevice?.reportError(device, BluetoothHidDevice.ERROR_RSP_SUCCESS)
            }
        }

        override fun onSetReport(device: BluetoothDevice?, type: Byte, id: Byte, data: ByteArray?) {
            if (device != null) {
                hidDevice?.reportError(device, BluetoothHidDevice.ERROR_RSP_SUCCESS)
            }
        }
    }

    fun init() {
        if (bluetoothAdapter?.isEnabled == true) {
            val supported = bluetoothAdapter.getProfileProxy(context, profileServiceListener, BluetoothProfile.HID_DEVICE)
            if (!supported) {
                _connectionStatus.value = ConnectionStatus.Disconnected
            }
        }
    }

    private fun registerHidApp() {
        val sdpSettings = BluetoothHidDeviceAppSdpSettings(
            "Dimensional Mouse",
            "Bluetooth Virtual Trackpad",
            "Dimensional",
            BluetoothHidDevice.SUBCLASS1_MOUSE, // Kunci: Memberitahu tablet bahwa ini adalah mouse
            HidReportDescriptor.COMPOSITE_DESCRIPTOR
        )
        hidDevice?.registerApp(sdpSettings, null, null, executor, hidCallback)
    }

    fun connectToDevice(device: BluetoothDevice): Boolean {
        return hidDevice?.connect(device) ?: false
    }

    fun sendMouseInput(buttonMask: Byte, deltaX: Byte, deltaY: Byte) {
        val host = connectedHost ?: return
        val report = byteArrayOf(buttonMask, deltaX, deltaY)
        hidDevice?.sendReport(host, HidReportDescriptor.REPORT_ID_MOUSE, report)
    }

    fun sendUndoMacro() {
        val host = connectedHost ?: return
        val keyDown = byteArrayOf(
            HidReportDescriptor.KEY_MOD_LCTRL,
            0x00.toByte(),
            HidReportDescriptor.KEY_Z, 0x00, 0x00, 0x00, 0x00, 0x00
        )
        hidDevice?.sendReport(host, HidReportDescriptor.REPORT_ID_KEYBOARD, keyDown)

        val keyUp = ByteArray(8) { 0x00 }
        hidDevice?.sendReport(host, HidReportDescriptor.REPORT_ID_KEYBOARD, keyUp)
    }

    fun release() {
        bluetoothAdapter?.closeProfileProxy(BluetoothProfile.HID_DEVICE, hidDevice)
    }
}
