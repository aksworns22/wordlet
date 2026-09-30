package io.github.aksworns22.home

import io.github.aksworns22.fsrs.Card
import io.github.aksworns22.fsrs.State
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.ZoneOffset

class WordTest {
    private val now = Instant.parse("2026-09-30T12:00:00Z")

    private fun reviewedWord(due: String) =
        Word(
            term = "nuance",
            meaning = "뉘앙스",
            card =
                Card(
                    id = 1,
                    state = State.Review,
                    due = Instant.parse(due),
                    lastReview = Instant.parse("2026-09-20T12:00:00Z")
                )
        )

    @Test
    fun `한 번도 복습하지 않은 단어는 New`() {
        val word = Word("nuance", "뉘앙스", Card(id = 1, due = now.minusSeconds(3600)))
        assertEquals(ReviewStatus.New, word.reviewStatus(now, ZoneOffset.UTC))
    }

    @Test
    fun `due가 지난 날짜면 지난 일수를 가진 Due`() {
        val status = reviewedWord("2026-09-28T09:00:00Z").reviewStatus(now, ZoneOffset.UTC)
        assertEquals(ReviewStatus.Due(2), status)
    }

    @Test
    fun `due가 오늘 지났으면 Due(0)`() {
        val status = reviewedWord("2026-09-30T08:00:00Z").reviewStatus(now, ZoneOffset.UTC)
        assertEquals(ReviewStatus.Due(0), status)
    }

    @Test
    fun `due가 미래면 남은 일수를 가진 Scheduled`() {
        val status = reviewedWord("2026-10-03T01:00:00Z").reviewStatus(now, ZoneOffset.UTC)
        assertEquals(ReviewStatus.Scheduled(3), status)
    }
}
