package com.ryzemc.pequenomundo

import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaMetadataRetriever
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import org.hamcrest.CoreMatchers.`is`
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OfflineAppTest {
    private val instrumentation
        get() = InstrumentationRegistry.getInstrumentation()

    private val context
        get() = instrumentation.targetContext

    private fun tagged(tag: String) = withTagValue(`is`(tag as Any))

    private fun configure(voice: Boolean = false) {
        context
            .getSharedPreferences("family", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("voice", voice)
            .putBoolean("music", false)
            .putInt("difficulty", 2)
            .putLong("elapsed", 0)
            .commit()
    }

    private fun awaitTag(
        scenario: ActivityScenario<MainActivity>,
        tag: String,
        timeout: Long = 10000,
    ) {
        val deadline = SystemClock.uptimeMillis() + timeout
        while (SystemClock.uptimeMillis() < deadline) {
            val found = AtomicBoolean(false)
            scenario.onActivity {
                found.set(it.window.decorView.findViewWithTag<View>(tag)?.isShown == true)
            }
            if (found.get()) return
            Thread.sleep(70)
        }
        fail("Screen did not appear: $tag")
    }

    private fun answer(scenario: ActivityScenario<MainActivity>): String {
        var selected = ""
        scenario.onActivity { activity ->
            val decor = activity.window.decorView
            val answers = decor.findViewWithTag<ViewGroup>("answers")
            assertNull(decor.findViewWithTag<View>("next"))
            for (i in 0 until answers.childCount) {
                val choice = answers.getChildAt(i)
                if (choice.isEnabled) choice.performClick()
                if (decor.findViewWithTag<View>("celebration") != null) {
                    selected = choice.tag.toString().removePrefix("answer_")
                    break
                }
            }
            assertTrue(selected.isNotEmpty())
        }
        return selected
    }

    private fun capture(name: String) {
        require(name.matches(Regex("[a-z0-9-]+")))
        instrumentation.waitForIdleSync()
        fun shell(command: String) {
            val output = instrumentation.uiAutomation.executeShellCommand(command)
            android.os.ParcelFileDescriptor.AutoCloseInputStream(output).use { it.readBytes() }
        }
        // UTP can uninstall the app after the suite; keep QA images outside its data directory.
        shell("mkdir -p /data/local/tmp/noah-qa")
        shell("screencap -p /data/local/tmp/noah-qa/$name.png")
    }

    @Test
    fun completeOfflineJourneyUsesNativeViewsAndPersistsArtwork() {
        configure()
        @Suppress("DEPRECATION")
        val permissions =
            context.packageManager
                .getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
                .requestedPermissions
                ?.toList() ?: emptyList()
        assertFalse(permissions.contains("android.permission.INTERNET"))
        assertFalse(
            permissions.any { it.contains("STORAGE") || it == "android.permission.RECORD_AUDIO" }
        )
        val audio = context.assets.list("audio")!!.toSet()
        assertTrue(
            audio.containsAll(
                listOf(
                    "welcome.wav",
                    "ask_red.wav",
                    "number_5.wav",
                    "sound_cow.wav",
                    "music.wav",
                    "reward_chime.wav",
                )
            )
        )
        for (file in audio) assertTrue(
            context.assets.open("audio/$file").use { it.readBytes().size } > 100
        )
        val directory = File(context.filesDir, "gallery")
        val before = directory.listFiles()?.count { it.extension == "png" } ?: 0
        val colorsBefore =
            LocalStore(context).let { it.successes("colors").also { _ -> it.close() } }
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                fun native(view: View): Boolean =
                    view !is WebView &&
                        (view !is ViewGroup ||
                            (0 until view.childCount).all { native(view.getChildAt(it)) })
                assertTrue(native(activity.window.decorView))
            }
            onView(withText("¡Hola, Noah!")).check(matches(isDisplayed()))
            capture("home-noah")
            onView(tagged("paint")).perform(scrollTo(), click())
            onView(tagged("paint_canvas")).perform(scrollTo(), swipeRight())
            scenario.recreate()
            scenario.onActivity {
                assertTrue(
                    it.window.decorView
                        .findViewWithTag<PaintCanvasView>("paint_canvas")
                        .painting
                        .marks
                        .isNotEmpty()
                )
            }
            capture("painting")
            onView(tagged("save")).perform(scrollTo(), click())
            val until = SystemClock.uptimeMillis() + 8000
            while (
                (directory.listFiles()?.count { it.extension == "png" } ?: 0) <= before &&
                    SystemClock.uptimeMillis() < until
            ) Thread.sleep(80)
            val saved =
                directory.listFiles()!!.filter { it.extension == "png" }.maxBy { it.lastModified() }
            assertTrue(saved.length() > 1000)
            onView(tagged("home")).perform(click())
            onView(tagged("gallery")).perform(scrollTo(), click())
            onView(tagged("drawing_${saved.name}")).perform(scrollTo(), click())
            onView(tagged("saved_drawing")).check(matches(isDisplayed()))
            onView(tagged("home")).perform(click())
            onView(tagged("learn")).perform(scrollTo(), click())
            onView(tagged("colors")).perform(scrollTo(), click())
            answer(scenario)
            awaitTag(scenario, "celebration")
            Thread.sleep(1100)
            capture("happy-star-fireworks")
            scenario.recreate()
            awaitTag(scenario, "celebration")
            onView(withText("¡Muy bien, Noah!")).check(matches(isDisplayed()))
            awaitTag(scenario, "screen_learning_shapes")
            val store = LocalStore(context)
            assertEquals(colorsBefore + 1, store.successes("colors"))
            store.close()
            onView(tagged("home")).perform(click())
            onView(tagged("family")).perform(click())
            onView(withText("Para adultos")).check(matches(isDisplayed()))
            onView(withText("Volver")).perform(click())
            onView(tagged("screen_home")).check(matches(isDisplayed()))
        }
    }

    @Test
    fun colorRegionsAndUndoSurviveSerialization() {
        val original =
            Painting(
                "house",
                mutableListOf(
                    PaintMark("fill", 0xff3388cc.toInt(), region = 1),
                    PaintMark("stroke", points = mutableListOf(12f to 23f, 30f to 45f)),
                ),
            )
        val restored = Painting.fromJson(original.toJson())
        assertEquals("house", restored.template)
        assertEquals(2, restored.marks.size)
        assertEquals(0xff3388cc.toInt(), restored.marks[0].color)
        instrumentation.runOnMainSync {
            val canvas = PaintCanvasView(context, restored)
            canvas.undo()
            assertEquals(1, canvas.painting.marks.size)
            val bitmap = canvas.bitmap()
            assertEquals(0xff3388cc.toInt(), bitmap.getPixel(400, 400))
            bitmap.recycle()
        }
    }

    @Test
    fun guidedCircuitAdvancesAcrossAllSixGames() {
        configure()
        val before = LocalStore(context).let { it.stars.also { _ -> it.close() } }
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            onView(tagged("learn")).perform(scrollTo(), click())
            onView(tagged("colors")).perform(scrollTo(), click())
            val kinds = listOf("colors", "shapes", "numbers", "animals")
            for ((index, kind) in kinds.withIndex()) {
                awaitTag(scenario, "screen_learning_$kind")
                answer(scenario)
                awaitTag(scenario, "celebration")
                awaitTag(
                    scenario,
                    if (index < 3) "screen_learning_${kinds[index+1]}" else "screen_memory",
                )
            }
            val images = mutableMapOf<Int, String>()
            fun revealed(index: Int) {
                scenario.onActivity { activity ->
                    val tile = activity.window.decorView.findViewWithTag<ViewGroup>("memory_$index")
                    if (tile.isEnabled) tile.performClick()
                    images[index] = (tile.getChildAt(0) as ArtView).art
                }
            }
            for (i in listOf(0, 2)) {
                revealed(i)
                revealed(i + 1)
                Thread.sleep(1400)
            }
            for (pair in images.entries.groupBy { it.value }.values) {
                assertEquals(2, pair.size)
                scenario.onActivity { activity ->
                    val decor = activity.window.decorView
                    if (decor.findViewWithTag<View>("celebration") == null) {
                        for (entry in pair) decor.findViewWithTag<View>("memory_${entry.key}").let {
                            if (it.isEnabled) it.performClick()
                        }
                    }
                }
            }
            awaitTag(scenario, "celebration")
            awaitTag(scenario, "screen_puzzle")
            scenario.onActivity { activity ->
                val decor = activity.window.decorView
                for (piece in 0..1) {
                    decor.findViewWithTag<View>("puzzle_piece_$piece").performClick()
                    decor.findViewWithTag<View>("puzzle_slot_$piece").performClick()
                }
                assertNull(decor.findViewWithTag<View>("next"))
            }
            awaitTag(scenario, "celebration")
            awaitTag(scenario, "screen_learning_colors")
            val store = LocalStore(context)
            assertEquals(before + 6, store.stars)
            store.close()
        }
    }

    @Test
    fun homeCancelsPendingAutomaticAdvance() {
        configure()
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            onView(tagged("learn")).perform(scrollTo(), click())
            onView(tagged("colors")).perform(scrollTo(), click())
            answer(scenario)
            awaitTag(scenario, "celebration")
            scenario.onActivity {
                it.window.decorView.findViewWithTag<View>("celebration_home").performClick()
            }
            Thread.sleep(CelebrationView.DURATION + 250)
            awaitTag(scenario, "screen_home")
            scenario.onActivity {
                assertNull(it.window.decorView.findViewWithTag<View>("celebration"))
            }
        }
    }

    @Test
    fun automaticAdvanceWaitsForTheFullNarration() {
        configure(voice = true)
        fun duration(key: String): Long {
            val metadata = MediaMetadataRetriever()
            return try {
                context.assets.openFd("audio/$key.wav").use {
                    metadata.setDataSource(it.fileDescriptor, it.startOffset, it.length)
                }
                metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)!!.toLong()
            } finally {
                metadata.release()
            }
        }
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            onView(tagged("learn")).perform(scrollTo(), click())
            onView(tagged("colors")).perform(scrollTo(), click())
            val began = SystemClock.elapsedRealtime()
            val selected = answer(scenario)
            awaitTag(scenario, "celebration")
            awaitTag(scenario, "screen_learning_shapes", 20000)
            val elapsed = SystemClock.elapsedRealtime() - began
            assertTrue(
                "Do not cut off the answer or reward voice",
                elapsed >= duration(selected) + duration("praise_3") - 250,
            )
            assertTrue(
                "Show the celebration for at least three seconds",
                elapsed >= CelebrationView.DURATION - 150,
            )
        }
    }
}
