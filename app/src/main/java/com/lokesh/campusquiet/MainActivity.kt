package com.lokesh.campusquiet

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private val homeFragment = HomeFragment()
    private val profileFragment = ProfileFragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val bottomNavigation: BottomNavigationView = findViewById(R.id.bottom_navigation)
        bottomNavigation.setOnNavigationItemSelectedListener(navListener)

        // Set the default fragment
        supportFragmentManager.beginTransaction().replace(R.id.fragment_container, homeFragment).commit()
    }

    private val navListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        lateinit var selectedFragment: Fragment
        when (item.itemId) {
            R.id.navigation_home -> selectedFragment = homeFragment
            R.id.navigation_profile -> selectedFragment = profileFragment
        }
        supportFragmentManager.beginTransaction().replace(R.id.fragment_container, selectedFragment).commit()
        true
    }
}
