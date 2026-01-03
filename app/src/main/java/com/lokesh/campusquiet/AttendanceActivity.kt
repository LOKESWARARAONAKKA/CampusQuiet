package com.lokesh.campusquiet

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class AttendanceActivity : AppCompatActivity() {

    private lateinit var attendanceStatusTv: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_attendance)

        attendanceStatusTv = findViewById(R.id.tvAttendanceStatus)
    }
}
