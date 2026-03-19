package com.example.beespeller.data

import android.content.Context
import android.net.Uri
import android.os.Environment
import com.example.beespeller.model.Word
import com.example.beespeller.model.AiHint
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class WordRepository(
    private val wordDao: WordDao,
    private val geminiContentProvider: GeminiContentProvider,
    private val context: Context
) {
    val allWords: Flow<List<Word>> = wordDao.getAllWords()

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
    
    // Adapter for full backup (Words + Hints)
    private val backupAdapter = moshi.adapter(BackupData::class.java)

    fun getWordsByType(isPreloaded: Boolean): Flow<List<Word>> {
        return wordDao.getWordsByType(isPreloaded)
    }

    suspend fun addWord(wordText: String, isPreloaded: Boolean = false) {
        val existingWord = wordDao.getWord(wordText)
        if (existingWord != null) return

        val details = fetchAiHint(wordText)
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

    suspend fun clearProgress() {
        wordDao.deleteAllWords()
        wordDao.deleteAllHints()
        // Re-preload initial words to maintain app state
        preloadInitialWords(PreloadedWords.list)
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
                    definition = pw.definition.ifEmpty { "Tap 'Meaning' to fetch info..." },
                    partOfSpeech = pw.partOfSpeech.ifEmpty { "N/A" },
                    example = pw.example.ifEmpty { "N/A" }
                ))
            } else if (existing.isPreloaded) {
                // Update existing preloaded words if their definitions or examples were updated in code
                var needsUpdate = false
                var updatedWord = existing.copy(
                    spanishTranslation = pw.spanish,
                    numericId = pw.id
                )
                
                if (existing.definition == "Tap 'Meaning' to fetch info..." && pw.definition.isNotEmpty()) {
                    updatedWord = updatedWord.copy(definition = pw.definition)
                    needsUpdate = true
                }
                if (existing.example == "N/A" && pw.example.isNotEmpty()) {
                    updatedWord = updatedWord.copy(example = pw.example)
                    needsUpdate = true
                }
                if (existing.partOfSpeech == "N/A" && pw.partOfSpeech.isNotEmpty()) {
                    updatedWord = updatedWord.copy(partOfSpeech = pw.partOfSpeech)
                    needsUpdate = true
                }
                
                if (needsUpdate || existing.spanishTranslation != pw.spanish || existing.numericId != pw.id) {
                    wordDao.updateWord(updatedWord)
                }
            }
        }
    }

    suspend fun isHintCached(word: String): Boolean {
        return wordDao.getAiHint(word) != null
    }

    suspend fun fetchAiHint(word: String): AiHint? {
        val cached = wordDao.getAiHint(word)
        if (cached != null) return cached

        val details = geminiContentProvider.fetchWordDetails(word)
        return if (details != null) {
            val hint = AiHint(
                word = word,
                definition = details.definition,
                partOfSpeech = details.partOfSpeech,
                example = details.example
            )
            wordDao.insertAiHint(hint)
            hint
        } else {
            null
        }
    }

    suspend fun refreshWordDetails(word: Word): Word {
        val details = fetchAiHint(word.word)
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

    suspend fun exportProgress(): String = withContext(Dispatchers.IO) {
        val words = wordDao.getAllWordsList()
        val hints = wordDao.getAllAiHints()
        val backup = BackupData(words, hints)
        
        val json = backupAdapter.toJson(backup)
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val file = File(downloadsDir, "beespeller_full_backup_${System.currentTimeMillis()}.json")
        FileOutputStream(file).use { 
            it.write(json.toByteArray())
        }
        file.absolutePath
    }

    suspend fun importProgress(uri: Uri) = withContext(Dispatchers.IO) {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            val json = inputStream.bufferedReader().use { it.readText() }
            val backup = backupAdapter.fromJson(json)
            if (backup != null) {
                wordDao.clearAndLoad(backup.words, backup.hints)
            }
        }
    }
}
