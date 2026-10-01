package io.github.aksworns22.data

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.aksworns22.fsrs.Card
import io.github.aksworns22.fsrs.State
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
class WordDaoTest {
    private lateinit var db: WordDatabase
    private lateinit var dao: WordDao

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, WordDatabase::class.java).build()
        dao = db.wordDao()
    }

    @After
    fun tearDown() = db.close()

    private suspend fun all() = dao.observeAll().first().map { it.toWord() }

    @Test
    fun insertedWordIsReadWithCard() =
        runBlocking {
            val word =
                Word(
                    term = "nuance",
                    meaning = "뉘앙스",
                    example = "a subtle nuance",
                    deckId = 2,
                    card =
                        Card(
                            id = 1,
                            state = State.Review,
                            stability = 5.0,
                            difficulty = 4.5,
                            due = Instant.ofEpochMilli(1_000),
                            lastReview = Instant.ofEpochMilli(500)
                        )
                )
            dao.insert(word.toEntity())

            assertEquals(listOf(word), all())
        }

    @Test
    fun recentlyAddedWordComesFirst() =
        runBlocking {
            dao.insert(Word("old", "옛", card = Card(id = 1)).toEntity())
            dao.insert(Word("new", "새", card = Card(id = 2)).toEntity())

            assertEquals(listOf("new", "old"), all().map { it.term })
        }

    @Test
    fun updateAndDeleteWord() =
        runBlocking {
            val word = Word("nuance", "뉘앙스", card = Card(id = 1))
            dao.insert(word.toEntity())

            val edited = word.copy(meaning = "미묘한 차이")
            dao.update(edited.toEntity())
            assertEquals(listOf(edited), all())

            dao.delete(edited.toEntity())
            assertEquals(emptyList<Word>(), all())
        }

    @Test
    fun inDeckReadsOnlyThatDecksWords() =
        runBlocking {
            dao.insert(Word("one", "하나", deckId = 1, card = Card(id = 1)).toEntity())
            dao.insert(Word("two", "둘", deckId = 2, card = Card(id = 2)).toEntity())

            assertEquals(listOf("two"), dao.inDeck(2).map { it.term })
        }
}
