package com.lokesh.campusquiet

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var tvEmail: TextView
    private lateinit var tvRollNo: TextView
    private lateinit var tvBranch: TextView
    private lateinit var tvSection: TextView
    private lateinit var btnLogout: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        tvEmail = view.findViewById(R.id.tvProfileEmail)
        tvRollNo = view.findViewById(R.id.tvProfileRollNo)
        tvBranch = view.findViewById(R.id.tvProfileBranch)
        tvSection = view.findViewById(R.id.tvProfileSection)
        btnLogout = view.findViewById(R.id.btnLogout)

        loadUserProfile()

        btnLogout.setOnClickListener {
            // Stop the beacon service first
            val serviceIntent = Intent(requireContext(), BeaconScanService::class.java)
            requireContext().stopService(serviceIntent)

            // Sign out from Firebase
            auth.signOut()

            // Go back to the Login screen
            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            requireActivity().finish()
        }

        return view
    }

    private fun loadUserProfile() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(context, "User not found", Toast.LENGTH_SHORT).show()
            return
        }

        db.collection("students").document(userId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    tvEmail.text = "Email: ${document.getString("email")}"
                    tvRollNo.text = "Roll No: ${document.getString("rollNo")}"
                    tvBranch.text = "Branch: ${document.getString("branch")}"
                    tvSection.text = "Section: ${document.getString("section")}"
                } else {
                    Toast.makeText(context, "No profile data found", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error loading profile: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
