package com.example.fitlifesmarthealthlifestyleapp.ui.auth

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.fitlifesmarthealthlifestyleapp.R
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainFragment : Fragment() {

    private lateinit var bottomNavigationView : BottomNavigationView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_main, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val bottomNav = view.findViewById<BottomNavigationView>(R.id.bottom_nav)

        val navHostFragment =
            childFragmentManager.findFragmentById(R.id.homeNavHost) as NavHostFragment

        val navController = navHostFragment.navController

        bottomNav.setupWithNavController(navController)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                // 1. Các màn hình chính (Tabs) -> HIỆN thanh điều hướng
                R.id.home, R.id.activity, R.id.nutrition, R.id.social, R.id.profile -> {
                    bottomNav.visibility = View.VISIBLE
                }

                // 2. Các màn hình con của tab Profile (Workout, Edit Profile) -> ẨN thanh điều hướng
                // Đồng thời ép icon Profile luôn sáng
                R.id.workoutProgramFragment, R.id.workoutDetailFragment, R.id.editProfileFragment -> {
                    bottomNav.visibility = View.GONE
                    bottomNav.menu.findItem(R.id.profile)?.isChecked = true
                }

                // 3. Các màn hình con khác (nếu có sau này) -> ẨN thanh điều hướng
                else -> {
                    bottomNav.visibility = View.GONE
                }
            }
        }
    }
}