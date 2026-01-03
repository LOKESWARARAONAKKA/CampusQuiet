package com.lokesh.campusquiet

import android.app.Application
import org.altbeacon.beacon.BeaconManager
import org.altbeacon.beacon.BeaconParser

class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        val beaconManager = BeaconManager.getInstanceForApplication(this)
        // This parser is for iBeacon, which is the format your ESP32 is using.
        beaconManager.beaconParsers.add(BeaconParser().setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"))
    }
}
