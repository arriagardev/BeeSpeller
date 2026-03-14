package com.example.beespeller.data

import com.example.beespeller.model.Word
import com.example.beespeller.model.SpellingStage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SpellingEngine(
    private val repository: WordRepository,
    private val ttsProvider: TtsProvider
) {
    private val _currentWord = MutableStateFlow<Word?>(null)
    val currentWord: StateFlow<Word?> = _currentWord.asStateFlow()

    private val _gameState = MutableStateFlow<GameState>(GameState.Idle)
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    sealed class GameState {
        object Idle : GameState()
        object Loading : GameState()
        data class Spelling(val word: Word, val attempts: Int = 0) : GameState()
        data class Feedback(val word: Word, val isCorrect: Boolean) : GameState()
        object Finished : GameState()
    }

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

    suspend fun submitSpelling(input: String, words: List<Word>, currentIndex: Int) {
        val word = _currentWord.value ?: return
        val isCorrect = input.trim().equals(word.word.trim(), ignoreCase = true)

        repository.updateWordProgress(word, isCorrect)

        _gameState.value = GameState.Feedback(word, isCorrect)

        if (isCorrect) {
            ttsProvider.speak("Correct! Well done.")
        } else {
            val spelling = word.word.toCharArray().joinToString(separator = ", ")
            ttsProvider.speak("That's not quite right. The word is spelled $spelling")
        }
    }

    fun proceedToNext(words: List<Word>, currentIndex: Int) {
        nextWord(words, currentIndex + 1)
    }
    
    fun repeatWord() {
        _currentWord.value?.let {
            ttsProvider.speak(it.word)
        }
    }

    fun provideDefinition() {
        _currentWord.value?.let {
            ttsProvider.speak("The definition is: ${it.definition}")
        }
    }

    fun provideExample() {
        _currentWord.value?.let {
            ttsProvider.speak("An example is: ${it.example}")
        }
    }
}
