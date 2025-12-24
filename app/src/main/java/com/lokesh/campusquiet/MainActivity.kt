package com.lokesh.campusquiet

import android.Manifest
import android.app.NotificationManager
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.*
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat

class MainActivity
    : AppCompatActivity() {

    private val btAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private lateinit var audioManager: AudioManager
    private lateinit var statusText: TextView

    private val TARGET_MAC = "4C:C3:82:BF:6D:AE"   // change to ESP32 MAC

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            handleScan(result)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        statusText = findViewById(R.id.statusText)

        val btnStart = findViewById<Button>(R.id.btnStart)
        btnStart.setOnClickListener {
            requestAllPermissions()
        }
    }

    private fun requestAllPermissions() {
        val perms = arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION
        )

        ActivityCompat.requestPermissions(this, perms, 100)

        if (!notificationAccessGranted()) {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
        }

        startScanning()
    }

    private fun notificationAccessGranted(): Boolean {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        return nm.isNotificationPolicyAccessGranted
    }

    private fun startScanning() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
            != PackageManager.PERMISSION_GRANTED) {
            statusText.text = "Scan permission not granted"
            return
        }

        val scanner = btAdapter?.bluetoothLeScanner ?: return

        statusText.text = "Scanning for ESP32…"

        val filter = ScanFilter.Builder()
            .setDeviceAddress(TARGET_MAC)
            .build()

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        scanner.startScan(listOf(filter), settings, scanCallback)
    }

    private fun handleScan(result: ScanResult) {
        val rssi = result.rssi
        Log.d("BLE", "Detected ${result.device.address} – RSSI: $rssi")

        if (rssi > -85) {
            audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
            statusText.text = "Inside the  range → Silent Mode"
        } else {
            audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
            statusText.text = "Outoff the range → Normal Mode"
        }
    }
}
