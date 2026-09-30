package io.github.aksworns22.add

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import io.github.aksworns22.home.Word
import io.github.aksworns22.ui.highlight
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** 홈 위로 올라오는 단어 추가 시트. 단어와 뜻만 적으면 바로 추가할 수 있다. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AddWordSheet(
    onAdd: (Word) -> Unit,
    onDismiss: () -> Unit
) {
    var term by rememberSaveable { mutableStateOf("") }
    var meaning by rememberSaveable { mutableStateOf("") }
    var example by rememberSaveable { mutableStateOf("") }
    val canAdd = term.isNotBlank() && meaning.isNotBlank()

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val add = {
        val word = Word(term.trim(), meaning.trim(), example.trim())
        scope.launch { sheetState.hide() }.invokeOnCompletion { onAdd(word) }
    }

    val termFocusRequester = remember { FocusRequester() }
    // 시트가 다 올라온 뒤에 키보드를 띄워, 키보드가 먼저 뜨고 시트가 뒤따라오지 않게 한다.
    LaunchedEffect(Unit) {
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
                placeholder = "word",
                value = term,
                onValueChange = { term = it },
                textStyle = MaterialTheme.typography.headlineSmallEmphasized,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = RoundedCornerShape(20.dp),
                autoCorrect = false,
                modifier = Modifier.focusRequester(termFocusRequester)
            )
            Spacer(Modifier.height(12.dp))
            // 뜻과 예문은 단어를 설명하는 한 묶음이라 segmented 그룹으로 붙인다.
            SheetField(
                placeholder = "뜻",
                value = meaning,
                onValueChange = { meaning = it },
                shape = RoundedCornerShape(20.dp, 20.dp, 4.dp, 4.dp)
            )
            Spacer(Modifier.height(2.dp))
            SheetField(
                placeholder = "예문 (선택)",
                value = example,
                onValueChange = { example = it },
                shape = RoundedCornerShape(4.dp, 4.dp, 20.dp, 20.dp),
                singleLine = false,
                // 예문 속 단어가 눈에 띄도록 입력 중인 단어를 강조한다.
                visualTransformation =
                    HighlightTransformation(
                        keyword = term.trim(),
                        style =
                            SpanStyle(
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                    )
            )
            Spacer(Modifier.height(16.dp))
            val height = 56.dp
            Button(
                onClick = { add() },
                enabled = canAdd,
                shapes = ButtonDefaults.shapesFor(height),
                contentPadding = ButtonDefaults.contentPaddingFor(height),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = height)
            ) {
                Text(
                    text = "추가하기",
                    style = MaterialTheme.typography.titleMediumEmphasized
                )
            }
        }
    }
}

@Composable
private fun SheetField(
    placeholder: String,
    value: String,
    onValueChange: (String) -> Unit,
    shape: Shape,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = MaterialTheme.typography.bodyLarge,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    singleLine: Boolean = true,
    autoCorrect: Boolean = true,
    visualTransformation: VisualTransformation = VisualTransformation.None
) {
    val style = textStyle.copy(color = contentColor)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        textStyle = style,
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 2,
        cursorBrush = SolidColor(contentColor),
        keyboardOptions =
            KeyboardOptions(
                autoCorrectEnabled = autoCorrect,
                imeAction = if (singleLine) ImeAction.Next else ImeAction.Default
            ),
        visualTransformation = visualTransformation,
        decorationBox = { innerTextField ->
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(containerColor, shape)
                        .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = style,
                        color = contentColor.copy(alpha = 0.6f)
                    )
                }
                innerTextField()
            }
        }
    )
}

/** 입력 중인 글자는 그대로 두고 [keyword]와 일치하는 부분만 강조한다. */
private class HighlightTransformation(
    private val keyword: String,
    private val style: SpanStyle
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText =
        TransformedText(text.text.highlight(keyword, style), OffsetMapping.Identity)

    override fun equals(other: Any?): Boolean = other is HighlightTransformation && other.keyword == keyword && other.style == style

    override fun hashCode(): Int = 31 * keyword.hashCode() + style.hashCode()
}
