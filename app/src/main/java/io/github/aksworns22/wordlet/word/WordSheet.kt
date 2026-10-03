package io.github.aksworns22.wordlet.word

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.OutputTransformation
import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.aksworns22.wordlet.home.Deck
import io.github.aksworns22.wordlet.home.Word
import io.github.aksworns22.wordlet.ui.SheetButtons
import io.github.aksworns22.wordlet.ui.SheetField
import kotlinx.coroutines.launch

/**
 * 홈 위로 올라오는 단어 추가 시트. 단어와 뜻만 적으면 바로 추가할 수 있다.
 * 맨 위에 넣을 단어장이 [initialDeck]으로 골라져 있어 어디에 들어가는지 보이고, 그 자리에서 바꿀 수도 있다.
 */
@Composable
fun AddWordSheet(
    decks: List<Deck>,
    initialDeck: Deck,
    onAdd: (Word, Deck) -> Unit,
    onDismiss: () -> Unit
) {
    var deckId by rememberSaveable { mutableLongStateOf(initialDeck.id) }
    val deck = decks.find { it.id == deckId } ?: initialDeck
    WordSheet(
        initial = null,
        submitLabel = "추가하기",
        onSubmit = { onAdd(it, deck) },
        onDelete = null,
        onDismiss = onDismiss,
        header = {
            DeckPicker(decks = decks, selectedId = deck.id, onSelect = { deckId = it.id })
        }
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
    onDismiss: () -> Unit,
    header: (@Composable () -> Unit)? = null
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
            if (header != null) {
                header()
                Spacer(Modifier.height(12.dp))
            }
            SheetField(
                state = term,
                placeholder = "word",
                textStyle = MaterialTheme.typography.headlineSmallEmphasized,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                placeholderColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
                shape = RoundedCornerShape(20.dp),
                autoCorrect = false
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

// 홈의 단어장 탭과 같은 크기라 같은 단어장을 고르는 것으로 보인다.
private val DeckChipHeight = 32.dp
private val DeckChipSelectedHeight = 40.dp

/** 단어를 넣을 단어장을 고르는 칩 줄. 홈의 단어장 탭처럼 고른 칩만 primary 알약으로 부푼다. */
@Composable
private fun DeckPicker(
    decks: List<Deck>,
    selectedId: Long,
    onSelect: (Deck) -> Unit
) {
    val listState = rememberLazyListState()
    // 처음 열 때 화면 밖의 단어장이 골라져 있으면 보이도록 스크롤한다.
    LaunchedEffect(Unit) {
        val index = decks.indexOfFirst { it.id == selectedId }
        if (index > 0) listState.scrollToItem(index)
    }
    LazyRow(
        state = listState,
        modifier = Modifier.fillMaxWidth().heightIn(min = DeckChipSelectedHeight),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(decks, key = { it.id }) { deck ->
            DeckChip(
                name = deck.name,
                selected = deck.id == selectedId,
                onClick = { onSelect(deck) }
            )
        }
    }
}

/** 골라지면 모서리, 키, 색이 스프링으로 함께 변해 사각형이 알약으로 부풀고, 누르면 모서리가 오므라든다. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DeckChip(
    name: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val height by animateDpAsState(
        targetValue = if (selected) DeckChipSelectedHeight else DeckChipHeight,
        animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec()
    )
    val corner by animateDpAsState(
        targetValue =
            when {
                pressed -> 6.dp
                selected -> DeckChipSelectedHeight / 2
                else -> 10.dp
            },
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec()
    )
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.92f else 1f,
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec()
    )
    val containerColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec()
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec()
    )
    Box(
        modifier =
            Modifier
                .height(height)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }.clip(RoundedCornerShape(corner))
                .background(containerColor)
                .selectable(
                    selected = selected,
                    interactionSource = interactionSource,
                    indication = ripple(),
                    role = Role.RadioButton,
                    onClick = onClick
                ).padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = name,
            style =
                if (selected) {
                    MaterialTheme.typography.titleSmallEmphasized
                } else {
                    MaterialTheme.typography.labelLarge
                },
            color = contentColor,
            maxLines = 1
        )
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
