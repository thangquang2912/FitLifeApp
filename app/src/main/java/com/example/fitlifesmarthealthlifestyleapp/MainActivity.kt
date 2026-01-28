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
    import com.example.fitlifesmarthealthlifestyleapp.workers.CaloriesReminderWorker
    import com.example.fitlifesmarthealthlifestyleapp.workers.StepsReminderWorker
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
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, ime.bottom)
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
            val currentTime = Calendar.getInstance()
            val dueTime = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 20)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
            }

            if (dueTime.before(currentTime)) {
                dueTime.add(Calendar.DAY_OF_YEAR, 1)
            }

            val initialDelay = dueTime.timeInMillis - currentTime.timeInMillis

            // 🔹 Water
            val waterWork = PeriodicWorkRequestBuilder<WaterReminderWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .build()

            // 🔹 Calories
            val caloriesWork = PeriodicWorkRequestBuilder<CaloriesReminderWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .build()

            // 🔹 Steps
            val stepsWork = PeriodicWorkRequestBuilder<StepsReminderWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .build()

            val workManager = WorkManager.getInstance(this)

            workManager.enqueueUniquePeriodicWork(
                "daily_water_reminder",
                ExistingPeriodicWorkPolicy.KEEP,
                waterWork
            )

            workManager.enqueueUniquePeriodicWork(
                "daily_calories_reminder",
                ExistingPeriodicWorkPolicy.KEEP,
                caloriesWork
            )

            workManager.enqueueUniquePeriodicWork(
                "daily_steps_reminder",
                ExistingPeriodicWorkPolicy.KEEP,
                stepsWork
            )
        }

    }