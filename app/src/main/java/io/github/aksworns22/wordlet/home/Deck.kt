package io.github.aksworns22.wordlet.home

data class Deck(
    val id: Long,
    val name: String
) {
    companion object {
        /** 처음 만들어지는 "기본" 단어장의 id */
        const val BASIC_ID = 1L
        const val BASIC_NAME = "기본"
    }
}
