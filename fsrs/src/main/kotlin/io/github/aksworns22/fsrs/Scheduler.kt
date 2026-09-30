package io.github.aksworns22.fsrs

import java.time.Duration
import java.time.Instant
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.round
import kotlin.random.Random

public class Scheduler(
    parameters: List<Double> = DEFAULT_PARAMETERS,
    public val desiredRetention: Double = 0.9,
    learningSteps: List<Duration> = listOf(Duration.ofMinutes(1), Duration.ofMinutes(10)),
    relearningSteps: List<Duration> = listOf(Duration.ofMinutes(10)),
    public val maximumInterval: Int = 36500,
    public val enableFuzzing: Boolean = true,
    private val random: Random = Random.Default,
) {
    // 바깥에서 넘긴 리스트가 나중에 바뀌어도 영향받지 않도록 복사해 둔다
    public val parameters: List<Double> = parameters.toList()
    public val learningSteps: List<Duration> = learningSteps.toList()
    public val relearningSteps: List<Duration> = relearningSteps.toList()

    init {
        validateParameters(parameters)
    }

    private val decay: Double = -parameters[20]
    internal val factor: Double = 0.9.pow(1 / decay) - 1

    public fun getCardRetrievability(
        card: Card,
        currentTime: Instant = Instant.now(),
    ): Double {
        val lastReview = card.lastReview ?: return 0.0
        val stability = card.stability ?: return 0.0

        val elapsedDays = max(0L, Duration.between(lastReview, currentTime).toDays())
        return (1 + factor * elapsedDays / stability).pow(decay)
    }

    public fun reviewCard(
        card: Card,
        rating: Rating,
        reviewTime: Instant = Instant.now(),
        reviewDuration: Long? = null,
    ): Pair<Card, ReviewLog> {
        val daysSinceLastReview = card.lastReview?.let { Duration.between(it, reviewTime).toDays() }
        val isSameDay = daysSinceLastReview != null && daysSinceLastReview < 1

        val stability: Double
        val difficulty: Double
        val next: NextStep

        when (card.state) {
            State.Learning -> {
                val step = checkNotNull(card.step)

                if (card.stability == null || card.difficulty == null) {
                    stability = initialStability(rating)
                    difficulty = initialDifficulty(rating, clamp = true)
                } else {
                    stability =
                        if (isSameDay) {
                            shortTermStability(card.stability, rating)
                        } else {
                            nextStability(
                                card.difficulty,
                                card.stability,
                                getCardRetrievability(card, reviewTime),
                                rating,
                            )
                        }
                    difficulty = nextDifficulty(card.difficulty, rating)
                }

                next = nextStep(learningSteps, State.Learning, step, stability, rating)
            }

            State.Review -> {
                val currentStability = checkNotNull(card.stability)
                val currentDifficulty = checkNotNull(card.difficulty)

                stability =
                    if (isSameDay) {
                        shortTermStability(currentStability, rating)
                    } else {
                        nextStability(
                            currentDifficulty,
                            currentStability,
                            getCardRetrievability(card, reviewTime),
                            rating,
                        )
                    }
                difficulty = nextDifficulty(currentDifficulty, rating)

                next =
                    if (rating == Rating.Again && relearningSteps.isNotEmpty()) {
                        NextStep(State.Relearning, 0, relearningSteps[0])
                    } else {
                        graduate(stability)
                    }
            }

            State.Relearning -> {
                val currentStability = checkNotNull(card.stability)
                val currentDifficulty = checkNotNull(card.difficulty)
                val step = checkNotNull(card.step)

                stability =
                    if (isSameDay) {
                        shortTermStability(currentStability, rating)
                    } else {
                        nextStability(
                            currentDifficulty,
                            currentStability,
                            getCardRetrievability(card, reviewTime),
                            rating,
                        )
                    }
                difficulty = nextDifficulty(currentDifficulty, rating)

                next = nextStep(relearningSteps, State.Relearning, step, stability, rating)
            }
        }

        val interval =
            if (enableFuzzing && next.state == State.Review) {
                fuzzedInterval(next.interval)
            } else {
                next.interval
            }

        val reviewedCard =
            card.copy(
                state = next.state,
                step = next.step,
                stability = stability,
                difficulty = difficulty,
                due = reviewTime.plus(interval),
                lastReview = reviewTime,
            )

        val reviewLog =
            ReviewLog(
                cardId = card.id,
                rating = rating,
                reviewTime = reviewTime,
                reviewDuration = reviewDuration,
            )

        return reviewedCard to reviewLog
    }

    public fun rescheduleCard(
        card: Card,
        reviewLogs: List<ReviewLog>
    ): Card {
        reviewLogs.forEach { reviewLog ->
            require(reviewLog.cardId == card.id) {
                "ReviewLog card_id ${reviewLog.cardId} does not match Card card_id ${card.id}"
            }
        }

        return reviewLogs
            .sortedBy { it.reviewTime }
            .fold(Card(id = card.id, due = card.due)) { rescheduledCard, reviewLog ->
                reviewCard(rescheduledCard, reviewLog.rating, reviewLog.reviewTime).first
            }
    }

    private data class NextStep(
        val state: State,
        val step: Int?,
        val interval: Duration
    )

    private fun graduate(stability: Double): NextStep = NextStep(State.Review, null, Duration.ofDays(nextInterval(stability).toLong()))

    /**
     * Learning / Relearning 상태에서 step과 다음 간격을 정한다.
     * 두 상태는 사용하는 steps만 다를 뿐 로직이 같다.
     */
    private fun nextStep(
        steps: List<Duration>,
        state: State,
        step: Int,
        stability: Double,
        rating: Rating,
    ): NextStep {
        // 현재 Scheduler보다 steps가 많았던 Scheduler로 스케줄된 카드도 처리한다
        if (steps.isEmpty() || (step >= steps.size && rating != Rating.Again)) {
            return graduate(stability)
        }

        return when (rating) {
            Rating.Again -> NextStep(state, 0, steps[0])

            Rating.Hard -> {
                val interval =
                    when {
                        step == 0 && steps.size == 1 -> steps[0].times(1.5)
                        step == 0 -> steps[0].plus(steps[1]).times(0.5)
                        else -> steps[step]
                    }
                NextStep(state, step, interval)
            }

            Rating.Good ->
                if (step + 1 == steps.size) {
                    graduate(stability)
                } else {
                    NextStep(state, step + 1, steps[step + 1])
                }

            Rating.Easy -> graduate(stability)
        }
    }

    internal fun fuzzedInterval(interval: Duration): Duration {
        val intervalDays = interval.toDays()

        // 2.5일보다 짧은 간격에는 fuzz를 적용하지 않는다
        if (intervalDays < 2.5) return interval

        var delta = 1.0
        FUZZ_RANGES.forEach { range ->
            delta += range.factor * max(min(intervalDays.toDouble(), range.end) - range.start, 0.0)
        }

        var minInterval = round(intervalDays - delta).toLong()
        var maxInterval = round(intervalDays + delta).toLong()

        minInterval = max(2L, minInterval)
        maxInterval = min(maxInterval, maximumInterval.toLong())
        minInterval = min(minInterval, maxInterval)

        val fuzzedIntervalDays =
            random.nextDouble() * (maxInterval - minInterval + 1) + minInterval

        return Duration.ofDays(min(round(fuzzedIntervalDays).toLong(), maximumInterval.toLong()))
    }

    internal fun nextInterval(stability: Double): Int {
        val nextInterval = (stability / factor) * (desiredRetention.pow(1 / decay) - 1)
        return round(nextInterval).toInt().coerceAtLeast(1).coerceAtMost(maximumInterval)
    }

    internal fun clampStability(stability: Double): Double = max(stability, STABILITY_MIN)

    internal fun initialStability(rating: Rating): Double = clampStability(parameters[rating.value - 1])

    internal fun nextStability(
        difficulty: Double,
        stability: Double,
        retrievability: Double,
        rating: Rating,
    ): Double {
        val nextStability =
            when (rating) {
                Rating.Again -> {
                    nextForgetStability(difficulty, stability, retrievability)
                }

                Rating.Hard, Rating.Good, Rating.Easy -> {
                    nextRecallStability(difficulty, stability, retrievability, rating)
                }
            }
        return clampStability(nextStability)
    }

    internal fun nextForgetStability(
        difficulty: Double,
        stability: Double,
        retrievability: Double,
    ): Double {
        val longTerm =
            parameters[11] *
                difficulty.pow(-parameters[12]) *
                ((stability + 1).pow(parameters[13]) - 1) *
                Math.E.pow((1 - retrievability) * parameters[14])

        val shortTerm = stability / Math.E.pow(parameters[17] * parameters[18])

        return min(longTerm, shortTerm)
    }

    internal fun nextRecallStability(
        difficulty: Double,
        stability: Double,
        retrievability: Double,
        rating: Rating,
    ): Double {
        val hardPenalty = if (rating == Rating.Hard) parameters[15] else 1.0
        val easyBonus = if (rating == Rating.Easy) parameters[16] else 1.0

        return stability * (
            1 + Math.E.pow(parameters[8]) * (11 - difficulty) *
                stability.pow(-parameters[9]) * (
                    Math.E.pow(
                        (1 - retrievability) *
                            parameters[10]
                    ) - 1
                ) * hardPenalty * easyBonus
        )
    }

    internal fun clampDifficulty(difficulty: Double): Double = min(max(difficulty, MIN_DIFFICULTY), MAX_DIFFICULTY)

    internal fun initialDifficulty(
        rating: Rating,
        clamp: Boolean
    ): Double {
        val initialDifficulty =
            parameters[4] - Math.E.pow(parameters[5] * (rating.value - 1)) + 1

        return if (clamp) clampDifficulty(initialDifficulty) else initialDifficulty
    }

    internal fun nextDifficulty(
        difficulty: Double,
        rating: Rating
    ): Double {
        fun linearDamping(
            deltaDifficulty: Double,
            difficulty: Double
        ): Double = (10.0 - difficulty) * deltaDifficulty / 9.0

        fun meanReversion(
            arg1: Double,
            arg2: Double
        ): Double = parameters[7] * arg1 + (1 - parameters[7]) * arg2

        val arg1 = initialDifficulty(Rating.Easy, clamp = false)

        val deltaDifficulty = -(parameters[6] * (rating.value - 3))
        val arg2 = difficulty + linearDamping(deltaDifficulty, difficulty)

        val nextDifficulty = meanReversion(arg1, arg2)

        return clampDifficulty(nextDifficulty)
    }

    internal fun shortTermStability(
        stability: Double,
        rating: Rating
    ): Double {
        var shortTermStabilityIncrease =
            Math.E.pow(parameters[17] * (rating.value - 3 + parameters[18])) *
                stability.pow(-parameters[19])

        if (rating == Rating.Hard || rating == Rating.Good || rating == Rating.Easy) {
            shortTermStabilityIncrease = max(shortTermStabilityIncrease, 1.0)
        }

        val shortTermStability = stability * shortTermStabilityIncrease

        return clampStability(shortTermStability)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Scheduler) return false

        return parameters == other.parameters &&
            desiredRetention == other.desiredRetention &&
            learningSteps == other.learningSteps &&
            relearningSteps == other.relearningSteps &&
            maximumInterval == other.maximumInterval &&
            enableFuzzing == other.enableFuzzing
    }

    override fun hashCode(): Int {
        var result = parameters.hashCode()
        result = 31 * result + desiredRetention.hashCode()
        result = 31 * result + learningSteps.hashCode()
        result = 31 * result + relearningSteps.hashCode()
        result = 31 * result + maximumInterval
        result = 31 * result + enableFuzzing.hashCode()
        return result
    }

    private class FuzzRange(
        val start: Double,
        val end: Double,
        val factor: Double
    )

    public companion object {
        public const val FSRS_DEFAULT_DECAY: Double = 0.1542

        public val DEFAULT_PARAMETERS: List<Double> =
            listOf(
                0.212,
                1.2931,
                2.3065,
                8.2956,
                6.4133,
                0.8334,
                3.0194,
                0.001,
                1.8722,
                0.1666,
                0.796,
                1.4835,
                0.0614,
                0.2629,
                1.6483,
                0.6014,
                1.8729,
                0.5425,
                0.0912,
                0.0658,
                FSRS_DEFAULT_DECAY,
            )

        internal const val STABILITY_MIN: Double = 0.001
        internal const val INITIAL_STABILITY_MAX: Double = 100.0
        internal const val MIN_DIFFICULTY: Double = 1.0
        internal const val MAX_DIFFICULTY: Double = 10.0

        internal val LOWER_BOUNDS_PARAMETERS: List<Double> =
            listOf(
                STABILITY_MIN,
                STABILITY_MIN,
                STABILITY_MIN,
                STABILITY_MIN,
                1.0,
                0.001,
                0.001,
                0.001,
                0.0,
                0.0,
                0.001,
                0.001,
                0.001,
                0.001,
                0.0,
                0.0,
                1.0,
                0.0,
                0.0,
                0.0,
                0.1,
            )

        internal val UPPER_BOUNDS_PARAMETERS: List<Double> =
            listOf(
                INITIAL_STABILITY_MAX,
                INITIAL_STABILITY_MAX,
                INITIAL_STABILITY_MAX,
                INITIAL_STABILITY_MAX,
                10.0,
                4.0,
                4.0,
                0.75,
                4.5,
                0.8,
                3.5,
                5.0,
                0.25,
                0.9,
                4.0,
                1.0,
                6.0,
                2.0,
                2.0,
                0.8,
                0.8,
            )

        private val FUZZ_RANGES: List<FuzzRange> =
            listOf(
                FuzzRange(start = 2.5, end = 7.0, factor = 0.15),
                FuzzRange(start = 7.0, end = 20.0, factor = 0.1),
                FuzzRange(start = 20.0, end = Double.POSITIVE_INFINITY, factor = 0.05),
            )

        private fun validateParameters(parameters: List<Double>) {
            require(parameters.size == LOWER_BOUNDS_PARAMETERS.size) {
                "Expected ${LOWER_BOUNDS_PARAMETERS.size} parameters, got ${parameters.size}."
            }

            val errorMessages =
                parameters.indices.mapNotNull { index ->
                    val parameter = parameters[index]
                    val lowerBound = LOWER_BOUNDS_PARAMETERS[index]
                    val upperBound = UPPER_BOUNDS_PARAMETERS[index]

                    if (parameter in lowerBound..upperBound) {
                        null
                    } else {
                        "parameters[$index] = $parameter is out of bounds: ($lowerBound, $upperBound)"
                    }
                }

            require(errorMessages.isEmpty()) {
                "One or more parameters are out of bounds:\n" + errorMessages.joinToString("\n")
            }
        }

        /**
         * Python timedelta 곱셈처럼 마이크로초 단위로 반올림(banker's rounding)한다.
         */
        private fun Duration.times(multiplier: Double): Duration {
            val micros = round(toNanos() / 1_000.0 * multiplier).toLong()
            return Duration.ofNanos(micros * 1_000)
        }
    }
}
