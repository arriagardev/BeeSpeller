package com.example.beespeller.model

enum class SpellingStage(val repeatsRequired: Int) {
    CLASS(0),
    ELIMINATORY(3),
    FINAL(5)
}
