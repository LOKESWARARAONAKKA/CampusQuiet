package com.lokesh.campusquiet

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import org.altbeacon.beacon.BeaconManager
import org.altbeacon.beacon.Region
import java.text.DecimalFormat

class BeaconScanService : Service() {

    private lateinit var beaconManager: BeaconManager
    private val region = Region("CampusQuiet", null, null, null)

    private var isPolicyEnforced = false

    private val DISTANCE_THRESHOLD = 10.0
    private val df = DecimalFormat("#.##")

    companion object {
        const val ACTION_STATUS_UPDATE = "com.lokesh.campusquiet.STATUS_UPDATE"
        const val EXTRA_STATUS_MESSAGE = "statusMessage"
        const val CHANNEL_ID = "BeaconScanServiceChannel"
    }

    override fun onCreate() {
        super.onCreate()
        beaconManager = BeaconManager.getInstanceForApplication(this)

        // *** THIS IS THE CRITICAL FIX FOR ACCURACY ***
        // Set for more aggressive scanning to improve responsiveness.
        // We will scan for 1.1 seconds and then wait only 0.2 seconds before scanning again.
        beaconManager.foregroundScanPeriod = 1100L
        beaconManager.foregroundBetweenScanPeriod = 200L

        beaconManager.addRangeNotifier { beacons, _ ->
            val distance = if (beacons.isNotEmpty()) beacons.minByOrNull { it.distance }!!.distance else -1.0
            val isInRange = distance != -1.0 && distance < DISTANCE_THRESHOLD

            if (isInRange) {
                enforceVibrateMode()
                val statusMessage = "In Classroom (${df.format(distance)} m). Mute Policy is Active."
                broadcastStatus(statusMessage)
                updateNotification(statusMessage)
            } else {
                restoreNormalMode()
                val statusMessage = "Out of Classroom. Phone in Normal Mode."
                broadcastStatus(statusMessage)
                updateNotification(statusMessage)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        val notification = createNotification("Scanning for classroom...")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE)
        } else {
            startForeground(1, notification)
        }
        beaconManager.startRangingBeacons(region)
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        beaconManager.stopRangingBeacons(region)
        restoreNormalMode() // Final cleanup
    }

    private fun enforceVibrateMode() {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        isPolicyEnforced = true

        if (audioManager.ringerMode != AudioManager.RINGER_MODE_VIBRATE) {
            audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE
        }
    }

    private fun restoreNormalMode() {
        if (isPolicyEnforced) {
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
            isPolicyEnforced = false
        }
    }

    // --- HELPER FUNCTIONS ---

    private fun broadcastStatus(message: String) {
        val intent = Intent(ACTION_STATUS_UPDATE).apply {
            putExtra(EXTRA_STATUS_MESSAGE, message)
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    private fun updateNotification(contentText: String) {
        val notification = createNotification(contentText)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(1, notification)
    }

    private fun createNotification(contentText: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("CampusQuiet Policy Active")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Attendance Policy Service", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
