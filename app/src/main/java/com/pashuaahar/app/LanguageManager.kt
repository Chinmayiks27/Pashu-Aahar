package com.pashuaahar.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object LanguageManager {
    var isKannada by mutableStateOf(false)

    val strings = mapOf(
        "app_title" to ("Pashu-Aahar Calculator" to "ಪಶು-ಆಹಾರ ಕ್ಯಾಲ್ಕುಲೇಟರ್"),
        "step1" to ("Step 1: Cow Profile" to "ಹಂತ 1: ಹಸುವಿನ ವಿವರ"),
        "step2" to ("Step 2: Yield Targets" to "ಹಂತ 2: ಹಾಲಿನ ಇಳುವರಿ"),
        "step3" to ("Final: Your Balanced Feed" to "ಅಂತಿಮ: ಸಮತೋಲಿತ ಆಹಾರ"),
        "breed" to ("Select Breed" to "ತಳಿಯನ್ನು ಆರಿಸಿ"),
        "age" to ("Age (Years)" to "ವಯಸ್ಸು (ವರ್ಷಗಳು)"),
        "weight" to ("Weight (kg)" to "ತೂಕ (ಕೆಜಿ)"),
        "next" to ("Next" to "ಮುಂದೆ"),
        "back" to ("Back" to "ಹಿಂದೆ"),
        "current_yield" to ("Current Daily Milk (Litres)" to "ಪ್ರಸ್ತುತ ದಿನದ ಹಾಲು (ಲೀಟರ್)"),
        "target_yield" to ("Target Milk Yield (Litres)" to "ಗುರಿ ಹಾಲಿನ ಇಳುವರಿ (ಲೀಟರ್)"),
        "get_recipe" to ("Get Recipe" to "ಆಹಾರದ ಪಟ್ಟಿ ಪಡೆಯಿರಿ"),
        "save_start" to ("Save & Start New" to "ಉಳಿಸಿ ಮತ್ತು ಹೊಸದು ಪ್ರಾರಂಭಿಸಿ"),
        "market_price" to ("Market Feed Price (50kg)" to "ಮಾರುಕಟ್ಟೆ ಆಹಾರದ ಬೆಲೆ (50ಕೆಜಿ)"),
        "monthly_savings" to ("Monthly Savings: ₹" to "ಮಾಸಿಕ ಉಳಿತಾಯ: ₹"),
        "history" to ("History" to "ಇತಿಹಾಸ"),
        "tips" to ("Tips" to "ಸಲಹೆಗಳು"),
        "calc" to ("Calculator" to "ಕ್ಯಾಲ್ಕುಲೇಟರ್"),
        "prosperity" to ("Dairy Prosperity Tracker" to "ಕ್ಷೀರ ಸಮೃದ್ಧಿ ಟ್ರ್ಯಾಕರ್"),
        "hygiene" to ("Hygiene & Cleanliness" to "ನೈರ್ಮಲ್ಯ ಮತ್ತು ಸ್ವಚ್ಛತೆ"),
        "storage" to ("Fodder Storage" to "ಮೇವು ಸಂಗ್ರಹಣೆ"),
        "water" to ("Clean Water" to "ಶುದ್ಧ ನೀರು"),
        "vaccination" to ("Vaccination" to "ಲಸಿಕೆ ಹಾಕುವಿಕೆ"),
        "login" to ("Login" to "ಲಾಗಿನ್"),
        "phone" to ("Mobile Number" to "ಮೊಬೈಲ್ ಸಂಖ್ಯೆ"),
        "password" to ("Password" to "ಪಾಸ್ವರ್ಡ್"),
        "saved_msg" to ("Record Saved for Prosperity Tracker" to "ದಾಖಲೆಯನ್ನು ಉಳಿಸಲಾಗಿದೆ"),
        "validation_err" to ("Please enter all details" to "ದಯವಿಟ್ಟು ಎಲ್ಲಾ ವಿವರಗಳನ್ನು ಭರ್ತಿ ಮಾಡಿ"),
        "yield_err" to ("Please enter milk yield" to "ದಯವಿಟ್ಟು ಹಾಲಿನ ಇಳುವರಿಯನ್ನು ಭರ್ತಿ ಮಾಡಿ"),
        "recent_calc" to ("Recent Calculations" to "ಇತ್ತೀಚಿನ ಲೆಕ್ಕಾಚಾರಗಳು"),
        "trend_title" to ("Actual Savings Trend (Daily ₹)" to "ಉಳಿತಾಯದ ಪ್ರವೃತ್ತಿ (ದಿನಕ್ಕೆ ₹)"),
        "saved_per_day" to ("saved/day" to "ಉಳಿತಾಯ/ದಿನ"),
        "scientific_matrix" to ("Scientific Nutrition Matrix" to "ವೈಜ್ಞಾನಿಕ ಪೌಷ್ಟಿಕಾಂಶದ ಮಾಹಿತಿ"),
        "select_ingredients" to ("Select Available Ingredients" to "ಲಭ್ಯವಿರುವ ಪದಾರ್ಥಗಳನ್ನು ಆರಿಸಿ"),
        "total_prosperity" to ("Total Lifetime Prosperity" to "ಒಟ್ಟು ಜೀವಿತಾವಧಿಯ ಉಳಿತಾಯ"),
        "prices" to ("Prices" to "ಬೆಲೆಗಳು"),
        "ai_advisor" to ("AI Advisor" to "AI ಸಲಹೆ"),
        
        // Ingredients
        "Maize" to ("Maize (Makka)" to "ಮೆಕ್ಕೆಜೋಳ"),
        "Barley" to ("Barley (Jau)" to "ಬಾರ್ಲಿ"),
        "Millet" to ("Millet (Bajra)" to "ಸಜ್ಜೆ"),
        "Cottonseed Cake" to ("Cottonseed Cake (Khal)" to "ಹತ್ತಿಬೀಜದ ಹಿಂಡಿ"),
        "Soy Meal" to ("Soy Meal (Khali)" to "ಸೋಯಾ ಹಿಂಡಿ"),
        "Mustard Cake" to ("Mustard Cake (Khal)" to "ಸಾಸಿವೆ ಹಿಂಡಿ"),
        "Wheat Bran" to ("Wheat Bran (Chokar)" to "ಗೋದಿಯ ತೌಡು"),
        "Rice Bran" to ("Rice Bran (Polish)" to "ಅಕ್ಕಿ ತೌಡು")
    )

    fun gs(key: String): String {
        val pair = strings[key] ?: (key to key)
        return if (isKannada) pair.second else pair.first
    }
}
