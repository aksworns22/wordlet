package io.github.aksworns22.wordlet.anki

import org.junit.Assert.assertEquals
import org.junit.Test

class AnkiNoteTypeTest {
    @Test
    fun dropsFieldsEmptyInEveryNote() {
        val type =
            AnkiNoteType(
                name = "Basic",
                fields = listOf("Front", "Audio", "Back"),
                notes = listOf(listOf("nuance", "", "뉘앙스"), listOf("word", "", "단어"))
            )
        assertEquals(
            AnkiNoteType(
                name = "Basic",
                fields = listOf("Front", "Back"),
                notes = listOf(listOf("nuance", "뉘앙스"), listOf("word", "단어"))
            ),
            type.withoutEmptyFields()
        )
    }

    @Test
    fun keepsFieldFilledInSomeNotes() {
        val type =
            AnkiNoteType(
                name = "Basic",
                fields = listOf("Front", "Example"),
                notes = listOf(listOf("nuance", ""), listOf("word", "a word"))
            )
        assertEquals(type, type.withoutEmptyFields())
    }
}
