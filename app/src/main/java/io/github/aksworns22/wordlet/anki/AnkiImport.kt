package io.github.aksworns22.wordlet.anki

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** 덱 파일을 고르고 읽는 과정을 담는다. 시트는 [AnkiImportHost]가 그린다. */
@Stable
class AnkiImportState internal constructor(
    private val context: Context,
    private val scope: CoroutineScope
) {
    internal lateinit var picker: ManagedActivityResultLauncher<Array<String>, Uri?>

    /** 시트를 띄우는 중인지. */
    internal var open by mutableStateOf(false)
        private set

    /** 새 단어장 이름의 처음 값. 덱 파일 이름을 쓴다. */
    internal var deckName = ""
        private set

    /** 읽은 노트 타입. 읽는 중이면 null이다. */
    internal var noteTypes by mutableStateOf<List<AnkiNoteType>?>(null)
        private set

    private var reading: Job? = null

    fun pickFile() {
        // .apkg는 표준 MIME 타입이 없어 기기마다 다르게 잡히므로 모든 파일을 보여준다.
        picker.launch(arrayOf("*/*"))
    }

    internal fun read(uri: Uri) {
        open = true
        noteTypes = null
        deckName = displayName(uri)?.substringBeforeLast('.')?.takeIf { it.isNotBlank() } ?: "Anki 덱"
        reading =
            scope.launch {
                val result =
                    runCatching {
                        withContext(Dispatchers.IO) {
                            val input = context.contentResolver.openInputStream(uri) ?: throw UnsupportedApkgException()
                            input.use { ApkgReader.read(it, context.cacheDir) }
                        }
                    }
                // 시트를 닫아 취소됐으면 실패로 알리지 않는다.
                ensureActive()
                val types = result.getOrNull()?.filter { it.notes.isNotEmpty() }
                when {
                    types == null -> fail("Anki 덱 파일(.apkg)을 읽지 못했어요")
                    types.isEmpty() -> fail("덱에 가져올 노트가 없어요")
                    else -> noteTypes = types
                }
            }
    }

    internal fun imported(count: Int) {
        close()
        Toast.makeText(context, "단어 ${count}개를 가져왔어요", Toast.LENGTH_SHORT).show()
    }

    internal fun close() {
        reading?.cancel()
        open = false
        noteTypes = null
    }

    private fun displayName(uri: Uri): String? =
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
            if (it.moveToFirst()) it.getString(0) else null
        }

    private fun fail(message: String) {
        close()
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun rememberAnkiImportState(): AnkiImportState {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val state = remember { AnkiImportState(context, scope) }
    state.picker =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) state.read(uri)
        }
    return state
}

/** [state]가 덱을 읽기 시작하면 필드를 짝짓는 시트를 띄운다. [onImport]는 새 단어장 이름과 단어를 받는다. */
@Composable
fun AnkiImportHost(
    state: AnkiImportState,
    onImport: (String, List<AnkiWord>) -> Unit
) {
    if (!state.open) return
    AnkiImportSheet(
        initialName = state.deckName,
        noteTypes = state.noteTypes,
        onImport = { name, words ->
            onImport(name, words)
            state.imported(words.size)
        },
        onDismiss = state::close
    )
}
