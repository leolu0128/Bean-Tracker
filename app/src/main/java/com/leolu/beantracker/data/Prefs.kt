package com.leolu.beantracker.data

import android.content.Context

enum class ThemeMode { System, Light, Dark }

class Prefs(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    var themeMode: ThemeMode
        get() = ThemeMode.entries.getOrElse(prefs.getInt(KEY_THEME, 0)) { ThemeMode.System }
        set(value) = prefs.edit().putInt(KEY_THEME, value.ordinal).apply()

    private companion object {
        const val KEY_THEME = "theme_mode"
    }
}
