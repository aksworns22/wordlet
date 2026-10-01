package io.github.aksworns22.data

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.aksworns22.fsrs.Card
import io.github.aksworns22.fsrs.State
import io.github.aksworns22.home.Deck
import io.github.aksworns22.home.Word
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class DeckDaoTest {
    private lateinit var db: WordDatabase
    private lateinit var dao: DeckDao

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, WordDatabase::class.java).build()
        dao = db.deckDao()
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun decksAreReadInCreatedOrder() =
        runBlocking {
            val first = dao.insert(DeckEntity(name = "토익"))
            val second = dao.insert(DeckEntity(name = "여행 회화"))

            assertEquals(
                listOf(Deck(first, "토익"), Deck(second, "여행 회화")),
                dao.observeAll().first().map { it.toDeck() }
            )
        }

    private suspend fun decks() = dao.observeAll().first().map { it.toDeck() }

    private suspend fun words() =
        db
            .wordDao()
            .observeAll()
            .first()
            .map { it.toWord() }

    private fun reviewed(
        id: Long,
        deckId: Long
    ) = Word(
        "word$id",
        "뜻",
        deckId = deckId,
        card =
            Card(
                id = id,
                state = State.Review,
                stability = 5.0,
                difficulty = 5.0,
                due = Instant.ofEpochMilli(9_000),
                lastReview = Instant.ofEpochMilli(1_000)
            )
    )

    @Test
    fun renameDeck() =
        runBlocking {
            val id = dao.insert(DeckEntity(name = "토익"))
            dao.rename(id, "토플")

            assertEquals(listOf(Deck(id, "토플")), decks())
        }

    @Test
    fun resetOnlyTouchesWordsInDeck() =
        runBlocking {
            val target = dao.insert(DeckEntity(name = "토익"))
            val other = dao.insert(DeckEntity(name = "토플"))
            val kept = reviewed(2, other)
            db.wordDao().insertAll(listOf(reviewed(1, target).toEntity(), kept.toEntity()))

            val due = Instant.ofEpochMilli(5_000)
            dao.reset(target, State.Learning, 0, due)

            val reset = words().first { it.card.id == 1L }.card
            assertEquals(Card(id = 1, state = State.Learning, step = 0, due = due), reset)
            assertEquals(kept, words().first { it.card.id == 2L })
        }

    @Test
    fun deleteRemovesDeckWithItsWords() =
        runBlocking {
            val target = dao.insert(DeckEntity(name = "토익"))
            val other = dao.insert(DeckEntity(name = "토플"))
            db.wordDao().insertAll(listOf(reviewed(1, target).toEntity(), reviewed(2, other).toEntity()))

            dao.delete(target)

            assertEquals(listOf(Deck(other, "토플")), decks())
            assertEquals(listOf(2L), words().map { it.card.id })
        }
}
