package io.github.aksworns22.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import io.github.aksworns22.fsrs.State
import io.github.aksworns22.home.Deck
import java.time.Instant

@Database(entities = [WordEntity::class, DeckEntity::class], version = 2)
@TypeConverters(Converters::class)
abstract class WordDatabase : RoomDatabase() {
    abstract fun wordDao(): WordDao

    abstract fun deckDao(): DeckDao

    companion object {
        @Volatile
        private var instance: WordDatabase? = null

        fun get(context: Context): WordDatabase =
            instance ?: synchronized(this) {
                instance ?: Room
                    .databaseBuilder(context.applicationContext, WordDatabase::class.java, "wordlet.db")
                    .addMigrations(AddDecks)
                    .addCallback(
                        object : Callback() {
                            override fun onCreate(db: SupportSQLiteDatabase) = db.insertBasicDeck()
                        }
                    ).build()
                    .also { instance = it }
            }
    }
}

/** 단어장을 추가하고, 이미 있던 단어는 모두 "기본" 단어장에 넣는다. */
private object AddDecks : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `decks` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL)"
        )
        db.insertBasicDeck()
        db.execSQL("ALTER TABLE `words` ADD COLUMN `deckId` INTEGER NOT NULL DEFAULT ${Deck.BASIC_ID}")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_words_deckId` ON `words` (`deckId`)")
    }
}

private fun SupportSQLiteDatabase.insertBasicDeck() =
    execSQL("INSERT INTO `decks` (`id`, `name`) VALUES (${Deck.BASIC_ID}, '${Deck.BASIC_NAME}')")

class Converters {
    @TypeConverter
    fun fromInstant(value: Instant?): Long? = value?.toEpochMilli()

    @TypeConverter
    fun toInstant(value: Long?): Instant? = value?.let(Instant::ofEpochMilli)

    @TypeConverter
    fun fromState(value: State): Int = value.value

    @TypeConverter
    fun toState(value: Int): State = State.entries.first { it.value == value }
}
