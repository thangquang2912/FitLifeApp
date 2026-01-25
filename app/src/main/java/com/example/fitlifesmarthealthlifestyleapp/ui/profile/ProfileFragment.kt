package com.example.fitlifesmarthealthlifestyleapp.ui.profile

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.fitlifesmarthealthlifestyleapp.R
import com.example.fitlifesmarthealthlifestyleapp.domain.model.User
import com.example.fitlifesmarthealthlifestyleapp.domain.usecase.calculateBMR
import com.example.fitlifesmarthealthlifestyleapp.domain.usecase.classifyBMI
import com.google.android.material.imageview.ShapeableImageView

class ProfileFragment : Fragment() {

    private lateinit var btnTheme : ImageButton
    private lateinit var ivAvatar: ShapeableImageView
    private lateinit var tvName : TextView
    private lateinit var cardHeight: View
    private lateinit var cardWeight: View
    private lateinit var cardAge: View
    private lateinit var tvBMI : TextView
    private lateinit var bmiCategory : TextView
    private lateinit var tvBMR : TextView
    private lateinit var bmrCategory : TextView
    private lateinit var btnEditProfile : TextView
    private lateinit var btnWorkoutPrograms : TextView
    private lateinit var btnLeaderboardChallenges : TextView
    private lateinit var btnWorkoutHistory : TextView
    private lateinit var btnLogout : TextView
    private lateinit var tvHeightLabel : TextView
    private lateinit var tvHeightValue : TextView
    private lateinit var tvWeightLabel : TextView
    private lateinit var tvWeightValue : TextView
    private lateinit var tvAgeLabel : TextView
    private lateinit var tvAgeValue : TextView
    private lateinit var tvSubtitle : TextView

    private val viewModel: ProfileViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btnTheme = view.findViewById<ImageButton>(R.id.btnTheme)
        ivAvatar = view.findViewById<ShapeableImageView>(R.id.ivAvatar)
        tvSubtitle = view.findViewById<TextView>(R.id.tvSubtitle)
        tvName = view.findViewById<TextView>(R.id.tvName)

        cardHeight = view.findViewById<View>(R.id.cardHeight)
        tvHeightLabel = cardHeight.findViewById<TextView>(R.id.tvLabel)
        tvHeightValue = cardHeight.findViewById<TextView>(R.id.tvValue)

        cardWeight = view.findViewById<View>(R.id.cardWeight)
        tvWeightLabel = cardWeight.findViewById<TextView>(R.id.tvLabel)
        tvWeightValue = cardWeight.findViewById<TextView>(R.id.tvValue)


        cardAge = view.findViewById<View>(R.id.cardAge)
        tvAgeLabel = cardAge.findViewById<TextView>(R.id.tvLabel)
        tvAgeValue = cardAge.findViewById<TextView>(R.id.tvValue)


        tvBMI = view.findViewById<TextView>(R.id.tvBMI)
        bmiCategory = view.findViewById<TextView>(R.id.bmiCategory)

        tvBMR = view.findViewById<TextView>(R.id.tvBMR)
        bmrCategory = view.findViewById<TextView>(R.id.bmrCategory)

        btnEditProfile = view.findViewById<TextView>(R.id.btnEditProfile)
        btnWorkoutPrograms = view.findViewById<TextView>(R.id.btnWorkoutPrograms)
        btnLeaderboardChallenges = view.findViewById<TextView>(R.id.btnLeaderboardChallenges)
        btnWorkoutHistory = view.findViewById<TextView>(R.id.btnWorkoutHistory)
        btnLogout = view.findViewById<TextView>(R.id.btnLogout)

        setupStaticLabels()

        if (viewModel.user.value == null) {
            viewModel.fetchUserProfile()
        }

        viewModel.user.observe(viewLifecycleOwner) { user ->
            if (user != null) {
                updateUI(user)
            }
        }

        // Lắng nghe trạng thái Loading (để hiện ProgressBar nếu cần)
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            // Ví dụ: binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        setupClickEvents()
    }

    private fun setupStaticLabels() {
        tvHeightLabel.text = "Height"
        tvWeightLabel.text = "Weight"
        tvAgeLabel.text = "Age"
    }

    private fun updateUI(user: User) {
        tvName.text = user.displayName
        tvSubtitle.text = user.email

        // Load Avatar
        Glide.with(this)
            .load(user.photoUrl)
            .placeholder(R.drawable.ic_user) // Ảnh chờ
            .error(R.drawable.ic_user)       // Ảnh lỗi
            .circleCrop()                                   // Cắt ảnh tròn
            .into(ivAvatar)

        // Cập nhật các thẻ chỉ số (Height, Weight)
        tvHeightValue.text = "${user.height} cm"
        tvWeightValue.text = "${user.weight} kg"

        // 4. Xử lý hiển thị Tuổi (Age)
        val age = user.age
        if (age <= 0) {
            // Nếu login Google chưa set ngày sinh -> Hiện nhắc nhở
            tvAgeValue.text = "--"
            tvAgeValue.setTextColor(resources.getColor(android.R.color.holo_red_light, null))
        } else {
            tvAgeValue.text = "$age"
            tvAgeValue.setTextColor(resources.getColor(R.color.black, null)) // Giả sử bạn có màu black
        }

        // Cập nhật BMI & BMR
        val bmi = user.bmi
        tvBMI.text = String.format("%.1f", bmi)
        bmiCategory.text = bmi.classifyBMI()

        // Đổi màu chữ BMI Category theo mức độ
        val colorRes = when {
            bmi < 18.5 || bmi >= 30 -> android.R.color.holo_red_dark // Gầy/Béo phì -> Đỏ
            bmi < 25 -> android.R.color.holo_green_dark           // Bình thường -> Xanh
            else -> android.R.color.holo_orange_dark              // Thừa cân -> Cam
        }
        bmiCategory.setTextColor(resources.getColor(colorRes, null))

        // BMR
        val bmr = user.calculateBMR()
        tvBMR.text = bmr.toString()
    }

    private fun setupClickEvents() {
        // Nút Edit Profile -> Mở màn hình chỉnh sửa
        btnEditProfile.setOnClickListener {
            val currentUser = viewModel.user.value

            if (currentUser != null) {
                // 2. Tạo Action kèm theo gói dữ liệu (User)
                val action = ProfileFragmentDirections.actionProfileToEditProfile(currentUser)

                // 3. Thực hiện điều hướng
                findNavController().navigate(action)
            } else {
                // Trường hợp mạng lag chưa tải xong user
                Toast.makeText(context, "Please wait, loading data...", Toast.LENGTH_SHORT).show()
            }
        }

        // Nút Logout -> Đăng xuất và về màn hình Login
        btnLogout.setOnClickListener {
            viewModel.signOut()

            val rootNavController = requireActivity().findNavController(R.id.navHostFragmentContainerView)

            val navOptions = androidx.navigation.NavOptions.Builder()
                .setPopUpTo(R.id.main_nav_graph, true) // Xóa toàn bộ stack của main_nav_graph
                .build()

            rootNavController.navigate(R.id.loginFragment, null, navOptions)
        }

        btnWorkoutPrograms.setOnClickListener {
            try {
                findNavController().navigate(R.id.workoutProgramFragment)
            } catch (e: Exception) {
                Toast.makeText(context, "Chưa thiết lập Navigation!", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        }

    }
}