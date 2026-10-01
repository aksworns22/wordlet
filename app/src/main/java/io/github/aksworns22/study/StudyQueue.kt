package io.github.aksworns22.study

import io.github.aksworns22.home.Word
import java.time.Duration
import java.time.Instant

/**
 * 다음에 학습할 단어를 고른다.
 * 1. 이미 본 단어 중 복습할 때가 된 단어(due가 이른 순)
 * 2. 새 단어(홈 목록과 같은 순서)
 * 3. 둘 다 없으면 due가 가장 가까운 단어를 미리 학습한다.
 * 같은 단어가 연달아 나오지 않도록 다른 단어가 있으면 [previousId]는 건너뛴다.
 */
fun nextWord(
    words: List<Word>,
    now: Instant,
    previousId: Long? = null
): Word? {
    val candidates = words.filter { it.card.id != previousId }.ifEmpty { words }
    val (seen, new) = candidates.partition { it.card.lastReview != null }
    return seen.filter { !it.card.due.isAfter(now) }.minByOrNull { it.card.due }
        ?: new.maxByOrNull { it.card.id }
        ?: seen.minByOrNull { it.card.due }
}

/** 다음 복습까지의 간격을 "10분", "3일"처럼 짧게 나타낸다. */
fun formatInterval(interval: Duration): String {
    val minutes = interval.toMinutes()
    val days = interval.toDays()
    return when {
        minutes < 60 -> "${minutes.coerceAtLeast(1)}분"
        days < 1 -> "${interval.toHours()}시간"
        days < 30 -> "${days}일"
        days < 365 -> "${days / 30}개월"
        else -> "${days / 365}년"
    }
}
