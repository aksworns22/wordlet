package io.github.aksworns22.wordlet.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import io.github.aksworns22.fsrs.State
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
abstract class DeckDao {
    // 먼저 만든 단어장이 앞에 온다.
    @Query("SELECT * FROM decks ORDER BY id")
    abstract fun observeAll(): Flow<List<DeckEntity>>

    /** 새 단어장의 id를 돌려준다. */
    @Insert
    abstract suspend fun insert(deck: DeckEntity): Long

    @Query("UPDATE decks SET name = :name WHERE id = :id")
    abstract suspend fun rename(
        id: Long,
        name: String
    )

    /** [id] 단어장의 모든 단어를 학습 기록이 없는 새 카드로 되돌린다. */
    @Query(
        "UPDATE words SET state = :state, step = :step, stability = NULL, difficulty = NULL, " +
            "due = :due, lastReview = NULL WHERE deckId = :id"
    )
    abstract suspend fun reset(
        id: Long,
        state: State,
        step: Int?,
        due: Instant
    )

    /** 단어장과 그 안의 단어를 함께 지운다. */
    @Transaction
    open suspend fun delete(id: Long) {
        deleteWords(id)
        deleteDeck(id)
    }

    @Query("DELETE FROM words WHERE deckId = :id")
    protected abstract suspend fun deleteWords(id: Long)

    @Query("DELETE FROM decks WHERE id = :id")
    protected abstract suspend fun deleteDeck(id: Long)
}
