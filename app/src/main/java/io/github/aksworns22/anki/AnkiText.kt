package io.github.aksworns22.anki

private val sound = Regex("""\[sound:[^\]]*\]""")
private val cloze = Regex("""\{\{c\d+::(.*?)(::.*?)?\}\}""", RegexOption.DOT_MATCHES_ALL)
private val hidden = Regex("""<(style|script)\b.*?</\1>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
private val lineBreak = Regex("""<br\s*/?>|</(div|p|li)>""", RegexOption.IGNORE_CASE)
private val tag = Regex("""<[^>]*>""")
private val entity = Regex("""&(#x[0-9a-fA-F]+|#\d+|[a-zA-Z]+);""")
private val namedEntities =
    mapOf("nbsp" to " ", "amp" to "&", "lt" to "<", "gt" to ">", "quot" to "\"", "apos" to "'")

/**
 * Anki 필드 값(HTML)을 화면에 보일 글자만 남긴다.
 * 소리·이미지는 버리고, cloze는 정답만 남기고, 줄바꿈은 살린다.
 */
fun cleanField(html: String): String =
    html
        .replace(sound, "")
        .replace(cloze, "$1")
        .replace(hidden, "")
        .replace(lineBreak, "\n")
        .replace(tag, "")
        .replace(entity) { decodeEntity(it.groupValues[1]) ?: it.value }
        .lines()
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .joinToString("\n")

private fun decodeEntity(name: String): String? =
    when {
        name.startsWith("#x") -> codePoint(name.drop(2).toIntOrNull(16))
        name.startsWith("#") -> codePoint(name.drop(1).toIntOrNull())
        else -> namedEntities[name]
    }

private fun codePoint(value: Int?): String? = value?.takeIf(Character::isValidCodePoint)?.let { String(Character.toChars(it)) }
