package io.github.aksworns22.wordlet.home

import io.github.aksworns22.fsrs.Card

/**
 * 단어를 얼마나 오래 기억할 수 있는지를 나눈 단계.
 * 시간이 지나면 떨어지는 복습 시점 대신, 학습할수록 쌓이는 stability(일)로 나눈다.
 */
enum class Mastery {
    New,
    Learning,
    Familiar,
    Strong,
    Mastered
}

fun Card.mastery(): Mastery {
    val days = stability ?: return Mastery.New
    return when {
        days < 1 -> Mastery.Learning
        days < 7 -> Mastery.Familiar
        days < 30 -> Mastery.Strong
        else -> Mastery.Mastered
    }
}
