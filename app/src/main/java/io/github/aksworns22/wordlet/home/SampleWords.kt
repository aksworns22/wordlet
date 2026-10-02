package io.github.aksworns22.wordlet.home

import io.github.aksworns22.fsrs.Card
import io.github.aksworns22.fsrs.State
import java.time.Duration
import java.time.Instant

// 미리보기에 쓰는 샘플 데이터
val sampleDecks =
    listOf(
        Deck(Deck.BASIC_ID, Deck.BASIC_NAME),
        Deck(2, "토익"),
        Deck(3, "여행 회화"),
        Deck(4, "논문 읽기")
    )

fun sampleWords(now: Instant = Instant.now()): List<Word> {
    fun reviewed(
        id: Long,
        dueInDays: Long,
        stability: Double = 5.0
    ) = Card(
        id = id,
        state = State.Review,
        stability = stability,
        difficulty = 5.0,
        due = now.plus(Duration.ofDays(dueInDays)),
        lastReview = now.minus(Duration.ofDays(3))
    )

    return listOf(
        Word("nuance", "미묘한 차이, 뉘앙스", card = Card(id = 1)),
        Word("inevitable", "피할 수 없는, 필연적인", card = reviewed(2, dueInDays = 3)),
        Word("compelling", "설득력 있는, 강렬한", card = Card(id = 3)),
        Word("reluctant", "꺼리는, 마지못한", card = Card(id = 4)),
        Word("diligent", "성실한, 부지런한", card = Card(id = 5)),
        Word("profound", "깊은, 심오한", card = Card(id = 6)),
        Word("tentative", "잠정적인, 머뭇거리는", card = reviewed(7, dueInDays = 21, stability = 40.0)),
        Word("scrutinize", "면밀히 조사하다", card = reviewed(8, dueInDays = -2, stability = 0.5)),
        Word("ambiguous", "애매모호한", card = reviewed(9, dueInDays = 0).copy(due = now)),
        Word("meticulous", "꼼꼼한, 세심한", card = Card(id = 10)),
        Word("resilient", "회복력 있는, 탄력 있는", card = reviewed(11, dueInDays = 7, stability = 12.0))
    )
}
