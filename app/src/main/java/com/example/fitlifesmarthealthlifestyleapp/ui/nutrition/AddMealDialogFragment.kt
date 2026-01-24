package com.example.fitlifesmarthealthlifestyleapp.ui.nutrition

import android.app.Activity
import android.app.TimePickerDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.example.fitlifesmarthealthlifestyleapp.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

class AddMealDialogFragment(
    private val onMealAdded: (String, Int, Float, Float, Float, Float, String?) -> Unit
) : DialogFragment() {

    private lateinit var etDishName: TextInputEditText
    private lateinit var etPortion: TextInputEditText
    private lateinit var etTime: TextInputEditText
    private lateinit var btnSave: MaterialButton
    private lateinit var btnClose: ImageView
    private lateinit var etCarbs: EditText
    private lateinit var etProtein: EditText
    private lateinit var etFat: EditText
    private lateinit var progressBarAi: ProgressBar

    // Các views liên quan đến ảnh
    private lateinit var imgCameraIcon: ImageView // Icon máy ảnh nhỏ
    private lateinit var tvUploadHint: TextView // Dòng chữ "Tap to add photo"
    private lateinit var imgSelectedPhoto: ImageView // View hiển thị ảnh full sau khi chọn
    private lateinit var layoutPhotoUpload: View // Container

    private val geminiHelper = GeminiNutritionHelper()
    private var currentImageUri: Uri? = null

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val imageUri: Uri? = result.data?.data
            if (imageUri != null) {
                currentImageUri = imageUri

                // --- THAY ĐỔI GIAO DIỆN KHI CÓ ẢNH ---
                // 1. Ẩn các view gợi ý ban đầu
                imgCameraIcon.visibility = View.GONE
                tvUploadHint.visibility = View.GONE

                // 2. Hiện view ảnh full và set ảnh vào
                imgSelectedPhoto.visibility = View.VISIBLE
                imgSelectedPhoto.setImageURI(imageUri)
                progressBarAi.visibility = View.VISIBLE

                // 3. Thông báo và bắt đầu phân tích AI
                Toast.makeText(context, "AI is analyzing...", Toast.LENGTH_SHORT).show()
                val bitmap = uriToBitmap(imageUri)
                if (bitmap != null) {
                    analyzeImageWithAI(bitmap)
                }
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.dialog_add_meal, container, false)
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog?.window?.requestFeature(Window.FEATURE_NO_TITLE)
        return view
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.95).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews(view)
        setupListeners()
    }

    private fun initViews(view: View) {
        etDishName = view.findViewById(R.id.etDishName)
        etPortion = view.findViewById(R.id.etPortion)
        etTime = view.findViewById(R.id.etTime)
        btnSave = view.findViewById(R.id.btnSaveMeal)
        btnClose = view.findViewById(R.id.btnClose)
        etCarbs = view.findViewById(R.id.etCarbs)
        etProtein = view.findViewById(R.id.etProtein)
        etFat = view.findViewById(R.id.etFat)

        // Ánh xạ các views ảnh
        imgCameraIcon = view.findViewById(R.id.imgCameraIcon)
        tvUploadHint = view.findViewById(R.id.tvUploadHint)
        imgSelectedPhoto = view.findViewById(R.id.imgSelectedPhoto)
        layoutPhotoUpload = view.findViewById(R.id.layoutPhotoUpload)
        progressBarAi = view.findViewById(R.id.progressBarAi)

        updateTimeText(Calendar.getInstance())
    }

    private fun setupListeners() {
        btnClose.setOnClickListener { dismiss() }
        etTime.setOnClickListener { showTimePicker() }

        // Bấm vào container để mở thư viện ảnh
        layoutPhotoUpload.setOnClickListener { openImagePicker() }

        // Nếu đã chọn ảnh rồi, bấm vào ảnh đó để chọn lại ảnh khác
        imgSelectedPhoto.setOnClickListener { openImagePicker() }

        btnSave.setOnClickListener {
            saveMeal()
        }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImageLauncher.launch(intent)
    }

    private fun saveMeal() {
        val name = etDishName.text.toString()
        val portionStr = etPortion.text.toString()

        if (name.isEmpty() || portionStr.isEmpty()) {
            Toast.makeText(context, "Please fill dish name and portion", Toast.LENGTH_SHORT).show()
            return
        }

        val carbs = etCarbs.text.toString().toFloatOrNull() ?: 0f
        val protein = etProtein.text.toString().toFloatOrNull() ?: 0f
        val fat = etFat.text.toString().toFloatOrNull() ?: 0f
        val portion = portionStr.toFloatOrNull() ?: 0f

        val calories = ((carbs * 4) + (protein * 4) + (fat * 9)).toInt()
        val imageUriString = currentImageUri?.toString()

        onMealAdded(name, calories, carbs, protein, fat, portion, imageUriString)
        dismiss()
    }

    private fun analyzeImageWithAI(bitmap: Bitmap) {
        lifecycleScope.launch {
            val result = geminiHelper.analyzeFoodImage(bitmap)
            progressBarAi.visibility = View.GONE
            if (result != null) {
                etDishName.setText(result.name)
                etPortion.setText(result.portion.toString())
                etCarbs.setText(result.carbs.toString())
                etProtein.setText(result.protein.toString())
                etFat.setText(result.fat.toString())
                Toast.makeText(context, "AI Analysis Done: ${result.name}", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "AI couldn't identify the food. Please enter manually.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun uriToBitmap(uri: Uri): Bitmap? {
        return try {
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun showTimePicker() {
        val calendar = Calendar.getInstance()
        val timeSetListener = TimePickerDialog.OnTimeSetListener { _, hour, minute ->
            calendar.set(Calendar.HOUR_OF_DAY, hour)
            calendar.set(Calendar.MINUTE, minute)
            updateTimeText(calendar)
        }
        TimePickerDialog(requireContext(), timeSetListener,
            calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
    }

    private fun updateTimeText(calendar: Calendar) {
        val sdf = java.text.SimpleDateFormat("HH:mm", Locale.getDefault())
        etTime.setText(sdf.format(calendar.time))
    }
}