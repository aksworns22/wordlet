package io.github.aksworns22.fsrs

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.assertThrows

class CardTest :
    FunSpec(
        {
            test("카드 id는 유니크하다") {
                val cardIds: MutableList<Long> = mutableListOf()
                (1..1000).forEach { _ ->
                    cardIds.add(Card().id)
                }
                cardIds.size shouldBe cardIds.toSet().size
            }

            test("카드가 Learning 상태면서 null인 step인 경우 예외를 반환한다") {
                assertThrows<IllegalArgumentException> {
                    Card(state = State.Learning, step = null)
                }
            }
        }
    )
