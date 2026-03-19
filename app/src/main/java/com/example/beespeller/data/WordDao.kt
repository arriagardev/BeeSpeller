package com.example.beespeller.data

import androidx.room.*
import com.example.beespeller.model.Word
import com.example.beespeller.model.AiHint
import kotlinx.coroutines.flow.Flow

@Dao
interface WordDao {
    @Query("SELECT * FROM words")
    fun getAllWords(): Flow<List<Word>>

    @Query("SELECT * FROM words")
    suspend fun getAllWordsList(): List<Word>

    @Query("SELECT * FROM words WHERE word = :word LIMIT 1")
    suspend fun getWord(word: String): Word?

    @Query("SELECT * FROM words WHERE isPreloaded = :isPreloaded")
    fun getWordsByType(isPreloaded: Boolean): Flow<List<Word>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWord(word: Word): Long

    @Update
    suspend fun updateWord(word: Word): Int

    @Delete
    suspend fun deleteWord(word: Word): Int

    @Query("DELETE FROM words")
    suspend fun deleteAllWords()

    @Transaction
    suspend fun clearAndLoad(words: List<Word>, hints: List<AiHint>) {
        deleteAllWords()
        words.forEach { insertWord(it) }
        deleteAllHints()
        hints.forEach { insertAiHint(it) }
    }

    // AI Hint Cache
    @Query("SELECT * FROM ai_hints")
    suspend fun getAllAiHints(): List<AiHint>

    @Query("SELECT * FROM ai_hints WHERE word = :word LIMIT 1")
    suspend fun getAiHint(word: String): AiHint?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAiHint(hint: AiHint)

    @Query("DELETE FROM ai_hints")
    suspend fun deleteAllHints()
}
