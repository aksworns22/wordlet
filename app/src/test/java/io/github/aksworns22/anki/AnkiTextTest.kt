package io.github.aksworns22.anki

import org.junit.Assert.assertEquals
import org.junit.Test

class AnkiTextTest {
    @Test
    fun removesTagsAndKeepsLineBreaks() {
        assertEquals("뉘앙스\n미묘한 차이", cleanField("<b>뉘앙스</b><br>미묘한 <i>차이</i>"))
        assertEquals("하나\n둘", cleanField("<div>하나</div><div>둘</div>"))
    }

    @Test
    fun decodesEntities() {
        assertEquals("a & b < c \"d\" é 😀", cleanField("a &amp; b&nbsp;&lt; c &quot;d&quot; &#233; &#x1F600;"))
        assertEquals("&unknown;", cleanField("&unknown;"))
    }

    @Test
    fun dropsSoundAndImage() {
        assertEquals("nuance", cleanField("nuance [sound:nuance.mp3]<img src=\"a.jpg\">"))
    }

    @Test
    fun keepsClozeAnswer() {
        assertEquals("a subtle nuance", cleanField("a {{c1::subtle::형용사}} {{c2::nuance}}"))
    }

    @Test
    fun dropsStyleBlock() {
        assertEquals("word", cleanField("<style>.a { color: red; }</style>word"))
    }
}
