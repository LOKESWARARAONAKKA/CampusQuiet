package com.lokesh.campusquiet

import android.app.Application
import org.altbeacon.beacon.BeaconManager
import org.altbeacon.beacon.BeaconParser

class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Passing an empty string disables the online update.
        BeaconManager.setDistanceModelUpdateUrl("")

        // Now it is safe to get the instance and set up the parser.
        val beaconManager = BeaconManager.getInstanceForApplication(this)
        beaconManager.beaconParsers.add(BeaconParser().setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"))
    }
}
