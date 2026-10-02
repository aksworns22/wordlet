package io.github.aksworns22.wordlet.home

import io.github.aksworns22.fsrs.Card

data class Word(
    val term: String,
    val meaning: String,
    val example: String = "",
    val deckId: Long = Deck.BASIC_ID,
    val card: Card = Card()
)
