package com.example.beespeller.data

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class SpeechRecognizerManager(private val context: Context) {
    private val appContext = context.applicationContext
    private var speechRecognizer: SpeechRecognizer? = null
    
    private val _recognizedLetter = MutableStateFlow("")
    val recognizedLetter: StateFlow<String> = _recognizedLetter.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val recognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            Log.d("SpeechManager", "Ready for speech")
            _isListening.value = true
        }

        override fun onBeginningOfSpeech() {
            Log.d("SpeechManager", "Beginning of speech")
        }
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        
        override fun onEndOfSpeech() {
            Log.d("SpeechManager", "End of speech")
            _isListening.value = false
        }

        override fun onError(error: Int) {
            val message = when (error) {
                SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                SpeechRecognizer.ERROR_CLIENT -> "Client side error"
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                SpeechRecognizer.ERROR_NETWORK -> "Network error"
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                SpeechRecognizer.ERROR_NO_MATCH -> "No match"
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Service busy"
                SpeechRecognizer.ERROR_SERVER -> "Server error"
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
                SpeechRecognizer.ERROR_SERVER_DISCONNECTED -> "Server disconnected (Error 11)"
                13 -> "Language not supported (Error 13)"
                else -> "Unknown error: $error"
            }
            Log.e("SpeechManager", "Error: $message ($error)")
            _isListening.value = false
            
            // If it's a language error or server disconnect, we might need to recreate the recognizer
            if (error == 11 || error == 13) {
                destroy()
            }
        }

        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            Log.d("SpeechManager", "Results: $matches")
            if (!matches.isNullOrEmpty()) {
                // Some engines return [s, h, y], others return [s h y]
                // We'll process all of them
                val combinedText = matches.joinToString(" ").lowercase().trim()
                val result = parseSpeechResult(combinedText)
                
                if (result.isNotEmpty()) {
                    Log.d("SpeechManager", "Matched result: $result")
                    _recognizedLetter.value = result
                } else {
                    // If no complex match, just take the first raw result if it's reasonably short
                    val first = matches[0].replace(" ", "").lowercase()
                    if (first.length <= 10) {
                        Log.d("SpeechManager", "Fallback matched: $first")
                        _recognizedLetter.value = first
                    }
                }
            }
            _isListening.value = false
        }

        override fun onPartialResults(partialResults: Bundle?) {}
        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    private fun parseSpeechResult(text: String): String {
        if (text.isEmpty()) return ""
        
        // 1. Check if the whole thing is a single letter name
        val singleLetter = matchToSingleLetter(text)
        if (singleLetter.isNotEmpty()) return singleLetter

        // 2. Handle "letter X"
        if (text.startsWith("letter ") && text.length > 7) {
            return text.substring(7, 8)
        }

        // 3. Handle multiple letters with spaces (e.g., "s h y")
        val parts = text.split(Regex("\\s+"))
        if (parts.size > 1) {
            val letters = parts.map { matchToSingleLetter(it) }
            if (letters.all { it.isNotEmpty() }) {
                return letters.joinToString("")
            }
        }

        // 4. Fallback: if it's a short word without spaces, just return it
        val clean = text.replace(" ", "")
        if (clean.length <= 5) {
             return clean
        }

        return ""
    }

    private fun matchToSingleLetter(text: String): String {
        return when (text) {
            "a", "ay", "hey" -> "a"
            "be", "bee", "b" -> "b"
            "see", "sea", "c" -> "c"
            "dee", "d" -> "d"
            "e" -> "e"
            "ef", "f" -> "f"
            "gee", "g" -> "g"
            "aitch", "h", "age" -> "h"
            "i", "eye" -> "i"
            "jay", "j" -> "j"
            "kay", "k" -> "k"
            "el", "l" -> "l"
            "em", "m" -> "m"
            "en", "n" -> "n"
            "o", "oh" -> "o"
            "pee", "p" -> "p"
            "cue", "q", "queue" -> "q"
            "ar", "are", "r" -> "r"
            "ess", "s" -> "s"
            "tee", "t", "tea" -> "t"
            "you", "u" -> "u"
            "vee", "v" -> "v"
            "double you", "w" -> "w"
            "ex", "x" -> "x"
            "why", "y" -> "y"
            "zee", "zed", "z" -> "z"
            else -> if (text.length == 1 && text[0] in 'a'..'z') text else ""
        }
    }

    fun startListening() {
        if (speechRecognizer == null) {
            // Standard recognizer is usually more reliable than forcing on-device if not configured
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(appContext)
            speechRecognizer?.setRecognitionListener(recognitionListener)
        }
        
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.US.toString())
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3) // Get more results to increase match chance
            // Removed EXTRA_PREFER_OFFLINE to fix Error 13
        }
        
        try {
            speechRecognizer?.startListening(intent)
            _isListening.value = true
        } catch (e: Exception) {
            Log.e("SpeechManager", "Failed to start listening", e)
            _isListening.value = false
        }
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
        _isListening.value = false
    }

    fun resetLetter() {
        _recognizedLetter.value = ""
    }

    fun destroy() {
        try {
            speechRecognizer?.destroy()
        } catch (e: Exception) {}
        speechRecognizer = null
    }
}
