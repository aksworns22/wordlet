package io.github.aksworns22.home

import io.github.aksworns22.fsrs.Card

data class Word(
    val term: String,
    val meaning: String,
    val card: Card = Card()
)
