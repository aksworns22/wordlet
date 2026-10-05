package io.github.aksworns22.wordlet.anki

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.edit
import androidx.core.net.toUri
import com.google.firebase.Firebase
import com.google.firebase.crashlytics.crashlytics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** 단어장 가져오기 온보딩을 본 적이 있는지. */
private const val ONBOARDED_KEY = "anki_import_onboarded"

/** Anki의 공유 덱 사이트. wordlet과 별개인 서비스라 브라우저로 연다. */
private const val SHARED_DECKS_URL = "https://ankiweb.net/shared/decks"

/**
 * 덱 파일을 구하고 읽는 과정을 담는다. 온보딩 시트, 안내 시트, 필드 짝짓기 시트는 [AnkiImportHost]가 그린다.
 * 앱은 기기의 .apkg 파일을 읽기만 한다. 덱은 브라우저로 연 AnkiWeb에서 받는다.
 */
@Stable
class AnkiImportState internal constructor(
    private val context: Context,
    private val scope: CoroutineScope,
    private val snackbarHostState: SnackbarHostState,
    private val prefs: SharedPreferences
) {
    internal lateinit var picker: ManagedActivityResultLauncher<Array<String>, Uri?>

    /** 가져오는 흐름을 보여주는 온보딩 시트를 띄우는 중인지. */
    internal var onboarding by mutableStateOf(false)
        private set

    /** 덱을 어디서 구할지 고르는 안내 시트를 띄우는 중인지. */
    internal var guiding by mutableStateOf(false)
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

    /** 덱을 어디서 구할지 고르는 안내 시트를 띄운다. 처음이면 온보딩부터 보여준다. */
    fun start() {
        if (prefs.getBoolean(ONBOARDED_KEY, false)) guiding = true else onboarding = true
    }

    internal fun showOnboarding() {
        guiding = false
        onboarding = true
    }

    internal fun finishOnboarding() {
        closeOnboarding()
        guiding = true
    }

    /** 끝까지 넘기지 않고 닫아도 본 것으로 친다. */
    internal fun closeOnboarding() {
        prefs.edit { putBoolean(ONBOARDED_KEY, true) }
        onboarding = false
    }

    internal fun closeGuide() {
        guiding = false
    }

    /** AnkiWeb 검색은 로그인하지 않으면 두 번만 되므로, 로그인이 남아 있을 브라우저로 연다. */
    internal fun browse() {
        guiding = false
        val intent = Intent(Intent.ACTION_VIEW, SHARED_DECKS_URL.toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            notify("브라우저를 열지 못했어요")
        }
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
                // .apkg가 아닌 파일은 예상한 실패라 보내지 않는다.
                result.exceptionOrNull()?.takeIf { it !is UnsupportedApkgException }?.let {
                    Firebase.crashlytics.recordException(it)
                }
                val types = result.getOrNull()?.filter { it.notes.isNotEmpty() }
                // 소리·이미지만 든 필드는 빠지므로 필드가 남지 않은 노트 타입은 띄우지 않는다.
                val usable = types?.filter { it.fields.isNotEmpty() }
                when {
                    types == null -> fail("Anki 덱 파일을 읽지 못했어요")
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
    val state =
        remember {
            AnkiImportState(
                context,
                scope,
                snackbarHostState,
                context.getSharedPreferences("wordlet", Context.MODE_PRIVATE)
            )
        }
    state.picker =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) state.read(uri)
        }
    return state
}

/**
 * [state]에 따라 온보딩 시트, 안내 시트, 필드를 짝짓는 시트를 띄운다. [onImport]는 새 단어장 이름과 단어를 받는다.
 */
@Composable
fun AnkiImportHost(
    state: AnkiImportState,
    onImport: (String, List<AnkiWord>) -> Unit
) {
    if (state.onboarding) {
        AnkiImportOnboardingSheet(
            onFinish = state::finishOnboarding,
            onDismiss = state::closeOnboarding
        )
    }
    if (state.guiding) {
        AnkiImportGuideSheet(
            onBrowse = state::browse,
            onPickFile = state::pickFile,
            onShowOnboarding = state::showOnboarding,
            onDismiss = state::closeGuide
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
