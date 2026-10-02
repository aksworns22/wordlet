package io.github.aksworns22.wordlet.word

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.aksworns22.fsrs.Card
import io.github.aksworns22.wordlet.anki.AnkiWord
import io.github.aksworns22.wordlet.data.DeckDao
import io.github.aksworns22.wordlet.data.DeckEntity
import io.github.aksworns22.wordlet.data.WordDao
import io.github.aksworns22.wordlet.data.WordDatabase
import io.github.aksworns22.wordlet.data.toDeck
import io.github.aksworns22.wordlet.data.toEntity
import io.github.aksworns22.wordlet.data.toWord
import io.github.aksworns22.wordlet.home.Deck
import io.github.aksworns22.wordlet.home.Word
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant

class WordViewModel(
    private val dao: WordDao,
    private val deckDao: DeckDao,
    private val prefs: SharedPreferences
) : ViewModel() {
    private val _deckId = MutableStateFlow(prefs.getLong(DECK_ID_KEY, Deck.BASIC_ID))

    /** 마지막으로 고른 단어장의 id. 앱을 다시 켜도 유지된다. */
    val deckId: StateFlow<Long> = _deckId.asStateFlow()

    /** DB에서 처음 읽어오기 전에는 null이다. "기본" 단어장이 있어 비어 있지 않다. */
    val decks: StateFlow<List<Deck>?> =
        deckDao
            .observeAll()
            .map { entities -> entities.map { it.toDeck() } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** DB에서 처음 읽어오기 전에는 null이다. */
    val words: StateFlow<List<Word>?> =
        dao
            .observeAll()
            .map { entities -> entities.map { it.toWord() } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun selectDeck(id: Long) {
        _deckId.value = id
        prefs.edit { putLong(DECK_ID_KEY, id) }
    }

    fun add(
        word: Word,
        deckId: Long
    ) {
        viewModelScope.launch { dao.insert(word.copy(deckId = deckId).toEntity()) }
    }

    fun renameDeck(
        deck: Deck,
        name: String
    ) {
        viewModelScope.launch { deckDao.rename(deck.id, name) }
    }

    /** [deck]의 모든 단어를 처음 추가한 것처럼 학습 기록이 없는 새 카드로 되돌린다. */
    fun resetDeck(deck: Deck) {
        val new = Card(id = 0)
        viewModelScope.launch { deckDao.reset(deck.id, new.state, new.step, new.due) }
    }

    /** [deck]은 남기고 그 안의 단어를 모두 지운다. */
    fun clearDeck(deck: Deck) {
        viewModelScope.launch { deckDao.clear(deck.id) }
    }

    /** [deck]을 그 안의 단어와 함께 지운다. */
    fun deleteDeck(deck: Deck) {
        viewModelScope.launch { deckDao.delete(deck.id) }
    }

    /**
     * [name] 단어장을 새로 만들어 가져온 단어를 모두 새 카드로 추가하고, 다 넣으면 그 단어장의 id로 [onImported]를 부른다.
     * 앞의 단어일수록 목록 위에 온다.
     */
    fun importDeck(
        name: String,
        words: List<AnkiWord>,
        onImported: (Long) -> Unit
    ) {
        viewModelScope.launch {
            val deckId = deckDao.insert(DeckEntity(name = name))
            // 카드 id는 생성 시각이라 한꺼번에 만들면 겹치므로, 지금부터 과거로 비어 있는 id를 하나씩 쓴다.
            val taken = dao.ids().toHashSet()
            var id = Instant.now().toEpochMilli()
            val entities =
                words.map {
                    while (id in taken) id--
                    Word(it.term, it.meaning, it.example, deckId, Card(id = id--)).toEntity()
                }
            dao.insertAll(entities)
            onImported(deckId)
        }
    }

    fun update(word: Word) {
        viewModelScope.launch { dao.update(word.toEntity()) }
    }

    fun delete(word: Word) {
        viewModelScope.launch { dao.delete(word.toEntity()) }
    }

    companion object {
        private const val DECK_ID_KEY = "deck_id"

        val Factory =
            viewModelFactory {
                initializer {
                    val app = this[APPLICATION_KEY]!!
                    val db = WordDatabase.get(app)
                    WordViewModel(db.wordDao(), db.deckDao(), app.getSharedPreferences("wordlet", Context.MODE_PRIVATE))
                }
            }
    }
}
