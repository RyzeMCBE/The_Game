package com.ryzemc.pequenomundo

import android.content.Context
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.util.AtomicFile
import java.io.File
import java.util.concurrent.Executors
import org.json.JSONArray

class LocalStore(context: Context) {
    private val prefs = context.getSharedPreferences("family", Context.MODE_PRIVATE)
    private val root = context.filesDir
    private val worker = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())
    private val draftCache =
        object : android.util.LruCache<String, String>(2 * 1024 * 1024) {
            override fun sizeOf(key: String, value: String) = value.length * 2
        }
    var voice: Boolean
        get() = prefs.getBoolean("voice", true)
        set(value) {
            prefs.edit().putBoolean("voice", value).apply()
        }

    var music: Boolean
        get() = prefs.getBoolean("music", false)
        set(value) {
            prefs.edit().putBoolean("music", value).apply()
        }

    var difficulty: Int
        get() = prefs.getInt("difficulty", 2)
        set(value) {
            prefs.edit().putInt("difficulty", value.coerceIn(2, 3)).apply()
        }

    var breakMinutes: Int
        get() = prefs.getInt("break", 10)
        set(value) {
            prefs.edit().putInt("break", value).apply()
        }

    var elapsed: Long
        get() = prefs.getLong("elapsed", 0)
        set(value) {
            prefs.edit().putLong("elapsed", value).apply()
        }

    fun successes(kind: String) = prefs.getInt("success_$kind", 0)

    fun record(kind: String) {
        prefs.edit().putInt("success_$kind", successes(kind) + 1).apply()
    }

    val stars: Int
        get() =
            listOf(
                    "colors",
                    "shapes",
                    "numbers",
                    "animals",
                    "memory",
                    "puzzle",
                    "shadows",
                    "sizes",
                    "sorting",
                    "paths",
                    "painting",
                )
                .sumOf(::successes)

    var countRange: Int
        get() = prefs.getInt("count_range", 0)
        set(value) {
            prefs.edit().putInt("count_range", if (value in listOf(3, 5, 10)) value else 0).apply()
        }

    val countMaximum: Int
        get() =
            if (countRange != 0) countRange
            else
                when {
                    successes("numbers") < 5 -> 3
                    successes("numbers") < 12 -> 5
                    else -> 10
                }

    private fun rotation(): DrawingRotation {
        val remaining = prefs.getString("drawing_remaining", null)?.let { readIds(it) }
        return DrawingRotation(
            DrawingCatalog.pages.associate { it.id to it.category },
            remaining,
            readIds(prefs.getString("drawing_recent", "[]")!!),
        )
    }

    private fun readIds(json: String): List<String> =
        runCatching {
                val a = JSONArray(json)
                (0 until a.length()).map { a.getString(it) }
            }
            .getOrDefault(emptyList())

    private fun persistRotation(rotation: DrawingRotation) {
        prefs
            .edit()
            .putString("drawing_remaining", JSONArray(rotation.remaining).toString())
            .putString("drawing_recent", JSONArray(rotation.recent).toString())
            .apply()
    }

    fun nextDrawing(): String = rotation().let { r -> r.next().also { persistRotation(r) } }

    fun chooseDrawing(id: String) {
        rotation().let { r ->
            r.choose(id)
            persistRotation(r)
        }
    }

    fun drawingOrder(): List<String> =
        rotation().let { r ->
            persistRotation(r)
            (r.remaining + DrawingCatalog.pages.map { it.id }).distinct()
        }

    fun readSheetDraft(id: String): Painting {
        if (id !in Painting.templates) return Painting()
        draftCache.get(id)?.let {
            return Painting.fromJson(it)
        }
        val raw =
            runCatching {
                    AtomicFile(File(root, "drafts/$id.json")).openRead().bufferedReader().use {
                        it.readText()
                    }
                }
                .getOrNull()
        return if (raw != null) Painting.fromJson(raw)
        else readDraft().takeIf { it.template == id } ?: Painting(id)
    }

    fun readDraft(): Painting =
        Painting.fromJson(
            draftCache.get("last")
                ?: runCatching {
                        AtomicFile(File(root, "draft.json")).openRead().bufferedReader().use {
                            it.readText()
                        }
                    }
                    .getOrNull()
        )

    fun saveDraft(painting: Painting) {
        val json = painting.toJson()
        val template = painting.template
        draftCache.put(template, json)
        draftCache.put("last", json)
        worker.execute {
            writeDraft(File(root, "draft.json"), json)
            val directory = File(root, "drafts").apply { mkdirs() }
            writeDraft(File(directory, "$template.json"), json)
        }
    }

    private fun writeDraft(destination: File, json: String) {
        val file = AtomicFile(destination)
        var stream: java.io.FileOutputStream? = null
        try {
            stream = file.startWrite()
            stream.write(json.toByteArray(Charsets.UTF_8))
            file.finishWrite(stream)
        } catch (_: Exception) {
            stream?.let { file.failWrite(it) }
        }
    }

    fun gallery(): List<File> =
        File(root, "gallery")
            .listFiles()
            ?.filter { it.extension == "png" }
            ?.sortedByDescending { it.name } ?: emptyList()

    fun savePicture(bitmap: Bitmap, callback: (Boolean) -> Unit) {
        worker.execute {
            val success =
                runCatching {
                        val directory = File(root, "gallery").apply { mkdirs() }
                        val file =
                            AtomicFile(File(directory, "dibujo-${System.currentTimeMillis()}.png"))
                        val stream = file.startWrite()
                        try {
                            check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream))
                            file.finishWrite(stream)
                        } catch (e: Exception) {
                            file.failWrite(stream)
                            throw e
                        }
                    }
                    .isSuccess
            bitmap.recycle()
            main.post { callback(success) }
        }
    }

    fun close() {
        worker.shutdown()
    }
}
