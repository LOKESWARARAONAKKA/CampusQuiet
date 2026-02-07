package com.lokesh.campusquiet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class StudentListFragment : Fragment() {

    private lateinit var studentAdapter: StudentAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_student_list, container, false)
        val recyclerView = view.findViewById<RecyclerView>(R.id.student_list_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(context)
        
        // --- THIS IS THE CRITICAL FIX ---
        // The new StudentAdapter has an empty constructor.
        studentAdapter = StudentAdapter()
        recyclerView.adapter = studentAdapter
        return view
    }

    fun updateList(newStudents: List<Student>) {
        // The new, efficient ListAdapter uses `submitList` to handle updates.
        if (::studentAdapter.isInitialized) {
            studentAdapter.submitList(newStudents)
        }
    }
}
