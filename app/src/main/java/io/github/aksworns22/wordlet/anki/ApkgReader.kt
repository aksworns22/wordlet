package io.github.aksworns22.wordlet.anki

import android.database.sqlite.SQLiteDatabase
import com.github.luben.zstd.ZstdInputStream
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.util.zip.ZipInputStream

/**
 * 같은 필드 구성을 가진 Anki 노트 묶음. [notes]의 각 값은 [fields] 순서를 따르며 [cleanField]로 정리돼 있다.
 * 내용이 있는 필드만 담는다.
 */
data class AnkiNoteType(
    val name: String,
    val fields: List<String>,
    val notes: List<List<String>>
)

/** 모든 노트에서 비어 있는 필드(소리·이미지만 든 필드 등)를 뺀다. */
fun AnkiNoteType.withoutEmptyFields(): AnkiNoteType {
    val kept = fields.indices.filter { i -> notes.any { it[i].isNotBlank() } }
    return copy(fields = kept.map(fields::get), notes = notes.map { note -> kept.map(note::get) })
}

class UnsupportedApkgException : Exception()

/** Anki 덱 파일(.apkg)에서 노트 타입별로 노트를 읽는다. 학습 기록은 읽지 않는다. */
object ApkgReader {
    // 최신 Anki가 내보낸 파일에도 구버전용 collection.anki2가 들어 있지만
    // "새 버전이 필요하다"는 안내 노트뿐이므로 새 형식을 먼저 쓴다.
    private val collections = listOf("collection.anki21b", "collection.anki21", "collection.anki2")

    /** [workDir]에 컬렉션 DB를 잠깐 풀어 읽고 지운다. */
    fun read(
        input: InputStream,
        workDir: File
    ): List<AnkiNoteType> {
        val extracted = mutableMapOf<String, File>()
        try {
            ZipInputStream(input.buffered()).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    if (entry.name !in collections) continue
                    val file = File.createTempFile("anki", ".db", workDir)
                    extracted[entry.name] = file
                    val source = if (entry.name.endsWith("b")) ZstdInputStream(zip) else zip
                    file.outputStream().use { source.copyTo(it) }
                }
            }
            val name = collections.firstOrNull { it in extracted } ?: throw UnsupportedApkgException()
            return readCollection(extracted.getValue(name))
        } finally {
            extracted.values.forEach { it.delete() }
        }
    }

    private fun readCollection(file: File): List<AnkiNoteType> {
        val db =
            SQLiteDatabase.openDatabase(
                file.path,
                null,
                SQLiteDatabase.OPEN_READONLY or SQLiteDatabase.NO_LOCALIZED_COLLATORS
            )
        return db.use {
            val types = if (it.hasTable("notetypes")) it.readNoteTypes() else it.readLegacyNoteTypes()
            val notes = it.readNotes()
            types
                .mapNotNull { (id, type) ->
                    val (name, fields) = type
                    val rows = notes[id] ?: return@mapNotNull null
                    AnkiNoteType(
                        name = name,
                        fields = fields,
                        // 필드가 노트 타입보다 적게 저장된 노트도 있어 빈 값으로 채운다.
                        notes = rows.map { row -> fields.indices.map { i -> cleanField(row.getOrElse(i) { "" }) } }
                    ).withoutEmptyFields()
                }
        }
    }

    private fun SQLiteDatabase.hasTable(name: String): Boolean =
        rawQuery("SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = ?", arrayOf(name)).use { it.moveToFirst() }

    /** 스키마 v18 이상(collection.anki21b): 노트 타입과 필드가 테이블로 나뉘어 있다. */
    private fun SQLiteDatabase.readNoteTypes(): Map<Long, Pair<String, List<String>>> {
        val fields = mutableMapOf<Long, MutableList<String>>()
        rawQuery("SELECT ntid, name FROM fields ORDER BY ntid, ord", null).use {
            while (it.moveToNext()) fields.getOrPut(it.getLong(0)) { mutableListOf() }.add(it.getString(1))
        }
        val types = linkedMapOf<Long, Pair<String, List<String>>>()
        rawQuery("SELECT id, name FROM notetypes ORDER BY id", null).use {
            while (it.moveToNext()) {
                val id = it.getLong(0)
                types[id] = it.getString(1) to fields[id].orEmpty()
            }
        }
        return types
    }

    /** 구 스키마(collection.anki2, anki21): col.models에 노트 타입이 JSON으로 들어 있다. */
    private fun SQLiteDatabase.readLegacyNoteTypes(): Map<Long, Pair<String, List<String>>> {
        val json = rawQuery("SELECT models FROM col", null).use { if (it.moveToFirst()) it.getString(0) else null }
        val models = JSONObject(json ?: return emptyMap())
        val types = sortedMapOf<Long, Pair<String, List<String>>>()
        for (key in models.keys()) {
            val model = models.getJSONObject(key)
            val flds = model.getJSONArray("flds")
            val fields =
                (0 until flds.length())
                    .map { flds.getJSONObject(it) }
                    .sortedBy { it.getInt("ord") }
                    .map { it.getString("name") }
            types[key.toLong()] = model.getString("name") to fields
        }
        return types
    }

    /** 노트 타입 id별 노트 목록. 덱에 추가된 순서를 따른다. */
    private fun SQLiteDatabase.readNotes(): Map<Long, List<List<String>>> {
        val notes = mutableMapOf<Long, MutableList<List<String>>>()
        rawQuery("SELECT mid, flds FROM notes ORDER BY id", null).use {
            while (it.moveToNext()) {
                notes.getOrPut(it.getLong(0)) { mutableListOf() }.add(it.getString(1).split('\u001f'))
            }
        }
        return notes
    }
}
