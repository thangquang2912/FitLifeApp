package com.example.fitlifesmarthealthlifestyleapp.domain.utils

import android.content.Context
import android.content.res.Configuration
import com.example.fitlifesmarthealthlifestyleapp.domain.model.Language
import java.util.Locale

object LanguageHelper {

    fun setLocale(context: Context, language: Language): Context {
        val locale = Locale(language.code)
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)

        return context.createConfigurationContext(config)
    }

    fun updateResources(context: Context, language: Language) {
        val locale = Locale(language.code)
        Locale.setDefault(locale)

        val config = context.resources.configuration
        config.setLocale(locale)

        context.resources.updateConfiguration(config, context.resources.displayMetrics)
    }
}