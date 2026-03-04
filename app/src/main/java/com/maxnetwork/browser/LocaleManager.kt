package com.maxnetwork.browser

import android.content.Context
import java.util.Locale

object LocaleManager {

    private const val PREF_KEY = "app_language"

    fun setLocale(context: Context, langCode: String) {
        context.getSharedPreferences("maxnetwork_prefs", Context.MODE_PRIVATE)
            .edit().putString(PREF_KEY, langCode).apply()
        applyLocale(context, langCode)
    }

    fun getLanguage(context: Context): String {
        return context.getSharedPreferences("maxnetwork_prefs", Context.MODE_PRIVATE)
            .getString(PREF_KEY, "en") ?: "en"
    }

    fun applyLocale(context: Context, langCode: String) {
        val locale = Locale(langCode)
        Locale.setDefault(locale)
        val config = context.resources.configuration
        config.setLocale(locale)
        context.resources.updateConfiguration(config, context.resources.displayMetrics)
    }

    fun isFirstLaunch(context: Context): Boolean {
        return !context.getSharedPreferences("maxnetwork_prefs", Context.MODE_PRIVATE)
            .contains(PREF_KEY)
    }
}