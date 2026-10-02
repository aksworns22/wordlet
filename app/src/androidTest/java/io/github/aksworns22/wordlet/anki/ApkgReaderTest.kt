package io.github.aksworns22.wordlet.anki

import android.database.sqlite.SQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.github.luben.zstd.ZstdOutputStream
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(AndroidJUnit4::class)
class ApkgReaderTest {
    private val cacheDir = InstrumentationRegistry.getInstrumentation().targetContext.cacheDir

    private val expected =
        listOf(
            AnkiNoteType(
                name = "Basic",
                fields = listOf("Front", "Back"),
                notes = listOf(listOf("nuance", "뉘앙스\n미묘한 차이"), listOf("subtle", ""))
            ),
            AnkiNoteType(
                name = "Vocab",
                fields = listOf("Word", "Meaning", "Example"),
                notes = listOf(listOf("ephemeral", "덧없는", "an ephemeral joy"))
            )
        )

    private val notes =
        listOf(
            1L to "nuance\u001f뉘앙스<br>미묘한 차이",
            1L to "subtle",
            2L to "<b>ephemeral</b>\u001f덧없는\u001fan ephemeral joy [sound:e.mp3]"
        )

    @Test
    fun readsLegacyCollection() {
        val apkg = apkg("collection.anki2" to collection(legacy = true))

        assertEquals(expected, ApkgReader.read(apkg, cacheDir))
    }

    @Test
    fun prefersZstdCollectionOverPlaceholder() {
        val placeholder = collection(legacy = true, notes = listOf(1L to "Please update to the latest Anki version"))
        val apkg =
            apkg(
                "collection.anki2" to placeholder,
                "collection.anki21b" to zstd(collection(legacy = false))
            )

        assertEquals(expected, ApkgReader.read(apkg, cacheDir))
    }

    @Test(expected = UnsupportedApkgException::class)
    fun rejectsZipWithoutCollection() {
        ApkgReader.read(apkg("media" to "{}".toByteArray()), cacheDir)
    }

    private fun collection(
        legacy: Boolean,
        notes: List<Pair<Long, String>> = this.notes
    ): ByteArray {
        val file = File.createTempFile("collection", ".db", cacheDir)
        file.delete()
        SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
            db.execSQL("CREATE TABLE notes (id INTEGER PRIMARY KEY, mid INTEGER, flds TEXT)")
            notes.forEachIndexed { i, (mid, flds) ->
                db.execSQL("INSERT INTO notes VALUES (?, ?, ?)", arrayOf(i + 1L, mid, flds))
            }
            val types = listOf(1L to ("Basic" to listOf("Front", "Back")), 2L to ("Vocab" to listOf("Word", "Meaning", "Example")))
            if (legacy) {
                val models = JSONObject()
                types.forEach { (id, type) ->
                    val flds = JSONArray()
                    // 필드는 ord로 정렬해야 하므로 일부러 거꾸로 넣는다.
                    type.second.withIndex().reversed().forEach { (ord, name) ->
                        flds.put(JSONObject().put("name", name).put("ord", ord))
                    }
                    models.put(id.toString(), JSONObject().put("name", type.first).put("flds", flds))
                }
                db.execSQL("CREATE TABLE col (models TEXT)")
                db.execSQL("INSERT INTO col VALUES (?)", arrayOf(models.toString()))
            } else {
                db.execSQL("CREATE TABLE notetypes (id INTEGER PRIMARY KEY, name TEXT)")
                db.execSQL("CREATE TABLE fields (ntid INTEGER, ord INTEGER, name TEXT)")
                types.forEach { (id, type) ->
                    db.execSQL("INSERT INTO notetypes VALUES (?, ?)", arrayOf(id, type.first))
                    type.second.forEachIndexed { ord, name ->
                        db.execSQL("INSERT INTO fields VALUES (?, ?, ?)", arrayOf(id, ord, name))
                    }
                }
            }
        }
        return file.readBytes().also { file.delete() }
    }

    private fun zstd(bytes: ByteArray): ByteArray =
        ByteArrayOutputStream().also { out -> ZstdOutputStream(out).use { it.write(bytes) } }.toByteArray()

    private fun apkg(vararg entries: Pair<String, ByteArray>): ByteArrayInputStream {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            entries.forEach { (name, bytes) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(bytes)
                zip.closeEntry()
            }
        }
        return ByteArrayInputStream(out.toByteArray())
    }
}
