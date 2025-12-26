package com.lokesh.campusquiet

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val emailEt = findViewById<EditText>(R.id.etEmail)
        val passwordEt = findViewById<EditText>(R.id.etPassword)
        val rollEt = findViewById<EditText>(R.id.etRollNo)
        val branchEt = findViewById<EditText>(R.id.etBranch)
        val sectionEt = findViewById<EditText>(R.id.etSection)
        val registerBtn = findViewById<Button>(R.id.btnRegister)

        registerBtn.setOnClickListener {

            val email = emailEt.text.toString().trim()
            val password = passwordEt.text.toString().trim()
            val rollNo = rollEt.text.toString().trim()
            val branch = branchEt.text.toString().trim()
            val section = sectionEt.text.toString().trim()

            if (email.isEmpty() || password.isEmpty() ||
                rollNo.isEmpty() || branch.isEmpty() || section.isEmpty()
            ) {
                Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener {

                    val uid = auth.currentUser!!.uid
                    val deviceId = Settings.Secure.getString(
                        contentResolver,
                        Settings.Secure.ANDROID_ID
                    )

                    val userData = hashMapOf(
                        "uid" to uid,
                        "email" to email,
                        "rollNo" to rollNo,
                        "branch" to branch,
                        "section" to section,
                        "deviceId" to deviceId
                    )

                    db.collection("students").document(uid)
                        .set(userData)
                        .addOnSuccessListener {

                            Toast.makeText(
                                this,
                                "Registration successful",
                                Toast.LENGTH_SHORT
                            ).show()

                            startActivity(
                                Intent(this, MainActivity::class.java)
                            )
                            finish()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(
                                this,
                                "Firestore error: ${e.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(
                        this,
                        "Auth error: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
        }
    }
}
