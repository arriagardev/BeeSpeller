package com.example.beespeller.data

import com.example.beespeller.model.Word
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SpellingEngine(
    private val repository: WordRepository,
    private val ttsProvider: TtsProvider,
    private val geminiProvider: GeminiContentProvider
) {
    private val _currentWord = MutableStateFlow<Word?>(null)
    val currentWord: StateFlow<Word?> = _currentWord.asStateFlow()

    private val _gameState = MutableStateFlow<GameState>(GameState.Idle)
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    sealed class GameState {
        object Idle : GameState()
        object Loading : GameState()
        data class Spelling(val word: Word) : GameState()
        data class Translating(val word: Word) : GameState()
        data class Feedback(val word: Word, val isCorrect: Boolean, val errorType: ErrorType? = null) : GameState()
        object Finished : GameState()
    }

    enum class ErrorType { SPELLING, TRANSLATION }

    fun startSession(words: List<Word>) {
        if (words.isEmpty()) {
            _gameState.value = GameState.Finished
            return
        }
        nextWord(words, 0)
    }

    private fun nextWord(words: List<Word>, index: Int) {
        if (index >= words.size) {
            _gameState.value = GameState.Finished
            return
        }
        val word = words[index]
        _currentWord.value = word
        _gameState.value = GameState.Spelling(word)
        
        // Dictate word at start
        ttsProvider.speak("The word is ${word.word}")
    }

    suspend fun submitSpelling(input: String) {
        val word = _currentWord.value ?: return
        val isCorrect = input.trim().equals(word.word.trim(), ignoreCase = true)

        if (isCorrect) {
            _gameState.value = GameState.Translating(word)
            ttsProvider.speak("Correct! Now, what is the Spanish translation?")
        } else {
            repository.updateWordProgress(word, false)
            _gameState.value = GameState.Feedback(word, false, ErrorType.SPELLING)
            val spelling = word.word.toCharArray().joinToString(separator = ", ")
            ttsProvider.speak("That's not quite right. The word is spelled $spelling")
        }
    }

    suspend fun submitTranslation(input: String) {
        val word = _currentWord.value ?: return
        val userInput = input.trim()
        val targetTranslation = word.spanishTranslation.trim()
        
        var isCorrect = if (targetTranslation.isEmpty()) {
            true
        } else {
            userInput.equals(targetTranslation, ignoreCase = true)
        }

        // If not an exact match, verify with Gemini for "Actual Translation" or synonyms
        if (!isCorrect && userInput.isNotBlank()) {
            _gameState.value = GameState.Loading
            isCorrect = geminiProvider.verifyTranslation(word.word, userInput)
        }

        repository.updateWordProgress(word, isCorrect)

        if (isCorrect) {
            _gameState.value = GameState.Feedback(word, true)
            ttsProvider.speak("Excellent! Both spelling and translation are correct.")
        } else {
            _gameState.value = GameState.Feedback(word, false, ErrorType.TRANSLATION)
            ttsProvider.speak("The translation is incorrect. The word in Spanish is ${word.spanishTranslation}")
        }
    }

    fun tryAgain() {
        val word = _currentWord.value ?: return
        _gameState.value = GameState.Spelling(word)
        ttsProvider.speak("Let's try again. The word is ${word.word}")
    }

    fun proceedToNext(words: List<Word>, currentIndex: Int) {
        nextWord(words, currentIndex + 1)
    }
    
    fun repeatWord(slow: Boolean = false) {
        _currentWord.value?.let {
            ttsProvider.speak(it.word, slow)
        }
    }

    suspend fun provideDefinition() {
        _currentWord.value?.let {
            if (it.definition == "" || it.definition.contains("Tap 'Meaning'")) {
                val updated = repository.refreshWordDetails(it)
                _currentWord.value = updated
                ttsProvider.speak("The definition is: ${updated.definition}")
            } else {
                ttsProvider.speak("The definition is: ${it.definition}")
            }
        }
    }

    suspend fun provideExample() {
        _currentWord.value?.let {
            if (it.example == "" || it.example == "N/A") {
                val updated = repository.refreshWordDetails(it)
                _currentWord.value = updated
                ttsProvider.speak("An example is: ${updated.example}")
            } else {
                ttsProvider.speak("An example is: ${it.example}")
            }
        }
    }
}
