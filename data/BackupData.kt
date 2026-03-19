package com.example.beespeller.data

import com.example.beespeller.model.Word
import com.example.beespeller.model.AiHint

data class BackupData(
    val words: List<Word>,
    val hints: List<AiHint>
)
