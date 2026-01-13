package com.example.fitlifesmarthealthlifestyleapp.ui.nutrition

import android.graphics.Bitmap
import com.example.fitlifesmarthealthlifestyleapp.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class GeminiNutritionHelper {
    private val apiKey = BuildConfig.API_KEY_GEMINI // Đảm bảo bạn đã có Key ở đây

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
                val prompt = """
                    Analyze this food image. Identify the main dish name (in English or Vietnamese) and estimate nutrition for 1 serving.
                    Return ONLY a raw JSON object with this structure (no markdown, no ```json tags):
                    {
                      "name": "dish name",
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

    // --- MỚI THÊM: Hàm gợi ý món ăn thông minh ---
    suspend fun suggestNextMeal(
        remainingCarbs: Int,
        remainingProtein: Int,
        remainingFat: Int,
        timeOfDay: String
    ): MealSuggestion? {
        return withContext(Dispatchers.IO) {
            try {
                val prompt = """
                    I have these macros remaining for today: 
                    - Carbs: ${remainingCarbs}g
                    - Protein: ${remainingProtein}g
                    - Fat: ${remainingFat}g
                    
                    It is currently $timeOfDay.
                    Suggest ONE specific Vietnamese or common healthy dish that fits these remaining macros well.
                    
                    Return ONLY a raw JSON object with this structure (no markdown):
                    {
                      "dishName": "Dish Name (Vietnamese)",
                      "reason": "Short explanation why (max 15 words) in Vietnamese",
                      "protein": 30,
                      "calories": 250,
                      "icon": "🍗" 
                    }
                """.trimIndent()

                val response = generativeModel.generateContent(prompt)
                val text = response.text ?: return@withContext null

                // Parse JSON
                val cleanJson = text.replace("```json", "").replace("```", "").trim()
                val json = JSONObject(cleanJson)

                MealSuggestion(
                    dishName = json.optString("dishName", "Món ăn nhẹ"),
                    reason = json.optString("reason", "Phù hợp mục tiêu dinh dưỡng"),
                    protein = json.optInt("protein", 0),
                    calories = json.optInt("calories", 0),
                    icon = json.optString("icon", "🍲")
                )
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
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