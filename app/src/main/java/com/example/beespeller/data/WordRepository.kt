package com.example.beespeller.data

import com.example.beespeller.model.Word
import kotlinx.coroutines.flow.Flow

class WordRepository(
    private val wordDao: WordDao,
    private val geminiContentProvider: GeminiContentProvider
) {
    val allWords: Flow<List<Word>> = wordDao.getAllWords()

    fun getWordsByType(isPreloaded: Boolean): Flow<List<Word>> {
        return wordDao.getWordsByType(isPreloaded)
    }

    suspend fun addWord(wordText: String, isPreloaded: Boolean = false) {
        val existingWord = wordDao.getWord(wordText)
        if (existingWord != null) return

        val details = geminiContentProvider.fetchWordDetails(wordText)
        val word = if (details != null) {
            Word(
                word = wordText,
                definition = details.definition,
                partOfSpeech = details.partOfSpeech,
                example = details.example,
                masteryLevel = 0,
                isPreloaded = isPreloaded
            )
        } else {
            Word(
                word = wordText,
                definition = "Definition not found",
                partOfSpeech = "Unknown",
                example = "Example not found",
                masteryLevel = 0,
                isPreloaded = isPreloaded
            )
        }
        wordDao.insertWord(word)
    }

    suspend fun updateWordProgress(word: Word, correct: Boolean) {
        val currentLevel = word.masteryLevel
        val newLevel = if (correct) {
            (currentLevel + 1).coerceAtMost(5)
        } else {
            (currentLevel - 1).coerceAtLeast(0)
        }

        wordDao.updateWord(word.copy(
            masteryLevel = newLevel,
            lastPracticed = System.currentTimeMillis()
        ))
    }

    suspend fun deleteWord(word: Word) {
        wordDao.deleteWord(word)
    }

    suspend fun preloadInitialWords(words: List<PreloadedWord>) {
        for (pw in words) {
            val existing = wordDao.getWord(pw.english)
            if (existing == null) {
                wordDao.insertWord(Word(
                    word = pw.english,
                    numericId = pw.id,
                    spanishTranslation = pw.spanish,
                    isPreloaded = true,
                    definition = "Tap 'Meaning' to fetch info...",
                    partOfSpeech = "N/A",
                    example = "N/A"
                ))
            }
        }
    }

    suspend fun refreshWordDetails(word: Word): Word {
        val details = geminiContentProvider.fetchWordDetails(word.word)
        return if (details != null) {
            val updated = word.copy(
                definition = details.definition,
                partOfSpeech = details.partOfSpeech,
                example = details.example
            )
            wordDao.updateWord(updated)
            updated
        } else {
            word
        }
    }
}
