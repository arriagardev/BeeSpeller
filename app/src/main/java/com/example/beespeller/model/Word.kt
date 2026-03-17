package com.example.beespeller.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "words")
data class Word(
    @PrimaryKey val word: String,
    val numericId: Int = 0,
    val spanishTranslation: String = "",
    val definition: String = "",
    val partOfSpeech: String = "",
    val example: String = "",
    val masteryLevel: Int = 0, // 0 to 5 stars
    val isPreloaded: Boolean = false,
    val lastPracticed: Long = 0L
)
