package com.example.beespeller.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ai_hints")
data class AiHint(
    @PrimaryKey val word: String,
    val definition: String,
    val partOfSpeech: String,
    val example: String
)
