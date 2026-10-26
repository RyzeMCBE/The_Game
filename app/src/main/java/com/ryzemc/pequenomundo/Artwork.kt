package com.ryzemc.pequenomundo

import android.content.Context
import android.graphics.*
import android.view.View
import kotlin.math.min

object Palette {
    val background = Color.rgb(255, 248, 236)
    val ink = Color.rgb(36, 61, 89)
    val muted = Color.rgb(83, 105, 98)
    val green = Color.rgb(52, 113, 92)
    val mint = Color.rgb(219, 238, 218)
    val peach = Color.rgb(255, 224, 194)
    val lilac = Color.rgb(231, 221, 249)
    val sky = Color.rgb(222, 240, 245)
    val colors =
        linkedMapOf(
            "red" to Color.rgb(230, 101, 91),
            "orange" to Color.rgb(247, 155, 65),
            "yellow" to Color.rgb(255, 207, 79),
            "green" to Color.rgb(105, 173, 121),
            "blue" to Color.rgb(52, 109, 187),
            "lightblue" to Color.rgb(118, 200, 234),
            "purple" to Color.rgb(156, 128, 206),
            "pink" to Color.rgb(241, 145, 183),
            "brown" to Color.rgb(159, 110, 73),
            "white" to Color.WHITE,
            "black" to Color.rgb(42, 43, 48),
            "gray" to Color.rgb(159, 166, 176),
        )
}

/**
 * Original, resolution-independent illustrations. No fonts, images or network downloads are needed.
 */
object Artwork {
    fun draw(canvas: Canvas, id: String, bounds: RectF) {
        if (id.startsWith("shadow_")) {
            DrawingCatalog.find(id.removePrefix("shadow_"))?.draw(canvas, bounds, shadow = true)
            return
        }
        if (
            id.startsWith("page_") ||
                id in
                    setOf(
                        "pig",
                        "sheep",
                        "goat",
                        "horse",
                        "hen",
                        "lion",
                        "elephant",
                        "monkey",
                        "frog",
                        "owl",
                        "giraffe",
                        "butterfly",
                        "turtle",
                        "rabbit",
                    )
        ) {
            DrawingCatalog.find(id)?.let {
                it.draw(canvas, bounds, colored = true)
                return
            }
        }
        val size = min(bounds.width(), bounds.height())
        canvas.save()
        canvas.translate(bounds.centerX() - size / 2, bounds.centerY() - size / 2)
        canvas.scale(size / 100f, size / 100f)
        drawUnit(canvas, id)
        canvas.restore()
    }

    private fun drawUnit(c: Canvas, id: String) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        fun color(hex: String) = Color.parseColor(hex)
        fun oval(x: Float, y: Float, w: Float, h: Float, fill: Int) {
            p.style = Paint.Style.FILL
            p.color = fill
            c.drawOval(x, y, x + w, y + h, p)
        }
        fun circle(x: Float, y: Float, r: Float, fill: Int) {
            p.style = Paint.Style.FILL
            p.color = fill
            c.drawCircle(x, y, r, p)
        }
        fun box(x: Float, y: Float, w: Float, h: Float, r: Float, fill: Int) {
            p.style = Paint.Style.FILL
            p.color = fill
            c.drawRoundRect(x, y, x + w, y + h, r, r, p)
        }
        fun line(x1: Float, y1: Float, x2: Float, y2: Float, fill: Int, width: Float = 2f) {
            p.color = fill
            p.style = Paint.Style.STROKE
            p.strokeWidth = width
            p.strokeCap = Paint.Cap.ROUND
            c.drawLine(x1, y1, x2, y2, p)
            p.style = Paint.Style.FILL
        }
        fun polygon(points: FloatArray, fill: Int) {
            val path = Path()
            path.moveTo(points[0], points[1])
            for (i in 2 until points.size step 2) path.lineTo(points[i], points[i + 1])
            path.close()
            p.style = Paint.Style.FILL
            p.color = fill
            c.drawPath(path, p)
        }
        val dark = color("#403A35")
        fun eyes(x1: Float = 37f, x2: Float = 63f, y: Float = 47f) {
            oval(x1 - 2.5f, y - 3.5f, 5f, 7f, dark)
            oval(x2 - 2.5f, y - 3.5f, 5f, 7f, dark)
            circle(x1 - .5f, y - 1.5f, .9f, Color.WHITE)
            circle(x2 - .5f, y - 1.5f, .9f, Color.WHITE)
        }
        fun smile(y: Float = 70f) {
            val path = Path()
            path.moveTo(42f, y)
            path.quadTo(50f, y + 7f, 58f, y)
            p.color = dark
            p.style = Paint.Style.STROKE
            p.strokeWidth = 2f
            p.strokeCap = Paint.Cap.ROUND
            c.drawPath(path, p)
            p.style = Paint.Style.FILL
        }
        when {
            id == "bear" || id == "sleep" -> {
                circle(24f, 25f, 15f, color("#B67E50"))
                circle(76f, 25f, 15f, color("#B67E50"))
                circle(24f, 25f, 8f, color("#E5B990"))
                circle(76f, 25f, 8f, color("#E5B990"))
                oval(15f, 21f, 70f, 70f, color("#D8A576"))
                oval(30f, 53f, 40f, 30f, color("#F7DEB7"))
                if (id == "sleep") {
                    line(32f, 48f, 42f, 48f, dark)
                    line(58f, 48f, 68f, 48f, dark)
                } else eyes()
                oval(44f, 58f, 12f, 8f, dark)
                line(50f, 65f, 50f, 71f, dark)
                smile(70f)
                oval(23f, 56f, 10f, 5f, color("#E9A28C"))
                oval(67f, 56f, 10f, 5f, color("#E9A28C"))
            }
            id == "cow" -> {
                polygon(floatArrayOf(28f, 30f, 24f, 11f, 38f, 25f), color("#D5AE75"))
                polygon(floatArrayOf(72f, 30f, 76f, 11f, 62f, 25f), color("#D5AE75"))
                oval(4f, 28f, 29f, 17f, color("#C3A68A"))
                oval(67f, 28f, 29f, 17f, color("#C3A68A"))
                oval(19f, 20f, 62f, 65f, color("#F5EFE2"))
                oval(20f, 22f, 25f, 24f, dark)
                eyes(36f, 64f, 47f)
                oval(23f, 57f, 54f, 30f, color("#EDB0A5"))
                circle(37f, 69f, 3f, dark)
                circle(63f, 69f, 3f, dark)
                smile(76f)
            }
            id == "cat" -> {
                polygon(floatArrayOf(19f, 45f, 19f, 10f, 43f, 31f), color("#E8A458"))
                polygon(floatArrayOf(81f, 45f, 81f, 10f, 57f, 31f), color("#E8A458"))
                oval(15f, 24f, 70f, 65f, color("#F1BB7B"))
                polygon(floatArrayOf(24f, 32f, 24f, 19f, 36f, 30f), color("#E89B96"))
                polygon(floatArrayOf(76f, 32f, 76f, 19f, 64f, 30f), color("#E89B96"))
                eyes(36f, 64f, 48f)
                polygon(floatArrayOf(44f, 61f, 56f, 61f, 50f, 67f), color("#C57675"))
                smile(70f)
                for (y in listOf(61f, 69f)) {
                    line(26f, y, 6f, y - 4, dark, 1.5f)
                    line(74f, y, 94f, y - 4, dark, 1.5f)
                }
            }
            id == "dog" -> {
                oval(17f, 23f, 66f, 65f, color("#E6BB88"))
                oval(5f, 23f, 25f, 49f, color("#A87552"))
                oval(70f, 23f, 25f, 49f, color("#A87552"))
                eyes(37f, 63f, 46f)
                oval(32f, 55f, 36f, 28f, color("#F8E3BF"))
                oval(43f, 56f, 14f, 10f, dark)
                oval(45f, 71f, 10f, 16f, color("#DD8C91"))
                smile(68f)
            }
            id == "bird" -> {
                oval(18f, 24f, 64f, 60f, color("#85BCDA"))
                oval(28f, 51f, 44f, 35f, color("#CEE9EC"))
                oval(8f, 47f, 22f, 20f, color("#4E9FC1"))
                oval(70f, 47f, 22f, 20f, color("#4E9FC1"))
                eyes(36f, 64f, 43f)
                polygon(
                    floatArrayOf(42f, 54f, 58f, 54f, 50f, 65f),
                    Palette.colors.getValue("yellow"),
                )
                line(40f, 84f, 36f, 91f, Palette.colors.getValue("orange"), 4f)
                line(60f, 84f, 64f, 91f, Palette.colors.getValue("orange"), 4f)
                line(47f, 25f, 43f, 14f, Palette.colors.getValue("blue"), 4f)
                line(53f, 25f, 59f, 16f, Palette.colors.getValue("blue"), 4f)
            }
            id == "duck" -> {
                oval(15f, 48f, 71f, 36f, color("#F4CF64"))
                circle(60f, 35f, 23f, color("#F5D976"))
                oval(41f, 59f, 29f, 18f, color("#E8B750"))
                circle(66f, 30f, 3f, dark)
                oval(74f, 39f, 22f, 9f, color("#E9974F"))
                polygon(floatArrayOf(17f, 60f, 5f, 45f, 8f, 70f), color("#F4CF64"))
                line(35f, 84f, 27f, 89f, color("#E9974F"), 4f)
                line(58f, 84f, 63f, 89f, color("#E9974F"), 4f)
            }
            id == "fish" -> {
                polygon(floatArrayOf(20f, 49f, 3f, 27f, 3f, 73f), Palette.colors.getValue("orange"))
                oval(15f, 24f, 75f, 52f, Palette.colors.getValue("blue"))
                polygon(floatArrayOf(47f, 27f, 38f, 11f, 68f, 25f), Palette.colors.getValue("blue"))
                circle(73f, 44f, 4f, dark)
                line(80f, 59f, 86f, 55f, dark)
                oval(43f, 43f, 20f, 21f, color("#A9D3E4"))
                circle(93f, 15f, 4f, Palette.sky)
            }
            id == "apple" -> {
                oval(18f, 29f, 40f, 55f, Palette.colors.getValue("red"))
                oval(42f, 29f, 40f, 55f, Palette.colors.getValue("red"))
                line(50f, 35f, 54f, 16f, color("#846542"), 5f)
                oval(54f, 15f, 23f, 12f, Palette.colors.getValue("green"))
                oval(29f, 39f, 8f, 20f, color("#F4B1A0"))
            }
            id == "flower" -> {
                line(50f, 47f, 50f, 91f, Palette.colors.getValue("green"), 5f)
                oval(25f, 63f, 25f, 13f, Palette.colors.getValue("green"))
                oval(50f, 74f, 25f, 12f, Palette.colors.getValue("green"))
                for (i in 0..5) {
                    val angle = i * Math.PI / 3
                    circle(
                        50f + 19f * kotlin.math.cos(angle).toFloat(),
                        37f + 19f * kotlin.math.sin(angle).toFloat(),
                        14f,
                        Palette.colors.getValue("red"),
                    )
                }
                circle(50f, 37f, 14f, Palette.colors.getValue("yellow"))
                circle(46f, 35f, 1.5f, dark)
                circle(54f, 35f, 1.5f, dark)
                smile(42f)
            }
            id == "house" -> {
                box(24f, 43f, 53f, 46f, 4f, Palette.colors.getValue("yellow"))
                polygon(floatArrayOf(13f, 45f, 50f, 10f, 87f, 45f), Palette.colors.getValue("red"))
                box(45f, 61f, 17f, 28f, 3f, Palette.colors.getValue("blue"))
                box(31f, 54f, 10f, 12f, 2f, Palette.sky)
                circle(57f, 76f, 1.5f, Palette.ink)
            }
            id == "paint" -> {
                oval(7f, 15f, 83f, 66f, color("#E5B57C"))
                oval(65f, 43f, 18f, 18f, Palette.background)
                circle(27f, 34f, 9f, Palette.colors.getValue("red"))
                circle(49f, 28f, 9f, Palette.colors.getValue("yellow"))
                circle(65f, 36f, 8f, Palette.colors.getValue("blue"))
                circle(26f, 59f, 8f, Palette.colors.getValue("green"))
                line(83f, 28f, 46f, 80f, Palette.colors.getValue("brown"), 7f)
                oval(35f, 74f, 16f, 20f, Palette.colors.getValue("purple"))
            }
            id == "play" -> {
                box(7f, 54f, 38f, 37f, 7f, Palette.colors.getValue("blue"))
                circle(69f, 71f, 20f, Palette.colors.getValue("red"))
                polygon(
                    floatArrayOf(47f, 5f, 21f, 45f, 73f, 45f),
                    Palette.colors.getValue("yellow"),
                )
            }
            id == "rainbow" -> {
                p.style = Paint.Style.STROKE
                p.strokeWidth = 12f
                p.strokeCap = Paint.Cap.ROUND
                listOf("red", "yellow", "green", "blue").forEachIndexed { index, k ->
                    p.color = Palette.colors.getValue(k)
                    val inset = 10f + index * 12
                    c.drawArc(
                        RectF(inset, 10f + index * 12, 100f - inset, 100f - index * 12),
                        180f,
                        180f,
                        false,
                        p,
                    )
                }
                p.style = Paint.Style.FILL
                oval(1f, 51f, 36f, 25f, Color.WHITE)
                oval(64f, 51f, 36f, 25f, Color.WHITE)
            }
            id == "star" -> {
                val points = FloatArray(20)
                for (i in 0 until 10) {
                    val a = -Math.PI / 2 + i * Math.PI / 5
                    val r = if (i % 2 == 0) 43f else 21f
                    points[i * 2] = 50f + r * kotlin.math.cos(a).toFloat()
                    points[i * 2 + 1] = 50f + r * kotlin.math.sin(a).toFloat()
                }
                polygon(points, Palette.colors.getValue("yellow"))
                eyes(40f, 60f, 49f)
                smile(62f)
            }
            id.startsWith("color_") -> {
                circle(50f, 50f, 34f, Palette.ink)
                circle(50f, 50f, 32f, Palette.colors[id.removePrefix("color_")] ?: Palette.green)
            }
            id == "circle" -> circle(50f, 50f, 32f, Palette.colors.getValue("red"))
            id == "square" -> box(18f, 18f, 64f, 64f, 8f, Palette.colors.getValue("blue"))
            id == "triangle" ->
                polygon(
                    floatArrayOf(50f, 12f, 90f, 83f, 10f, 83f),
                    Palette.colors.getValue("yellow"),
                )
            id.startsWith("number_") -> {
                p.color = Palette.ink
                p.typeface = Typeface.create("sans-serif-rounded", Typeface.BOLD)
                p.textSize = if (id == "number_10") 62f else 76f
                p.textAlign = Paint.Align.CENTER
                c.drawText(id.removePrefix("number_"), 50f, 78f, p)
            }
            id.startsWith("apples_") -> {
                val count = id.removePrefix("apples_").toIntOrNull()?.coerceIn(1, 10) ?: 1
                for (i in 0 until count) {
                    val columns = if (count > 5) 5 else min(count, 3)
                    val row = i / columns
                    val inRow = min(columns, count - row * columns)
                    val col = i % columns
                    val step = if (columns == 5) 19f else 31f
                    val radius = if (columns == 5) 11f else 18f
                    val x = 50f + (col - (inRow - 1) / 2f) * step
                    val y = if (count > columns) 27f + row * 43 else 48f
                    draw(c, "apple", RectF(x - radius, y - radius, x + radius, y + radius))
                }
            }
            else -> drawUnit(c, "bear")
        }
    }
}

class ArtView(context: Context, var art: String, label: String? = null) : View(context) {
    init {
        if (label != null) contentDescription = label
        importantForAccessibility =
            if (label == null) IMPORTANT_FOR_ACCESSIBILITY_NO else IMPORTANT_FOR_ACCESSIBILITY_YES
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        Artwork.draw(
            canvas,
            art,
            RectF(
                paddingLeft.toFloat(),
                paddingTop.toFloat(),
                (width - paddingRight).toFloat(),
                (height - paddingBottom).toFloat(),
            ),
        )
    }
}
