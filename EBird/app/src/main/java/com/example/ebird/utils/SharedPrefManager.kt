package com.example.ebird.utils

import android.content.Context
import android.content.SharedPreferences

class SharedPrefManager(context: Context) {

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFERENCE_NAME = "user_preferences"
        private const val SWITCH_STATUS_KEY = "isSwitchOn"
    }

    // Save the switch status to SharedPreferences
    fun saveSwitchStatus(isSwitchON: Boolean) {
        val editor = sharedPreferences.edit()
        editor.putBoolean(SWITCH_STATUS_KEY, isSwitchON)
        editor.apply()  // apply() is asynchronous and faster than commit()
    }

    // Retrieve the switch status from SharedPreferences
    fun getSwitchStatus(): Boolean {
        return sharedPreferences.getBoolean(SWITCH_STATUS_KEY, false) // default is false
    }

    // Clear all preferences (optional, if you need a method to reset preferences)
    fun clearPreferences() {
        val editor = sharedPreferences.edit()
        editor.clear()
        editor.apply()
    }
}
