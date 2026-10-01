package io.github.aksworns22

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.AndroidComposeUiFlags
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.aksworns22.deck.DeckAction
import io.github.aksworns22.deck.DeleteDeckDialog
import io.github.aksworns22.deck.RenameDeckSheet
import io.github.aksworns22.home.Deck
import io.github.aksworns22.home.HomeScreen
import io.github.aksworns22.ui.theme.WordletTheme
import io.github.aksworns22.word.AddWordSheet
import io.github.aksworns22.word.EditWordSheet
import io.github.aksworns22.word.WordViewModel

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalComposeUiApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 기본 스케줄러는 입력칸을 옮길 때 이전 칸의 키보드 숨김과 새 칸의 키보드 표시를 따로 처리해
        // 키보드가 잠깐 내려갔다 올라온다(삼성 키보드에서 확인). 프레임 단위로 묶어 처리하도록 끈다.
        AndroidComposeUiFlags.isOutOfFrameSchedulerForTextInputEventsEnabled = false
        enableEdgeToEdge()
        setContent {
            WordletTheme {
                val viewModel: WordViewModel = viewModel(factory = WordViewModel.Factory)
                // DB에서 처음 읽어오기 전에 빈 홈이 잠깐 보이지 않도록 그리지 않는다.
                val allWords = viewModel.words.collectAsStateWithLifecycle().value ?: return@WordletTheme
                val decks = viewModel.decks.collectAsStateWithLifecycle().value ?: return@WordletTheme
                var deckId by rememberSaveable { mutableStateOf(Deck.BASIC_ID) }
                // 고른 단어장이 없으면 첫 단어장을 보여준다.
                val deck = decks.find { it.id == deckId } ?: decks.first()
                val words = remember(allWords, deck) { allWords.filter { it.deckId == deck.id } }
                var deckAction by rememberSaveable { mutableStateOf<DeckAction?>(null) }
                var adding by rememberSaveable { mutableStateOf(false) }
                var editingId by rememberSaveable { mutableStateOf<Long?>(null) }
                HomeScreen(
                    decks = decks,
                    deck = deck,
                    words = words,
                    onDeckSelect = { deckId = it.id },
                    onDeckAction = { deckAction = it },
                    onAddClick = { adding = true },
                    onWordClick = { editingId = it.card.id },
                    onStudyClick = {}
                )
                if (adding) {
                    AddWordSheet(
                        onAdd = {
                            viewModel.add(it, deck.id)
                            adding = false
                        },
                        onDismiss = { adding = false }
                    )
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
