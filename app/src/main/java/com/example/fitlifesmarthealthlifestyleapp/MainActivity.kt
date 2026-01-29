    package com.example.fitlifesmarthealthlifestyleapp

    import android.Manifest
    import android.content.Context
    import android.content.Intent
    import android.content.pm.PackageManager
    import android.os.Build
    import android.os.Bundle
    import androidx.activity.enableEdgeToEdge
    import androidx.appcompat.app.AppCompatActivity
    import androidx.core.app.ActivityCompat
    import androidx.core.content.ContextCompat
    import androidx.core.view.ViewCompat
    import androidx.lifecycle.ViewModelProvider
    import androidx.core.view.WindowInsetsCompat
    import androidx.lifecycle.lifecycleScope
    import androidx.navigation.NavController
    import androidx.navigation.findNavController
    import androidx.navigation.fragment.NavHostFragment
    import androidx.work.ExistingPeriodicWorkPolicy
    import androidx.work.PeriodicWorkRequestBuilder
    import androidx.work.WorkManager
    import com.example.fitlifesmarthealthlifestyleapp.data.repository.StepRepository
    import com.example.fitlifesmarthealthlifestyleapp.domain.service.StepSensorManager
    import com.example.fitlifesmarthealthlifestyleapp.workers.WaterReminderWorker
    import com.example.fitlifesmarthealthlifestyleapp.workers.CaloriesReminderWorker
    import com.example.fitlifesmarthealthlifestyleapp.workers.StepsReminderWorker
    import com.example.fitlifesmarthealthlifestyleapp.domain.utils.LanguagePreference
    import com.example.fitlifesmarthealthlifestyleapp.domain.utils.LanguageHelper
    import com.google.firebase.auth.FirebaseAuth
    import kotlinx.coroutines.launch
    import java.util.Calendar
    import java.util.concurrent.TimeUnit
    import kotlin.math.max

    class MainActivity : AppCompatActivity() {
        private lateinit var navController : NavController
        private lateinit var deepLinkViewModel: DeepLinkViewModel

        // --- Quản lý đếm bước chân toàn cục ---
        private var stepSensorManager: StepSensorManager? = null
        private val stepRepository = StepRepository()

        override fun attachBaseContext(newBase: Context) {
            val languagePreference = LanguagePreference(newBase)
            val language = languagePreference.getLanguage()

            val context = LanguageHelper.setLocale(newBase, language)

            super.attachBaseContext(context)
        }

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

            if (currentUser != null) {
                graph.setStartDestination(R.id.mainFragment)
                // Nếu đã đăng nhập, bắt đầu đếm bước chân ngay
                checkAndStartStepCounter()
            } else {
                graph.setStartDestination(R.id.loginFragment)
            }

            navController.graph = graph

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {

                    requestPermissions(
                        arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                        101
                    )
                }
            }
        }

        // Kiểm tra quyền và khởi động cảm biến đếm bước
        private fun checkAndStartStepCounter() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION)
                    == PackageManager.PERMISSION_GRANTED) {
                    startStepCounter()
                } else {
                    ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACTIVITY_RECOGNITION), 102)
                }
            } else {
                startStepCounter()
            }
        }

        private fun startStepCounter() {
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
            if (stepSensorManager == null) {
                stepSensorManager = StepSensorManager(this) { delta ->
                    // Cộng dồn bước chân vào Firestore ngay khi phát hiện di chuyển
                    lifecycleScope.launch {
                        stepRepository.incrementSteps(uid, delta)
                    }
                }
            }
            stepSensorManager?.startListening()
        }

        override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            if (requestCode == 102 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startStepCounter()
            }
        }

        override fun onResume() {
            super.onResume()
            // Tiếp tục lắng nghe khi app vào foreground
            if (FirebaseAuth.getInstance().currentUser != null) {
                stepSensorManager?.startListening()
            }
        }

        override fun onDestroy() {
            super.onDestroy()
            stepSensorManager?.stopListening()
        }

        override fun onNewIntent(intent: Intent) {
            super.onNewIntent(intent)
            setIntent(intent)
            val newId = getPostIdFromIntent(intent)
            if (newId != null) {
                deepLinkViewModel.setPostId(newId)
                if (FirebaseAuth.getInstance().currentUser != null) {
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

            val waterWork = PeriodicWorkRequestBuilder<WaterReminderWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .build()

            val caloriesWork = PeriodicWorkRequestBuilder<CaloriesReminderWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .build()

            val stepsWork = PeriodicWorkRequestBuilder<StepsReminderWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .addTag("water_reminder")
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