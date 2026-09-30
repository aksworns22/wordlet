package io.github.aksworns22.ui

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString

/** [keyword]와 일치하는 모든 부분에 [style]을 입힌다. */
fun String.highlight(
    keyword: String,
    style: SpanStyle
): AnnotatedString =
    buildAnnotatedString {
        append(this@highlight)
        if (keyword.isEmpty()) return@buildAnnotatedString
        var start = this@highlight.indexOf(keyword, ignoreCase = true)
        while (start >= 0) {
            addStyle(style, start, start + keyword.length)
            start = this@highlight.indexOf(keyword, start + keyword.length, ignoreCase = true)
        }
    }
