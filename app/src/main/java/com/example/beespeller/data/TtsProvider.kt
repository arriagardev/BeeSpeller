package com.example.beespeller.data

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

class TtsProvider(context: Context) {
    private var tts: TextToSpeech? = null
    private var isReady = false

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.let {
                    it.setLanguage(Locale.US)
                    isReady = true
                }
            }
        }
    }

    fun speak(text: String, slow: Boolean = false) {
        if (isReady) {
            tts?.let {
                it.setSpeechRate(if (slow) 0.4f else 1.0f)
                it.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
            }
        }
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
    }
}
