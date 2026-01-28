package com.example.fitlifesmarthealthlifestyleapp.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import com.example.fitlifesmarthealthlifestyleapp.R
import com.example.fitlifesmarthealthlifestyleapp.domain.utils.LanguagePreference
import com.example.fitlifesmarthealthlifestyleapp.domain.model.Language
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton

class LanguageBottomSheet(
    private val onLanguageSelected: (Language) -> Unit
) : BottomSheetDialogFragment() {

    private lateinit var languagePreference: LanguagePreference

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_sheet_language, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        languagePreference = LanguagePreference(requireContext())
        val currentLanguage = languagePreference.getLanguage()

        val tvTitle = view.findViewById<TextView>(R.id.tvLanguageTitle)
        val radioGroup = view.findViewById<RadioGroup>(R.id.language_radio_group)
        val radioVietnamese = view.findViewById<RadioButton>(R.id.radio_vietnamese)
        val radioEnglish = view.findViewById<RadioButton>(R.id.radio_english)
        val btnConfirm = view.findViewById<MaterialButton>(R.id.btn_confirm)
        val btnCancel = view.findViewById<MaterialButton>(R.id.btn_cancel)

        // Set current selection
        when (currentLanguage) {
            Language.ENGLISH -> radioGroup.check(R.id.radio_english)
            Language.VIETNAMESE -> radioGroup.check(R.id.radio_vietnamese)
        }

        btnConfirm.setOnClickListener {
            val selectedLanguage = when (radioGroup.checkedRadioButtonId) {
                R.id.radio_english -> Language.ENGLISH
                R.id.radio_vietnamese -> Language.VIETNAMESE
                else -> Language.VIETNAMESE
            }

            // Save language preference
            languagePreference.saveLanguage(selectedLanguage)

            // Notify parent
            onLanguageSelected(selectedLanguage)

            dismiss()
        }

        btnCancel.setOnClickListener {
            dismiss()
        }
    }
}