package com.ryzemc.pequenomundo

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.view.MotionEvent
import android.view.View
import kotlin.math.min

/** Quiet scenery leaves the activity itself as the strongest visual element. */
/** Soft illustrated paper backdrop shared by all native screens. */
class StorybookBackground : Drawable() {
    private val brush = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun draw(canvas: Canvas) {
        val w = bounds.width().toFloat()
        val h = bounds.height().toFloat()
        canvas.save()
        canvas.translate(bounds.left.toFloat(), bounds.top.toFloat())
        brush.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(0xffdff4ff.toInt(), Palette.background, 0xfffff1e0.toInt()),
            floatArrayOf(0f, .48f, 1f), Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, w, h, brush)
        brush.shader = null

        // Quiet clouds and tiny watercolor-like dots never compete with content.
        brush.color = 0x99ffffff.toInt()
        listOf(.13f to .13f, .89f to .3f).forEach { (x, y) ->
            val cx = w * x
            val cy = h * y
            canvas.drawOval(cx - w * .17f, cy, cx + w * .17f, cy + w * .075f, brush)
            canvas.drawCircle(cx - w * .065f, cy + w * .008f, w * .065f, brush)
            canvas.drawCircle(cx + w * .05f, cy - w * .005f, w * .075f, brush)
        }
        brush.color = 0x337bc9c1
        canvas.drawOval(-w * .3f, h - w * .19f, w * .7f, h + w * .19f, brush)
        brush.color = 0x22e8ad85
        canvas.drawOval(w * .42f, h - w * .09f, w * 1.28f, h + w * .22f, brush)
        brush.color = 0x88ffe1a4.toInt()
        for ((x, y, r) in listOf(
            Triple(.12f, .39f, 3f), Triple(.87f, .57f, 4f),
            Triple(.24f, .76f, 3f), Triple(.77f, .87f, 3f),
        )) canvas.drawCircle(w * x, h * y, r, brush)
        canvas.restore()
    }

    override fun setAlpha(alpha: Int) = Unit
    override fun setColorFilter(filter: ColorFilter?) = Unit
    @Deprecated("Opacity is not used") override fun getOpacity() = PixelFormat.OPAQUE
}

/** Original high-detail, resolution-independent Milo illustration for the welcome scene. */
class MiloHeroView(context: Context) : View(context) {
    private val brush = Paint(Paint.ANTI_ALIAS_FLAG)

    init {
        tag = "home_hero_art"
        contentDescription = "Milo, un osito artista, saluda a Noah en un jardín de colores"
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    private fun ellipse(c: Canvas, x: Float, y: Float, w: Float, h: Float, color: Int) {
        brush.style = Paint.Style.FILL
        brush.color = color
        c.drawOval(x, y, x + w, y + h, brush)
    }

    private fun circle(c: Canvas, x: Float, y: Float, r: Float, color: Int) {
        brush.style = Paint.Style.FILL
        brush.color = color
        c.drawCircle(x, y, r, brush)
    }

    private fun stroke(c: Canvas, x: Float, y: Float, tx: Float, ty: Float, color: Int, thick: Float) {
        brush.style = Paint.Style.STROKE
        brush.strokeCap = Paint.Cap.ROUND
        brush.strokeWidth = thick
        brush.color = color
        c.drawLine(x, y, tx, ty, brush)
        brush.style = Paint.Style.FILL
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width == 0 || height == 0) return
        canvas.save()
        canvas.scale(width / 600f, height / 250f)
        brush.shader = LinearGradient(
            0f, 0f, 560f, 250f,
            intArrayOf(0xffdcf4ff.toInt(), 0xffe2f8ef.toInt(), 0xffffecdb.toInt()),
            null, Shader.TileMode.CLAMP,
        )
        canvas.drawRoundRect(0f, 0f, 600f, 250f, 42f, 42f, brush)
        brush.shader = null
        circle(canvas, 513f, 61f, 42f, 0x88fff3bc.toInt())
        circle(canvas, 513f, 61f, 31f, 0xffffe59a.toInt())
        brush.color = Color.WHITE
        listOf(290f to 34f, 375f to 64f).forEach { (x, y) ->
            ellipse(canvas, x, y, 80f, 25f, 0xcaffffff.toInt())
            circle(canvas, x + 24f, y, 23f, 0xcaffffff.toInt())
            circle(canvas, x + 49f, y - 6f, 28f, 0xcaffffff.toInt())
        }
        ellipse(canvas, 130f, 211f, 430f, 130f, 0xffa6ddbb.toInt())
        ellipse(canvas, 315f, 221f, 300f, 110f, 0xff79c7a9.toInt())
        ellipse(canvas, 300f, 225f, 310f, 46f, 0xffd6eeb5.toInt())

        // Little paper stars and flowers around Milo.
        for ((x, y, r) in listOf(Triple(330f, 38f, 5f), Triple(557f, 104f, 4f),
            Triple(357f, 145f, 3f), Triple(548f, 184f, 4f))) {
            stroke(canvas, x-r, y, x+r, y, 0xffffe08a.toInt(), 3f)
            stroke(canvas, x, y-r, x, y+r, 0xffffe08a.toInt(), 3f)
        }

        // The bear is built from rounded, layered illustration shapes instead of icons.
        ellipse(canvas, 398f, 217f, 162f, 20f, 0x44655d72)
        ellipse(canvas, 408f, 149f, 142f, 111f, 0xff93694f.toInt())
        ellipse(canvas, 411f, 145f, 138f, 112f, 0xffce9a72.toInt())
        ellipse(canvas, 430f, 168f, 99f, 76f, 0xffffe4bd.toInt())
        ellipse(canvas, 395f, 183f, 49f, 45f, 0xffb88261.toInt())
        ellipse(canvas, 522f, 181f, 44f, 45f, 0xffb88261.toInt())
        ellipse(canvas, 399f, 192f, 42f, 33f, 0xffdcaa82.toInt())
        ellipse(canvas, 524f, 194f, 37f, 28f, 0xffdcaa82.toInt())

        // Ears with warm centers, tufts, and soft cheek shapes.
        circle(canvas, 421f, 85f, 35f, 0xffab795b.toInt())
        circle(canvas, 519f, 85f, 35f, 0xffab795b.toInt())
        circle(canvas, 423f, 88f, 24f, 0xffefb29f.toInt())
        circle(canvas, 519f, 88f, 24f, 0xffefb29f.toInt())
        ellipse(canvas, 395f, 65f, 148f, 124f, 0xffb9825e.toInt())
        ellipse(canvas, 398f, 62f, 146f, 126f, 0xffdcaa7f.toInt())
        ellipse(canvas, 425f, 127f, 96f, 59f, 0xffffe1bc.toInt())
        ellipse(canvas, 414f, 134f, 31f, 18f, 0xffec9f94.toInt())
        ellipse(canvas, 500f, 134f, 31f, 18f, 0xffec9f94.toInt())
        ellipse(canvas, 424f, 85f, 93f, 15f, 0x33fff8e9)
        circle(canvas, 441f, 125f, 6.6f, 0xff374051.toInt())
        circle(canvas, 496f, 125f, 6.6f, 0xff374051.toInt())
        circle(canvas, 443f, 123f, 2.2f, Color.WHITE)
        circle(canvas, 498f, 123f, 2.2f, Color.WHITE)
        ellipse(canvas, 459f, 143f, 20f, 13f, 0xff694e4e.toInt())
        brush.color = 0xff74585b.toInt()
        brush.style = Paint.Style.STROKE
        brush.strokeWidth = 3.5f
        brush.strokeCap = Paint.Cap.ROUND
        val smile = Path().apply { moveTo(460f, 158f); quadTo(469f, 171f, 481f, 158f) }
        canvas.drawPath(smile, brush)
        brush.style = Paint.Style.FILL
        ellipse(canvas, 450f, 175f, 53f, 12f, 0xff86c7c7.toInt())

        // Paintbrush in one paw: cheerful and recognizable at small sizes.
        stroke(canvas, 544f, 200f, 568f, 112f, 0xff895a4b.toInt(), 13f)
        stroke(canvas, 544f, 200f, 568f, 112f, 0xffe0b07a.toInt(), 7f)
        stroke(canvas, 567f, 120f, 573f, 98f, 0xffd6dbe1.toInt(), 12f)
        ellipse(canvas, 566f, 88f, 14f, 24f, 0xffec779b.toInt())
        ellipse(canvas, 569f, 92f, 6f, 10f, 0xffffc2d6.toInt())
        ellipse(canvas, 523f, 178f, 33f, 35f, 0xffdca682.toInt())
        ellipse(canvas, 527f, 186f, 13f, 13f, 0xffffd5ac.toInt())

        // Small flowers anchor the garden scene.
        for ((x, y) in listOf(355f to 217f, 577f to 220f)) {
            stroke(canvas, x, y+21f, x, y-1f, 0xff6cb889.toInt(), 4f)
            listOf(-9f to 0f, 9f to 0f, 0f to -9f, 0f to 9f).forEach { (dx,dy) ->
                circle(canvas, x+dx, y+dy, 9f, 0xffffd1d9.toInt())
            }
            circle(canvas, x, y, 7f, 0xffffd065.toInt())
        }
        canvas.restore()
    }
}

/** Illustrated activity medallion used by the new home cards. */
class HomeAdventureArtView(context: Context, private val kind: String) : View(context) {
    private val brush = Paint(Paint.ANTI_ALIAS_FLAG)

    init {
        tag = "home_art_" + kind
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width == 0 || height == 0) return
        val size = min(width, height).toFloat()
        canvas.save()
        canvas.translate((width - size) / 2f, (height - size) / 2f)
        canvas.scale(size / 100f, size / 100f)
        brush.color = 0x66ffffff
        canvas.drawCircle(50f, 51f, 47f, brush)
        brush.color = 0x55c7b4a8
        canvas.drawOval(16f, 83f, 84f, 96f, brush)
        brush.color = when(kind) {
            "paint" -> 0xfffff1bf.toInt()
            "play" -> 0xffdeeaff.toInt()
            else -> 0xfff3e3ff.toInt()
        }
        canvas.drawCircle(49f, 46f, 42f, brush)
        Artwork.draw(canvas, kind, RectF(11f, 8f, 90f, 88f))
        brush.color = 0xffffdf8c.toInt()
        canvas.drawCircle(89f, 16f, 3.3f, brush)
        canvas.drawCircle(8f, 75f, 2.8f, brush)
        canvas.restore()
    }
}

class FingerPathView(context: Context, val game: TracingGame, val onComplete: () -> Unit) :
    View(context) {
    private val brush =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
    private var pointer = -1
    var onProgress: (() -> Unit)? = null

    init {
        tag = "finger_path"
        isClickable = true
        contentDescription = "Sigue el camino con el dedo, desde el círculo hasta el patito"
    }

    override fun onMeasure(w: Int, h: Int) {
        val width = MeasureSpec.getSize(w)
        setMeasuredDimension(width, resolveSize((width * .75f).toInt(), h))
    }

    override fun onDraw(canvas: Canvas) {
        val scale = min(width / 600f, height / 450f)
        canvas.save()
        canvas.translate((width - 600 * scale) / 2, (height - 450 * scale) / 2)
        canvas.scale(scale, scale)
        val path =
            Path().apply {
                game.points.forEachIndexed { i, p ->
                    if (i == 0) moveTo(p.first, p.second) else lineTo(p.first, p.second)
                }
            }
        brush.style = Paint.Style.STROKE
        brush.color = Palette.sky
        brush.strokeWidth = 76f
        canvas.drawPath(path, brush)
        brush.color = 0xffb7d6da.toInt()
        brush.strokeWidth = 5f
        brush.pathEffect = DashPathEffect(floatArrayOf(8f, 15f), 0f)
        canvas.drawPath(path, brush)
        brush.pathEffect = null
        if (game.reached > 0) {
            val traced =
                Path().apply {
                    game.points.take(game.reached).forEachIndexed { i, p ->
                        if (i == 0) moveTo(p.first, p.second) else lineTo(p.first, p.second)
                    }
                }
            brush.color = Palette.colors.getValue("green")
            brush.strokeWidth = 19f
            canvas.drawPath(traced, brush)
        }
        brush.style = Paint.Style.FILL
        brush.color = Palette.colors.getValue("green")
        val start = game.points.first()
        canvas.drawCircle(start.first, start.second, 27f, brush)
        brush.color = Color.WHITE
        canvas.drawCircle(start.first, start.second, 10f, brush)
        val end = game.points.last()
        Artwork.draw(
            canvas,
            "duck",
            RectF(end.first - 42, end.second - 52, end.first + 42, end.second + 32),
        )
        if (game.reached in 1 until game.points.size) {
            val target = game.points[game.reached]
            brush.color = Palette.colors.getValue("yellow")
            canvas.drawCircle(target.first, target.second, 10f, brush)
        }
        canvas.restore()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        fun sample(x: Float, y: Float) {
            if (game.completed) return
            val scale = min(width / 600f, height / 450f)
            if (
                game.touch(
                    (x - (width - 600 * scale) / 2) / scale,
                    (y - (height - 450 * scale) / 2) / scale,
                )
            ) {
                invalidate()
                onProgress?.invoke()
                if (game.completed) onComplete()
            }
        }
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                pointer = event.getPointerId(0)
                parent?.requestDisallowInterceptTouchEvent(true)
                sample(event.x, event.y)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val i = event.findPointerIndex(pointer)
                if (i >= 0) {
                    for (n in 0 until event.historySize) sample(
                        event.getHistoricalX(i, n),
                        event.getHistoricalY(i, n),
                    )
                    sample(event.getX(i), event.getY(i))
                }
                return true
            }
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {
                pointer = -1
                parent?.requestDisallowInterceptTouchEvent(false)
                if (event.actionMasked == MotionEvent.ACTION_UP) performClick()
                return true
            }
        }
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }
}
