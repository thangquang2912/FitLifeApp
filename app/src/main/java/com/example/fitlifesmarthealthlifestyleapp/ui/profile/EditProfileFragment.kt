package com.example.fitlifesmarthealthlifestyleapp.ui.profile

import android.app.Dialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import com.bumptech.glide.Glide
import com.example.fitlifesmarthealthlifestyleapp.R
import com.example.fitlifesmarthealthlifestyleapp.domain.model.User
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.core.widget.addTextChangedListener
import androidx.activity.result.PickVisualMediaRequest
import com.google.android.material.bottomsheet.BottomSheetDialog

class EditProfileFragment : BottomSheetDialogFragment() {

    private val viewModel: ProfileViewModel by activityViewModels()
    private var selectedImageUri: Uri? = null
    private lateinit var currentUser: User
    private lateinit var btnClose : ImageView
    private lateinit var ivAvatarEdit : ShapeableImageView
    private lateinit var etName : TextInputEditText
    private lateinit var btnMale : MaterialButton
    private lateinit var btnFemale : MaterialButton
    private lateinit var btnOther : MaterialButton
    private lateinit var etDob : TextInputEditText
    private lateinit var etWeight : TextInputEditText
    private lateinit var etHeight : TextInputEditText
    private lateinit var btnSave : MaterialButton


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog

        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_edit_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btnClose = view.findViewById<ImageView>(R.id.btnClose)
        ivAvatarEdit = view.findViewById<ShapeableImageView>(R.id.ivAvatarEdit)
        etName = view.findViewById<TextInputEditText>(R.id.etName)
        btnMale = view.findViewById<MaterialButton>(R.id.btnMale)
        btnFemale = view.findViewById<MaterialButton>(R.id.btnFemale)
        btnOther = view.findViewById<MaterialButton>(R.id.btnOther)
        etDob = view.findViewById<TextInputEditText>(R.id.etDob)
        etWeight = view.findViewById<TextInputEditText>(R.id.etWeight)
        etHeight = view.findViewById<TextInputEditText>(R.id.etHeight)
        btnSave = view.findViewById<MaterialButton>(R.id.btnSave)

        val userArg = arguments?.getParcelable<User>("user_data")

        if (userArg != null) {
            currentUser = userArg.copy()
            setupInitialData()
            setupActions()
            observeViewModel()
        } else {
            Toast.makeText(context, "Error loading user data", Toast.LENGTH_SHORT).show()
            dismiss()
        }
    }

    private fun setupInitialData() {
        // Text Inputs
        etName.setText(currentUser.displayName)
        etHeight.setText(currentUser.height.toString())
        etWeight.setText(currentUser.weight.toString())

        // Avatar
        Glide.with(this)
            .load(currentUser.photoUrl)
            .placeholder(R.drawable.ic_user)
            .error(R.drawable.ic_user)
            .circleCrop()
            .into(ivAvatarEdit)

        // Date of Birth
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.US)
        etDob.setText(sdf.format(currentUser.birthday.toDate()))

        // Gender (Quan trọng: Gọi hàm update giao diện ngay khi mở lên)
        updateGenderUI(currentUser.gender)
    }

    private fun setupActions() {
        // Nút đóng
        btnClose.setOnClickListener { dismiss() }

        ivAvatarEdit.setOnClickListener {
            pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        // 1. Xử lý Logic chọn giới tính
        btnMale.setOnClickListener {
            currentUser.gender = "Male"
            updateGenderUI("Male")
        }
        btnFemale.setOnClickListener {
            currentUser.gender = "Female"
            updateGenderUI("Female")
        }
        btnOther.setOnClickListener {
            currentUser.gender = "Other"
            updateGenderUI("Other")
        }

        // 2. Xử lý chọn ngày sinh (DatePicker)
        etDob.setOnClickListener {
            showDatePicker()
        }

        // 3. Xử lý Lưu (Save)
        btnSave.setOnClickListener {
            saveChanges()
        }

        etName.addTextChangedListener { etName.error = null }
        etWeight.addTextChangedListener { etWeight.error = null }
        etHeight.addTextChangedListener { etHeight.error = null }
    }

    private val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            selectedImageUri = uri
            // Hiển thị review ngay lập tức
            Glide.with(this).load(uri).circleCrop().into(ivAvatarEdit)
        }
    }

    private fun updateGenderUI(selectedGender: String) {
        // Định nghĩa màu sắc
        val colorOrange = ContextCompat.getColor(requireContext(), R.color.orange_primary)
        val colorGrayText = ContextCompat.getColor(requireContext(), R.color.gray_text) // Hoặc Color.GRAY
        val colorWhite = Color.WHITE
        val colorTransparent = Color.TRANSPARENT
        val colorBorder = Color.parseColor("#E0E0E0") // Màu viền xám nhạt

        // Hàm local để reset nút về trạng thái "Chưa chọn"
        fun setUnselected(btn: MaterialButton) {
            btn.backgroundTintList = ColorStateList.valueOf(colorTransparent)
            btn.setTextColor(colorGrayText)
            btn.strokeWidth = 3
            btn.strokeColor = ColorStateList.valueOf(colorBorder)
        }

        // Hàm local để set nút về trạng thái "Đang chọn"
        fun setSelected(btn: MaterialButton) {
            btn.backgroundTintList = ColorStateList.valueOf(colorOrange)
            btn.setTextColor(colorWhite)
            btn.strokeWidth = 0
        }

        // Reset tất cả trước
        setUnselected(btnMale)
        setUnselected(btnFemale)
        setUnselected(btnOther)

        // Chỉ bật sáng nút được chọn
        when (selectedGender) {
            "Male" -> setSelected(btnMale)
            "Female" -> setSelected(btnFemale)
            else -> setSelected(btnOther)
        }
    }

    private fun showDatePicker() {
        val selection = currentUser.birthday.toDate().time

        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select Date of Birth")
            .setSelection(selection)
            .setTheme(R.style.ThemeOverlay_App_DatePicker)
            .build()

        datePicker.addOnPositiveButtonClickListener { timestamp ->
            val date = Date(timestamp)
            // Hiển thị lên EditText
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            etDob.setText(sdf.format(date))
            // Lưu vào biến user
            currentUser.birthday = Timestamp(date)
        }

        datePicker.show(parentFragmentManager, "DOB_PICKER")
    }

    private fun saveChanges() {
        // 1. Lấy dữ liệu Text mới nhất từ input
        val name = etName.text.toString().trim()
        val heightStr = etHeight.text.toString().trim()
        val weightStr = etWeight.text.toString().trim()

        // 2. Validate
        var isValid = true

        // --- Kiểm tra Tên ---
        if (name.isEmpty()) {
            etName.error = "Full Name cannot be empty"
            etName.requestFocus()
            isValid = false
        } else {
            etName.error = null
        }

        // --- Kiểm tra Cân nặng ---
        if (weightStr.isEmpty()) {
            etWeight.error = "Weight cannot be empty"
            if (isValid) etWeight.requestFocus()
            isValid = false
        } else {
            etWeight.error = null
        }

        // --- Kiểm tra Chiều cao ---
        if (heightStr.isEmpty()) {
            etHeight.error = "Height cannot be empty"
            if (isValid) etHeight.requestFocus()
            isValid = false
        } else {
            etHeight.error = null
        }

        if (heightStr.toIntOrNull() == null || heightStr.toInt() <= 0) {
            etHeight.error = "Invalid height"
            isValid = false
        }

        if (weightStr.toFloatOrNull() == null || weightStr.toFloat() <= 0) {
            etWeight.error = "Invalid weight"
            isValid = false
        }

        // Nếu có bất kỳ lỗi nào -> Dừng không lưu
        if (!isValid) return

        // 3. Update vào object currentUser
        currentUser.displayName = name
        currentUser.height = heightStr.toIntOrNull() ?: 0
        currentUser.weight = weightStr.toFloatOrNull() ?: 0f

        // 4. Gọi ViewModel để đẩy lên Firebase
        viewModel.saveUserProfile(currentUser, selectedImageUri)

    }

    private fun observeViewModel() {
        // 1. Lắng nghe trạng thái Loading
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            btnSave.isEnabled = !isLoading // Khóa nút khi đang lưu
            btnSave.text = if (isLoading) "Saving..." else "Save"
        }

        // 2. Lắng nghe thông báo (Thành công/Thất bại)
        viewModel.statusMessage.observe(viewLifecycleOwner) { message ->
            if (message != null) {
                // 1. Luôn hiện Toast cho người dùng biết
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()

                // 2. Kiểm tra nếu là thông báo thành công thì đóng dialog
                if (message.contains("successfully") || message.contains("thành công")) {
                    dismiss()
                }

                viewModel.clearStatusMessage()
            }
        }
    }
}