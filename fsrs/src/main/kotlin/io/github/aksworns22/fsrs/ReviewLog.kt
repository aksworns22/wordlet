package io.github.aksworns22.fsrs

import java.time.Instant

public data class ReviewLog(
    val cardId: Long,
    val rating: Rating,
    val reviewTime: Instant,
    val reviewDuration: Long?,
)
