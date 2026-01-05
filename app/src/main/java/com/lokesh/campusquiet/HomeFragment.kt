package com.lokesh.campusquiet

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager

class HomeFragment : Fragment() {

    private lateinit var zoneCircle: FrameLayout
    private lateinit var tvZoneStatus: TextView
    private lateinit var tvZoneSubtitle: TextView
    private lateinit var tvAttendanceStatusDetail: TextView
    private lateinit var tvBeaconStatus: TextView
    private lateinit var tvLocationPermission: TextView
    private lateinit var tvBluetoothPermission: TextView
    private lateinit var tvNotificationPermission: TextView
    private lateinit var tvDndPermission: TextView

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val message = intent.getStringExtra(BeaconScanService.EXTRA_STATUS_MESSAGE)
            tvBeaconStatus.text = message

            val isInClassroom = message?.contains("In Classroom") ?: false
            updateZoneUi(isInClassroom)
        }
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            // After the user responds, this will be called.
            // We re-run the checks to update the UI and proceed if permissions are granted.
            checkAndRequestPermissions()
        }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        // Initialize all views
        zoneCircle = view.findViewById(R.id.zone_circle)
        tvZoneStatus = view.findViewById(R.id.tvZoneStatus)
        tvZoneSubtitle = view.findViewById(R.id.tvZoneSubtitle)
        tvAttendanceStatusDetail = view.findViewById(R.id.tvAttendanceStatusDetail)
        tvBeaconStatus = view.findViewById(R.id.tvBeaconStatus)
        tvLocationPermission = view.findViewById(R.id.tvLocationPermission)
        tvBluetoothPermission = view.findViewById(R.id.tvBluetoothPermission)
        tvNotificationPermission = view.findViewById(R.id.tvNotificationPermission)
        tvDndPermission = view.findViewById(R.id.tvDndPermission)
        return view
    }

    override fun onResume() {
        super.onResume()
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(receiver, IntentFilter(BeaconScanService.ACTION_STATUS_UPDATE))
        // Every time the user returns to the screen, check permissions.
        checkAndRequestPermissions()
    }

    override fun onPause() {
        super.onPause()
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(receiver)
    }

    private fun checkAndRequestPermissions() {
        updateAllPermissionUis()

        // Location Permission
        if (!hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
            showPermissionRationaleDialog("Location", "CampusQuiet needs location access to find nearby beacons and determine if you are in a classroom.") { 
                requestPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))
            }
            return
        }

        // Bluetooth Permissions (for newer Android versions)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && (!hasPermission(Manifest.permission.BLUETOOTH_SCAN) || !hasPermission(Manifest.permission.BLUETOOTH_CONNECT))) {
            showPermissionRationaleDialog("Bluetooth", "CampusQuiet needs Bluetooth access to scan for classroom beacons.") {
                requestPermissionLauncher.launch(arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT))
            }
            return
        }
        
        // Notification Permission (for Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasPermission(Manifest.permission.POST_NOTIFICATIONS)) {
            showPermissionRationaleDialog("Notifications", "CampusQuiet uses notifications to show the current attendance status while running in the background.") { 
                requestPermissionLauncher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
            }
            return
        }

        // Do Not Disturb Permission (special case)
        val dndManager = requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        if (!dndManager.isNotificationPolicyAccessGranted) {
            showDndPermissionDialog()
            return
        }

        // If we get here, all permissions are granted.
        startBeaconService()
    }

    private fun showPermissionRationaleDialog(name: String, message: String, onAccept: () -> Unit) {
        AlertDialog.Builder(requireContext())
            .setTitle("$name Permission Needed")
            .setMessage(message)
            .setPositiveButton("Grant") { _, _ -> onAccept() }
            .setNegativeButton("Cancel") { dialog, _ -> 
                dialog.dismiss()
                Toast.makeText(context, "$name permission is required for the app to function.", Toast.LENGTH_LONG).show()
            }
            .setCancelable(false)
            .show()
    }

    private fun showDndPermissionDialog() {
        showPermissionRationaleDialog("Do Not Disturb", "To automatically manage your phone's ringer, CampusQuiet needs 'Do Not Disturb' access. Please grant this on the next screen.") {
            val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
            startActivity(intent)
        }
    }

    private fun startBeaconService() {
        val intent = Intent(requireContext(), BeaconScanService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            requireContext().startForegroundService(intent)
        } else {
            requireContext().startService(intent)
        }
    }
    
    private fun updateAllPermissionUis() {
        updatePermissionUi(tvLocationPermission, "Location", hasPermission(Manifest.permission.ACCESS_FINE_LOCATION))
        val hasBt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) hasPermission(Manifest.permission.BLUETOOTH_SCAN) else true
        updatePermissionUi(tvBluetoothPermission, "Bluetooth", hasBt)
        val hasNotifications = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) hasPermission(Manifest.permission.POST_NOTIFICATIONS) else true
        updatePermissionUi(tvNotificationPermission, "Notifications", hasNotifications)
        val dndManager = requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        updatePermissionUi(tvDndPermission, "Do Not Disturb", dndManager.isNotificationPolicyAccessGranted)
    }

    private fun updatePermissionUi(textView: TextView, name: String, granted: Boolean) {
        textView.text = "$name: ${if (granted) "Granted" else "Denied"}"
        val icon = if (granted) R.drawable.ic_check_circle else R.drawable.ic_warning
        textView.setCompoundDrawablesWithIntrinsicBounds(icon, 0, 0, 0)
    }
    
    private fun updateZoneUi(inZone: Boolean) {
        if (inZone) {
            zoneCircle.background = ContextCompat.getDrawable(requireContext(), R.drawable.circle_background_green)
            tvZoneStatus.text = "IN-ZONE"
            tvZoneSubtitle.text = "Classroom Policy is Active"
            tvAttendanceStatusDetail.text = "PRESENT"
            tvAttendanceStatusDetail.setTextColor(ContextCompat.getColor(requireContext(), R.color.green))
        } else {
            zoneCircle.background = ContextCompat.getDrawable(requireContext(), R.drawable.circle_background_gray)
            tvZoneStatus.text = "OUT-OF-ZONE"
            tvZoneSubtitle.text = "Searching for Classroom..."
            tvAttendanceStatusDetail.text = "ABSENT"
            tvAttendanceStatusDetail.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray))
        }
    }

    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED
    }
}
