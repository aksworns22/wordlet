package io.github.aksworns22.wordlet.study

import io.github.aksworns22.fsrs.State
import io.github.aksworns22.wordlet.home.Word
import java.time.Duration
import java.time.Instant

/**
 * 동시에 익히는 단어 수의 상한.
 * 학습은 끝없이 이어지지만, 새 단어는 익히는 중인 단어가 이만큼 차 있으면 들이지 않는다.
 */
const val LEARNING_LIMIT = 10

/**
 * 다음에 학습할 단어를 고른다.
 * 1. 이미 본 단어 중 복습할 때가 된 단어(due가 이른 순)
 * 2. 익히는 중인 단어가 [LEARNING_LIMIT]보다 적으면 새 단어(홈 목록과 같은 순서)
 * 3. 그 밖에는 due가 가장 가까운 단어를 미리 학습한다.
 * 같은 단어가 연달아 나오지 않도록 다른 단어가 있으면 [previousId]는 건너뛴다.
 */
fun nextWord(
    words: List<Word>,
    now: Instant,
    previousId: Long? = null
): Word? {
    val candidates = words.filter { it.card.id != previousId }.ifEmpty { words }
    val (seen, new) = candidates.partition { it.card.lastReview != null }
    val learning = words.count { it.card.lastReview != null && it.card.state != State.Review }
    return seen.filter { !it.card.due.isAfter(now) }.minByOrNull { it.card.due }
        ?: new.takeIf { learning < LEARNING_LIMIT }?.maxByOrNull { it.card.id }
        ?: seen.minByOrNull { it.card.due }
}

/** 다음 복습까지의 간격을 "10분", "3일"처럼 짧게 나타낸다. */
fun formatInterval(interval: Duration): String {
    val minutes = interval.toMinutes()
    val days = interval.toDays()
    return when {
        minutes < 60 -> "${minutes.coerceAtLeast(1)}분 뒤"
        days < 1 -> "${interval.toHours()}시간 뒤"
        days < 30 -> "${days}일 뒤"
        days < 365 -> "${days / 30}개월 뒤"
        else -> "${days / 365}년 뒤"
    }
}
