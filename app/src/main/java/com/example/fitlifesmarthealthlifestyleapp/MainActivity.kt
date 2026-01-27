package com.example.fitlifesmarthealthlifestyleapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.fitlifesmarthealthlifestyleapp.workers.WaterReminderWorker
import com.google.firebase.auth.FirebaseAuth
import java.util.Calendar
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    private lateinit var navController: NavController
    private lateinit var deepLinkViewModel: DeepLinkViewModel // [MỚI]

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, ime.bottom)
            insets
        }

        // 1. Khởi tạo ViewModel
        deepLinkViewModel = ViewModelProvider(this)[DeepLinkViewModel::class.java]

        // 2. Lấy ID từ Link (nếu mở app lần đầu) và nạp vào ViewModel
        val linkId = getPostIdFromIntent(intent)
        if (linkId != null) {
            deepLinkViewModel.setPostId(linkId)
        }

        setupDailyReminder()

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.navHostFragmentContainerView) as NavHostFragment
        navController = navHostFragment.navController

        val navInflater = navController.navInflater
        val graph = navInflater.inflate(R.navigation.main_nav_graph)
        val currentUser = FirebaseAuth.getInstance().currentUser

        // 3. Logic điều hướng đăng nhập
        if (currentUser != null) {
            graph.setStartDestination(R.id.mainFragment)
        } else {
            graph.setStartDestination(R.id.loginFragment)
        }

        navController.graph = graph
        checkNotificationPermission()
    }

    // Xử lý khi App đang chạy ngầm mà bấm Link
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)

        val newId = getPostIdFromIntent(intent)
        if (newId != null) {
            // Cập nhật ID mới vào ViewModel
            deepLinkViewModel.setPostId(newId)

            // Nếu user đã login, đảm bảo quay về màn hình chính để MainFragment xử lý
            if (FirebaseAuth.getInstance().currentUser != null) {
                // Pop về MainFragment nếu đang ở các trang con
                navController.popBackStack(R.id.mainFragment, false)
            }
        }
    }

    private fun getPostIdFromIntent(intent: Intent?): String? {
        val data = intent?.data
        if (data != null && data.pathSegments.contains("post")) {
            return data.lastPathSegment
        }
        return null
    }

    private fun checkNotificationPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    private fun setupDailyReminder() {
        val currentTime = Calendar.getInstance()
        val dueTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 20)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }
        if (dueTime.before(currentTime)) {
            dueTime.add(Calendar.HOUR_OF_DAY, 24)
        }
        val initialDelay = dueTime.timeInMillis - currentTime.timeInMillis

        val workRequest = PeriodicWorkRequestBuilder<WaterReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .addTag("water_reminder")
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "daily_water_check",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }
}