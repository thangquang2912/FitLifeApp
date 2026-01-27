package com.example.fitlifesmarthealthlifestyleapp.ui.auth

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.fitlifesmarthealthlifestyleapp.DeepLinkViewModel // [MỚI]
import com.example.fitlifesmarthealthlifestyleapp.R
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainFragment : Fragment() {

    private lateinit var deepLinkViewModel: DeepLinkViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_main, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val bottomNav = view.findViewById<BottomNavigationView>(R.id.bottom_nav)
        val navHostFragment = childFragmentManager.findFragmentById(R.id.homeNavHost) as NavHostFragment
        val navController = navHostFragment.navController

        // 1. Setup BottomNav
        bottomNav.setupWithNavController(navController)

        // 2. Logic ẩn hiện BottomBar (Giữ nguyên)
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.home, R.id.activity, R.id.nutrition, R.id.social, R.id.profile -> {
                    bottomNav.visibility = View.VISIBLE
                }
                R.id.workoutProgramFragment, R.id.workoutDetailFragment, R.id.editProfileFragment -> {
                    bottomNav.visibility = View.GONE
                    if (destination.id == R.id.editProfileFragment || destination.id == R.id.workoutProgramFragment) {
                        bottomNav.menu.findItem(R.id.profile)?.isChecked = true
                    }
                }
                else -> {
                    bottomNav.visibility = View.GONE
                }
            }
        }

        // 3. [FIX LỖI NAV] Lắng nghe ViewModel để chuyển Tab
        // Lấy ViewModel từ Activity cha
        deepLinkViewModel = ViewModelProvider(requireActivity())[DeepLinkViewModel::class.java]

        deepLinkViewModel.targetPostId.observe(viewLifecycleOwner) { postId ->
            if (postId != null) {
                // Thay vì navigate, ta "giả vờ" bấm vào nút Social
                // Việc này giúp BottomNav tự động xử lý backstack và icon chính xác
                if (bottomNav.selectedItemId != R.id.social) {
                    bottomNav.selectedItemId = R.id.social
                }
            }
        }
    }
}