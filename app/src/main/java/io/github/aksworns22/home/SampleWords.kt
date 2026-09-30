package io.github.aksworns22.home

import io.github.aksworns22.fsrs.Card
import io.github.aksworns22.fsrs.State
import java.time.Duration
import java.time.Instant

// 저장소가 생기기 전까지 홈 화면에 보여줄 임시 데이터
fun sampleWords(now: Instant = Instant.now()): List<Word> {
    fun reviewed(
        id: Long,
        dueInDays: Long
    ) = Card(
        id = id,
        state = State.Review,
        stability = 5.0,
        difficulty = 5.0,
        due = now.plus(Duration.ofDays(dueInDays)),
        lastReview = now.minus(Duration.ofDays(3))
    )

    return listOf(
        Word("nuance", "미묘한 차이, 뉘앙스", Card(id = 1)),
        Word("inevitable", "피할 수 없는, 필연적인", reviewed(2, dueInDays = 3)),
        Word("compelling", "설득력 있는, 강렬한", Card(id = 3)),
        Word("reluctant", "꺼리는, 마지못한", Card(id = 4)),
        Word("diligent", "성실한, 부지런한", Card(id = 5)),
        Word("profound", "깊은, 심오한", Card(id = 6)),
        Word("tentative", "잠정적인, 머뭇거리는", reviewed(7, dueInDays = 21)),
        Word("scrutinize", "면밀히 조사하다", reviewed(8, dueInDays = -2)),
        Word("ambiguous", "애매모호한", reviewed(9, dueInDays = 0).copy(due = now)),
        Word("meticulous", "꼼꼼한, 세심한", Card(id = 10)),
        Word("resilient", "회복력 있는, 탄력 있는", reviewed(11, dueInDays = 7))
    )
}
