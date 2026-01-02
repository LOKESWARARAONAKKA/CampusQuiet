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

    private lateinit var beaconManager: BeaconManager
    private val region = Region("CampusQuiet", null, null, null)
    private var currentState: State = State.OUT_OF_CLASSROOM

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
        beaconManager = BeaconManager.getInstanceForApplication(this)

        beaconManager.foregroundScanPeriod = 1100L
        beaconManager.foregroundBetweenScanPeriod = 200L

        beaconManager.addRangeNotifier { beacons, _ ->
            val distance = if (beacons.isNotEmpty()) beacons.minByOrNull { it.distance }!!.distance else -1.0
            val newState = if (distance != -1.0 && distance < DISTANCE_THRESHOLD) State.IN_CLASSROOM else State.OUT_OF_CLASSROOM

            if (newState != currentState) {
                Log.d("BeaconService", "State changed from $currentState to $newState")
                currentState = newState

                if (currentState == State.IN_CLASSROOM) {
                    setVibrateMode()
                    updateStudentStatus(true)
                } else {
                    setNormalMode()
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
                .addOnSuccessListener { Log.d("BeaconService", "Successfully set isPresent to $present for user $userId") }
                .addOnFailureListener { e -> Log.e("BeaconService", "Failed to update isPresent for user $userId", e) }
        } else {
            Log.w("BeaconService", "Cannot update status, user is not logged in.")
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
        setNormalMode()
        updateStudentStatus(false) // Final cleanup on service destroy
    }
    
    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        Log.d("BeaconService", "Task removed, cleaning up.")
        setNormalMode()
        updateStudentStatus(false)
        stopSelf()
    }

    private fun setVibrateMode() {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (audioManager.ringerMode != AudioManager.RINGER_MODE_VIBRATE) {
            audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE
        }
    }

    private fun setNormalMode() {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (audioManager.ringerMode != AudioManager.RINGER_MODE_NORMAL) {
            audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
        }
    }

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
