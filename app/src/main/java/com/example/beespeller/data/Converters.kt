package com.example.beespeller.data

import androidx.room.TypeConverter
import com.example.beespeller.model.SpellingStage

class Converters {
    @TypeConverter
    fun fromSpellingStage(stage: SpellingStage): String {
        return stage.name
    }

    @TypeConverter
    fun toSpellingStage(value: String): SpellingStage {
        return SpellingStage.valueOf(value)
    }
}
