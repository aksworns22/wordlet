package io.github.aksworns22.word

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.aksworns22.data.DeckDao
import io.github.aksworns22.data.WordDao
import io.github.aksworns22.data.WordDatabase
import io.github.aksworns22.data.toDeck
import io.github.aksworns22.data.toEntity
import io.github.aksworns22.data.toWord
import io.github.aksworns22.fsrs.Card
import io.github.aksworns22.home.Deck
import io.github.aksworns22.home.Word
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WordViewModel(
    private val dao: WordDao,
    private val deckDao: DeckDao
) : ViewModel() {
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

    /** [deck]을 그 안의 단어와 함께 지운다. */
    fun deleteDeck(deck: Deck) {
        viewModelScope.launch { deckDao.delete(deck.id) }
    }

    fun update(word: Word) {
        viewModelScope.launch { dao.update(word.toEntity()) }
    }

    fun delete(word: Word) {
        viewModelScope.launch { dao.delete(word.toEntity()) }
    }

    companion object {
        val Factory =
            viewModelFactory {
                initializer {
                    val db = WordDatabase.get(this[APPLICATION_KEY]!!)
                    WordViewModel(db.wordDao(), db.deckDao())
                }
            }
    }
}
