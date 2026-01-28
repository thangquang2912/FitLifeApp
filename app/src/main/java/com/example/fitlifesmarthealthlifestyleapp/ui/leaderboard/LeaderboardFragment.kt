package com.example.fitlifesmarthealthlifestyleapp.ui.leaderboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fitlifesmarthealthlifestyleapp.R
import com.example.fitlifesmarthealthlifestyleapp.domain.model.LeaderboardUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class LeaderboardFragment : Fragment() {

    private lateinit var rvLeaderboard: RecyclerView
    private lateinit var adapter: LeaderboardAdapter
    private lateinit var progressBar: ProgressBar
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Lưu ý: Bạn cần xóa cái ToggleGroup trong file XML đi nhé
        return inflater.inflate(R.layout.fragment_leaderboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvLeaderboard = view.findViewById(R.id.rvLeaderboard)
        progressBar = view.findViewById(R.id.progressBar)
        val btnBack = view.findViewById<ImageButton>(R.id.btnBack)

        // Setup Adapter (Mặc định mode false = hiển thị KM)
        adapter = LeaderboardAdapter(emptyList(), isSteps = false)
        rvLeaderboard.layoutManager = LinearLayoutManager(context)
        rvLeaderboard.adapter = adapter

        btnBack.setOnClickListener { findNavController().navigateUp() }

        // Tải dữ liệu
        loadLeaderboardData()
    }

    private fun loadLeaderboardData() {
        progressBar.visibility = View.VISIBLE

        db.collection("users")
            .orderBy("totalDistanceKm", Query.Direction.DESCENDING) // Xếp hạng theo KM
            .limit(50)
            .get()
            .addOnSuccessListener { documents ->
                val userList = documents.map { doc ->
                    val dist = doc.getDouble("totalDistanceKm") ?: 0.0

                    // Tự động tính số bước chân từ KM để hiển thị cho sinh động (1km ~ 1300 bước)
                    val estimatedSteps = (dist * 1300).toLong()

                    LeaderboardUser(
                        id = doc.id,
                        name = doc.getString("displayName") ?: doc.getString("name") ?: "Unknown",
                        avatarUrl = doc.getString("photoUrl") ?: "",
                        totalSteps = estimatedSteps, // Fake steps từ Distance
                        totalDistanceKm = dist
                    )
                }
                adapter.updateData(userList, isStepsMode = false) // False để hiển thị đơn vị Km
                progressBar.visibility = View.GONE
            }
            .addOnFailureListener {
                progressBar.visibility = View.GONE
            }
    }
}