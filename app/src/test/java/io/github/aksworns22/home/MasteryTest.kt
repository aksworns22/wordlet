package io.github.aksworns22.home

import io.github.aksworns22.fsrs.Card
import io.github.aksworns22.fsrs.State
import org.junit.Assert.assertEquals
import org.junit.Test

class MasteryTest {
    private fun reviewed(stability: Double) = Card(id = 1, state = State.Review, stability = stability, difficulty = 5.0)

    @Test
    fun 학습하지_않은_단어는_New() {
        assertEquals(Mastery.New, Card(id = 1).mastery())
    }

    @Test
    fun stability가_커질수록_단계가_오른다() {
        assertEquals(Mastery.Learning, reviewed(0.5).mastery())
        assertEquals(Mastery.Familiar, reviewed(1.0).mastery())
        assertEquals(Mastery.Familiar, reviewed(6.9).mastery())
        assertEquals(Mastery.Strong, reviewed(7.0).mastery())
        assertEquals(Mastery.Mastered, reviewed(30.0).mastery())
    }
}
