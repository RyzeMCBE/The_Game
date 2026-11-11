package com.ryzemc.pequenomundo

import android.content.Context
import android.graphics.*
import android.view.View
import kotlin.math.min
import org.json.JSONObject

data class VectorLayer(
    val path: Path,
    val color: Int,
    val region: Boolean,
    val width: Float,
    val ink: Boolean,
)

class ColoringSheet(
    val id: String,
    val name: String,
    val category: String,
    val categoryName: String,
    val layers: List<VectorLayer>,
) {
    val regions: List<Path> = layers.filter { it.region }.map { it.path }
    /** Remove foreground parts so a body stroke cannot color its belly, eyes or feet. */
    val masks: List<Path> by lazy {
        regions.mapIndexed { index, path ->
            Path(path).apply {
                for (upper in regions.drop(index + 1)) op(upper, Path.Op.DIFFERENCE)
            }
        }
    }
    val outlineClips: List<Path> by lazy {
        regions.indices.map { index ->
            Path().apply {
                addRect(0f, 0f, 600f, 450f, Path.Direction.CW)
                for (upper in regions.drop(index + 1)) op(upper, Path.Op.DIFFERENCE)
            }
        }
    }
    /**
     * Connected white areas include divisions made by internal outline strokes. Computing only a
     * selected page keeps album browsing light. The outside white component is excluded, and
     * eyes/contours are never part of a paintable area.
     */
    val hitRegions: List<Region> by lazy { segmentAreas() }
    val paintAreas: List<Path> by lazy { hitRegions.map { it.boundaryPath } }

    private fun segmentAreas(): List<Region> {
        val width = 600
        val height = 450
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        Canvas(bitmap).let { canvas ->
            canvas.drawColor(Color.WHITE)
            draw(canvas, RectF(0f, 0f, 600f, 450f))
        }
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        bitmap.recycle()
        val labels =
            IntArray(pixels.size) { index ->
                val c = pixels[index]
                if (Color.red(c) > 246 && Color.green(c) > 246 && Color.blue(c) > 246) -1 else -2
            }
        val queue = IntArray(pixels.size)
        val result = mutableListOf<Region>()
        var component = 0
        for (seed in labels.indices) {
            if (labels[seed] != -1) continue
            var start = 0
            var end = 1
            queue[0] = seed
            labels[seed] = component
            var left = width
            var top = height
            var right = 0
            var bottom = 0
            var outside = false
            fun add(index: Int) {
                if (labels[index] == -1) {
                    labels[index] = component
                    queue[end++] = index
                }
            }
            while (start < end) {
                val index = queue[start++]
                val x = index % width
                val y = index / width
                left = minOf(left, x)
                right = maxOf(right, x)
                top = minOf(top, y)
                bottom = maxOf(bottom, y)
                if (x == 0 || y == 0 || x == width - 1 || y == height - 1) outside = true
                if (x > 0) add(index - 1)
                if (x < width - 1) add(index + 1)
                if (y > 0) add(index - width)
                if (y < height - 1) add(index + width)
            }
            if (!outside && end >= 16) {
                val region = Region()
                for (y in top..bottom) {
                    var x = left
                    while (x <= right) {
                        while (x <= right && labels[y * width + x] != component) x++
                        val first = x
                        while (x <= right && labels[y * width + x] == component) x++
                        if (first < x) region.op(first, y, x, y + 1, Region.Op.UNION)
                    }
                }
                result.add(region)
            }
            component++
        }
        return result
    }

    fun hit(x: Float, y: Float): Int = hitRegions.indexOfLast { it.contains(x.toInt(), y.toInt()) }

    /** Blend opaque colors without depending on AndroidX or a network asset. */
    private fun mix(a: Int, b: Int, weight: Float): Int {
        val t = weight.coerceIn(0f, 1f)
        fun part(x: Int, y: Int) = (x * (1 - t) + y * t).toInt().coerceIn(0, 255)
        return Color.rgb(
            part(Color.red(a), Color.red(b)),
            part(Color.green(a), Color.green(b)),
            part(Color.blue(a), Color.blue(b)),
        )
    }

    /**
     * A different pastel world for every subject category. This is shown in the illustrated
     * album, never in the blank paint template or hit-test bitmap.
     */
    private fun drawBackdrop(c: Canvas, p: Paint) {
        val base = when (category) {
            "animals" -> 0xffd9f5e9.toInt()
            "dinosaurs" -> 0xfff1e8ce.toInt()
            "vehicles" -> 0xffd8edff.toInt()
            "nature" -> 0xffe5f5db.toInt()
            "food" -> 0xffffe9de.toInt()
            else -> 0xffeee5ff.toInt()
        }
        val border = RectF(13f, 13f, 587f, 437f)
        p.style = Paint.Style.FILL
        p.color = 0x19909aaf
        c.drawRoundRect(RectF(17f, 19f, 590f, 442f), 38f, 38f, p)
        p.shader = LinearGradient(
            13f, 13f, 585f, 435f,
            intArrayOf(mix(base, Color.WHITE, .58f), base, mix(base, Palette.paper, .45f)),
            floatArrayOf(0f, .48f, 1f),
            Shader.TileMode.CLAMP,
        )
        c.drawRoundRect(border, 38f, 38f, p)
        p.shader = null

        // Low-contrast, original scenery: clouds, ground and lively tiny details.
        val scenery = id.fold(0L) { value, letter -> (value * 31L + letter.code) and 0x7fffffffL }
        fun circle(x: Float, y: Float, radius: Float, tint: Int) {
            p.color = tint
            c.drawCircle(x, y, radius, p)
        }
        fun oval(left: Float, top: Float, right: Float, bottom: Float, tint: Int) {
            p.color = tint
            c.drawOval(left, top, right, bottom, p)
        }
        c.save()
        c.clipPath(Path().apply { addRoundRect(border, 38f, 38f, Path.Direction.CW) })
        when (category) {
            "animals", "nature", "dinosaurs" -> {
                oval(-10f, 371f, 385f, 493f, mix(base, 0xff76ba8b.toInt(), .30f))
                oval(254f, 395f, 650f, 479f, mix(base, 0xff65b6a1.toInt(), .26f))
                oval(72f, 65f, 191f, 91f, 0xaaffffff.toInt())
                circle(112f, 62f, 29f, 0xaaffffff.toInt())
                circle(151f, 58f, 34f, 0xaaffffff.toInt())
                oval(453f, 88f, 568f, 107f, 0x99ffffff.toInt())
            }
            "vehicles" -> {
                oval(-20f, 357f, 620f, 505f, 0xffd2d7e2.toInt())
                p.color = 0xaaffffff.toInt()
                for (x in 35..570 step 142) c.drawRoundRect(
                    x.toFloat(), 412f, x + 66f, 419f, 4f, 4f, p,
                )
                circle(485f, 75f, 40f, 0x70ffe6b4)
            }
            "food" -> {
                p.color = 0xfffff9ed.toInt()
                c.drawRoundRect(26f, 334f, 574f, 424f, 28f, 28f, p)
                p.color = 0x55e7b79d
                c.drawRoundRect(40f, 349f, 560f, 361f, 5f, 5f, p)
                circle(486f, 84f, 34f, 0x88ffe2a6.toInt())
            }
            else -> {
                oval(-14f, 384f, 606f, 485f, 0x55c6b0e9)
                for (x in 30..550 step 105) circle(x.toFloat(), 78f, 12f, 0x55ffffff)
            }
        }
        for (i in 0 until 11) {
            val x = 35f + ((scenery + i * 137L) % 522L).toFloat()
            val y = 31f + (((scenery / 7L) + i * 83L) % 355L).toFloat()
            val r = (2 + ((scenery + i) % 3L)).toFloat()
            p.color = if (i % 2 == 0) 0x66ffe3a7 else 0x66ffffff
            p.style = Paint.Style.STROKE
            p.strokeWidth = 2.4f
            c.drawLine(x - r, y, x + r, y, p)
            c.drawLine(x, y - r, x, y + r, p)
            p.style = Paint.Style.FILL
        }
        c.restore()
    }

    fun draw(canvas: Canvas, bounds: RectF, colored: Boolean = false, shadow: Boolean = false) {
        val scale = min(bounds.width() / 600f, bounds.height() / 450f)
        canvas.save()
        canvas.translate(bounds.centerX() - 300f * scale, bounds.centerY() - 225f * scale)
        canvas.scale(scale, scale)
        val brush = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        if (colored && !shadow) drawBackdrop(canvas, brush)
        for (layer in layers) {
            brush.shader = null
            brush.color =
                if (shadow) Palette.ink
                else if (layer.region && !colored) Color.WHITE else layer.color
            brush.style = if (layer.region || layer.ink) Paint.Style.FILL else Paint.Style.STROKE
            brush.strokeWidth = layer.width
            if (colored && !shadow && layer.region) {
                val area = RectF()
                layer.path.computeBounds(area, true)
                if (area.width() > 1f && area.height() > 1f) {
                    brush.shader = LinearGradient(
                        area.left, area.top, area.right, area.bottom,
                        mix(layer.color, Color.WHITE, .36f),
                        mix(layer.color, Palette.ink, .13f),
                        Shader.TileMode.CLAMP,
                    )
                }
                canvas.drawPath(layer.path, brush)
                brush.shader = null
                if (area.width() > 24f && area.height() > 20f) {
                    canvas.save()
                    canvas.clipPath(layer.path)
                    brush.color = 0x4fffffff
                    canvas.drawOval(
                        area.left + area.width() * .10f,
                        area.top + area.height() * .07f,
                        area.left + area.width() * .44f,
                        area.top + area.height() * .22f,
                        brush,
                    )
                    canvas.restore()
                }
            } else {
                canvas.drawPath(layer.path, brush)
            }
            if (layer.region) {
                brush.shader = null
                brush.color = Palette.ink
                brush.style = Paint.Style.STROKE
                brush.strokeWidth = layer.width
                canvas.drawPath(layer.path, brush)
            }
        }
        brush.shader = null
        canvas.restore()
    }
}

/** A deliberately small, validated path vocabulary, rendered by Android Canvas. */
object DrawingCatalog {
    var pages: List<ColoringSheet> = emptyList()
        private set

    private var byId: Map<String, ColoringSheet> = emptyMap()
    val categories
        get() = pages.map { it.category to it.categoryName }.distinct()

    @Synchronized
    fun load(context: Context) {
        if (pages.isNotEmpty()) return
        val data =
            JSONObject(
                context.assets.open("drawings/catalog.json").bufferedReader().use { it.readText() }
            )
        val array = data.getJSONArray("pages")
        pages =
            (0 until array.length()).map { index ->
                val page = array.getJSONObject(index)
                val layers = page.getJSONArray("layers")
                ColoringSheet(
                    page.getString("id"),
                    page.getString("name"),
                    page.getString("category"),
                    page.getString("categoryName"),
                    (0 until layers.length()).map { n ->
                        val layer = layers.getJSONObject(n)
                        VectorLayer(
                            parsePath(layer.getString("d")),
                            Color.parseColor(layer.getString("color")),
                            layer.getBoolean("region"),
                            layer.getDouble("width").toFloat(),
                            layer.getBoolean("ink"),
                        )
                    },
                )
            }
        check(pages.size >= 200 && pages.map { it.id }.distinct().size == pages.size)
        byId = pages.associateBy { it.id }
    }

    fun find(id: String): ColoringSheet? = byId[id] ?: byId["page_$id"]

    private fun parsePath(data: String): Path {
        val parts = Regex("[MLQCZ]|-?(?:\\d*\\.)?\\d+").findAll(data).map { it.value }.toList()
        var i = 0
        fun number() = parts[i++].toFloat().also { require(it.isFinite()) }
        return Path().apply {
            while (i < parts.size) when (parts[i++]) {
                "M" -> moveTo(number(), number())
                "L" -> lineTo(number(), number())
                "Q" -> quadTo(number(), number(), number(), number())
                "C" -> cubicTo(number(), number(), number(), number(), number(), number())
                "Z" -> close()
                else -> error("Unsupported bundled path")
            }
        }
    }
}

class SheetView(context: Context, val sheet: ColoringSheet, private val colored: Boolean = false) :
    View(context) {
    init {
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    override fun onDraw(canvas: Canvas) {
        sheet.draw(canvas, RectF(0f, 0f, width.toFloat(), height.toFloat()), colored)
    }
}
