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
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import org.altbeacon.beacon.BeaconManager
import org.altbeacon.beacon.Region
import java.text.DecimalFormat

class BeaconScanService : Service() {

    private lateinit var beaconManager: org.altbeacon.beacon.BeaconManager
    private val region = Region("CampusQuiet", null, null, null)

    private var currentState: State = State.OUT_OF_CLASSROOM
    private var outOfRangeCounter = 0
    private val OUT_OF_RANGE_GRACE_PERIOD = 2 // ~12 seconds with new scan cycle

    private val DISTANCE_THRESHOLD = 10.0
    private val df = DecimalFormat("#.##")

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    enum class State { IN_CLASSROOM, OUT_OF_CLASSROOM }

    companion object {
        const val ACTION_STATUS_UPDATE = "com.lokesh.campusquiet.STATUS_UPDATE"
        const val EXTRA_STATUS_MESSAGE = "statusMessage"
        const val CHANNEL_ID = "BeaconScanServiceChannel"
    }

    override fun onCreate() {
        super.onCreate()
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        beaconManager = org.altbeacon.beacon.BeaconManager.getInstanceForApplication(this)

        // *** THE FINAL, CORRECTED SCANNING SETTINGS ***
        beaconManager.foregroundScanPeriod = 1100L // Scan for 1.1 seconds
        beaconManager.foregroundBetweenScanPeriod = 5000L // Rest for 5 seconds to be safe

        beaconManager.addRangeNotifier { beacons, _ ->
            val distance = if (beacons.isNotEmpty()) beacons.minByOrNull { it.distance }!!.distance else -1.0
            val isInRange = distance != -1.0 && distance < DISTANCE_THRESHOLD

            if (isInRange) {
                outOfRangeCounter = 0
                if (currentState != State.IN_CLASSROOM) {
                    Log.d("BeaconService", "State changing to IN_CLASSROOM")
                    currentState = State.IN_CLASSROOM
                    updateStudentStatus(true)
                }
                enforceVibrateMode()
            } else {
                outOfRangeCounter++
                if (outOfRangeCounter >= OUT_OF_RANGE_GRACE_PERIOD && currentState == State.IN_CLASSROOM) {
                    Log.d("BeaconService", "State changing to OUT_OF_CLASSROOM")
                    currentState = State.OUT_OF_CLASSROOM
                    restoreNormalMode()
                    updateStudentStatus(false)
                }
            }

            val statusMessage = if (currentState == State.IN_CLASSROOM) {
                "In Classroom (${df.format(distance)} m). Mute Policy is Active."
            } else {
                "Out of Classroom. Phone in Normal Mode."
            }
            broadcastStatus(statusMessage)
            updateNotification(statusMessage)
        }
    }

    private fun updateStudentStatus(present: Boolean) {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            db.collection("students").document(userId).update("isPresent", present)
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
        restoreNormalMode()
        updateStudentStatus(false)
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        restoreNormalMode()
        updateStudentStatus(false)
    }
	
    private fun enforceVibrateMode() {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (audioManager.ringerMode != AudioManager.RINGER_MODE_VIBRATE) {
            audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE
        }
    }

    private fun restoreNormalMode() {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (audioManager.ringerMode != AudioManager.RINGER_MODE_NORMAL) {
            audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
        }
    }

    private fun broadcastStatus(message: String) {
        val intent = Intent(ACTION_STATUS_UPDATE).apply { putExtra(EXTRA_STATUS_MESSAGE, message) }
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
