package com.ryzemc.pequenomundo

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.view.MotionEvent
import android.view.View
import kotlin.math.min

/** Quiet scenery leaves the activity itself as the strongest visual element. */
class StorybookBackground : Drawable() {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun draw(canvas: Canvas) {
        val w = bounds.width().toFloat()
        val h = bounds.height().toFloat()
        paint.shader =
            LinearGradient(
                0f,
                0f,
                0f,
                h,
                intArrayOf(0xffeaf8ff.toInt(), Palette.background, 0xffedf5df.toInt()),
                floatArrayOf(0f, .42f, 1f),
                Shader.TileMode.CLAMP,
            )
        canvas.drawRect(bounds, paint)
        paint.shader = null
        paint.color = 0x99ffffff.toInt()
        for ((x, y) in listOf(.06f to .15f, .94f to .28f)) {
            canvas.drawOval(w * x - w * .15f, h * y, w * x + w * .15f, h * y + w * .11f, paint)
            canvas.drawCircle(w * x, h * y, w * .08f, paint)
        }
        paint.color = 0x228acb9b
        canvas.drawOval(-w * .3f, h - w * .16f, w * .8f, h + w * .3f, paint)
        canvas.drawOval(w * .4f, h - w * .1f, w * 1.3f, h + w * .2f, paint)
    }

    override fun setAlpha(alpha: Int) = Unit

    override fun setColorFilter(filter: ColorFilter?) = Unit

    @Deprecated("Opacity is not used") override fun getOpacity() = PixelFormat.OPAQUE
}

class MiloHeroView(context: Context) : View(context) {
    private val brush = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        brush.color = Palette.sky
        canvas.drawRoundRect(0f, 0f, w, h, 32f, 32f, brush)
        brush.color = 0xffc8e4ae.toInt()
        canvas.drawOval(-w * .15f, h * .63f, w * .7f, h * 1.55f, brush)
        brush.color = 0xff9dce9e.toInt()
        canvas.drawOval(w * .3f, h * .72f, w * 1.2f, h * 1.6f, brush)
        brush.color = Color.WHITE
        canvas.drawOval(w * .06f, h * .15f, w * .25f, h * .33f, brush)
        canvas.drawCircle(w * .14f, h * .17f, h * .075f, brush)
        val s = min(h * .85f, w * .38f)
        brush.color = 0xff6d9bc1.toInt()
        canvas.drawRoundRect(
            w - s * .95f,
            h * .61f,
            w - s * .13f,
            h * 1.08f,
            s * .15f,
            s * .15f,
            brush,
        )
        Artwork.draw(canvas, "bear", RectF(w - s * 1.07f, h * .01f, w - s * .03f, h * .01f + s))
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
