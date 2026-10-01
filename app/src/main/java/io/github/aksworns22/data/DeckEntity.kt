package io.github.aksworns22.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import io.github.aksworns22.home.Deck

@Entity(tableName = "decks")
data class DeckEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String
)

fun DeckEntity.toDeck() = Deck(id = id, name = name)
