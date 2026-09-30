package io.github.aksworns22.home

import io.github.aksworns22.fsrs.Card
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

data class Word(
    val term: String,
    val meaning: String,
    val card: Card = Card()
)

sealed interface ReviewStatus {
    /** 한 번도 학습하지 않은 단어 */
    data object New : ReviewStatus

    /** 복습 시점이 되었거나 지난 단어. [overdueDays]가 0이면 오늘 */
    data class Due(
        val overdueDays: Long
    ) : ReviewStatus

    /** 복습 예정인 단어 */
    data class Scheduled(
        val inDays: Long
    ) : ReviewStatus
}

fun Word.reviewStatus(
    now: Instant = Instant.now(),
    zone: ZoneId = ZoneId.systemDefault()
): ReviewStatus {
    if (card.lastReview == null) return ReviewStatus.New
    val today = now.atZone(zone).toLocalDate()
    val dueDate = card.due.atZone(zone).toLocalDate()
    return if (!card.due.isAfter(now)) {
        ReviewStatus.Due(ChronoUnit.DAYS.between(dueDate, today))
    } else {
        ReviewStatus.Scheduled(ChronoUnit.DAYS.between(today, dueDate))
    }
}
