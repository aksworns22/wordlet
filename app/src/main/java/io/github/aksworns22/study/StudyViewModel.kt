package io.github.aksworns22.study

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.aksworns22.data.WordDao
import io.github.aksworns22.data.WordDatabase
import io.github.aksworns22.data.toEntity
import io.github.aksworns22.data.toWord
import io.github.aksworns22.fsrs.Card
import io.github.aksworns22.fsrs.Rating
import io.github.aksworns22.fsrs.Scheduler
import io.github.aksworns22.home.Word
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant

/** 평가했을 때 바뀔 카드와 다음 복습까지의 간격 */
data class RatingPreview(
    val card: Card,
    val interval: Duration
)

data class StudyState(
    val word: Word,
    /** 정답을 보기 전에는 비어 있다. */
    val previews: Map<Rating, RatingPreview> = emptyMap()
) {
    val revealed: Boolean get() = previews.isNotEmpty()
}

/** 학습은 끝이 없고, 사용자가 뒤로 나가면 [stop]으로 끝난다. */
class StudyViewModel(
    private val dao: WordDao,
    private val scheduler: Scheduler = Scheduler(),
    private val clock: () -> Instant = Instant::now
) : ViewModel() {
    private val _state = MutableStateFlow<StudyState?>(null)

    /** 단어를 읽어오기 전에는 null이다. */
    val state: StateFlow<StudyState?> = _state.asStateFlow()

    private var deckId: Long? = null
    private var words: List<Word> = emptyList()
    private var loading: Job? = null

    /** [deckId] 단어장의 학습을 시작한다. 화면이 회전해 다시 불려도 이어서 학습한다. */
    fun start(deckId: Long) {
        if (this.deckId == deckId) return
        this.deckId = deckId
        _state.value = null
        loading?.cancel()
        loading =
            viewModelScope.launch {
                words = dao.inDeck(deckId).map { it.toWord() }
                _state.value = nextWord(words, clock())?.let(::StudyState)
            }
    }

    /** 정답을 보여주고, 평가마다 바뀔 카드를 미리 계산해 둔다. */
    fun reveal() {
        val current = _state.value ?: return
        if (current.revealed) return
        val now = clock()
        val previews =
            Rating.entries.associateWith { rating ->
                val card = scheduler.reviewCard(current.word.card, rating, now).first
                RatingPreview(card, Duration.between(now, card.due))
            }
        _state.value = current.copy(previews = previews)
    }

    /**
     * [rating]으로 평가한 카드를 저장하고 다음 단어로 넘어간다.
     * fuzz 때문에 다시 계산하면 보여준 간격과 달라지므로 미리 계산한 카드를 그대로 쓴다.
     */
    fun rate(rating: Rating) {
        val current = _state.value ?: return
        val card = current.previews[rating]?.card ?: return
        val reviewed = current.word.copy(card = card)
        words = words.map { if (it.card.id == card.id) reviewed else it }
        viewModelScope.launch { dao.update(reviewed.toEntity()) }
        _state.value = nextWord(words, clock(), previousId = card.id)?.let(::StudyState)
    }

    fun stop() {
        loading?.cancel()
        deckId = null
        words = emptyList()
        _state.value = null
    }

    companion object {
        val Factory =
            viewModelFactory {
                initializer {
                    StudyViewModel(WordDatabase.get(this[APPLICATION_KEY]!!).wordDao())
                }
            }
    }
}
