package com.example.fitlifesmarthealthlifestyleapp

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import com.google.firebase.auth.FirebaseAuth
import kotlin.math.max

class MainActivity : AppCompatActivity() {
    private lateinit var navController : NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            val bottomPadding = max(systemBars.bottom, ime.bottom)
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, bottomPadding)
            insets
        }

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.navHostFragmentContainerView) as NavHostFragment

        navController = navHostFragment.navController

        // 2. Tạo graph từ file XML
        val navInflater = navController.navInflater
        val graph = navInflater.inflate(R.navigation.main_nav_graph)

        // 3. Kiểm tra user đã đăng nhập chưa
        val currentUser = FirebaseAuth.getInstance().currentUser

        if (currentUser != null) {
            // Đã Login -> Vào thẳng MainFragment (Home)
            graph.setStartDestination(R.id.mainFragment)
        } else {
            // Chưa Login -> Vào LoginFragment
            graph.setStartDestination(R.id.loginFragment)
        }

        // 4. Gán graph đã chỉnh sửa vào Controller để bắt đầu chạy
        navController.graph = graph
    }

    override fun onSupportNavigateUp() : Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}