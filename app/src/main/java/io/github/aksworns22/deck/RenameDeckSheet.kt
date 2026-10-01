package io.github.aksworns22.deck

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.aksworns22.home.Deck
import io.github.aksworns22.ui.SheetButtons
import io.github.aksworns22.ui.SheetField
import kotlinx.coroutines.launch

/** [deck]의 이름을 바꾸는 시트. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun RenameDeckSheet(
    deck: Deck,
    onRename: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val name = rememberTextFieldState(deck.name)
    val trimmed = name.text.trim().toString()

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
            SheetField(
                state = name,
                placeholder = "단어장 이름",
                textStyle = MaterialTheme.typography.headlineSmallEmphasized,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = RoundedCornerShape(20.dp)
            )
            Spacer(Modifier.height(16.dp))
            SheetButtons(
                submitLabel = "저장하기",
                canSubmit = trimmed.isNotEmpty() && trimmed != deck.name,
                onSubmit = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion { onRename(trimmed) }
                },
                onDelete = null
            )
        }
    }
}

/** [deck]과 그 안의 단어를 모두 지울지 묻는다. */
@Composable
fun DeleteDeckDialog(
    deck: Deck,
    wordCount: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    ConfirmDialog(
        title = "단어장을 삭제할까요?",
        text = "'${deck.name}' 단어장과 그 안의 단어 ${wordCount}개가 모두 삭제돼요. 되돌릴 수 없어요.",
        confirmLabel = "삭제",
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}

@Composable
private fun ConfirmDialog(
    title: String,
    text: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmLabel, color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("취소") }
        }
    )
}
