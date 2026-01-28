package com.example.fitlifesmarthealthlifestyleapp.domain.utils

import android.content.Context
import android.content.SharedPreferences
import com.example.fitlifesmarthealthlifestyleapp.domain.model.Language

class LanguagePreference(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("app_preferences", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_LANGUAGE = "selected_language"
    }

    fun saveLanguage(language: Language) {
        prefs.edit().putString(KEY_LANGUAGE, language.code).apply()
    }

    fun getLanguage(): Language {
        val code = prefs.getString(KEY_LANGUAGE, Language.VIETNAMESE.code)
        return Language.values().find { it.code == code } ?: Language.VIETNAMESE
    }
}