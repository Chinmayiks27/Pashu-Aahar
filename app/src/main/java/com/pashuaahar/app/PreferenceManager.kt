package com.pashuaahar.app

import android.content.Context
import android.content.SharedPreferences

class PreferenceManager(context: Context) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("PashuAaharPrefs", Context.MODE_PRIVATE)

    fun saveUserPhone(phone: String) {
        sharedPreferences.edit().putString("user_phone", phone).apply()
    }

    fun getUserPhone(): String? {
        return sharedPreferences.getString("user_phone", null)
    }

    fun clearUserPhone() {
        sharedPreferences.edit().remove("user_phone").apply()
    }

    fun setAutoAccumulateEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean("auto_accumulate_enabled", enabled).apply()
    }

    fun isAutoAccumulateEnabled(): Boolean {
        return sharedPreferences.getBoolean("auto_accumulate_enabled", false)
    }

    fun saveData(breed: String, age: String, weight: String, yield: String) {
        val editor = sharedPreferences.edit()
        editor.putString("breed", breed)
        editor.putString("age", age)
        editor.putString("weight", weight)
        editor.putString("yield", yield)
        editor.apply()
    }

    fun getData(): Map<String, String> {
        return mapOf(
            "breed" to (sharedPreferences.getString("breed", "Desi") ?: "Desi"),
            "age" to (sharedPreferences.getString("age", "") ?: ""),
            "weight" to (sharedPreferences.getString("weight", "") ?: ""),
            "yield" to (sharedPreferences.getString("yield", "") ?: "")
        )
    }

    fun clearData() {
        val editor = sharedPreferences.edit()
        editor.clear()
        editor.apply()
    }
}
