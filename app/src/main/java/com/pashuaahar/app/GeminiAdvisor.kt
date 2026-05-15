package com.pashuaahar.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.URL
import javax.net.ssl.HttpsURLConnection

object GeminiAdvisor {

    private val API_KEY: String get() = BuildConfig.GEMINI_API_KEY
    private const val MODEL = "gemini-2.5-flash"
    private val BASE_URL get() = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL:generateContent?key=$API_KEY"

    suspend fun getAdvice(
        breed: String,
        age: Double,
        weight: Double,
        targetYield: Double,
        recipe: Map<String, Double>,
        language: String
    ): String = withContext(Dispatchers.IO) {
        try {
            val currentYield = recipe["Current Yield"] ?: 0.0
            val dailySaving = recipe["Daily Saving"] ?: 0.0
            val prompt = """
                You are an expert dairy farming advisor for small Indian farmers.
                Answer in $language language only.
                Cow: $breed, Age: ${age.toInt()} yrs, Weight: ${weight.toInt()} kg
                Current yield: $currentYield L/day, Target: $targetYield L/day
                Daily saving with home feed: Rs.${"%.0f".format(dailySaving)}
                
                Do not greet the user. Do not say Namaste, Hello, or any greeting. Do not address them by any name or term like "Kisan bhai". Start directly with the report.
                Write a short report with EXACTLY these 4 headers (no asterisks, no markdown):
                FEED ASSESSMENT: (2 sentences)
                TOP 3 TIPS: (numbered 1,2,3)
                HEALTH ALERT: (1 sentence)
                COST ADVICE: (1 sentence)
                Keep total under 200 words. Use simple village language. Do not use any markdown formatting.
            """.trimIndent()
            callApi(prompt)
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }

    suspend fun chat(message: String, breed: String, language: String): String =
        withContext(Dispatchers.IO) {
            // Detect calculation requests locally — no API call needed
            val calcKeywords = listOf(
                "calculate", "recipe", "feed mix", "how much to feed",
                "kg of", "maize", "barley", "cost comparison", "savings",
                "ration", "formula", "compute", "daily feed", "ingredient"
            )
            val isCalcRequest = calcKeywords.any {
                message.lowercase().contains(it)
            }

            if (isCalcRequest) {
                return@withContext if (language == "Kannada")
                    "ದಯವಿಟ್ಟು ಕ್ಯಾಲ್ಕುಲೇಟರ್ ಟ್ಯಾಬ್ ಬಳಸಿ.\n\nನಿಮ್ಮ ಹಸುವಿನ ತೂಕ, ತಳಿ ಮತ್ತು ಹಾಲಿನ ಗುರಿಯ ಆಧಾರದ ಮೇಲೆ ನಿಖರವಾದ ಫಲಿತಾಂಶಗಳನ್ನು ಕ್ಯಾಲ್ಕುಲೇಟರ್ ನೀಡುತ್ತದೆ. 🧮"
                else
                    "Please use the Calculator tab for feed recipes and cost calculations.\n\nThe Calculator uses scientific veterinary nutrition formulas to give you accurate results based on your cow's exact weight, breed and milk yield target. 🧮"
            }

            // For all other questions — ask Gemini
            try {
                val prompt = """
                You are a helpful dairy farming assistant for Indian farmers.
                The farmer has a $breed cow.
                Answer in $language using simple village language.
                Do not use markdown or asterisks.
                Keep answer under 100 words.
                Do not greet the user. Do not say Namaste, Hello, or any greeting. Do not use any name or address like "Kisan bhai" or any other term. Start your answer directly with the farming information.
                Only answer farming knowledge questions — health, feeding times, hygiene, symptoms.
                Question: $message
            """.trimIndent()
                callApi(prompt)
            } catch (e: Exception) {
                "Unable to connect. Please check your internet and try again."
            }
        }

    private fun cleanResponse(text: String): String {
        return text
            .replace("**", "")
            .replace("##", "")
            .replace("* ", "• ")
            .replace("*", "")
            .trim()
    }

    private fun callApi(prompt: String): String {
        val connection = URL(BASE_URL).openConnection() as HttpsURLConnection
        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-Type", "application/json")
        connection.doOutput = true
        connection.connectTimeout = 20000
        connection.readTimeout = 20000

        val body = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", prompt) })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.7)
            })
        }

        OutputStreamWriter(connection.outputStream).use { it.write(body.toString()) }

        val code = connection.responseCode
        val stream = if (code == 200) connection.inputStream else connection.errorStream
        val response = BufferedReader(InputStreamReader(stream)).readText()

        if (code != 200) return "AI service temporarily unavailable. Please try again."

        val rawText = JSONObject(response)
            .getJSONArray("candidates")
            .getJSONObject(0)
            .getJSONObject("content")
            .getJSONArray("parts")
            .getJSONObject(0)
            .getString("text")

        return cleanResponse(rawText)
    }
}