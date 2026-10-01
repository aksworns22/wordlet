package io.github.aksworns22.study

import io.github.aksworns22.fsrs.Card
import io.github.aksworns22.fsrs.State
import io.github.aksworns22.home.Word
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Duration
import java.time.Instant

class StudyQueueTest {
    private val now = Instant.ofEpochSecond(1_000_000)

    private fun new(id: Long) = Word("new$id", "", card = Card(id = id, due = now.minusSeconds(id)))

    private fun seen(
        id: Long,
        dueIn: Duration
    ) = Word(
        "seen$id",
        "",
        card =
            Card(
                id = id,
                state = State.Review,
                stability = 1.0,
                difficulty = 5.0,
                due = now.plus(dueIn),
                lastReview = now.minus(Duration.ofDays(1))
            )
    )

    @Test
    fun dueReviewComesBeforeNewWords() {
        val words = listOf(new(10), seen(1, Duration.ofMinutes(-5)), seen(2, Duration.ofMinutes(-30)))
        assertEquals(2L, nextWord(words, now)?.card?.id)
    }

    @Test
    fun newWordsFollowHomeListOrder() {
        val words = listOf(new(1), new(3), new(2), seen(4, Duration.ofDays(1)))
        assertEquals(3L, nextWord(words, now)?.card?.id)
    }

    @Test
    fun studiesAheadWhenNothingIsDue() {
        val words = listOf(seen(1, Duration.ofDays(3)), seen(2, Duration.ofMinutes(10)))
        assertEquals(2L, nextWord(words, now)?.card?.id)
    }

    private fun learning(
        id: Long,
        dueIn: Duration
    ) = Word(
        "learning$id",
        "",
        card =
            Card(
                id = id,
                state = State.Learning,
                stability = 0.5,
                difficulty = 5.0,
                due = now.plus(dueIn),
                lastReview = now.minusSeconds(30)
            )
    )

    @Test
    fun newWordsComeWhileLearningWordsAreBelowLimit() {
        val words = (1L until LEARNING_LIMIT).map { learning(it, Duration.ofMinutes(it)) } + new(100)
        assertEquals(100L, nextWord(words, now)?.card?.id)
    }

    @Test
    fun learningWordsComeAheadOfNewWordsWhenLimitIsReached() {
        val words = (1L..LEARNING_LIMIT).map { learning(it, Duration.ofMinutes(it)) } + new(100)
        assertEquals(1L, nextWord(words, now)?.card?.id)
    }

    @Test
    fun skipsPreviousWordWhenOthersExist() {
        val words = listOf(seen(1, Duration.ofMinutes(-1)), seen(2, Duration.ofDays(3)))
        assertEquals(2L, nextWord(words, now, previousId = 1)?.card?.id)
    }

    @Test
    fun repeatsPreviousWordWhenItIsTheOnlyOne() {
        val words = listOf(seen(1, Duration.ofMinutes(1)))
        assertEquals(1L, nextWord(words, now, previousId = 1)?.card?.id)
    }

    @Test
    fun noWordsGivesNull() {
        assertNull(nextWord(emptyList(), now))
    }

    @Test
    fun formatsInterval() {
        assertEquals("1분", formatInterval(Duration.ofSeconds(20)))
        assertEquals("10분", formatInterval(Duration.ofMinutes(10)))
        assertEquals("5시간", formatInterval(Duration.ofHours(5)))
        assertEquals("3일", formatInterval(Duration.ofDays(3)))
        assertEquals("2개월", formatInterval(Duration.ofDays(65)))
        assertEquals("1년", formatInterval(Duration.ofDays(400)))
    }
}
