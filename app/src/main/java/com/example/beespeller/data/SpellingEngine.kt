package com.example.beespeller.data

import com.example.beespeller.model.Word
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SpellingEngine(
    private val repository: WordRepository,
    private val ttsProvider: TtsProvider,
    private val geminiProvider: GeminiContentProvider,
    private val settingsManager: SettingsManager
) {
    private val engineScope = CoroutineScope(Dispatchers.Main)

    private val _currentWord = MutableStateFlow<Word?>(null)
    val currentWord: StateFlow<Word?> = _currentWord.asStateFlow()

    private val _gameState = MutableStateFlow<GameState>(GameState.Idle)
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    private val _sessionWords = MutableStateFlow<List<Word>>(emptyList())
    val sessionWords: StateFlow<List<Word>> = _sessionWords.asStateFlow()

    private val _isHintCached = MutableStateFlow(false)
    val isHintCached: StateFlow<Boolean> = _isHintCached.asStateFlow()

    private var challengeAiUsages = 0

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

        val preparedWords = words.distinctBy { it.word.lowercase().trim() }
            .shuffled()
            .take(10)
            .sortedBy { it.masteryLevel > 3 }

        _sessionWords.value = preparedWords
        challengeAiUsages = 0
        nextWord(0)
    }

    private fun nextWord(index: Int) {
        val words = _sessionWords.value
        if (index >= words.size) {
            _gameState.value = GameState.Finished
            return
        }
        val word = words[index]
        _currentWord.value = word
        _gameState.value = GameState.Spelling(word)
        
        updateCachedStatus(word.word)
        
        ttsProvider.speak("The word is ${word.word}")
    }

    private fun updateCachedStatus(word: String) {
        engineScope.launch {
            _isHintCached.value = repository.isHintCached(word)
        }
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

        if (!isCorrect && userInput.isNotBlank() && settingsManager.canUseAi()) {
            _gameState.value = GameState.Loading
            isCorrect = geminiProvider.verifyTranslation(word.word, userInput)
            settingsManager.incrementAiUsage()
        }

        repository.updateWordProgress(word, isCorrect)

        if (isCorrect) {
            _gameState.value = GameState.Feedback(word, true)
            ttsProvider.speak("Excellent!")
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

    fun proceedToNext(currentIndex: Int) {
        nextWord(currentIndex + 1)
    }
    
    fun repeatWord(slow: Boolean = false) {
        _currentWord.value?.let {
            ttsProvider.speak(it.word, if (slow) 0.4f else 1.0f)
        }
    }

    fun isAiOptionAvailable(): Boolean {
        return _isHintCached.value || (challengeAiUsages < settingsManager.challengeLimit && settingsManager.canUseAi())
    }

    suspend fun provideDefinition() {
        if (!isAiOptionAvailable()) return
        
        _currentWord.value?.let {
            val alreadyCached = repository.isHintCached(it.word)
            
            _gameState.value = GameState.Loading
            val updated = if (it.definition == "" || it.definition.contains("Tap 'Meaning'")) {
                if (!alreadyCached) {
                    challengeAiUsages++
                    settingsManager.incrementAiUsage()
                }
                repository.refreshWordDetails(it)
            } else {
                it
            }
            _currentWord.value = updated
            _isHintCached.value = true 
            _gameState.value = GameState.Spelling(updated)
            ttsProvider.speak("The definition is: ${updated.definition}", 0.7f)
        }
    }

    suspend fun provideExample() {
        if (!isAiOptionAvailable()) return
        
        _currentWord.value?.let {
            val alreadyCached = repository.isHintCached(it.word)

            _gameState.value = GameState.Loading
            val updated = if (it.example == "" || it.example == "N/A") {
                if (!alreadyCached) {
                    challengeAiUsages++
                    settingsManager.incrementAiUsage()
                }
                repository.refreshWordDetails(it)
            } else {
                it
            }
            _currentWord.value = updated
            _isHintCached.value = true
            _gameState.value = GameState.Spelling(updated)
            ttsProvider.speak("An example is: ${updated.example}", 0.7f)
        }
    }
}
