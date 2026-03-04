package com.maxnetwork.browser

import android.app.Activity
import android.content.Context

object ThemeManager {

    private const val PREF_KEY = "app_theme"
    const val DARK = "dark"
    const val LIGHT = "light"

    fun setTheme(context: Context, theme: String) {
        context.getSharedPreferences("maxnetwork_prefs", Context.MODE_PRIVATE)
            .edit().putString(PREF_KEY, theme).apply()
    }

    fun getTheme(context: Context): String {
        return context.getSharedPreferences("maxnetwork_prefs", Context.MODE_PRIVATE)
            .getString(PREF_KEY, DARK) ?: DARK
    }

    fun applyTheme(activity: Activity) {
        val theme = getTheme(activity)
        if (theme == LIGHT) {
            activity.setTheme(R.style.Theme_MaxBrowser_Light)
        } else {
            activity.setTheme(R.style.Theme_MaxBrowser)
        }
    }
}