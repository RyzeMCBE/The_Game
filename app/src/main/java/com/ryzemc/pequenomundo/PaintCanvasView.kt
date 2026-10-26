package com.ryzemc.pequenomundo

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.View
import kotlin.math.*
import org.json.JSONArray
import org.json.JSONObject

data class PaintMark(
    val type: String,
    val color: Int = Color.RED,
    val size: Float = 12f,
    val region: Int = -1,
    val art: String = "star",
    val points: MutableList<Pair<Float, Float>> = mutableListOf(),
)

class Painting(
    var template: String = "blank",
    val marks: MutableList<PaintMark> = mutableListOf(),
) {
    fun toJson(): String {
        val array = JSONArray()
        marks.forEach { mark ->
            val points = JSONArray()
            mark.points.forEach { (x, y) ->
                points.put(
                    JSONArray().put((x * 10).roundToInt() / 10.0).put((y * 10).roundToInt() / 10.0)
                )
            }
            array.put(
                JSONObject()
                    .put("type", mark.type)
                    .put("color", mark.color)
                    .put("size", mark.size.toDouble())
                    .put("region", mark.region)
                    .put("art", mark.art)
                    .put("points", points)
            )
        }
        return JSONObject().put("template", template).put("marks", array).toString()
    }

    companion object {
        val templates
            get() =
                listOf("blank", "house", "flower", "bear", "fish") +
                    DrawingCatalog.pages.map { it.id }

        fun fromJson(raw: String?): Painting =
            runCatching {
                    if (raw == null) return Painting()
                    val obj = JSONObject(raw)
                    val template =
                        obj.optString("template", "blank").takeIf { it in templates } ?: "blank"
                    val result = Painting(template)
                    val array = obj.optJSONArray("marks") ?: JSONArray()
                    for (i in 0 until min(array.length(), 1200)) {
                        val m = array.getJSONObject(i)
                        val points = m.optJSONArray("points") ?: JSONArray()
                        val list = mutableListOf<Pair<Float, Float>>()
                        for (j in 0 until min(points.length(), 4096)) {
                            val p = points.getJSONArray(j)
                            list.add(
                                p.getDouble(0).toFloat().coerceIn(0f, 600f) to
                                    p.getDouble(1).toFloat().coerceIn(0f, 450f)
                            )
                        }
                        result.marks.add(
                            PaintMark(
                                m.getString("type"),
                                m.optInt("color", Color.RED),
                                m.optDouble("size", 12.0).toFloat().coerceIn(4f, 60f),
                                m.optInt("region", -1),
                                m.optString("art", "star"),
                                list,
                            )
                        )
                    }
                    result
                }
                .getOrDefault(Painting())
    }
}

class PaintCanvasView(context: Context, var painting: Painting) : View(context) {
    var tool = "brush"
    var paintColor = Palette.colors.getValue("red")
    var brushSize = 16f
    var stamp = "star"
    var onChanged: (() -> Unit)? = null
    private val brush = Paint(Paint.ANTI_ALIAS_FLAG)
    private var active: PaintMark? = null
    private var pointerId = -1
    private var sheet: ColoringSheet? = null
    private var paths: List<Path> = emptyList()
    private var hits: List<Region> = emptyList()

    init {
        DrawingCatalog.load(context)
        prepareRegions()
        contentDescription =
            "Lienzo. Pinta con tu dedo. La ayuda mantiene el color dentro de cada parte."
        setLayerType(LAYER_TYPE_SOFTWARE, null)
        isClickable = true
        tag = "paint_canvas"
    }

    private fun prepareRegions() {
        sheet = DrawingCatalog.find(painting.template)
        paths = sheet?.paintAreas ?: legacyRegions()
        hits =
            sheet?.hitRegions
                ?: paths.map { Region().apply { setPath(it, Region(0, 0, 600, 450)) } }
    }

    private fun legacyRegions(): List<Path> {
        fun oval(l: Float, t: Float, r: Float, b: Float) =
            Path().apply { addOval(l, t, r, b, Path.Direction.CW) }
        fun rect(l: Float, t: Float, r: Float, b: Float) =
            Path().apply { addRoundRect(l, t, r, b, 6f, 6f, Path.Direction.CW) }
        fun poly(vararg coordinates: Float) =
            Path().apply {
                moveTo(coordinates[0], coordinates[1])
                for (i in 2 until coordinates.size step 2) lineTo(
                    coordinates[i],
                    coordinates[i + 1],
                )
                close()
            }
        return when (painting.template) {
            "house" ->
                listOf(
                    oval(470f, 28f, 550f, 108f),
                    rect(150f, 216f, 450f, 403f),
                    poly(97f, 220f, 300f, 63f, 503f, 220f),
                    rect(268f, 290f, 335f, 403f),
                    rect(183f, 261f, 238f, 319f),
                    rect(365f, 261f, 420f, 319f),
                )
            "flower" -> {
                val paths =
                    mutableListOf(
                        rect(292f, 200f, 308f, 407f),
                        oval(205f, 295f, 292f, 338f),
                        oval(308f, 343f, 395f, 386f),
                    )
                for (i in 0..5) {
                    val a = i * Math.PI / 3
                    val x = 300f + 69f * cos(a).toFloat()
                    val y = 154f + 69f * sin(a).toFloat()
                    paths.add(oval(x - 46, y - 46, x + 46, y + 46))
                }
                paths.add(oval(251f, 105f, 349f, 203f))
                paths
            }
            "bear" ->
                listOf(
                    oval(142f, 48f, 249f, 155f),
                    oval(351f, 48f, 458f, 155f),
                    oval(162f, 92f, 438f, 386f),
                    oval(220f, 239f, 380f, 355f),
                )
            "fish" ->
                listOf(
                    poly(177f, 215f, 64f, 108f, 64f, 323f),
                    poly(277f, 145f, 275f, 58f, 369f, 136f),
                    oval(145f, 114f, 515f, 330f),
                    oval(277f, 209f, 363f, 290f),
                )
            else -> emptyList()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        val wanted =
            (w * .75f)
                .roundToInt()
                .coerceAtLeast((220 * resources.displayMetrics.density).roundToInt())
        setMeasuredDimension(w, resolveSize(wanted, heightMeasureSpec))
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        render(canvas, width, height)
    }

    private fun render(canvas: Canvas, w: Int, h: Int) {
        canvas.save()
        canvas.drawColor(Color.WHITE)
        val scale = min(w / 600f, h / 450f)
        canvas.translate((w - 600f * scale) / 2, (h - 450f * scale) / 2)
        canvas.scale(scale, scale)
        canvas.clipRect(0f, 0f, 600f, 450f)
        canvas.drawColor(Color.WHITE)
        val regions = paths
        val fills = IntArray(regions.size) { Color.WHITE }
        painting.marks
            .filter { it.type == "fill" && it.region in fills.indices }
            .forEach { fills[it.region] = it.color }
        brush.strokeJoin = Paint.Join.ROUND
        brush.strokeCap = Paint.Cap.ROUND
        regions.forEachIndexed { i, path ->
            brush.style = Paint.Style.FILL
            brush.color = fills[i]
            canvas.drawPath(path, brush)
        }
        for (mark in painting.marks) {
            if ((mark.type == "stroke" || mark.type == "erase") && mark.points.isNotEmpty()) {
                canvas.save()
                if (mark.region in regions.indices) canvas.clipPath(regions[mark.region])
                brush.color = if (mark.type == "erase") Color.WHITE else mark.color
                brush.strokeWidth = mark.size
                if (mark.points.size == 1) {
                    brush.style = Paint.Style.FILL
                    canvas.drawCircle(
                        mark.points[0].first,
                        mark.points[0].second,
                        mark.size / 2,
                        brush,
                    )
                } else {
                    val path = Path()
                    mark.points.forEachIndexed { i, (x, y) ->
                        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    brush.style = Paint.Style.STROKE
                    canvas.drawPath(path, brush)
                }
                canvas.restore()
            } else if (mark.type == "stamp" && mark.points.isNotEmpty()) {
                val (x, y) = mark.points[0]
                Artwork.draw(canvas, mark.art, RectF(x - 42, y - 42, x + 42, y + 42))
            }
        }
        brush.style = Paint.Style.STROKE
        brush.color = Palette.ink
        brush.strokeWidth = 4f
        if (sheet != null) {
            var regionIndex = 0
            for (layer in sheet!!.layers) {
                canvas.save()
                if (layer.region) {
                    canvas.clipPath(sheet!!.outlineClips[regionIndex++])
                    brush.style = Paint.Style.STROKE
                    brush.color = Palette.ink
                } else {
                    brush.style = if (layer.ink) Paint.Style.FILL else Paint.Style.STROKE
                    brush.color = layer.color
                }
                brush.strokeWidth = layer.width
                canvas.drawPath(layer.path, brush)
                canvas.restore()
            }
        } else regions.forEach { canvas.drawPath(it, brush) }
        brush.style = Paint.Style.FILL
        if (painting.template == "bear") {
            canvas.drawCircle(247f, 215f, 9f, brush)
            canvas.drawCircle(353f, 215f, 9f, brush)
            canvas.drawOval(283f, 269f, 317f, 291f, brush)
            brush.style = Paint.Style.STROKE
            brush.strokeWidth = 5f
            canvas.drawArc(270f, 295f, 330f, 327f, 0f, 180f, false, brush)
        } else if (painting.template == "fish") {
            canvas.drawCircle(439f, 195f, 11f, brush)
            brush.style = Paint.Style.STROKE
            canvas.drawArc(452f, 232f, 499f, 266f, 10f, 100f, false, brush)
        }
        canvas.restore()
    }

    fun bitmap(): Bitmap =
        Bitmap.createBitmap(960, 720, Bitmap.Config.ARGB_8888).also { render(Canvas(it), 960, 720) }

    fun undo() {
        if (painting.marks.isNotEmpty()) {
            painting.marks.removeAt(painting.marks.lastIndex)
            invalidate()
            onChanged?.invoke()
        }
    }

    fun changeTemplate(id: String) {
        painting = Painting(id)
        tool = "brush"
        prepareRegions()
        invalidate()
        onChanged?.invoke()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        fun position(index: Int): Pair<Float, Float> {
            val scale = min(width / 600f, height / 450f)
            return ((event.getX(index) - (width - 600f * scale) / 2) / scale) to
                ((event.getY(index) - (height - 450f * scale) / 2) / scale)
        }
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (painting.marks.size >= 1200) return true
                pointerId = event.getPointerId(0)
                val point = position(0)
                if (point.first !in 0f..600f || point.second !in 0f..450f) return true
                parent.requestDisallowInterceptTouchEvent(true)
                val hit =
                    hits.indexOfLast {
                        it.contains(point.first.roundToInt(), point.second.roundToInt())
                    }
                if (painting.template != "blank" && hit < 0 && tool != "stamp") return true
                when (tool) {
                    "brush",
                    "eraser" -> {
                        active =
                            PaintMark(
                                if (tool == "eraser") "erase" else "stroke",
                                paintColor,
                                brushSize,
                                region = hit,
                                points = mutableListOf(point),
                            )
                        painting.marks.add(active!!)
                    }
                    "stamp" ->
                        painting.marks.add(
                            PaintMark("stamp", art = stamp, points = mutableListOf(point))
                        )
                    "fill" -> {
                        if (hit >= 0)
                            painting.marks.add(PaintMark("fill", paintColor, region = hit))
                    }
                }
                invalidate()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val index = event.findPointerIndex(pointerId)
                if (index >= 0)
                    active?.let { mark ->
                        val point = position(index)
                        if (
                            mark.points.size < 4096 &&
                                hypot(
                                    point.first - mark.points.last().first,
                                    point.second - mark.points.last().second,
                                ) > 1.5f
                        ) {
                            mark.points.add(point)
                            invalidate()
                        }
                    }
                return true
            }
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {
                active = null
                pointerId = -1
                parent.requestDisallowInterceptTouchEvent(false)
                onChanged?.invoke()
                invalidate()
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
