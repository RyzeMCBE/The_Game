package com.ryzemc.pequenomundo

import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.MediaMetadataRetriever
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.FrameLayout
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.min
import org.hamcrest.CoreMatchers.`is`
import org.json.JSONArray
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
        DrawingCatalog.load(context)
        val ids =
            DrawingCatalog.pages
                .map { it.id }
                .toMutableList()
                .apply {
                    remove("page_cat")
                    add(0, "page_cat")
                }
        context
            .getSharedPreferences("family", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("voice", voice)
            .putBoolean("music", false)
            .putInt("difficulty", 2)
            .putLong("elapsed", 0)
            .putInt("break", 20)
            .putInt("count_range", 0)
            .putInt("success_numbers", 0)
            .putString("drawing_remaining", JSONArray(ids).toString())
            .putString("drawing_recent", "[]")
            .commit()
    }

    private fun awaitTag(
        scenario: ActivityScenario<MainActivity>,
        tag: String,
        timeout: Long = 15000,
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

    private fun clickTag(scenario: ActivityScenario<MainActivity>, tag: String) {
        scenario.onActivity {
            val v = it.window.decorView.findViewWithTag<View>(tag)
            assertNotNull("Missing $tag", v)
            assertTrue("Disabled $tag", v.isEnabled)
            v.performClick()
        }
    }

    private fun awaitNoTag(scenario: ActivityScenario<MainActivity>, tag: String) {
        val deadline = SystemClock.uptimeMillis() + 15000
        while (SystemClock.uptimeMillis() < deadline) {
            val gone = AtomicBoolean(false)
            scenario.onActivity { gone.set(it.window.decorView.findViewWithTag<View>(tag) == null) }
            if (gone.get()) return
            Thread.sleep(70)
        }
        fail("View remained present: $tag")
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
            val fd = instrumentation.uiAutomation.executeShellCommand(command)
            android.os.ParcelFileDescriptor.AutoCloseInputStream(fd).use { it.readBytes() }
        }
        shell("mkdir -p /data/local/tmp/noah-qa")
        shell("screencap -p /data/local/tmp/noah-qa/$name.png")
    }

    /** Send real view touch events using the same letterbox transform as the child sees. */
    private fun stroke(view: View, points: List<Pair<Float, Float>>) {
        val scale = min(view.width / 600f, view.height / 450f)
        val dx = (view.width - 600 * scale) / 2
        val dy = (view.height - 450 * scale) / 2
        val began = SystemClock.uptimeMillis()
        fun event(action: Int, p: Pair<Float, Float>, step: Int) {
            val e =
                MotionEvent.obtain(
                    began,
                    began + step * 16,
                    action,
                    dx + p.first * scale,
                    dy + p.second * scale,
                    0,
                )
            view.dispatchTouchEvent(e)
            e.recycle()
        }
        event(MotionEvent.ACTION_DOWN, points.first(), 0)
        points.drop(1).forEachIndexed { i, p -> event(MotionEvent.ACTION_MOVE, p, i + 1) }
        event(MotionEvent.ACTION_UP, points.last(), points.size)
    }

    private fun completeCounting(scenario: ActivityScenario<MainActivity>) {
        scenario.onActivity { activity ->
            val decor = activity.window.decorView
            val game = decor.findViewWithTag<CountingPanel>("count_panel").game
            if (game.mode == CountingMode.MATCH)
                decor.findViewWithTag<View>("count_answer_${game.target}").performClick()
            else
                for (i in 0 until game.target) decor
                    .findViewWithTag<View>("count_item_$i")
                    .performClick()
        }
        awaitTag(scenario, "celebration")
    }

    @Test
    fun completeOfflineJourneyUsesSeparateAlbumAndCanvasAndPersistsArtwork() {
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
        val sounds = context.assets.list("audio")!!.toSet()
        for (id in GameContent.animals.map { it.id }) assertTrue(
            "Real recording for $id",
            sounds.contains("sound_$id.wav"),
        )
        assertTrue(sounds.contains("count_10.wav"))
        assertTrue(sounds.contains("total_10.wav"))
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
            capture("home-noah-v12")
            onView(tagged("paint")).perform(scrollTo(), click())
            awaitTag(scenario, "screen_album")
            scenario.onActivity {
                assertNull(it.window.decorView.findViewWithTag<View>("paint_canvas"))
            }
            capture("album-200")
            onView(tagged("sheet_page_cat")).perform(scrollTo(), click())
            awaitTag(scenario, "paint_canvas")
            scenario.onActivity { activity ->
                val decor = activity.window.decorView
                assertNull(decor.findViewWithTag<View>("screen_album"))
                assertNull(decor.findViewWithTag<View>("templates"))
                for (key in Palette.colors.keys) assertTrue(
                    decor.findViewWithTag<View>("paint_$key").isShown
                )
                val canvas = decor.findViewWithTag<PaintCanvasView>("paint_canvas")
                for (y in listOf(140f, 157f, 192f, 210f)) stroke(
                    canvas,
                    (220..360 step 12).map { it.toFloat() to y },
                )
            }
            scenario.recreate()
            scenario.onActivity {
                assertEquals(
                    4,
                    it.window.decorView
                        .findViewWithTag<PaintCanvasView>("paint_canvas")
                        .painting
                        .marks
                        .size,
                )
            }
            capture("painting-alone")
            onView(tagged("save")).perform(click())
            awaitTag(scenario, "celebration")
            scenario.recreate()
            awaitTag(scenario, "celebration")
            awaitNoTag(scenario, "celebration")
            awaitTag(scenario, "paint_canvas")
            scenario.onActivity {
                assertNotEquals(
                    "page_cat",
                    it.window.decorView
                        .findViewWithTag<PaintCanvasView>("paint_canvas")
                        .painting
                        .template,
                )
            }
            val saved =
                directory.listFiles()!!.filter { it.extension == "png" }.maxBy { it.lastModified() }
            assertEquals(before + 1, directory.listFiles()!!.count { it.extension == "png" })
            assertTrue(saved.length() > 1000)
            onView(tagged("album_back")).perform(click())
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
            capture("happy-star-fireworks-v12")
            scenario.recreate()
            awaitTag(scenario, "celebration")
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
    fun legacyColorRegionsAndUndoSurviveSerialization() {
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
    fun guidedCircuitAdvancesAcrossAllTenActivities() {
        configure()
        val before = LocalStore(context).let { it.stars.also { _ -> it.close() } }
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            onView(tagged("learn")).perform(scrollTo(), click())
            onView(tagged("colors")).perform(scrollTo(), click())
            answer(scenario)
            awaitTag(scenario, "screen_learning_shapes")
            answer(scenario)
            awaitTag(scenario, "screen_counting")
            completeCounting(scenario)
            awaitTag(scenario, "screen_learning_animals")
            answer(scenario)
            awaitTag(scenario, "screen_memory")
            val images = mutableMapOf<Int, String>()
            fun reveal(i: Int) {
                scenario.onActivity {
                    val tile = it.window.decorView.findViewWithTag<ViewGroup>("memory_$i")
                    if (tile.isEnabled) tile.performClick()
                    images[i] = (tile.getChildAt(0) as ArtView).art
                }
            }
            for (i in listOf(0, 2)) {
                reveal(i)
                reveal(i + 1)
                Thread.sleep(1400)
            }
            for (pair in images.entries.groupBy { it.value }.values) scenario.onActivity { activity
                ->
                val decor = activity.window.decorView
                if (decor.findViewWithTag<View>("celebration") == null)
                    for (entry in pair) decor.findViewWithTag<View>("memory_${entry.key}").let {
                        if (it.isEnabled) it.performClick()
                    }
            }
            awaitTag(scenario, "screen_puzzle")
            scenario.onActivity {
                val decor = it.window.decorView
                for (piece in 0..1) {
                    decor.findViewWithTag<View>("puzzle_piece_$piece").performClick()
                    decor.findViewWithTag<View>("puzzle_slot_$piece").performClick()
                }
            }
            for (activity in listOf("shadows", "sizes")) {
                awaitTag(scenario, "screen_$activity")
                scenario.onActivity {
                    val decor = it.window.decorView
                    val game = decor.findViewWithTag<DiscoveryPanel>("discovery_panel").game
                    decor.findViewWithTag<View>("discovery_${game.target}").performClick()
                }
            }
            awaitTag(scenario, "screen_sorting")
            scenario.onActivity {
                val decor = it.window.decorView
                val game = decor.findViewWithTag<SortingPanel>("sorting_panel").game
                for (i in game.items.indices) {
                    decor.findViewWithTag<View>("sort_item_$i").performClick()
                    decor.findViewWithTag<View>("sort_bin_${game.items[i]}").performClick()
                }
            }
            awaitTag(scenario, "screen_paths")
            capture("finger-paths")
            scenario.onActivity {
                val path = it.window.decorView.findViewWithTag<FingerPathView>("finger_path")
                stroke(path, path.game.points)
            }
            awaitTag(scenario, "screen_learning_colors")
            val store = LocalStore(context)
            assertEquals(before + 10, store.stars)
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
            clickTag(scenario, "celebration_home")
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
            val m = MediaMetadataRetriever()
            return try {
                context.assets.openFd("audio/$key.wav").use {
                    m.setDataSource(it.fileDescriptor, it.startOffset, it.length)
                }
                m.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)!!.toLong()
            } finally {
                m.release()
            }
        }
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            onView(tagged("learn")).perform(scrollTo(), click())
            onView(tagged("colors")).perform(scrollTo(), click())
            val began = SystemClock.elapsedRealtime()
            val selected = answer(scenario)
            awaitTag(scenario, "celebration")
            awaitTag(scenario, "screen_learning_shapes", 25000)
            val elapsed = SystemClock.elapsedRealtime() - began
            assertTrue(elapsed >= duration(selected) + duration("praise_3") - 250)
            assertTrue(elapsed >= CelebrationView.DURATION - 150)
        }
    }

    @Test
    fun everyOneOfThe200SheetsRendersAndHasPaintableAreas() {
        DrawingCatalog.load(context)
        assertEquals(200, DrawingCatalog.pages.size)
        assertEquals(200, DrawingCatalog.pages.map { it.id }.toSet().size)
        for (sheet in DrawingCatalog.pages) {
            assertTrue("Paintable ${sheet.id}", sheet.hitRegions.isNotEmpty())
            assertTrue(sheet.hitRegions.all { !it.isEmpty })
        }
        assertTrue(
            "Internal outline divisions must be separate zones",
            DrawingCatalog.find("page_pineapple")!!.paintAreas.size > 10,
        )
        assertEquals(12, GameContent.colors.size)
        assertEquals(15, GameContent.animals.size)
    }

    @Test
    fun manualBrushAndEraserStayInsideTheSelectedZoneAndKeepTheOutline() {
        DrawingCatalog.load(context)
        instrumentation.runOnMainSync {
            val canvas = PaintCanvasView(context, Painting("page_house"))
            FrameLayout(context).addView(canvas)
            canvas.layout(0, 0, 600, 450)
            canvas.paintColor = 0xff008855.toInt()
            canvas.brushSize = 30f
            stroke(canvas, (200..580 step 10).map { it.toFloat() to 305f })
            assertEquals(1, canvas.painting.marks.size)
            assertTrue(canvas.painting.marks[0].region >= 0)
            var bitmap = canvas.bitmap()
            assertEquals(0xff008855.toInt(), bitmap.getPixel(352, 488))
            assertEquals("Neighboring door remains white", Color.WHITE, bitmap.getPixel(480, 488))
            assertEquals(
                "Outside the sheet drawing remains white",
                Color.WHITE,
                bitmap.getPixel(912, 488),
            )
            bitmap.recycle()
            val restored = Painting.fromJson(canvas.painting.toJson())
            assertEquals(canvas.painting.marks[0].region, restored.marks[0].region)
            canvas.tool = "eraser"
            stroke(canvas, (200..580 step 10).map { it.toFloat() to 305f })
            bitmap = canvas.bitmap()
            assertEquals(Color.WHITE, bitmap.getPixel(352, 488))
            assertNotEquals(Color.WHITE, bitmap.getPixel(416, 488))
            bitmap.recycle()
            canvas.undo()
            bitmap = canvas.bitmap()
            assertEquals(0xff008855.toInt(), bitmap.getPixel(352, 488))
            bitmap.recycle()
        }
    }

    @Test
    fun drawingRotationSurvivesNewStoreInstancesAndTheNextCycleAvoidsRecentSheets() {
        configure()
        val seen = mutableListOf<String>()
        repeat(200) {
            val store = LocalStore(context)
            val next = store.nextDrawing()
            store.close()
            assertFalse(next in seen)
            seen.add(next)
        }
        assertEquals(200, seen.toSet().size)
        val store = LocalStore(context)
        assertFalse(store.nextDrawing() in seen.takeLast(20))
        store.close()
    }

    @Test
    fun allThreeCountingModesWorkAndTouchProgressSurvivesRecreation() {
        configure()
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            for (mode in CountingMode.entries) {
                clickTag(scenario, "learn")
                clickTag(scenario, "numbers")
                clickTag(scenario, "range_10")
                clickTag(scenario, "count_mode_${mode.name.lowercase()}")
                awaitTag(scenario, "screen_counting")
                if (mode == CountingMode.TOUCH) {
                    scenario.onActivity {
                        val panel =
                            it.window.decorView.findViewWithTag<CountingPanel>("count_panel")
                        if (panel.game.target > 1)
                            it.window.decorView.findViewWithTag<View>("count_item_0").performClick()
                    }
                    scenario.recreate()
                }
                if (mode == CountingMode.TOUCH) {
                    scenario.onActivity {
                        val decor = it.window.decorView
                        val game = decor.findViewWithTag<CountingPanel>("count_panel").game
                        for (i in 0 until game.target) if (i !in game.counted)
                            decor.findViewWithTag<View>("count_item_$i").performClick()
                    }
                    awaitTag(scenario, "celebration")
                } else completeCounting(scenario)
                clickTag(scenario, "celebration_home")
            }
            val store = LocalStore(context)
            assertEquals(3, store.successes("numbers"))
            store.close()
        }
    }

    @Test
    fun legacyPaintingsRemainAvailableAndLandscapeKeepsTheAlbumSeparate() {
        configure()
        val legacy =
            Painting("house", mutableListOf(PaintMark("fill", 0xff3388cc.toInt(), region = 1)))
        File(context.filesDir, "draft.json").writeText(legacy.toJson())
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            clickTag(scenario, "paint")
            clickTag(scenario, "continue_paint")
            awaitTag(scenario, "paint_canvas")
            scenario.onActivity {
                assertEquals(
                    "house",
                    it.window.decorView
                        .findViewWithTag<PaintCanvasView>("paint_canvas")
                        .painting
                        .template,
                )
                it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            }
            awaitTag(scenario, "paint_canvas")
            Thread.sleep(700)
            scenario.onActivity {
                val decor = it.window.decorView
                assertNull(decor.findViewWithTag<View>("screen_album"))
                assertEquals(
                    "house",
                    decor.findViewWithTag<PaintCanvasView>("paint_canvas").painting.template,
                )
                assertTrue(decor.findViewWithTag<View>("paint_canvas").width > 0)
            }
            capture("painting-landscape")
            onView(tagged("save")).check(matches(isCompletelyDisplayed()))
            onView(tagged("eraser")).check(matches(isCompletelyDisplayed()))
            onView(tagged("undo")).check(matches(isCompletelyDisplayed()))
            for (key in Palette.colors.keys) onView(tagged("paint_$key"))
                .check(matches(isCompletelyDisplayed()))
            onView(tagged("save")).perform(click())
            awaitTag(scenario, "celebration")
            awaitNoTag(scenario, "celebration")
            scenario.onActivity {
                assertNotEquals(
                    "house",
                    it.window.decorView
                        .findViewWithTag<PaintCanvasView>("paint_canvas")
                        .painting
                        .template,
                )
            }
            scenario.onActivity {
                it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
        }
    }
}
