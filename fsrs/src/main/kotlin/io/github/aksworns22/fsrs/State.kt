package io.github.aksworns22.fsrs

public enum class State(
    public val value: Int
) {
    Learning(1),
    Review(2),
    Relearning(3)
}
