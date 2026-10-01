package io.github.aksworns22.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WordDao {
    // 카드 id는 생성 시각이므로 최근에 추가한 단어가 위에 온다.
    @Query("SELECT * FROM words ORDER BY id DESC")
    fun observeAll(): Flow<List<WordEntity>>

    @Query("SELECT * FROM words WHERE deckId = :deckId")
    suspend fun inDeck(deckId: Long): List<WordEntity>

    @Query("SELECT id FROM words")
    suspend fun ids(): List<Long>

    @Insert
    suspend fun insert(word: WordEntity)

    @Insert
    suspend fun insertAll(words: List<WordEntity>)

    @Update
    suspend fun update(word: WordEntity)

    @Delete
    suspend fun delete(word: WordEntity)
}
