package io.github.aksworns22.wordlet.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import io.github.aksworns22.fsrs.Card
import io.github.aksworns22.fsrs.State
import io.github.aksworns22.wordlet.home.Word
import java.time.Instant

// fsrs 모듈은 Room에 의존하지 않으므로 Card 필드를 펼쳐서 저장한다.
@Entity(tableName = "words", indices = [Index("deckId")])
data class WordEntity(
    @PrimaryKey val id: Long,
    val deckId: Long,
    val term: String,
    val meaning: String,
    val example: String,
    val state: State,
    val step: Int?,
    val stability: Double?,
    val difficulty: Double?,
    val due: Instant,
    val lastReview: Instant?
)

fun WordEntity.toWord() =
    Word(
        term = term,
        meaning = meaning,
        example = example,
        deckId = deckId,
        card =
            Card(
                id = id,
                state = state,
                step = step,
                stability = stability,
                difficulty = difficulty,
                due = due,
                lastReview = lastReview
            )
    )

fun Word.toEntity() =
    WordEntity(
        id = card.id,
        deckId = deckId,
        term = term,
        meaning = meaning,
        example = example,
        state = card.state,
        step = card.step,
        stability = card.stability,
        difficulty = card.difficulty,
        due = card.due,
        lastReview = card.lastReview
    )
