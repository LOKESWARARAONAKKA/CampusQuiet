package com.lokesh.campusquiet

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val emailEt = findViewById<EditText>(R.id.etEmail)
        val passwordEt = findViewById<EditText>(R.id.etPassword)
        val loginBtn = findViewById<Button>(R.id.btnLogin)
        val registerTv = findViewById<TextView>(R.id.tvRegister)

        loginBtn.setOnClickListener {
            val email = emailEt.text.toString().trim()
            val password = passwordEt.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Enter email & password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener { authResult ->
                    val userId = authResult.user!!.uid
                    checkUserRoleAndRedirect(userId)
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Login failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }

        registerTv.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    override fun onStart() {
        super.onStart()
        val currentUser = auth.currentUser
        if (currentUser != null) {
            checkUserRoleAndRedirect(currentUser.uid)
        }
    }

    private fun checkUserRoleAndRedirect(userId: String) {
        db.collection("students").document(userId).get()
            .addOnSuccessListener { document ->
                val role = document?.getString("role")
                if (role == "teacher") {
                    startActivity(Intent(this, TeacherDashboardActivity::class.java))
                } else {
                    startActivity(Intent(this, MainActivity::class.java))
                }
                finish()
            }
            .addOnFailureListener {
                // Default to student view on failure
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
    }
}
