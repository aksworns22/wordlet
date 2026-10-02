package io.github.aksworns22.wordlet.word

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.OutputTransformation
import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.aksworns22.wordlet.home.Word
import io.github.aksworns22.wordlet.ui.SheetButtons
import io.github.aksworns22.wordlet.ui.SheetField
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** 홈 위로 올라오는 단어 추가 시트. 단어와 뜻만 적으면 바로 추가할 수 있다. */
@Composable
fun AddWordSheet(
    onAdd: (Word) -> Unit,
    onDismiss: () -> Unit
) {
    WordSheet(
        initial = null,
        submitLabel = "추가하기",
        onSubmit = onAdd,
        onDelete = null,
        onDismiss = onDismiss
    )
}

/** 추가 시트와 같은 모양으로 [word]를 고치거나 지우는 시트. 학습 기록은 그대로 둔다. */
@Composable
fun EditWordSheet(
    word: Word,
    onSave: (Word) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    WordSheet(
        initial = word,
        submitLabel = "저장하기",
        onSubmit = onSave,
        onDelete = onDelete,
        onDismiss = onDismiss
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun WordSheet(
    initial: Word?,
    submitLabel: String,
    onSubmit: (Word) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit
) {
    val term = rememberTextFieldState(initial?.term.orEmpty())
    val meaning = rememberTextFieldState(initial?.meaning.orEmpty())
    val example = rememberTextFieldState(initial?.example.orEmpty())
    val canSubmit = term.text.isNotBlank() && meaning.text.isNotBlank()

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val submit = {
        val word =
            (initial ?: Word("", "")).copy(
                term = term.text.trim().toString(),
                meaning = meaning.text.trim().toString(),
                example = example.text.trim().toString()
            )
        scope.launch { sheetState.hide() }.invokeOnCompletion { onSubmit(word) }
    }
    val delete = { scope.launch { sheetState.hide() }.invokeOnCompletion { onDelete?.invoke() } }

    val termFocusRequester = remember { FocusRequester() }
    // 시트가 다 올라온 뒤에 키보드를 띄워, 키보드가 먼저 뜨고 시트가 뒤따라오지 않게 한다.
    // 수정할 때는 내용을 먼저 보도록 키보드를 띄우지 않는다.
    LaunchedEffect(Unit) {
        if (initial != null) return@LaunchedEffect
        snapshotFlow { sheetState.currentValue }.first { it == SheetValue.Expanded }
        termFocusRequester.requestFocus()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier =
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
        ) {
            SheetField(
                state = term,
                placeholder = "word",
                textStyle = MaterialTheme.typography.headlineSmallEmphasized,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                placeholderColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
                shape = RoundedCornerShape(20.dp),
                autoCorrect = false,
                modifier = Modifier.focusRequester(termFocusRequester)
            )
            Spacer(Modifier.height(12.dp))
            // 뜻과 추가 설명은 단어를 설명하는 한 묶음이라 segmented 그룹으로 붙인다.
            SheetField(
                state = meaning,
                placeholder = "뜻",
                shape = RoundedCornerShape(20.dp, 20.dp, 4.dp, 4.dp)
            )
            Spacer(Modifier.height(2.dp))
            SheetField(
                state = example,
                placeholder = "추가 설명 (선택)",
                shape = RoundedCornerShape(4.dp, 4.dp, 20.dp, 20.dp),
                singleLine = false,
                // 추가 설명 속 단어가 눈에 띄도록 입력 중인 단어를 강조한다.
                outputTransformation =
                    HighlightTransformation(
                        keyword = term.text.trim().toString(),
                        style =
                            SpanStyle(
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                    )
            )
            Spacer(Modifier.height(16.dp))
            SheetButtons(
                submitLabel = submitLabel,
                canSubmit = canSubmit,
                onSubmit = { submit() },
                onDelete = if (onDelete != null) ({ delete() }) else null
            )
        }
    }
}

/** 입력 중인 글자는 그대로 두고 [keyword]와 일치하는 부분만 강조한다. */
private data class HighlightTransformation(
    private val keyword: String,
    private val style: SpanStyle
) : OutputTransformation {
    override fun TextFieldBuffer.transformOutput() {
        if (keyword.isEmpty()) return
        val text = toString()
        var start = text.indexOf(keyword, ignoreCase = true)
        while (start >= 0) {
            addStyle(style, start, start + keyword.length)
            start = text.indexOf(keyword, start + keyword.length, ignoreCase = true)
        }
    }
}
