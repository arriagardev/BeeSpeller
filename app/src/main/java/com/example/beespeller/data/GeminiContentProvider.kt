package com.example.beespeller.data

import com.example.beespeller.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class GeminiContentProvider {
    // The 404 error "model not found for API version v1beta" with the Google AI SDK 0.9.0
    // is often caused by an outdated internal endpoint or an incompatibility with
    // specific model identifiers like "gemini-1.5-flash".
    // 
    // Using "gemini-pro" is more stable for older SDK versions.
    //
    // Using "gemini-2.5-flash" is more stable for newer SDK versions.
    private val generativeModel = GenerativeModel(
        modelName = "gemini-2.5-flash",
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

    suspend fun verifyTranslation(english: String, spanish: String): Boolean = withContext(Dispatchers.IO) {
        val prompt = """
            Is "$spanish" a correct Spanish translation for the English word "$english"? 
            Consider synonyms and context. 
            Return "true" or "false".
        """.trimIndent()

        try {
            val response = generativeModel.generateContent(prompt)
            response.text?.trim()?.lowercase()?.contains("true") == true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    data class WordDetails(
        val definition: String,
        val partOfSpeech: String,
        val example: String
    )
}
