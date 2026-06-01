package com.example.util

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

data class ParsedReceipt(
    val amount: Double,
    val currency: String,
    val category: String,
    val notes: String
)

object GeminiClient {
    private const val TAG = "GeminiClient"
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun parseReceipt(bitmap: Bitmap): ParsedReceipt? = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.w(TAG, "Empty or placeholder Gemini API Key detected.")
            return@withContext null
        }

        // Convert bitmap to Base64 JPEG string
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
        val imageBytes = outputStream.toByteArray()
        val base64Image = Base64.encodeToString(imageBytes, Base64.NO_WRAP)

        // Build prompt instruction
        val promptText = """
            Analyze this receipt image and extract the expense detail. Return a raw JSON object with this exact schema:
            {
              "amount": 12.34, // Number representating total amount from receipt
              "currency": "USD", // "USD" or "INR" strictly. Look for currency symbols on the receipt to decide, default to "USD" if not found.
              "category": "Food", // Choose exactly one of: "Food", "Travel", "Bills", "Shopping", "Entertainment", "Others"
              "notes": "Starbucks Coffee" // Short description or merchant name
            }
            Do not include backticks, markdown markers, or any text other than the raw JSON itself.
        """.trimIndent()

        // Construct JSON Payload
        val jsonPayload = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", promptText)
                        })
                        put(JSONObject().apply {
                            put("inlineData", JSONObject().apply {
                                put("mimeType", "image/jpeg")
                                put("data", base64Image)
                            })
                        })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("responseMimeType", "application/json")
            })
        }

        val requestBody = jsonPayload.toString().toRequestBody("application/json".toMediaType())
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.e(TAG, "API Call failed with code: ${response.code}, message: ${response.message}")
                return@withContext null
            }

            val bodyString = response.body?.string() ?: return@withContext null
            Log.d(TAG, "Response: $bodyString")

            val jsonResponse = JSONObject(bodyString)
            val candidates = jsonResponse.getJSONArray("candidates")
            val content = candidates.getJSONObject(0).getJSONObject("content")
            val parts = content.getJSONArray("parts")
            val rawText = parts.getJSONObject(0).getString("text").trim()

            val parsedObj = JSONObject(rawText)
            val amount = parsedObj.optDouble("amount", 0.0)
            val currency = parsedObj.optString("currency", "USD").uppercase()
            val category = parsedObj.optString("category", "Others")
            val notes = parsedObj.optString("notes", "Scanned Receipt")

            ParsedReceipt(
                amount = amount,
                currency = if (currency == "INR" || currency == "₹") "INR" else "USD",
                category = when (category) {
                    "Food", "Travel", "Bills", "Shopping", "Entertainment", "Others" -> category
                    else -> "Others"
                },
                notes = notes
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse receipt with Gemini", e)
            null
        }
    }

    suspend fun getInsights(expensesJson: String, monthlyBudget: Double, currency: String): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "Set your real Gemini API key in the secrets panel to get live personal expense optimization insights."
        }

        val promptText = """
            Below are the recent expenses recorded by the user in currency: $currency.
            Monthly Budget: $monthlyBudget $currency.

            Expenses List:
            $expensesJson

            Analyze these expenses and write a concise, ultra-personalized financial optimization tip or insight in exactly 2-3 impact-driven lines. Mention specific details (e.g. "Your biggest expense is Food, which consumes 40% of your budget"). Say something encouraging and actionable. Make sure you don't list JSON, formatting or code blocks. Keep it simple, conversational and personal.
        """.trimIndent()

        val jsonPayload = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", promptText)
                        })
                    })
                })
            })
        }

        val requestBody = jsonPayload.toString().toRequestBody("application/json".toMediaType())
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext "Check your monthly budget and try again to calculate insights."

            val bodyString = response.body?.string() ?: return@withContext "No insights text."
            val jsonResponse = JSONObject(bodyString)
            val candidates = jsonResponse.getJSONArray("candidates")
            val content = candidates.getJSONObject(0).getJSONObject("content")
            val parts = content.getJSONArray("parts")
            parts.getJSONObject(0).getString("text").trim()
        } catch (e: Exception) {
            "Budget successfully tracking. Complete more expenses to trigger specialized AI visual recommendations."
        }
    }
}
