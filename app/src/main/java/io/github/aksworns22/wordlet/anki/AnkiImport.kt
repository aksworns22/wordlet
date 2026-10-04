package io.github.aksworns22.wordlet.anki

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.CookieManager
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import com.google.firebase.Firebase
import com.google.firebase.crashlytics.crashlytics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** 덱 파일을 내려받지 못했다. 네트워크 문제라 Crashlytics에 보내지 않는다. */
private class DownloadException(
    cause: Throwable
) : IOException(cause)

/**
 * 덱 파일을 구하고 읽는 과정을 담는다. 안내 시트, AnkiWeb 화면, 필드 짝짓기 시트는 [AnkiImportHost]가 그린다.
 * 덱은 앱 안에서 AnkiWeb으로 내려받거나 기기의 파일에서 고른다.
 */
@Stable
class AnkiImportState internal constructor(
    private val context: Context,
    private val scope: CoroutineScope,
    private val snackbarHostState: SnackbarHostState
) {
    internal lateinit var picker: ManagedActivityResultLauncher<Array<String>, Uri?>

    /** 덱을 어디서 구할지 고르는 안내 시트를 띄우는 중인지. */
    internal var guiding by mutableStateOf(false)
        private set

    /** AnkiWeb 공유 덱 화면을 띄우는 중인지. */
    internal var browsing by mutableStateOf(false)
        private set

    /** 필드 짝짓기 시트를 띄우는 중인지. */
    internal var open by mutableStateOf(false)
        private set

    /** 새 단어장 이름의 처음 값. 덱 이름이나 파일 이름을 쓴다. */
    internal var deckName = ""
        private set

    /** 읽은 노트 타입. 읽는 중이면 null이다. */
    internal var noteTypes by mutableStateOf<List<AnkiNoteType>?>(null)
        private set

    private var reading: Job? = null

    /** 덱을 어디서 구할지 고르는 안내 시트를 띄운다. */
    fun start() {
        guiding = true
    }

    internal fun closeGuide() {
        guiding = false
    }

    internal fun browse() {
        guiding = false
        browsing = true
    }

    internal fun closeBrowser() {
        browsing = false
    }

    internal fun pickFile() {
        guiding = false
        // .apkg는 표준 MIME 타입이 없어 기기마다 다르게 잡히므로 모든 파일을 보여준다.
        picker.launch(arrayOf("*/*"))
    }

    internal fun read(uri: Uri) =
        read(displayName(uri)?.substringBeforeLast('.')) {
            val input = context.contentResolver.openInputStream(uri) ?: throw UnsupportedApkgException()
            input.use { ApkgReader.read(it, context.cacheDir) }
        }

    /** AnkiWeb 화면에서 시작된 다운로드를 앱이 직접 받아 읽는다. 받은 파일은 읽고 나면 지운다. */
    internal fun download(
        url: String,
        userAgent: String,
        name: String?
    ) {
        browsing = false
        read(name) {
            val file = File.createTempFile("download", ".apkg", context.cacheDir)
            try {
                try {
                    fetch(url, userAgent, file)
                } catch (e: IOException) {
                    throw DownloadException(e)
                }
                file.inputStream().use { ApkgReader.read(it, context.cacheDir) }
            } finally {
                file.delete()
            }
        }
    }

    /** [name]이라는 덱을 [load]로 읽어 필드 짝짓기 시트에 띄운다. */
    private fun read(
        name: String?,
        load: () -> List<AnkiNoteType>
    ) {
        open = true
        noteTypes = null
        deckName = name?.takeIf { it.isNotBlank() } ?: "Anki 덱"
        reading =
            scope.launch {
                val result = runCatching { withContext(Dispatchers.IO) { load() } }
                // 시트를 닫아 취소됐으면 실패로 알리지 않는다.
                ensureActive()
                // .apkg가 아닌 파일과 네트워크 문제는 예상한 실패라 보내지 않는다.
                val error = result.exceptionOrNull()
                error?.takeIf { it !is UnsupportedApkgException && it !is DownloadException }?.let {
                    Firebase.crashlytics.recordException(it)
                }
                val types = result.getOrNull()?.filter { it.notes.isNotEmpty() }
                // 소리·이미지만 든 필드는 빠지므로 필드가 남지 않은 노트 타입은 띄우지 않는다.
                val usable = types?.filter { it.fields.isNotEmpty() }
                when {
                    error is DownloadException -> fail("덱을 내려받지 못했어요. 인터넷 연결을 확인해 주세요")
                    types == null -> fail("Anki 덱 파일(.apkg)을 읽지 못했어요")
                    types.isEmpty() -> fail("덱에 가져올 노트가 없어요")
                    usable.isNullOrEmpty() -> fail("단어장으로 만들 수 없어요")
                    else -> noteTypes = usable
                }
            }
    }

    internal fun imported(count: Int) {
        close()
        notify("단어 ${count}개를 가져왔어요")
    }

    internal fun close() {
        reading?.cancel()
        open = false
        noteTypes = null
    }

    /** WebView에서 로그인했을 수 있으므로 WebView의 쿠키와 User-Agent를 그대로 쓴다. */
    private fun fetch(
        url: String,
        userAgent: String,
        file: File
    ) {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 15_000
            connection.readTimeout = 30_000
            connection.setRequestProperty("User-Agent", userAgent)
            CookieManager.getInstance().getCookie(url)?.let { connection.setRequestProperty("Cookie", it) }
            if (connection.responseCode !in 200..299) throw IOException("HTTP ${connection.responseCode}")
            connection.inputStream.use { input -> file.outputStream().use { input.copyTo(it) } }
        } finally {
            connection.disconnect()
        }
    }

    private fun displayName(uri: Uri): String? =
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
            if (it.moveToFirst()) it.getString(0) else null
        }

    private fun fail(message: String) {
        close()
        notify(message)
    }

    /** 읽는 작업은 [close]로 취소되므로 스낵바는 따로 띄운다. 앞의 스낵바는 기다리지 않고 바로 바꾼다. */
    private fun notify(message: String) {
        snackbarHostState.currentSnackbarData?.dismiss()
        scope.launch { snackbarHostState.showSnackbar(message) }
    }
}

@Composable
fun rememberAnkiImportState(snackbarHostState: SnackbarHostState): AnkiImportState {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val state = remember { AnkiImportState(context, scope, snackbarHostState) }
    state.picker =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) state.read(uri)
        }
    return state
}

/**
 * [state]에 따라 안내 시트, AnkiWeb 화면, 필드를 짝짓는 시트를 띄운다. [onImport]는 새 단어장 이름과 단어를 받는다.
 * AnkiWeb 화면이 앱 화면을 덮도록 화면들보다 나중에 그린다.
 */
@Composable
fun AnkiImportHost(
    state: AnkiImportState,
    onImport: (String, List<AnkiWord>) -> Unit
) {
    if (state.guiding) {
        AnkiImportGuideSheet(
            onBrowse = state::browse,
            onPickFile = state::pickFile,
            onDismiss = state::closeGuide
        )
    }
    val spec = MaterialTheme.motionScheme.defaultSpatialSpec<IntOffset>()
    AnimatedVisibility(
        visible = state.browsing,
        enter = slideInVertically(spec) { it / 4 } + fadeIn(),
        exit = slideOutVertically(spec) { it / 4 } + fadeOut()
    ) {
        AnkiWebScreen(
            onDownload = state::download,
            onClose = state::closeBrowser
        )
    }
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
