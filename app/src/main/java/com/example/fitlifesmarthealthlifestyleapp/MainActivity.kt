    package com.example.fitlifesmarthealthlifestyleapp

    import android.os.Bundle
    import androidx.activity.enableEdgeToEdge
    import androidx.appcompat.app.AppCompatActivity
    import androidx.core.view.ViewCompat
    import androidx.core.view.WindowInsetsCompat
    import androidx.navigation.NavController
    import androidx.navigation.findNavController
    import androidx.navigation.fragment.NavHostFragment
    import androidx.work.ExistingPeriodicWorkPolicy
    import androidx.work.PeriodicWorkRequestBuilder
    import androidx.work.WorkManager
    import com.example.fitlifesmarthealthlifestyleapp.workers.WaterReminderWorker
    import com.google.firebase.auth.FirebaseAuth
    import java.util.Calendar
    import java.util.concurrent.TimeUnit
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

                // CHỈ LẤY PADDING CỦA BÀN PHÍM (IME), KHÔNG LẤY CỦA SYSTEM BAR
                val bottomPadding = ime.bottom

                // Áp dụng padding: Lề dưới bây giờ sẽ là 0 khi không mở bàn phím
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, bottomPadding)
                insets
            }

            setupDailyReminder()

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

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {

                    requestPermissions(
                        arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                        101 // Request Code tùy chọn
                    )
                }
            }
        }

        override fun onSupportNavigateUp() : Boolean {
            return navController.navigateUp() || super.onSupportNavigateUp()
        }

        private fun setupDailyReminder() {
            // 1. Tính toán thời gian delay để chạy vào đúng 20:00 tối
            val currentTime = Calendar.getInstance()
            val dueTime = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 20) // 20 giờ
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
            }

            if (dueTime.before(currentTime)) {
                dueTime.add(Calendar.HOUR_OF_DAY, 24) // Nếu qua 20h rồi thì dời sang hôm sau
            }

            val initialDelay = dueTime.timeInMillis - currentTime.timeInMillis

            // 2. Tạo Request lặp lại mỗi 24 giờ
            val workRequest = PeriodicWorkRequestBuilder<WaterReminderWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .addTag("water_reminder")
                .build()

            // 3. Gửi cho WorkManager (Dùng KEEP để không bị trùng lặp task khi mở app nhiều lần)
            WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "daily_water_check",
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )

    //        val testRequest = androidx.work.OneTimeWorkRequestBuilder<WaterReminderWorker>()
    //            .setInitialDelay(10, TimeUnit.SECONDS) // Chờ 10 giây rồi bắn
    //            .build()
    //
    //        WorkManager.getInstance(this).enqueueUniqueWork(
    //            "test_notification_immediate",
    //            androidx.work.ExistingWorkPolicy.REPLACE, // Dùng REPLACE để đè task cũ, chạy task mới ngay
    //            testRequest
    //        )
        }
    }