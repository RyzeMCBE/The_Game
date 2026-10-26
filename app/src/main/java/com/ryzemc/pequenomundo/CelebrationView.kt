package com.ryzemc.pequenomundo

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.os.Build
import android.os.SystemClock
import android.view.View
import kotlin.math.*

/** A bounded three-second celebration, drawn locally without textures or downloads. */
class CelebrationView(context: Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var startedAt = 0L
    private var running = false
    private var progress = 0f
    private var finished: (() -> Unit)? = null
    private val colors =
        listOf("yellow", "blue", "purple", "green", "red").map { Palette.colors.getValue(it) }
    private val motionEnabled
        get() = Build.VERSION.SDK_INT < 26 || ValueAnimator.areAnimatorsEnabled()

    private val frame =
        object : Runnable {
            override fun run() {
                if (!running) return
                progress =
                    ((SystemClock.uptimeMillis() - startedAt) / DURATION.toFloat()).coerceIn(0f, 1f)
                invalidate()
                if (progress < 1f && motionEnabled) postOnAnimation(this)
            }
        }
    private val deadline = Runnable {
        if (running) {
            running = false
            removeCallbacks(frame)
            finished?.also { finished = null }?.invoke()
        }
    }

    init {
        tag = "celebration_art"
        contentDescription =
            "Una estrella feliz salta y saluda a Noah, con fuegos artificiales de colores."
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
    }

    fun start(onFinished: () -> Unit) {
        cancel()
        finished = onFinished
        startedAt = SystemClock.uptimeMillis()
        running = true
        progress = if (motionEnabled) 0f else .6f
        invalidate()
        if (motionEnabled) postOnAnimation(frame)
        // Keep the pause even when system animations are disabled.
        postDelayed(deadline, DURATION)
    }

    fun cancel() {
        running = false
        finished = null
        removeCallbacks(frame)
        removeCallbacks(deadline)
    }

    override fun onDetachedFromWindow() {
        cancel()
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0) return
        val t = progress
        fireworks(canvas, w * .16f, h * .30f, min(w, h) * .19f, t, .10f, 0)
        fireworks(canvas, w * .84f, h * .29f, min(w, h) * .20f, t, .29f, 2)
        fireworks(canvas, w * .50f, h * .20f, min(w, h) * .18f, t, .47f, 4)

        val entrance = (t / .21f).coerceIn(0f, 1f)
        val scale =
            if (entrance < 1f) 1f - (1f - entrance).pow(3) * cos(entrance * PI.toFloat() * 2)
            else 1f
        val side = min(w * .67f, h * .43f)
        val bob = if (motionEnabled) sin(t * PI.toFloat() * 4) * side * .027f else 0f
        val cx = w / 2
        val cy = h * (if (w > h) .57f else .54f) + bob
        paint.shader = null
        paint.style = Paint.Style.FILL
        paint.color = Color.argb(20, 124, 103, 55)
        canvas.drawOval(
            cx - side * .32f,
            cy + side * .50f,
            cx + side * .32f,
            cy + side * .56f,
            paint,
        )
        canvas.save()
        canvas.translate(cx, cy)
        canvas.rotate(if (motionEnabled) sin(t * PI.toFloat() * 5) * 5 else 0f)
        canvas.scale(scale, scale)
        canvas.scale(side / 100, side / 100)
        canvas.translate(-50f, -50f)
        star(canvas, t)
        canvas.restore()

        for (i in 0 until 25) {
            val phase = ((t - .18f) / .82f).coerceIn(0f, 1f)
            if (phase <= 0) break
            val x = (i * .618034f % 1f) * w
            val y = h * .22f + (i * .1732f % .5f) * h + phase * h * .23f
            val opacity = (sin(phase * PI) * 170).toInt().coerceIn(0, 170)
            paint.color = colors[i % colors.size]
            paint.alpha = opacity
            canvas.save()
            canvas.translate(x, y)
            canvas.rotate(i * 29f + phase * 80)
            val size = 3f * resources.displayMetrics.density
            canvas.drawRoundRect(-size, -size / 2, size, size / 2, 2f, 2f, paint)
            canvas.restore()
        }
        paint.alpha = 255
    }

    private fun fireworks(
        c: Canvas,
        x: Float,
        y: Float,
        radius: Float,
        t: Float,
        start: Float,
        offset: Int,
    ) {
        val p = ((t - start) / .42f).coerceIn(0f, 1f)
        if (p <= 0 || p >= 1) return
        val expansion = 1f - (1f - p).pow(2)
        paint.shader = null
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2.3f * resources.displayMetrics.density
        paint.strokeCap = Paint.Cap.ROUND
        for (i in 0 until 12) {
            val a = i * PI / 6 + offset * .23
            val inner = radius * (expansion - .17f).coerceAtLeast(0f)
            val outer = radius * expansion
            paint.color = colors[(i + offset) % colors.size]
            paint.alpha = ((1f - p) * 230).toInt()
            c.drawLine(
                x + cos(a).toFloat() * inner,
                y + sin(a).toFloat() * inner,
                x + cos(a).toFloat() * outer,
                y + sin(a).toFloat() * outer,
                paint,
            )
            paint.style = Paint.Style.FILL
            c.drawCircle(
                x + cos(a).toFloat() * outer,
                y + sin(a).toFloat() * outer,
                paint.strokeWidth * .65f,
                paint,
            )
            paint.style = Paint.Style.STROKE
        }
        paint.alpha = 255
        paint.style = Paint.Style.FILL
    }

    private fun star(c: Canvas, t: Float) {
        paint.shader = null
        paint.alpha = 255
        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeWidth = 5f
        paint.color = Color.rgb(244, 178, 49)
        val wave = if (motionEnabled) sin(t * PI.toFloat() * 9) * 4 else 0f
        c.drawLine(19f, 54f, 6f, 42f + wave, paint)
        c.drawLine(81f, 54f, 94f, 39f - wave, paint)
        val path = Path()
        for (i in 0 until 10) {
            val angle = -PI / 2 + i * PI / 5
            val r = if (i % 2 == 0) 43f else 23f
            val x = 50 + cos(angle).toFloat() * r
            val y = 47 + sin(angle).toFloat() * r
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        paint.style = Paint.Style.FILL
        paint.shader =
            LinearGradient(
                30f,
                8f,
                65f,
                90f,
                Color.rgb(255, 227, 119),
                Color.rgb(255, 189, 52),
                Shader.TileMode.CLAMP,
            )
        c.drawPath(path, paint)
        paint.shader = null
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.4f
        paint.strokeJoin = Paint.Join.ROUND
        paint.color = Color.rgb(231, 163, 39)
        c.drawPath(path, paint)
        paint.style = Paint.Style.FILL
        paint.color = Palette.ink
        val wink = motionEnabled && t > .54f && t < .61f
        c.drawOval(36f, 39f, 43f, 49f, paint)
        if (wink) {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 2.4f
            c.drawArc(57f, 42f, 65f, 48f, 180f, 180f, false, paint)
            paint.style = Paint.Style.FILL
        } else c.drawOval(57f, 39f, 64f, 49f, paint)
        paint.color = Color.WHITE
        c.drawCircle(39f, 42f, 1.6f, paint)
        if (!wink) c.drawCircle(60f, 42f, 1.6f, paint)
        paint.color = Color.rgb(242, 147, 113)
        c.drawOval(28f, 49f, 39f, 55f, paint)
        c.drawOval(61f, 49f, 72f, 55f, paint)
        paint.color = Palette.ink
        c.drawArc(RectF(40f, 48f, 60f, 64f), 0f, 180f, true, paint)
        paint.color = Color.rgb(246, 142, 147)
        c.drawOval(45f, 57f, 55f, 61f, paint)
        paint.color = Color.argb(130, 255, 255, 255)
        c.drawOval(45f, 16f, 50f, 28f, paint)
    }

    companion object {
        const val DURATION = 3000L
    }
}
