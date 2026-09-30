package io.github.aksworns22.fsrs

import java.lang.Thread.sleep
import java.time.Instant

public data class Card(
    val id: Long = generateId(),
    val state: State = State.Learning,
    val step: Int? = if (state == State.Learning) 0 else null,
    val stability: Double? = null,
    val difficulty: Double? = null,
    val due: Instant = Instant.now(),
    val lastReview: Instant? = null,
) {
    init {
        require(!(state == State.Learning && step == null)) {
            "[에러] State.Learning 상태의 card는 step이 null일 수 없습니다."
        }
    }

    private companion object {
        fun generateId(): Long {
            val id = Instant.now().toEpochMilli()
            sleep(1)
            return id
        }
    }
}
