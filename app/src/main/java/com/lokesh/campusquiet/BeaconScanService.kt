package com.lokesh.campusquiet

import android.Manifest
import android.app.*
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.*
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat

class BeaconScanService : Service() {

    private val CHANNEL_ID = "CampusQuietChannel"
    private val NOTIFICATION_ID = 1

    // 🔁 Replace with your ESP32 MAC
    private val TARGET_MAC = "4C:C3:82:BF:6D:AE"

    private val bluetoothAdapter: BluetoothAdapter? =
        BluetoothAdapter.getDefaultAdapter()

    private lateinit var audioManager: AudioManager

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val rssi = result.rssi
            Log.d("BeaconScan", "RSSI = $rssi")

            if (rssi > -85) {
                audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
            } else {
                audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager

        // ✅ MUST be called immediately
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())

        startScanning()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    private fun startScanning() {

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e("BeaconScan", "BLUETOOTH_SCAN permission missing")
            stopSelf()
            return
        }

        val scanner = bluetoothAdapter?.bluetoothLeScanner ?: return

        val filter = ScanFilter.Builder()
            .setDeviceAddress(TARGET_MAC)
            .build()

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        scanner.startScan(listOf(filter), settings, scanCallback)
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("CampusQuiet Active")
            .setContentText("Monitoring silent zone")
            .setSmallIcon(android.R.drawable.ic_lock_silent_mode)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "CampusQuiet Background Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
