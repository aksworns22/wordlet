package io.github.aksworns22.word

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.OutputTransformation
import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.foundation.text.input.TextFieldDecorator
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import io.github.aksworns22.R
import io.github.aksworns22.home.Word
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
                shape = RoundedCornerShape(20.dp),
                autoCorrect = false,
                modifier = Modifier.focusRequester(termFocusRequester)
            )
            Spacer(Modifier.height(12.dp))
            // 뜻과 예문은 단어를 설명하는 한 묶음이라 segmented 그룹으로 붙인다.
            SheetField(
                state = meaning,
                placeholder = "뜻",
                shape = RoundedCornerShape(20.dp, 20.dp, 4.dp, 4.dp)
            )
            Spacer(Modifier.height(2.dp))
            SheetField(
                state = example,
                placeholder = "예문 (선택)",
                shape = RoundedCornerShape(4.dp, 4.dp, 20.dp, 20.dp),
                singleLine = false,
                // 예문 속 단어가 눈에 띄도록 입력 중인 단어를 강조한다.
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

/**
 * 시트 아래의 버튼 묶음. 누른 버튼은 넓어지고 옆 버튼은 좁아지며 모양이 바뀐다.
 * 삭제는 되돌릴 수 없어 error 색으로 저장과 구분한다.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SheetButtons(
    submitLabel: String,
    canSubmit: Boolean,
    onSubmit: () -> Unit,
    onDelete: (() -> Unit)?
) {
    val height = ButtonDefaults.MediumContainerHeight
    val deleteInteraction = remember { MutableInteractionSource() }
    val submitInteraction = remember { MutableInteractionSource() }
    val errorContainer = MaterialTheme.colorScheme.errorContainer
    val onErrorContainer = MaterialTheme.colorScheme.onErrorContainer
    ButtonGroup(
        overflowIndicator = {},
        modifier = Modifier.fillMaxWidth()
    ) {
        if (onDelete != null) {
            customItem(
                buttonGroupContent = {
                    Button(
                        onClick = onDelete,
                        shapes = ButtonDefaults.shapesFor(height),
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = errorContainer,
                                contentColor = onErrorContainer
                            ),
                        contentPadding = ButtonDefaults.contentPaddingFor(height),
                        interactionSource = deleteInteraction,
                        modifier =
                            Modifier
                                .heightIn(min = height)
                                .animateWidth(deleteInteraction)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_delete),
                            contentDescription = "삭제",
                            modifier = Modifier.size(ButtonDefaults.iconSizeFor(height))
                        )
                    }
                },
                menuContent = {}
            )
        }
        customItem(
            buttonGroupContent = {
                Button(
                    onClick = onSubmit,
                    enabled = canSubmit,
                    shapes = ButtonDefaults.shapesFor(height),
                    contentPadding = ButtonDefaults.contentPaddingFor(height),
                    interactionSource = submitInteraction,
                    modifier =
                        Modifier
                            .weight(1f)
                            .heightIn(min = height)
                            .animateWidth(submitInteraction)
                ) {
                    Text(
                        text = submitLabel,
                        style = MaterialTheme.typography.titleMediumEmphasized
                    )
                }
            },
            menuContent = {}
        )
    }
}

/** 시트의 입력칸. */
@Composable
private fun SheetField(
    state: TextFieldState,
    placeholder: String,
    shape: Shape,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = MaterialTheme.typography.bodyLarge,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    singleLine: Boolean = true,
    autoCorrect: Boolean = true,
    outputTransformation: OutputTransformation? = null
) {
    val style = textStyle.copy(color = contentColor)
    BasicTextField(
        state = state,
        modifier = modifier.fillMaxWidth(),
        textStyle = style,
        lineLimits =
            if (singleLine) TextFieldLineLimits.SingleLine else TextFieldLineLimits.MultiLine(minHeightInLines = 2),
        cursorBrush = SolidColor(contentColor),
        keyboardOptions =
            KeyboardOptions(
                autoCorrectEnabled = autoCorrect,
                imeAction = if (singleLine) ImeAction.Next else ImeAction.Default
            ),
        outputTransformation = outputTransformation,
        decorator =
            TextFieldDecorator { innerTextField ->
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .background(containerColor, shape)
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    if (state.text.isEmpty()) {
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
