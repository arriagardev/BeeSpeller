package com.example.beespeller.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "words")
data class Word(
    @PrimaryKey val word: String,
    val definition: String,
    val partOfSpeech: String,
    val example: String,
    val stage: SpellingStage = SpellingStage.CLASS,
    val repeats: Int = 0,
    val lastPracticed: Long = 0L
)
