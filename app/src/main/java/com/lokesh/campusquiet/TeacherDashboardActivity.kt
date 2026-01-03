package com.lokesh.campusquiet

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ktx.toObjects

// 1. Data Class for our Student model
@IgnoreExtraProperties
data class Student(val rollNo: String = "")

// 2. The main Activity for the Dashboard
class TeacherDashboardActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var studentAdapter: StudentAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_teacher_dashboard)

        db = FirebaseFirestore.getInstance()

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerViewStudents)
        recyclerView.layoutManager = LinearLayoutManager(this)
        studentAdapter = StudentAdapter(emptyList())
        recyclerView.adapter = studentAdapter

        val refreshButton = findViewById<ImageButton>(R.id.btnRefresh)
        refreshButton.setOnClickListener {
            Toast.makeText(this, "Refreshing...", Toast.LENGTH_SHORT).show()
            listenForPresentStudents()
        }

        listenForPresentStudents()
    }

    private fun listenForPresentStudents() {
        db.collection("students")
            .whereEqualTo("isPresent", true)
            .get() // Use .get() for a one-time fetch
            .addOnSuccessListener { snapshots ->
                if (snapshots != null) {
                    val studentList = snapshots.toObjects<Student>()
                    studentAdapter.updateStudents(studentList)
                } else {
                    studentAdapter.updateStudents(emptyList())
                }
            }
            .addOnFailureListener { e ->
                Log.w("TeacherDashboard", "Listen failed.", e)
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
}

// 3. The Adapter for the RecyclerView
class StudentAdapter(private var studentList: List<Student>) : RecyclerView.Adapter<StudentAdapter.StudentViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StudentViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.student_list_item, parent, false)
        return StudentViewHolder(view)
    }

    override fun onBindViewHolder(holder: StudentViewHolder, position: Int) {
        val student = studentList[position]
        holder.bind(student)
    }

    override fun getItemCount() = studentList.size

    fun updateStudents(newStudents: List<Student>) {
        studentList = newStudents
        notifyDataSetChanged()
    }

    class StudentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val rollNoTextView: TextView = itemView.findViewById(R.id.tvStudentRollNo)

        fun bind(student: Student) {
            rollNoTextView.text = student.rollNo
        }
    }
}
