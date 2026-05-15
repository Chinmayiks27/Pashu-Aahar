package com.pashuaahar.app

import android.content.Context

object IngredientPriceManager {
    private const val PREFS_NAME = "IngredientPrices"

    fun getPrice(ingredient: String, context: Context): Double {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val defaultPrice = NutritionEngine.INGREDIENT_COSTS[ingredient] ?: 0.0
        return prefs.getFloat(ingredient, defaultPrice.toFloat()).toDouble()
    }

    fun setPrice(ingredient: String, price: Double, context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putFloat(ingredient, price.toFloat()).apply()
    }

    fun getAllPrices(context: Context): Map<String, Double> {
        return NutritionEngine.INGREDIENT_COSTS.keys.associateWith { getPrice(it, context) }
    }
    
    fun resetToDefaults(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }
}
