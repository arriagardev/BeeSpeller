package com.example.beespeller.data

import androidx.room.*
import com.example.beespeller.model.Word
import com.example.beespeller.model.SpellingStage
import kotlinx.coroutines.flow.Flow

@Dao
interface WordDao {
    @Query("SELECT * FROM words")
    fun getAllWords(): Flow<List<Word>>

    @Query("SELECT * FROM words WHERE word = :word LIMIT 1")
    suspend fun getWord(word: String): Word?

    @Query("SELECT * FROM words WHERE stage = :stage")
    fun getWordsByStage(stage: SpellingStage): Flow<List<Word>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWord(word: Word)

    @Update
    suspend fun updateWord(word: Word)

    @Delete
    suspend fun deleteWord(word: Word)

    @Query("DELETE FROM words")
    suspend fun deleteAllWords()
}
