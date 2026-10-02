package io.github.aksworns22.wordlet

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.AndroidComposeUiFlags
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.aksworns22.wordlet.anki.AnkiImportHost
import io.github.aksworns22.wordlet.anki.rememberAnkiImportState
import io.github.aksworns22.wordlet.deck.ClearDeckDialog
import io.github.aksworns22.wordlet.deck.DeckAction
import io.github.aksworns22.wordlet.deck.DeleteDeckDialog
import io.github.aksworns22.wordlet.deck.RenameDeckSheet
import io.github.aksworns22.wordlet.home.HomeScreen
import io.github.aksworns22.wordlet.study.StudiedWord
import io.github.aksworns22.wordlet.study.StudyResultScreen
import io.github.aksworns22.wordlet.study.StudyScreen
import io.github.aksworns22.wordlet.study.StudyViewModel
import io.github.aksworns22.wordlet.ui.theme.WordletTheme
import io.github.aksworns22.wordlet.word.AddWordSheet
import io.github.aksworns22.wordlet.word.EditWordSheet
import io.github.aksworns22.wordlet.word.WordViewModel
import kotlinx.coroutines.launch

private enum class Screen { Home, Study, Result }

class MainActivity : ComponentActivity() {
    private val viewModel: WordViewModel by viewModels { WordViewModel.Factory }

    @OptIn(ExperimentalComposeUiApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        // DB에서 처음 읽어 올 때까지 스플래시를 띄워 둔다. 그 사이 빈 화면이 보이지 않는다.
        installSplashScreen().setKeepOnScreenCondition {
            viewModel.words.value == null || viewModel.decks.value == null
        }
        super.onCreate(savedInstanceState)
        // 기본 스케줄러는 입력칸을 옮길 때 이전 칸의 키보드 숨김과 새 칸의 키보드 표시를 따로 처리해
        // 키보드가 잠깐 내려갔다 올라온다(삼성 키보드에서 확인). 프레임 단위로 묶어 처리하도록 끈다.
        AndroidComposeUiFlags.isOutOfFrameSchedulerForTextInputEventsEnabled = false
        enableEdgeToEdge()
        setContent {
            WordletTheme {
                // DB에서 처음 읽어오기 전에 빈 홈이 잠깐 보이지 않도록 그리지 않는다.
                val allWords = viewModel.words.collectAsStateWithLifecycle().value ?: return@WordletTheme
                val decks = viewModel.decks.collectAsStateWithLifecycle().value ?: return@WordletTheme
                val deckId by viewModel.deckId.collectAsStateWithLifecycle()
                // 고른 단어장이 없으면 첫 단어장을 보여준다.
                val deck = decks.find { it.id == deckId } ?: decks.first()
                val words = remember(allWords, deck) { allWords.filter { it.deckId == deck.id } }
                var deckAction by rememberSaveable { mutableStateOf<DeckAction?>(null) }
                var adding by rememberSaveable { mutableStateOf(false) }
                val snackbarHostState = remember { SnackbarHostState() }
                val scope = rememberCoroutineScope()
                val ankiImport = rememberAnkiImportState(snackbarHostState)
                var editingId by rememberSaveable { mutableStateOf<Long?>(null) }
                val studyViewModel: StudyViewModel = viewModel(factory = StudyViewModel.Factory)
                var screen by rememberSaveable { mutableStateOf(Screen.Home) }
                var studied by remember { mutableStateOf(emptyList<StudiedWord>()) }
                val studySpec = MaterialTheme.motionScheme.defaultSpatialSpec<IntOffset>()
                val resultSpec = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
                AnimatedContent(
                    // 화면이 다시 만들어져 학습 결과를 잃으면 홈으로 돌아간다.
                    targetState = if (screen == Screen.Result && studied.isEmpty()) Screen.Home else screen,
                    transitionSpec = {
                        when (targetState) {
                            Screen.Study ->
                                (slideInVertically(studySpec) { it / 4 } + fadeIn()).togetherWith(fadeOut())
                            Screen.Result ->
                                (scaleIn(resultSpec, initialScale = 0.9f) + fadeIn()).togetherWith(fadeOut())
                            Screen.Home ->
                                fadeIn().togetherWith(slideOutVertically(studySpec) { it / 4 } + fadeOut())
                        }
                    },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                ) { target ->
                    when (target) {
                        Screen.Study -> {
                            LaunchedEffect(deck.id) { studyViewModel.start(deck.id) }
                            StudyScreen(
                                state = studyViewModel.state.collectAsStateWithLifecycle().value,
                                onReveal = studyViewModel::reveal,
                                onRate = studyViewModel::rate,
                                onFinish = {
                                    studied = studyViewModel.stop()
                                    screen = if (studied.isEmpty()) Screen.Home else Screen.Result
                                }
                            )
                        }
                        Screen.Result ->
                            StudyResultScreen(
                                studied = studied,
                                // 사라지는 동안에도 결과가 보이도록 결과는 지우지 않는다.
                                onDone = { screen = Screen.Home }
                            )
                        Screen.Home ->
                            HomeScreen(
                                decks = decks,
                                deck = deck,
                                words = words,
                                onDeckSelect = { viewModel.selectDeck(it.id) },
                                onDeckAction = { deckAction = it },
                                onImportClick = ankiImport::start,
                                onAddClick = { adding = true },
                                onWordClick = { editingId = it.card.id },
                                onStudyClick = { screen = Screen.Study },
                                snackbarHostState = snackbarHostState
                            )
                    }
                }
                if (adding) {
                    AddWordSheet(
                        onAdd = {
                            viewModel.add(it, deck.id)
                            adding = false
                            snackbarHostState.currentSnackbarData?.dismiss()
                            scope.launch { snackbarHostState.showSnackbar("단어를 추가했어요") }
                        },
                        onDismiss = { adding = false }
                    )
                }
                AnkiImportHost(ankiImport) { name, imported ->
                    viewModel.importDeck(name, imported, viewModel::selectDeck)
                }
                val closeDeckAction = { deckAction = null }
                when (deckAction) {
                    DeckAction.Rename ->
                        RenameDeckSheet(
                            deck = deck,
                            onRename = {
                                viewModel.renameDeck(deck, it)
                                closeDeckAction()
                            },
                            onDismiss = closeDeckAction
                        )
                    DeckAction.Clear ->
                        ClearDeckDialog(
                            deck = deck,
                            wordCount = words.size,
                            onConfirm = {
                                viewModel.clearDeck(deck)
                                closeDeckAction()
                            },
                            onDismiss = closeDeckAction
                        )
                    DeckAction.Delete ->
                        DeleteDeckDialog(
                            deck = deck,
                            wordCount = words.size,
                            onConfirm = {
                                viewModel.deleteDeck(deck)
                                closeDeckAction()
                            },
                            onDismiss = closeDeckAction
                        )
                    null -> {}
                }
                words.find { it.card.id == editingId }?.let { word ->
                    EditWordSheet(
                        word = word,
                        onSave = {
                            viewModel.update(it)
                            editingId = null
                        },
                        onDelete = {
                            viewModel.delete(word)
                            editingId = null
                        },
                        onDismiss = { editingId = null }
                    )
                }
            }
        }
    }
}
