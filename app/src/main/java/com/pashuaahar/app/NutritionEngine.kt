package com.pashuaahar.app

import android.content.Context
import kotlin.collections.*

data class CattleProfile(
    val breed: String,
    val age: Double,
    val weight: Double,
    val milkYield: Double
)

data class NutritionMatrix(
    val dryMatter: Double,
    val crudeProtein: Double,
    val tdn: Double // Total Digestible Nutrients (Energy)
)

object NutritionEngine {

    val INGREDIENT_COSTS = mapOf(
        "Maize" to 18.0, "Barley" to 20.0, "Millet" to 19.0,
        "Cottonseed Cake" to 25.0, "Soy Meal" to 36.0, "Mustard Cake" to 23.0,
        "Wheat Bran" to 15.0, "Rice Bran" to 14.0
    )

    private fun computeHomemadeCostPerDay(
        concKg: Double,
        grains: List<String>,
        cakes: List<String>,
        brans: List<String>,
        context: Context? = null
    ): Double {
        val usedGrains = if (grains.isEmpty()) listOf("Maize") else grains
        val usedCakes = if (cakes.isEmpty()) listOf("Cottonseed Cake") else cakes
        val usedBrans = if (brans.isEmpty()) listOf("Wheat Bran") else brans

        val grainPortion = concKg * 0.50
        val cakePortion = concKg * 0.35
        val branPortion = concKg * 0.15

        fun getPrice(ingredient: String): Double {
            return context?.let { IngredientPriceManager.getPrice(ingredient, it) }
                ?: INGREDIENT_COSTS[ingredient] ?: 0.0
        }

        val avgGrainCost = usedGrains.map { getPrice(it) }.average()
        val avgCakeCost = usedCakes.map { getPrice(it) }.average()
        val avgBranCost = usedBrans.map { getPrice(it) }.average()

        return (grainPortion * avgGrainCost) +
                (cakePortion * avgCakeCost) +
                (branPortion * avgBranCost) +
                (0.05 * 50.0) +   // flat 50g mineral mix @ ₹50/kg
                (0.03 * 5.0)      // flat 30g salt
    }
    
    /**
     * Calculates the detailed scientific matrix for the cattle's requirement.
     * Based on NRC standards (simplified for PashuAahar).
     */
    fun calculateMatrix(profile: CattleProfile, targetYield: Double): NutritionMatrix {
        // Maintenance requirements - Weight based
        var maintDM = profile.weight * 0.02 
        var maintCP = profile.weight * 0.001 
        var maintTDN = profile.weight * 0.01 

        // Age impact: Younger cows (heifers < 3 years) need extra for growth
        if (profile.age < 3.0) {
            maintDM *= 1.2
            maintCP *= 1.25
            maintTDN *= 1.15
        }

        // Production requirements (per litre of milk)
        val prodDM = targetYield * 0.4
        val prodCP = targetYield * 0.085
        val prodTDN = targetYield * 0.33

        return NutritionMatrix(
            dryMatter = maintDM + prodDM,
            crudeProtein = maintCP + prodCP,
            tdn = maintTDN + prodTDN
        )
    }

    /**
     * Validates if the target yield is realistic for the breed.
     * @return Error message if invalid, null if valid.
     */
    fun validateTargetYield(breed: String, targetYield: Double): String? {
        val limit = when(breed) {
            "Desi" -> 15.0
            "Jersey" -> 25.0
            "HF" -> 35.0
            else -> 20.0
        }
        return if (targetYield > limit) {
            if (LanguageManager.isKannada) 
                "ಈ ತಳಿಗೆ ಗರಿಷ್ಠ ಗುರಿ ${limit.toInt()} ಲೀಟರ್ ಆಗಿದೆ." 
            else 
                "Maximum target for $breed is ${limit.toInt()}L."
        } else null
    }

    /**
     * Calculates a truly "Balanced Feed" including Concentrates and Roughage.
     * @param availableIngredients List of ingredients farmer has.
     */
    fun calculateRecipe(
        profile: CattleProfile, 
        targetYield: Double,
        availableIngredients: List<String> = listOf("Maize", "Cottonseed Cake", "Wheat Bran")
    ): Map<String, Double> {
        val weight = if (profile.weight > 0) profile.weight else 400.0
        val breedMultiplier = when(profile.breed) {
            "HF" -> 1.2
            "Jersey" -> 1.1
            else -> 1.0
        }

        // 1. Total Dry Matter Requirement
        val matrix = calculateMatrix(profile, targetYield)
        val totalDMReq = matrix.dryMatter
        
        // 2. Concentrate Requirement
        val concKg = (1.5 + (targetYield * 0.4)) * breedMultiplier
        val currentYield = profile.milkYield
        
        // 3. Roughage Requirement
        val roughageDM = totalDMReq - concKg
        
        val dryFodderAmount = (roughageDM * 0.4) / 0.9
        val greenFodderAmount = (roughageDM * 0.6) / 0.2

        // Recipe split portions
        val grainPortion = concKg * 0.50
        val proteinBoost = if (targetYield > currentYield) 0.5 else 0.0
        val cakePortion  = (concKg * 0.35) + proteinBoost
        val branPortion  = concKg * 0.15

        return mutableMapOf<String, Double>().apply {
            put("🌿 Green Fodder (Chara)", greenFodderAmount)
            put("🌾 Dry Fodder (Bhusa/Kadbi)", dryFodderAmount)
            
            val selectedGrains = availableIngredients.filter { it in listOf("Maize", "Barley", "Millet") }
            val selectedCakes = availableIngredients.filter { it in listOf("Cottonseed Cake", "Soy Meal", "Mustard Cake") }
            val selectedBrans = availableIngredients.filter { it in listOf("Wheat Bran", "Rice Bran") }

            val grainList = if (selectedGrains.isEmpty()) listOf("Maize") else selectedGrains
            val cakeList = if (selectedCakes.isEmpty()) listOf("Cottonseed Cake") else selectedCakes
            val branList = if (selectedBrans.isEmpty()) listOf("Wheat Bran") else selectedBrans
            
            if ("Maize" in grainList) put("🌽 Maize (Makka)", grainPortion / grainList.size)
            if ("Barley" in grainList) put("🌾 Barley (Jau)", grainPortion / grainList.size)
            if ("Millet" in grainList) put("🌾 Millet (Bajra)", grainPortion / grainList.size)
            
            if ("Cottonseed Cake" in cakeList) put("🌻 Cottonseed Cake (Khal)", cakePortion / cakeList.size)
            if ("Soy Meal" in cakeList) put("🫘 Soy Meal (Khali)", cakePortion / cakeList.size)
            if ("Mustard Cake" in cakeList) put("🌾 Mustard Cake (Sarson)", cakePortion / cakeList.size)
            
            if ("Wheat Bran" in branList) put("🌾 Wheat Bran (Chokar)", branPortion / branList.size)
            if ("Rice Bran" in branList) put("🍚 Rice Bran (Polish)", branPortion / branList.size)
            
            put("🧂 Mineral Mixture", 0.05) 
            put("🧂 Salt", 0.03)
        }
    }

    fun calculateSavings(
        concKg: Double,
        marketBagPrice: Double,
        targetYield: Double,
        currentYield: Double,
        availableIngredients: List<String>,
        context: Context? = null
    ): Double {
        if (marketBagPrice <= 0 || concKg <= 0) return 0.0

        val grains = availableIngredients.filter { it in listOf("Maize","Barley","Millet") }
        val cakes  = availableIngredients.filter { it in listOf("Cottonseed Cake","Soy Meal","Mustard Cake") }
        val brans  = availableIngredients.filter { it in listOf("Wheat Bran","Rice Bran") }

        val homemadeCostDay = computeHomemadeCostPerDay(concKg, grains, cakes, brans, context)
        val marketCostDay = concKg * (marketBagPrice / 50.0)

        return maxOf(marketCostDay - homemadeCostDay, 0.0)
    }

    fun calculateHomemadeCost(
        concKg: Double,
        targetYield: Double,
        currentYield: Double,
        availableIngredients: List<String>,
        context: Context? = null
    ): Double {
        val grains = availableIngredients.filter { it in listOf("Maize","Barley","Millet") }
        val cakes  = availableIngredients.filter { it in listOf("Cottonseed Cake","Soy Meal","Mustard Cake") }
        val brans  = availableIngredients.filter { it in listOf("Wheat Bran","Rice Bran") }
        
        return computeHomemadeCostPerDay(concKg, grains, cakes, brans, context)
    }
}
