package com.ryzemc.pequenomundo

import android.content.pm.PackageManager
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
import org.hamcrest.CoreMatchers.`is`
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OfflineAppTest {
    private fun tagged(tag: String) = withTagValue(`is`(tag as Any))

    @Test
    fun completeOfflineJourneyUsesNativeViewsAndPersistsArtwork() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
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
                listOf("welcome.wav", "ask_red.wav", "number_5.wav", "sound_cow.wav", "music.wav")
            )
        )
        for (file in audio) assertTrue(
            "Bundled audio is not empty: $file",
            context.assets.open("audio/$file").use { it.readBytes().size } > 100,
        )
        val directory = File(context.filesDir, "gallery")
        val before = directory.listFiles()?.count { it.extension == "png" } ?: 0

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                fun native(view: View): Boolean =
                    view !is WebView &&
                        (view !is ViewGroup ||
                            (0 until view.childCount).all { native(view.getChildAt(it)) })
                assertTrue(native(activity.window.decorView))
            }
            onView(tagged("paint")).perform(scrollTo(), click())
            onView(tagged("paint_canvas")).perform(scrollTo(), swipeRight())
            scenario.recreate()
            scenario.onActivity { activity ->
                assertTrue(
                    activity.window.decorView
                        .findViewWithTag<PaintCanvasView>("paint_canvas")
                        .painting
                        .marks
                        .isNotEmpty()
                )
            }
            onView(tagged("save")).perform(scrollTo(), click())
            val until = System.currentTimeMillis() + 8000
            while (
                (directory.listFiles()?.count { it.extension == "png" } ?: 0) <= before &&
                    System.currentTimeMillis() < until
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
            scenario.onActivity { activity ->
                val answers = activity.window.decorView.findViewWithTag<ViewGroup>("answers")
                for (i in 0 until answers.childCount) if (answers.getChildAt(i).isEnabled)
                    answers.getChildAt(i).performClick()
                assertEquals(
                    View.VISIBLE,
                    activity.window.decorView.findViewWithTag<View>("next").visibility,
                )
            }
            scenario.recreate()
            onView(tagged("next")).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
            onView(tagged("home")).perform(click())
            onView(tagged("family")).perform(click())
            onView(withText("Para adultos")).check(matches(isDisplayed()))
            onView(withText("Volver")).perform(click())
            onView(tagged("screen_home")).check(matches(isDisplayed()))
        }
    }

    @Test
    fun colorRegionsAndUndoSurviveSerialization() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
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
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val canvas = PaintCanvasView(context, restored)
            canvas.undo()
            assertEquals(1, canvas.painting.marks.size)
            val bitmap = canvas.bitmap()
            assertEquals(0xff3388cc.toInt(), bitmap.getPixel(400, 400))
            bitmap.recycle()
        }
    }
}
