package com.lokesh.campusquiet

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ktx.toObjects

@IgnoreExtraProperties
data class Student(val rollNo: String = "")

class TeacherDashboardActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var studentAdapter: StudentAdapter
    private lateinit var tvStudentCount: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_teacher_dashboard)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerViewStudents)
        tvStudentCount = findViewById(R.id.tvStudentCount)
        val logoutButton = findViewById<ImageButton>(R.id.btnLogout)
        val refreshButton = findViewById<ImageButton>(R.id.btnRefresh)

        recyclerView.layoutManager = LinearLayoutManager(this)
        studentAdapter = StudentAdapter()
        recyclerView.adapter = studentAdapter

        logoutButton.setOnClickListener {
            auth.signOut()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        refreshButton.setOnClickListener {
            Toast.makeText(this, "Refreshing...", Toast.LENGTH_SHORT).show()
            listenForPresentStudents(true)
        }

        listenForPresentStudents(false) // Initial load
    }

    private fun listenForPresentStudents(forceRefresh: Boolean) {
        val query = db.collection("students").whereEqualTo("isPresent", true)
        
        val source = if (forceRefresh) com.google.firebase.firestore.Source.SERVER else com.google.firebase.firestore.Source.DEFAULT

        query.get(source).addOnSuccessListener { snapshots ->
                val studentList = snapshots?.toObjects<Student>() ?: emptyList()
                studentAdapter.submitList(studentList)
                tvStudentCount.text = getString(R.string.student_count_format, studentList.size)
            }
            .addOnFailureListener { e ->
                Log.w("TeacherDashboard", "Listen failed.", e)
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
}

class StudentAdapter : ListAdapter<Student, StudentAdapter.StudentViewHolder>(StudentDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StudentViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.student_list_item, parent, false)
        return StudentViewHolder(view)
    }

    override fun onBindViewHolder(holder: StudentViewHolder, position: Int) {
        val student = getItem(position)
        holder.bind(student)
    }

    class StudentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val rollNoTextView: TextView = itemView.findViewById(R.id.tvStudentRollNo)

        fun bind(student: Student) {
            rollNoTextView.text = student.rollNo
        }
    }
}

class StudentDiffCallback : DiffUtil.ItemCallback<Student>() {
    override fun areItemsTheSame(oldItem: Student, newItem: Student): Boolean {
        return oldItem.rollNo == newItem.rollNo
    }

    override fun areContentsTheSame(oldItem: Student, newItem: Student): Boolean {
        return oldItem == newItem
    }
}
