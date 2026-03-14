package com.example.beespeller.data

import com.example.beespeller.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class GeminiContentProvider {
    private val generativeModel = GenerativeModel(
        modelName = "gemini-1.5-flash",
        apiKey = BuildConfig.GEMINI_API_KEY
    )

    suspend fun fetchWordDetails(word: String): WordDetails? = withContext(Dispatchers.IO) {
        val prompt = """
            Provide a 4th-grade appropriate definition, part of speech, and a usage example for the word: "$word".
            Return the result ONLY as a JSON object with the following keys:
            - definition: A simple definition for a 9-year-old.
            - partOfSpeech: The part of speech (e.g., noun, verb, adjective).
            - example: A simple sentence using the word.
        """.trimIndent()

        try {
            val response = generativeModel.generateContent(prompt)
            val jsonText = response.text?.trim() ?: return@withContext null
            
            // Clean up possible markdown formatting
            val cleanJson = if (jsonText.startsWith("```json")) {
                jsonText.removePrefix("```json").removeSuffix("```").trim()
            } else if (jsonText.startsWith("```")) {
                jsonText.removePrefix("```").removeSuffix("```").trim()
            } else {
                jsonText
            }

            val jsonObject = JSONObject(cleanJson)
            WordDetails(
                definition = jsonObject.getString("definition"),
                partOfSpeech = jsonObject.getString("partOfSpeech"),
                example = jsonObject.getString("example")
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    data class WordDetails(
        val definition: String,
        val partOfSpeech: String,
        val example: String
    )
}
