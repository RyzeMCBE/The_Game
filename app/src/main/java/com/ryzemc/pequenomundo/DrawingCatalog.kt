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

    fun draw(canvas: Canvas, bounds: RectF, colored: Boolean = false, shadow: Boolean = false) {
        val scale = min(bounds.width() / 600f, bounds.height() / 450f)
        canvas.save()
        canvas.translate(bounds.centerX() - 300f * scale, bounds.centerY() - 225f * scale)
        canvas.scale(scale, scale)
        val brush =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
            }
        for (layer in layers) {
            brush.color =
                if (shadow) Palette.ink
                else if (layer.region && !colored) Color.WHITE else layer.color
            brush.style = if (layer.region || layer.ink) Paint.Style.FILL else Paint.Style.STROKE
            brush.strokeWidth = layer.width
            canvas.drawPath(layer.path, brush)
            if (layer.region) {
                brush.color = Palette.ink
                brush.style = Paint.Style.STROKE
                canvas.drawPath(layer.path, brush)
            }
        }
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
