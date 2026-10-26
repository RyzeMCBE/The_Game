package com.ryzemc.pequenomundo

import android.content.Context
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.util.AtomicFile
import java.io.File
import java.util.concurrent.Executors

class LocalStore(context: Context) {
    private val prefs = context.getSharedPreferences("family", Context.MODE_PRIVATE)
    private val root = context.filesDir
    private val worker = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())
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
            listOf("colors", "shapes", "numbers", "animals", "memory", "puzzle").sumOf(::successes)

    fun readDraft(): Painting =
        Painting.fromJson(
            runCatching {
                    AtomicFile(File(root, "draft.json")).openRead().bufferedReader().use {
                        it.readText()
                    }
                }
                .getOrNull()
        )

    fun saveDraft(painting: Painting) {
        val json = painting.toJson()
        worker.execute {
            val file = AtomicFile(File(root, "draft.json"))
            var stream: java.io.FileOutputStream? = null
            try {
                stream = file.startWrite()
                stream.write(json.toByteArray(Charsets.UTF_8))
                file.finishWrite(stream)
            } catch (_: Exception) {
                stream?.let { file.failWrite(it) }
            }
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
