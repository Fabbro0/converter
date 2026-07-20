package com.megaconverter.app.library

import android.content.Context
import com.megaconverter.app.converter.FileFormat
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

/**
 * Persists the user's saved-file library as a small JSON index plus copies of the
 * files themselves in app-private storage, so they survive independently of the
 * conversion cache (which the system can clear at any time). No database dependency
 * (Room etc.) on purpose: this is a personal, small, on-device list — a JSON file
 * read into memory is simpler and has zero extra build-time risk.
 */
object LibraryStore {

    private const val FILES_DIR = "library_files"
    private const val INDEX_FILE = "library_index.json"

    private fun libraryDir(context: Context): File = File(context.filesDir, FILES_DIR).apply { mkdirs() }
    private fun indexFile(context: Context): File = File(context.filesDir, INDEX_FILE)

    fun loadAll(context: Context): List<LibraryItem> {
        val file = indexFile(context)
        if (!file.exists()) return emptyList()
        return try {
            val array = JSONArray(file.readText(Charsets.UTF_8))
            (0 until array.length()).mapNotNull { i -> parseItem(array.optJSONObject(i)) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun parseItem(obj: JSONObject?): LibraryItem? {
        if (obj == null) return null
        val format = FileFormat.fromExtension(obj.optString("format")) ?: return null
        val tagsArray = obj.optJSONArray("tags")
        val tags = if (tagsArray != null) (0 until tagsArray.length()).map { tagsArray.getString(it) } else emptyList()
        return LibraryItem(
            id = obj.optString("id"),
            fileName = obj.optString("fileName"),
            displayName = obj.optString("displayName"),
            format = format,
            tags = tags,
            addedAt = obj.optLong("addedAt"),
        )
    }

    private fun saveAll(context: Context, items: List<LibraryItem>) {
        val array = JSONArray()
        items.forEach { item ->
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("fileName", item.fileName)
            obj.put("displayName", item.displayName)
            obj.put("format", item.format.extension)
            obj.put("tags", JSONArray(item.tags))
            obj.put("addedAt", item.addedAt)
            array.put(obj)
        }
        indexFile(context).writeText(array.toString(), Charsets.UTF_8)
    }

    fun addItem(context: Context, sourceFile: File, displayName: String, format: FileFormat): LibraryItem {
        val id = UUID.randomUUID().toString()
        val storedFile = File(libraryDir(context), "$id.${format.extension}")
        sourceFile.copyTo(storedFile, overwrite = true)
        val item = LibraryItem(
            id = id,
            fileName = storedFile.name,
            displayName = displayName,
            format = format,
            tags = emptyList(),
            addedAt = System.currentTimeMillis(),
        )
        saveAll(context, loadAll(context) + item)
        return item
    }

    fun updateTags(context: Context, itemId: String, tags: List<String>) {
        val items = loadAll(context).map { if (it.id == itemId) it.copy(tags = tags) else it }
        saveAll(context, items)
    }

    fun deleteItem(context: Context, item: LibraryItem) {
        File(libraryDir(context), item.fileName).delete()
        saveAll(context, loadAll(context).filterNot { it.id == item.id })
    }

    fun fileFor(context: Context, item: LibraryItem): File = File(libraryDir(context), item.fileName)
}
