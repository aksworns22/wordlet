package io.github.aksworns22.fsrs

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.doubles.shouldBeGreaterThanOrEqual
import io.kotest.matchers.longs.shouldBeGreaterThanOrEqual
import io.kotest.matchers.longs.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.time.Duration
import java.time.Instant
import kotlin.random.Random

private val TEST_RATINGS_1 = List(6) { Rating.Good } + List(2) { Rating.Again } + List(5) { Rating.Good }

/** Python random.random()의 첫 값을 그대로 돌려주는 가짜 Random */
private fun fixedRandom(value: Double): Random =
    object : Random() {
        override fun nextBits(bitCount: Int): Int = error("사용하지 않음")

        override fun nextDouble(): Double = value
    }

private fun intervalDays(card: Card): Long = Duration.between(card.lastReview, card.due).toDays()

private fun Scheduler.reviewAll(
    card: Card,
    ratings: List<Rating>
): Pair<Card, List<ReviewLog>> {
    var current = card
    val logs = mutableListOf<ReviewLog>()
    ratings.forEach { rating ->
        val (next, log) = reviewCard(current, rating, current.due)
        current = next
        logs.add(log)
    }
    return current to logs
}

class SchedulerTest :
    FunSpec(
        {
            val scheduler = Scheduler()
            val t0 = Instant.parse("2022-11-29T12:30:00Z")

            test("기본 파라미터의 factor는 0.9803464944134797이다") {
                scheduler.factor shouldBe 0.9803464944134797
            }

            context("getCardRetrievability") {
                test("평가받은 적 없는 카드의 retrievability는 0이다") {
                    scheduler.getCardRetrievability(Card(), t0) shouldBe 0.0
                }

                test("t = s 일 때 retrievability는 0.9이다") {
                    val card = Card(stability = 10.0, lastReview = t0)
                    val now = t0.plus(Duration.ofDays(10))

                    scheduler.getCardRetrievability(card, now) shouldBe 0.9
                }

                test("23시간 뒤는 0일로 처리되어 retrievability는 1.0이다") {
                    val card = Card(stability = 10.0, lastReview = t0)
                    val now = t0.plus(Duration.ofHours(23))

                    scheduler.getCardRetrievability(card, now) shouldBe 1.0
                }
            }

            context("nextInterval") {
                test("desiredRetention이 0.9면 간격은 S와 같다") {
                    scheduler.nextInterval(10.0) shouldBe 10
                }

                test("2.5는 banker's rounding으로 2가 된다") {
                    scheduler.nextInterval(2.5) shouldBe 2
                }

                test("3.5는 banker's rounding으로 4가 된다") {
                    scheduler.nextInterval(3.5) shouldBe 4
                }

                test("간격은 최소 1일이다") {
                    scheduler.nextInterval(0.3) shouldBe 1
                }

                test("간격은 maximumInterval을 넘지 않는다") {
                    Scheduler(maximumInterval = 100).nextInterval(1000.0) shouldBe 100
                }

                test("desiredRetention이 0.8이면 간격이 길어진다") {
                    Scheduler(desiredRetention = 0.8).nextInterval(10.0) shouldBe 33
                }
            }

            context("initialStability") {
                test("첫 평가에 따라 w[0]~w[3]이 초기 S가 된다") {
                    scheduler.initialStability(Rating.Again) shouldBe 0.212
                    scheduler.initialStability(Rating.Hard) shouldBe 1.2931
                    scheduler.initialStability(Rating.Good) shouldBe 2.3065
                    scheduler.initialStability(Rating.Easy) shouldBe 8.2956
                }
            }

            context("nextStability - recall") {
                // py-fsrs: _next_stability(difficulty=5.0, stability=10.0, retrievability=0.9, rating=...)
                test("Hard는 hardPenalty가 적용된다") {
                    scheduler.nextStability(
                        difficulty = 5.0,
                        stability = 10.0,
                        retrievability = 0.9,
                        rating = Rating.Hard,
                    ) shouldBe 23.246875110466814
                }

                test("Good") {
                    scheduler.nextStability(
                        difficulty = 5.0,
                        stability = 10.0,
                        retrievability = 0.9,
                        rating = Rating.Good,
                    ) shouldBe 32.02672948198672
                }

                test("Easy는 easyBonus가 적용된다") {
                    scheduler.nextStability(
                        difficulty = 5.0,
                        stability = 10.0,
                        retrievability = 0.9,
                        rating = Rating.Easy,
                    ) shouldBe 51.253861646812936
                }
            }

            context("nextStability - forget") {
                test("long term 값이 더 작으면 long term을 선택한다") {
                    // long_term 1.3919…, short_term 9.5172…
                    scheduler.nextStability(
                        difficulty = 5.0,
                        stability = 10.0,
                        retrievability = 0.9,
                        rating = Rating.Again,
                    ) shouldBe 1.3919869729546932
                }

                test("short term 값이 더 작으면 short term을 선택한다") {
                    // long_term 1.5414…, short_term 0.9517…
                    scheduler.nextStability(
                        difficulty = 1.0,
                        stability = 1.0,
                        retrievability = 0.0,
                        rating = Rating.Again,
                    ) shouldBe 0.9517279993343508
                }
            }

            context("clampStability") {
                test("S는 STABILITY_MIN(0.001)보다 작아질 수 없다") {
                    scheduler.clampStability(0.0) shouldBe 0.001
                }
            }

            context("initialDifficulty") {
                test("clamp = true면 첫 평가에 따른 초기 D는 1~10 범위다") {
                    scheduler.initialDifficulty(Rating.Again, clamp = true) shouldBe 6.4133
                    scheduler.initialDifficulty(Rating.Hard, clamp = true) shouldBe 5.112170705601056
                    scheduler.initialDifficulty(Rating.Good, clamp = true) shouldBe 2.118103970459016
                    scheduler.initialDifficulty(Rating.Easy, clamp = true) shouldBe 1.0
                }

                test("clamp = false면 Easy의 초기 D는 범위를 벗어난 값 그대로다") {
                    scheduler.initialDifficulty(Rating.Easy, clamp = false) shouldBe -4.771630703161737
                }
            }

            context("nextDifficulty") {
                // py-fsrs: _next_difficulty(difficulty=5.0, rating=...)
                test("D = 5에서 Again은 D가 올라간다") {
                    scheduler.nextDifficulty(difficulty = 5.0, rating = Rating.Again) shouldBe 8.341762369296838
                }

                test("D = 5에서 Hard는 D가 올라간다") {
                    scheduler.nextDifficulty(difficulty = 5.0, rating = Rating.Hard) shouldBe 6.665995369296838
                }

                test("D = 5에서 Good은 mean reversion으로 D가 살짝 내려간다") {
                    scheduler.nextDifficulty(difficulty = 5.0, rating = Rating.Good) shouldBe 4.9902283692968386
                }

                test("D = 5에서 Easy는 D가 내려간다") {
                    scheduler.nextDifficulty(difficulty = 5.0, rating = Rating.Easy) shouldBe 3.3144613692968385
                }

                test("D = 10이면 linear damping으로 ΔD가 사라져 mean reversion만 적용된다") {
                    scheduler.nextDifficulty(difficulty = 10.0, rating = Rating.Again) shouldBe 9.985228369296838
                }

                test("D는 MIN_DIFFICULTY(1.0)보다 작아질 수 없다") {
                    scheduler.nextDifficulty(difficulty = 1.0, rating = Rating.Good) shouldBe 1.0
                }
            }

            context("shortTermStability") {
                // py-fsrs: _short_term_stability(stability=..., rating=...)
                test("S = 1에서 Again은 S가 줄어든다") {
                    scheduler.shortTermStability(stability = 1.0, rating = Rating.Again) shouldBe 0.3550402910611492
                }

                test("S = 1에서 Hard는 SInc가 1로 제한되어 S가 그대로다") {
                    scheduler.shortTermStability(stability = 1.0, rating = Rating.Hard) shouldBe 1.0
                }

                test("S = 1에서 Good은 S가 늘어난다") {
                    scheduler.shortTermStability(stability = 1.0, rating = Rating.Good) shouldBe 1.0507203746232234
                }

                test("S = 1에서 Easy는 S가 늘어난다") {
                    scheduler.shortTermStability(stability = 1.0, rating = Rating.Easy) shouldBe 1.8075566207325262
                }

                test("S = 10에서 Good은 SInc가 1로 제한되어 S가 그대로다") {
                    scheduler.shortTermStability(stability = 10.0, rating = Rating.Good) shouldBe 10.0
                }
            }

            context("clampDifficulty") {
                test("D는 1~10 범위로 제한된다") {
                    scheduler.clampDifficulty(0.5) shouldBe 1.0
                    scheduler.clampDifficulty(11.0) shouldBe 10.0
                }
            }

            context("parameter validation") {
                test("기본 파라미터는 유효하다") {
                    Scheduler(parameters = Scheduler.DEFAULT_PARAMETERS)
                }

                test("범위를 벗어난 파라미터는 예외를 던진다") {
                    val tooHigh = Scheduler.DEFAULT_PARAMETERS.toMutableList().apply { this[6] = 100.0 }
                    val tooLow = Scheduler.DEFAULT_PARAMETERS.toMutableList().apply { this[10] = -42.0 }
                    val twoBad =
                        Scheduler.DEFAULT_PARAMETERS.toMutableList().apply {
                            this[0] = 0.0
                            this[3] = 101.0
                        }

                    listOf(tooHigh, tooLow, twoBad).forEach { parameters ->
                        shouldThrow<IllegalArgumentException> { Scheduler(parameters = parameters) }
                    }
                }

                test("파라미터 개수가 21개가 아니면 예외를 던진다") {
                    listOf(
                        emptyList(),
                        Scheduler.DEFAULT_PARAMETERS.dropLast(1),
                        Scheduler.DEFAULT_PARAMETERS + listOf(1.0, 2.0, 3.0),
                    ).forEach { parameters ->
                        shouldThrow<IllegalArgumentException> { Scheduler(parameters = parameters) }
                    }
                }

                test("생성 후 넘긴 리스트를 바꿔도 Scheduler는 영향받지 않는다") {
                    val parameters = Scheduler.DEFAULT_PARAMETERS.toMutableList()
                    val learningSteps = mutableListOf(Duration.ofMinutes(1))
                    val relearningSteps = mutableListOf(Duration.ofMinutes(10))
                    val copied =
                        Scheduler(
                            parameters = parameters,
                            learningSteps = learningSteps,
                            relearningSteps = relearningSteps,
                        )

                    parameters[20] = 0.8
                    learningSteps.clear()
                    relearningSteps.add(Duration.ofHours(1))

                    copied.parameters shouldBe Scheduler.DEFAULT_PARAMETERS
                    copied.learningSteps shouldBe listOf(Duration.ofMinutes(1))
                    copied.relearningSteps shouldBe listOf(Duration.ofMinutes(10))
                }
            }

            context("equals") {
                test("설정이 같으면 같은 Scheduler다") {
                    Scheduler() shouldBe Scheduler()
                    Scheduler() shouldNotBe Scheduler(desiredRetention = 0.91)
                }
            }

            context("reviewCard") {
                // py-fsrs: test_review_card
                test("TEST_RATINGS_1의 간격 기록은 py-fsrs와 같다") {
                    val noFuzz = Scheduler(enableFuzzing = false)
                    var card = Card(due = t0)
                    var reviewTime = t0
                    val ivlHistory = mutableListOf<Long>()

                    TEST_RATINGS_1.forEach { rating ->
                        card = noFuzz.reviewCard(card, rating, reviewTime).first
                        ivlHistory.add(intervalDays(card))
                        reviewTime = card.due
                    }

                    ivlHistory shouldBe listOf(0L, 2, 11, 46, 163, 498, 0, 0, 2, 4, 7, 12, 21)
                }

                test("TEST_RATINGS_1의 각 단계 상태·S·D·due는 py-fsrs와 같다") {
                    data class Expected(
                        val state: State,
                        val step: Int?,
                        val stability: Double,
                        val difficulty: Double,
                        val due: String,
                    )

                    val expected =
                        listOf(
                            Expected(State.Learning, 1, 2.3065, 2.118103970459016, "2022-11-29T12:40:00Z"),
                            Expected(State.Review, null, 2.3065, 2.111214235785395, "2022-12-01T12:40:00Z"),
                            Expected(State.Review, null, 10.971048263078135, 2.1043313908464483, "2022-12-12T12:40:00Z"),
                            Expected(State.Review, null, 46.316858440073425, 2.0974554287524403, "2023-01-27T12:40:00Z"),
                            Expected(State.Review, null, 162.99981577472244, 2.0905863426205262, "2023-07-09T12:40:00Z"),
                            Expected(State.Review, null, 497.8765551245907, 2.083724125574744, "2024-11-18T12:40:00Z"),
                            Expected(State.Relearning, 0, 6.890412507565338, 7.383202320049203, "2024-11-18T12:50:00Z"),
                            Expected(State.Relearning, 0, 2.154598374301973, 9.125104766121234, "2024-11-18T13:00:00Z"),
                            Expected(State.Review, null, 2.154598374301973, 9.11120803065195, "2024-11-20T13:00:00Z"),
                            Expected(State.Review, null, 3.9831233795187773, 9.097325191918136, "2024-11-24T13:00:00Z"),
                            Expected(State.Review, null, 7.236254476883319, 9.083456236023055, "2024-12-01T13:00:00Z"),
                            Expected(State.Review, null, 12.483043578674472, 9.06960114908387, "2024-12-13T13:00:00Z"),
                            Expected(State.Review, null, 20.77035728499242, 9.055759917231622, "2025-01-03T13:00:00Z"),
                        )

                    val noFuzz = Scheduler(enableFuzzing = false)
                    var card = Card(due = t0)
                    var reviewTime = t0

                    TEST_RATINGS_1.zip(expected).forEach { (rating, exp) ->
                        card = noFuzz.reviewCard(card, rating, reviewTime).first
                        card.state shouldBe exp.state
                        card.step shouldBe exp.step
                        // Python libm pow와 JVM Math.pow의 1 ulp 차이가 누적될 수 있어 허용 오차를 둔다
                        card.stability!! shouldBe (exp.stability plusOrMinus 1e-9)
                        card.difficulty!! shouldBe (exp.difficulty plusOrMinus 1e-9)
                        card.due shouldBe Instant.parse(exp.due)
                        reviewTime = card.due
                    }
                }

                // py-fsrs: test_memo_state
                test("Again 후 Good 5번이면 S·D는 py-fsrs와 같다") {
                    val ratings = listOf(Rating.Again) + List(5) { Rating.Good }
                    val ivlHistory = listOf(0L, 0, 1, 3, 8, 21)

                    var card = Card(due = t0)
                    var reviewTime = t0
                    ratings.zip(ivlHistory).forEach { (rating, ivl) ->
                        reviewTime = reviewTime.plus(Duration.ofDays(ivl))
                        card = scheduler.reviewCard(card, rating, reviewTime).first
                    }

                    card.stability shouldBe 53.626902917141365
                    card.difficulty shouldBe 6.357487083997829
                    card.stability!! shouldBe (53.62691 plusOrMinus 1e-4)
                }

                // py-fsrs: test_repeated_correct_reviews
                test("Easy를 반복하면 D는 1.0이 된다") {
                    val noFuzz = Scheduler(enableFuzzing = false)
                    var card = Card(due = t0)
                    (0L until 10L).forEach { i ->
                        card = noFuzz.reviewCard(card, Rating.Easy, t0.plusNanos(i * 1_000)).first
                    }

                    card.difficulty shouldBe 1.0
                }

                test("ReviewLog에는 카드 id, rating, 시각, 소요 시간이 기록된다") {
                    val card = Card(due = t0)
                    val (_, log) = scheduler.reviewCard(card, Rating.Good, t0, reviewDuration = 1500)

                    log shouldBe ReviewLog(cardId = card.id, rating = Rating.Good, reviewTime = t0, reviewDuration = 1500)
                }

                test("reviewCard는 원래 카드를 바꾸지 않는다") {
                    val card = Card(due = t0)
                    val (reviewed, _) = scheduler.reviewCard(card, Rating.Good, t0)

                    reviewed shouldNotBe card
                    card.lastReview shouldBe null
                }
            }

            context("fuzz") {
                // py-fsrs: test_fuzz (random.seed(42) → 0.6394267984578837, random.seed(12345) → 0.41661987254534116)
                test("fuzz 난수에 따라 간격이 py-fsrs와 같다") {
                    listOf(0.6394267984578837 to 12L, 0.41661987254534116 to 11L).forEach { (randomValue, expectedDays) ->
                        val fuzzScheduler = Scheduler(random = fixedRandom(randomValue))
                        var card = Card(due = t0)
                        card = fuzzScheduler.reviewCard(card, Rating.Good, t0).first
                        card = fuzzScheduler.reviewCard(card, Rating.Good, card.due).first
                        val prevDue = card.due
                        card = fuzzScheduler.reviewCard(card, Rating.Good, card.due).first

                        Duration.between(prevDue, card.due).toDays() shouldBe expectedDays
                    }
                }

                test("2.5일보다 짧은 간격에는 fuzz를 적용하지 않는다") {
                    val fuzzScheduler = Scheduler(random = fixedRandom(0.99))
                    fuzzScheduler.fuzzedInterval(Duration.ofDays(2)) shouldBe Duration.ofDays(2)
                }

                test("fuzz된 간격은 maximumInterval을 넘지 않는다") {
                    val fuzzScheduler = Scheduler(maximumInterval = 100, random = fixedRandom(0.99))
                    fuzzScheduler.fuzzedInterval(Duration.ofDays(100)) shouldBe Duration.ofDays(100)
                }
            }

            context("learning steps") {
                test("Good이면 다음 step으로 가고 10분 뒤가 due다") {
                    val card = Card(due = t0)
                    val (reviewed, _) = scheduler.reviewCard(card, Rating.Good, card.due)

                    reviewed.state shouldBe State.Learning
                    reviewed.step shouldBe 1
                    reviewed.due shouldBe t0.plus(Duration.ofMinutes(10))

                    val (graduated, _) = scheduler.reviewCard(reviewed, Rating.Good, reviewed.due)
                    graduated.state shouldBe State.Review
                    graduated.step shouldBe null
                    Duration.between(t0, graduated.due).toHours() shouldBeGreaterThanOrEqual 24
                }

                test("Again이면 step 0으로 돌아가고 1분 뒤가 due다") {
                    val (reviewed, _) = scheduler.reviewCard(Card(due = t0), Rating.Again, t0)

                    reviewed.state shouldBe State.Learning
                    reviewed.step shouldBe 0
                    reviewed.due shouldBe t0.plus(Duration.ofMinutes(1))
                }

                test("step 0에서 Hard면 첫 두 step의 평균(5.5분) 뒤가 due다") {
                    val (reviewed, _) = scheduler.reviewCard(Card(due = t0), Rating.Hard, t0)

                    reviewed.state shouldBe State.Learning
                    reviewed.step shouldBe 0
                    reviewed.due shouldBe t0.plus(Duration.ofSeconds(330))
                }

                test("Easy면 바로 Review로 가고 1일 이상 뒤가 due다") {
                    val (reviewed, _) = scheduler.reviewCard(Card(due = t0), Rating.Easy, t0)

                    reviewed.state shouldBe State.Review
                    reviewed.step shouldBe null
                    Duration.between(t0, reviewed.due).toDays() shouldBeGreaterThanOrEqual 1
                }

                test("step이 하나일 때 Hard면 step의 1.5배 뒤가 due다") {
                    val oneStep = Scheduler(learningSteps = listOf(Duration.ofMinutes(10)))
                    val (reviewed, _) = oneStep.reviewCard(Card(due = t0), Rating.Hard, t0)

                    reviewed.state shouldBe State.Learning
                    reviewed.due shouldBe t0.plus(Duration.ofMinutes(15))
                }

                test("step 1에서 Hard면 step 1의 간격(10분) 뒤가 due다") {
                    val (first, _) = scheduler.reviewCard(Card(due = t0), Rating.Good, t0)
                    val (second, _) = scheduler.reviewCard(first, Rating.Hard, first.due)

                    second.state shouldBe State.Learning
                    second.step shouldBe 1
                    Duration.between(first.due, second.due) shouldBe Duration.ofMinutes(10)
                }

                test("learning steps가 없으면 Again이어도 바로 Review로 간다") {
                    val noSteps = Scheduler(learningSteps = emptyList())
                    val (reviewed, _) = noSteps.reviewCard(Card(due = t0), Rating.Again, t0)

                    reviewed.state shouldBe State.Review
                    intervalDays(reviewed) shouldBeGreaterThanOrEqual 1
                }
            }

            context("review / relearning") {
                test("Review에서 Good이면 1일 이상, Again이면 Relearning으로 10분 뒤가 due다") {
                    val noFuzz = Scheduler(enableFuzzing = false)
                    var (card, _) = noFuzz.reviewAll(Card(due = t0), listOf(Rating.Good, Rating.Good))
                    card.state shouldBe State.Review
                    card.step shouldBe null

                    var prevDue = card.due
                    card = noFuzz.reviewCard(card, Rating.Good, card.due).first
                    card.state shouldBe State.Review
                    Duration.between(prevDue, card.due).toHours() shouldBeGreaterThanOrEqual 24

                    prevDue = card.due
                    card = noFuzz.reviewCard(card, Rating.Again, card.due).first
                    card.state shouldBe State.Relearning
                    card.step shouldBe 0
                    Duration.between(prevDue, card.due) shouldBe Duration.ofMinutes(10)

                    prevDue = card.due
                    card = noFuzz.reviewCard(card, Rating.Again, card.due).first
                    card.state shouldBe State.Relearning
                    card.step shouldBe 0
                    Duration.between(prevDue, card.due) shouldBe Duration.ofMinutes(10)

                    prevDue = card.due
                    card = noFuzz.reviewCard(card, Rating.Good, card.due).first
                    card.state shouldBe State.Review
                    card.step shouldBe null
                    Duration.between(prevDue, card.due).toHours() shouldBeGreaterThanOrEqual 24
                }

                test("relearning steps가 없으면 Again이어도 Review에 머문다") {
                    val noSteps = Scheduler(relearningSteps = emptyList())
                    val (card, _) = noSteps.reviewAll(Card(due = t0), listOf(Rating.Good, Rating.Good, Rating.Again))

                    card.state shouldBe State.Review
                    intervalDays(card) shouldBeGreaterThanOrEqual 1
                }

                test("relearning step이 하나일 때 Hard면 step의 1.5배 뒤가 due다") {
                    val oneStep = Scheduler(relearningSteps = listOf(Duration.ofMinutes(10)))
                    var (card, _) = oneStep.reviewAll(Card(due = t0), listOf(Rating.Easy, Rating.Again))
                    card.state shouldBe State.Relearning
                    card.step shouldBe 0

                    val prevDue = card.due
                    card = oneStep.reviewCard(card, Rating.Hard, prevDue).first
                    card.state shouldBe State.Relearning
                    card.step shouldBe 0
                    Duration.between(prevDue, card.due) shouldBe Duration.ofMinutes(15)
                }

                test("relearning step이 둘일 때 Hard는 step에 따라 평균 또는 해당 step 간격이다") {
                    val twoSteps = Scheduler(relearningSteps = listOf(Duration.ofMinutes(1), Duration.ofMinutes(10)))
                    var (card, _) = twoSteps.reviewAll(Card(due = t0), listOf(Rating.Easy, Rating.Again))

                    var prevDue = card.due
                    card = twoSteps.reviewCard(card, Rating.Hard, prevDue).first
                    card.state shouldBe State.Relearning
                    card.step shouldBe 0
                    Duration.between(prevDue, card.due) shouldBe Duration.ofSeconds(330)

                    card = twoSteps.reviewCard(card, Rating.Good, card.due).first
                    card.state shouldBe State.Relearning
                    card.step shouldBe 1

                    prevDue = card.due
                    card = twoSteps.reviewCard(card, Rating.Hard, prevDue).first
                    card.state shouldBe State.Relearning
                    card.step shouldBe 1
                    Duration.between(prevDue, card.due) shouldBe Duration.ofMinutes(10)

                    card = twoSteps.reviewCard(card, Rating.Easy, prevDue).first
                    card.state shouldBe State.Review
                    card.step shouldBe null
                }

                test("Relearning 카드를 하루 넘게 늦게 복습하면 long-term 공식으로 Review로 간다") {
                    var (card, _) = scheduler.reviewAll(Card(due = t0), listOf(Rating.Easy, Rating.Again))
                    card.state shouldBe State.Relearning

                    card = scheduler.reviewCard(card, Rating.Good, card.due.plus(Duration.ofDays(1))).first
                    card.state shouldBe State.Review
                }

                test("한 카드를 step 수가 다른 여러 Scheduler로 복습해도 상태가 올바르다") {
                    val oneMinute = Duration.ofMinutes(1)
                    val tenMinutes = Duration.ofMinutes(10)
                    var card = Card(due = t0)

                    card = Scheduler(learningSteps = listOf(oneMinute, tenMinutes)).reviewCard(card, Rating.Good, t0).first
                    card.state shouldBe State.Learning
                    card.step shouldBe 1

                    card = Scheduler(learningSteps = listOf(oneMinute)).reviewCard(card, Rating.Again, t0).first
                    card.state shouldBe State.Learning
                    card.step shouldBe 0

                    card = Scheduler(learningSteps = emptyList()).reviewCard(card, Rating.Hard, t0).first
                    card.state shouldBe State.Review
                    card.step shouldBe null

                    val twoRelearning = Scheduler(relearningSteps = listOf(oneMinute, tenMinutes))
                    card = twoRelearning.reviewCard(card, Rating.Again, t0).first
                    card.state shouldBe State.Relearning
                    card.step shouldBe 0

                    card = twoRelearning.reviewCard(card, Rating.Good, t0).first
                    card.state shouldBe State.Relearning
                    card.step shouldBe 1

                    card = Scheduler(relearningSteps = listOf(oneMinute)).reviewCard(card, Rating.Again, t0).first
                    card.state shouldBe State.Relearning
                    card.step shouldBe 0

                    card = Scheduler(relearningSteps = emptyList()).reviewCard(card, Rating.Hard, t0).first
                    card.state shouldBe State.Review
                    card.step shouldBe null
                }

                test("간격은 maximumInterval을 넘지 않는다") {
                    val limited = Scheduler(maximumInterval = 100)
                    var card = Card(due = t0)
                    listOf(Rating.Easy, Rating.Good, Rating.Easy, Rating.Good).forEach { rating ->
                        card = limited.reviewCard(card, rating, card.due).first
                        intervalDays(card) shouldBeLessThanOrEqual 100
                    }
                }

                test("Again을 반복해도 S는 STABILITY_MIN 이상이다") {
                    var card = Card(due = t0)
                    repeat(1000) {
                        card = scheduler.reviewCard(card, Rating.Again, card.due.plus(Duration.ofDays(1))).first
                        card.stability!! shouldBeGreaterThanOrEqual Scheduler.STABILITY_MIN
                    }
                }

                test("같은 날 Hard로 복습해도 S가 줄지 않는다") {
                    val noSteps = Scheduler(learningSteps = emptyList())
                    val reviewTime = Instant.parse("2026-01-01T00:00:00Z")
                    val (card, _) = noSteps.reviewCard(Card(), Rating.Good, reviewTime)
                    val (reviewed, _) = noSteps.reviewCard(card, Rating.Hard, reviewTime.plus(Duration.ofMinutes(1)))

                    reviewed.stability shouldBe card.stability
                }
            }

            context("rescheduleCard") {
                val noFuzz = Scheduler(enableFuzzing = false)

                test("같은 Scheduler로 재스케줄하면 같은 카드가 된다") {
                    val (card, logs) = noFuzz.reviewAll(Card(due = t0), TEST_RATINGS_1)

                    noFuzz.rescheduleCard(card, logs.shuffled()) shouldBe card
                }

                test("파라미터가 다르면 S·D·due가 달라진다") {
                    val (card, logs) = noFuzz.reviewAll(Card(due = t0), TEST_RATINGS_1)
                    val differentParameters =
                        listOf(
                            0.12340357383516173,
                            1.2931,
                            2.397673571899466,
                            8.2956,
                            6.686820427099132,
                            0.45021679958387956,
                            3.077875127553957,
                            0.053520395733247045,
                            1.6539992229052127,
                            0.1466206769107436,
                            0.6300772488850335,
                            1.611965002575047,
                            0.012840136810798864,
                            0.34853762746216305,
                            1.8878958285806287,
                            0.8546376191171063,
                            1.8729,
                            0.6748536823468675,
                            0.20451266082721842,
                            0.22622814695113844,
                            0.46030603398979064,
                        )

                    val rescheduled = Scheduler(parameters = differentParameters).rescheduleCard(card, logs)

                    rescheduled.id shouldBe card.id
                    rescheduled.state shouldBe card.state
                    rescheduled.step shouldBe card.step
                    rescheduled.stability shouldNotBe card.stability
                    rescheduled.difficulty shouldNotBe card.difficulty
                    rescheduled.lastReview shouldBe card.lastReview
                    rescheduled.due shouldNotBe card.due
                }

                test("desiredRetention이 낮으면 due가 늦어진다") {
                    val (card, logs) = noFuzz.reviewAll(Card(due = t0), TEST_RATINGS_1)

                    val rescheduled = Scheduler(desiredRetention = 0.8).rescheduleCard(card, logs)

                    rescheduled.state shouldBe card.state
                    rescheduled.stability shouldBe card.stability
                    rescheduled.difficulty shouldBe card.difficulty
                    rescheduled.lastReview shouldBe card.lastReview
                    (rescheduled.due > card.due) shouldBe true
                }

                test("learning steps가 다르면 state·step이 달라진다") {
                    val (card, logs) = noFuzz.reviewAll(Card(due = t0), TEST_RATINGS_1)
                    val differentSteps = List(logs.size) { Duration.ofMinutes(1) }

                    val rescheduled = Scheduler(learningSteps = differentSteps).rescheduleCard(card, logs)

                    rescheduled.state shouldNotBe card.state
                    rescheduled.step shouldNotBe card.step
                    rescheduled.stability shouldBe card.stability
                    rescheduled.difficulty shouldBe card.difficulty
                    rescheduled.lastReview shouldBe card.lastReview
                    (rescheduled.due < card.due) shouldBe true
                }

                test("다른 카드의 ReviewLog가 섞여 있으면 예외를 던진다") {
                    val (card, logs) = noFuzz.reviewAll(Card(due = t0), TEST_RATINGS_1)
                    val wrongLogs = listOf(logs.first().copy(cardId = 123)) + logs.drop(1)

                    val exception =
                        shouldThrow<IllegalArgumentException> {
                            noFuzz.rescheduleCard(card, wrongLogs)
                        }
                    exception.message shouldBe "ReviewLog card_id 123 does not match Card card_id ${card.id}"
                }
            }
        }
    )
