package com.example.fitlifesmarthealthlifestyleapp.ui.nutrition

import android.graphics.Bitmap
import android.util.Log
import com.example.fitlifesmarthealthlifestyleapp.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class GeminiNutritionHelper {
    private val apiKey = BuildConfig.API_KEY_GEMINI

    private val generativeModel = GenerativeModel(
        modelName = "gemini-2.5-flash",
        apiKey = apiKey
    )

    // Data class cho kết quả phân tích ảnh (Cũ)
    data class NutritionResult(
        val name: String,
        val calories: Int,
        val carbs: Float,
        val protein: Float,
        val fat: Float,
        val portion: Int
    )

    // --- MỚI THÊM: Data class cho gợi ý món ăn ---
    data class MealSuggestion(
        val dishName: String,       // Tên món
        val reason: String,         // Lý do gợi ý (ngắn gọn)
        val protein: Int,
        val calories: Int,
        val icon: String = "🍲"     // Icon emoji cho sinh động
    )

    // Hàm phân tích ảnh (Giữ nguyên code cũ của bạn)
    suspend fun analyzeFoodImage(image: Bitmap): NutritionResult? {
        return withContext(Dispatchers.IO) {
            try {
                // SỬA ĐOẠN PROMPT NÀY
                val prompt = """
                    Analyze this food image. Identify the main dish name in Vietnamese (Tiếng Việt) and estimate nutrition for 1 serving.
                    Return ONLY a raw JSON object with this structure (no markdown, no ```json tags):
                    {
                      "name": "Tên món ăn bằng Tiếng Việt",
                      "calories": 400,
                      "carbs": 50.0,
                      "protein": 30.0,
                      "fat": 15.0,
                      "portion": 250
                    }
                """.trimIndent()

                val response = generativeModel.generateContent(
                    content {
                        image(image)
                        text(prompt)
                    }
                )
                val responseText = response.text ?: return@withContext null
                parseJsonToResult(responseText)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    // --- Hàm gợi ý món ăn thông minh ---
    suspend fun suggestNextMeal(
        remainingCarbs: Int,
        remainingProtein: Int,
        remainingFat: Int,
        timeOfDay: String
    ): MealSuggestion? {
        return withContext(Dispatchers.IO) {
            try {
                // Xử lý logic nếu ăn lố (macro bị âm)
                val constraint = if (remainingCarbs < 0 || remainingProtein < 0 || remainingFat < 0) {
                    "User exceeded macros. Suggest a very light, low-calorie dish."
                } else {
                    "Remaining: ${remainingCarbs}g Carbs, ${remainingProtein}g Protein, ${remainingFat}g Fat."
                }
                val prompt = """
                    Context: It is $timeOfDay in Vietnam. $constraint
                    Task: Suggest ONE common Vietnamese healthy dish fitting these stats.
                    Response format: JSON ONLY.
                    {
                      "dishName": "Vietnamese Dish Name",
                      "reason": "Why (max 10 words in Vietnamese)",
                      "protein": 0,
                      "calories": 0,
                      "icon": "Emoji"
                    }
                """.trimIndent()

                val response = generativeModel.generateContent(prompt)
                val text = response.text ?: return@withContext null
                parseJsonToMealSuggestion(text)
            } catch (e: Exception) {
                Log.e("GeminiHelper", "AI Suggestion Error: ${e.message}")
                e.printStackTrace()
                null
            }
        }
    }
    private fun parseJsonToMealSuggestion(rawText: String): MealSuggestion? {
        return try {
            val startIndex = rawText.indexOf('{')
            val endIndex = rawText.lastIndexOf('}')

            if (startIndex == -1 || endIndex == -1) return null

            val jsonString = rawText.substring(startIndex, endIndex + 1)
            val json = JSONObject(jsonString)

            MealSuggestion(
                dishName = json.optString("dishName", "Gợi ý món ăn"),
                reason = json.optString("reason", "Phù hợp dinh dưỡng"),
                protein = json.optInt("protein", 0),
                calories = json.optInt("calories", 0),
                icon = json.optString("icon", "🍽️")
            )
        } catch (e: Exception) {
            Log.e("GeminiHelper", "JSON Parse Error: ${e.message}")
            null
        }
    }



    private fun parseJsonToResult(jsonString: String): NutritionResult? {
        return try {
            val cleanJson = jsonString.replace("```json", "").replace("```", "").trim()
            val json = JSONObject(cleanJson)
            NutritionResult(
                name = json.optString("name", "Unknown Food"),
                calories = json.optInt("calories", 0),
                carbs = json.optDouble("carbs", 0.0).toFloat(),
                protein = json.optDouble("protein", 0.0).toFloat(),
                fat = json.optDouble("fat", 0.0).toFloat(),
                portion = json.optInt("portion", 100)
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}