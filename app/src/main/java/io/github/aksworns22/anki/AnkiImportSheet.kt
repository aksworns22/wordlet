package io.github.aksworns22.anki

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.aksworns22.ui.SheetField
import kotlinx.coroutines.launch

/** Anki에서 가져와 단어로 만들 내용. */
data class AnkiWord(
    val term: String,
    val meaning: String,
    val example: String
)

/**
 * 노트 타입 하나를 단어로 바꾸는 방법. 필드는 [AnkiNoteType.fields]의 위치다.
 * 단어나 뜻을 고르지 않은 노트 타입은 가져오지 않는다.
 */
private data class FieldMapping(
    val term: Int? = null,
    val meaning: Int? = null,
    val example: Int? = null
) {
    fun wordsOf(type: AnkiNoteType): List<AnkiWord> {
        if (term == null || meaning == null) return emptyList()
        return type.notes
            .map { AnkiWord(it[term], it[meaning], example?.let(it::get).orEmpty()) }
            .filter { it.term.isNotBlank() && it.meaning.isNotBlank() }
    }
}

/**
 * Anki 덱의 필드를 단어·뜻·예문에 직접 짝지어 새 단어장으로 가져오는 시트.
 * 단어장 이름은 [initialName]에서 시작해 고칠 수 있다.
 * [noteTypes]가 null이면 덱을 읽는 중이다.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AnkiImportSheet(
    initialName: String,
    noteTypes: List<AnkiNoteType>?,
    onImport: (String, List<AnkiWord>) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val mappings = remember(noteTypes) { mutableStateListOf(*Array(noteTypes.orEmpty().size) { FieldMapping() }) }
    val words = noteTypes.orEmpty().zip(mappings).flatMap { (type, mapping) -> mapping.wordsOf(type) }
    val name = rememberTextFieldState(initialName)
    val trimmedName = name.text.trim().toString()
    val canImport = trimmedName.isNotEmpty() && words.isNotEmpty()

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
            Text(
                text = "새 단어장의 이름과, 어떤 필드를 단어와 뜻으로 쓸지 정해 주세요",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            Spacer(Modifier.height(16.dp))
            SheetField(
                state = name,
                placeholder = "단어장 이름",
                textStyle = MaterialTheme.typography.titleLargeEmphasized,
                shape = RoundedCornerShape(20.dp)
            )
            Spacer(Modifier.height(12.dp))
            if (noteTypes == null) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(240.dp),
                    contentAlignment = Alignment.Center
                ) {
                    LoadingIndicator()
                }
                return@Column
            }
            noteTypes.forEachIndexed { index, type ->
                NoteTypeCard(
                    type = type,
                    mapping = mappings[index],
                    onMappingChange = { mappings[index] = it }
                )
                Spacer(Modifier.height(12.dp))
            }
            Spacer(Modifier.height(4.dp))
            val height = ButtonDefaults.MediumContainerHeight
            Button(
                onClick = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion { onImport(trimmedName, words) }
                },
                enabled = canImport,
                shapes = ButtonDefaults.shapesFor(height),
                contentPadding = ButtonDefaults.contentPaddingFor(height),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = height)
            ) {
                Text(
                    text = if (words.isEmpty()) "가져오기" else "${words.size}개 가져오기",
                    style = MaterialTheme.typography.titleMediumEmphasized
                )
            }
        }
    }
}

/** 노트 타입 하나의 필드 짝짓기와 첫 노트 미리보기를 한 컨테이너로 묶는다. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun NoteTypeCard(
    type: AnkiNoteType,
    mapping: FieldMapping,
    onMappingChange: (FieldMapping) -> Unit
) {
    // 필드를 고를 때 무엇이 들어 있는지 보이도록 내용이 있는 첫 값을 예시로 쓴다.
    val samples =
        remember(type) {
            type.fields.indices.map { i ->
                type.notes
                    .firstOrNull { it[i].isNotBlank() }
                    ?.get(i)
                    .orEmpty()
            }
        }
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = "단어 ${type.notes.size}개",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp)
            )
            Column(Modifier.padding(top = 12.dp)) {
                // 단어는 단어 추가 시트처럼 primary로 강조하고, 뜻과 예문은 segmented 그룹으로 붙인다.
                FieldPicker(
                    role = "단어",
                    fields = type.fields,
                    samples = samples,
                    selected = mapping.term,
                    onSelect = { onMappingChange(mapping.copy(term = it)) },
                    shape = RoundedCornerShape(20.dp),
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(Modifier.height(8.dp))
                FieldPicker(
                    role = "뜻",
                    fields = type.fields,
                    samples = samples,
                    selected = mapping.meaning,
                    onSelect = { onMappingChange(mapping.copy(meaning = it)) },
                    shape = RoundedCornerShape(20.dp, 20.dp, 4.dp, 4.dp)
                )
                Spacer(Modifier.height(2.dp))
                FieldPicker(
                    role = "예문",
                    fields = type.fields,
                    samples = samples,
                    selected = mapping.example,
                    onSelect = { onMappingChange(mapping.copy(example = it)) },
                    shape = RoundedCornerShape(4.dp, 4.dp, 20.dp, 20.dp),
                    optional = true
                )
                mapping.wordsOf(type).firstOrNull()?.let {
                    Spacer(Modifier.height(12.dp))
                    WordPreview(it)
                }
            }
        }
    }
}

/** [role]에 쓸 필드를 고르는 드롭다운. [optional]이면 "없음"도 고를 수 있다. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FieldPicker(
    role: String,
    fields: List<String>,
    samples: List<String>,
    selected: Int?,
    onSelect: (Int?) -> Unit,
    shape: Shape,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    optional: Boolean = false
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        Surface(
            shape = shape,
            color = containerColor,
            contentColor = contentColor,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
        ) {
            Row(
                modifier = Modifier.padding(start = 20.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = role,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.width(32.dp)
                )
                Column(Modifier.weight(1f)) {
                    Text(
                        text = selected?.let(fields::get) ?: if (optional) "없음" else "필드 선택",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (selected == null) contentColor.copy(alpha = 0.6f) else contentColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    selected?.let(samples::get)?.takeIf { it.isNotEmpty() }?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = contentColor.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            }
        }
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            if (optional) {
                DropdownMenuItem(
                    text = { Text("없음") },
                    onClick = {
                        onSelect(null)
                        expanded = false
                    }
                )
            }
            fields.forEachIndexed { index, name ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (samples[index].isNotEmpty()) {
                                Text(
                                    text = samples[index],
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    },
                    onClick = {
                        onSelect(index)
                        expanded = false
                    }
                )
            }
        }
    }
}

/** 지금 짝지은 대로 가져오면 첫 단어가 어떻게 보이는지 보여준다. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun WordPreview(word: AnkiWord) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            Text(
                text = "미리보기",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.tertiary
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = word.term,
                style = MaterialTheme.typography.titleLargeEmphasized,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = word.meaning,
                style = MaterialTheme.typography.bodyLarge
            )
            if (word.example.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = word.example,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
