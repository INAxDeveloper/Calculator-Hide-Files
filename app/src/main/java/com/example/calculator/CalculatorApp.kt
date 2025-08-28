package com.example.calculator

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.example.calculator.utils.PrefsUtil
import com.google.android.material.color.DynamicColors

class CalculatorApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val prefs = PrefsUtil(this)
        val themeMode = prefs.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        AppCompatDelegate.setDefaultNightMode(themeMode)
        if (prefs.getBoolean("dynamic_theme", true)) {
            DynamicColors.applyToActivitiesIfAvailable(this)
        }
    }
} 