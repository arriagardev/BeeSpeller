package com.example.beespeller.data

import com.example.beespeller.model.Word
import com.example.beespeller.model.SpellingStage
import kotlinx.coroutines.flow.Flow

class WordRepository(
    private val wordDao: WordDao,
    private val geminiContentProvider: GeminiContentProvider
) {
    val allWords: Flow<List<Word>> = wordDao.getAllWords()

    fun getWordsByStage(stage: SpellingStage): Flow<List<Word>> {
        return wordDao.getWordsByStage(stage)
    }

    suspend fun addWord(wordText: String) {
        val existingWord = wordDao.getWord(wordText)
        if (existingWord != null) return

        val details = geminiContentProvider.fetchWordDetails(wordText)
        if (details != null) {
            val word = Word(
                word = wordText,
                definition = details.definition,
                partOfSpeech = details.partOfSpeech,
                example = details.example,
                stage = SpellingStage.CLASS,
                repeats = 0
            )
            wordDao.insertWord(word)
        } else {
            // Fallback or error handling if Gemini fails
            val fallbackWord = Word(
                word = wordText,
                definition = "Definition not found",
                partOfSpeech = "Unknown",
                example = "Example not found",
                stage = SpellingStage.CLASS,
                repeats = 0
            )
            wordDao.insertWord(fallbackWord)
        }
    }

    suspend fun updateWordProgress(word: Word, correct: Boolean) {
        var newRepeats = word.repeats
        var newStage = word.stage

        if (correct) {
            newRepeats++
            if (newRepeats >= newStage.repeatsRequired) {
                // Move to next stage
                newStage = when (newStage) {
                    SpellingStage.CLASS -> SpellingStage.ELIMINATORY
                    SpellingStage.ELIMINATORY -> SpellingStage.FINAL
                    SpellingStage.FINAL -> SpellingStage.FINAL // Already at max
                }
                newRepeats = 0 // Reset repeats for the new stage
            }
        } else {
            // Optional: reset repeats on failure or just stay
            newRepeats = 0
        }

        wordDao.updateWord(word.copy(repeats = newRepeats, stage = newStage, lastPracticed = System.currentTimeMillis()))
    }

    suspend fun deleteWord(word: Word) {
        wordDao.deleteWord(word)
    }
}
