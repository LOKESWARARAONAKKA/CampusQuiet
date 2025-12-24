package com.lokesh.campusquiet

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
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

        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val etRollNo = findViewById<EditText>(R.id.etRollNo)
        val etBranch = findViewById<EditText>(R.id.etBranch)
        val etSection = findViewById<EditText>(R.id.etSection)
        val btnLogin = findViewById<Button>(R.id.btnLogin)

        btnLogin.setOnClickListener {

            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val rollNo = etRollNo.text.toString().trim()
            val branch = etBranch.text.toString().trim()
            val section = etSection.text.toString().trim()

            if (email.isEmpty() || password.isEmpty() ||
                rollNo.isEmpty() || branch.isEmpty() || section.isEmpty()
            ) {
                Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener {

                    val uid = auth.currentUser!!.uid

                    val studentData = hashMapOf(
                        "email" to email,
                        "rollNo" to rollNo,
                        "branch" to branch,
                        "section" to section,
                        "deviceId" to android.provider.Settings.Secure.getString(
                            contentResolver,
                            android.provider.Settings.Secure.ANDROID_ID
                        )
                    )

                    db.collection("students")
                        .document(uid)
                        .set(studentData)
                        .addOnSuccessListener {

                            startActivity(
                                Intent(this, MainActivity::class.java)
                            )
                            finish()
                        }
                }
                .addOnFailureListener {
                    Toast.makeText(
                        this,
                        "Login failed: ${it.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
        }
    }
}
