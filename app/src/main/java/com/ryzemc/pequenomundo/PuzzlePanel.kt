package com.ryzemc.pequenomundo

import android.content.ClipData
import android.content.Context
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.view.DragEvent
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView

class PuzzlePanel(
    context: Context,
    private val game: PuzzleGame,
    private val audio: AudioController,
    private val onFinished: () -> Unit,
) : LinearLayout(context) {
    private var selected: Int? = null
    private val sources = mutableListOf<PieceView>()
    private val targets = mutableListOf<PieceView>()
    private val message = TextView(context)

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    init {
        orientation = VERTICAL
        message.text = "Toca una pieza y después su lugar."
        message.gravity = Gravity.CENTER
        message.setTextColor(Palette.muted)
        message.textSize = 17f
        message.setPadding(0, dp(8), 0, dp(14))
        addView(message)
        val wide = resources.configuration.screenWidthDp >= 650
        val panels =
            LinearLayout(context).apply {
                orientation = if (wide) HORIZONTAL else VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
            }
        val available = resources.configuration.screenWidthDp - 48
        val panelWidth = dp(if (wide) 270 else minOf(available, 340))
        fun grid(bank: Boolean): LinearLayout {
            val outer =
                LinearLayout(context).apply {
                    orientation = VERTICAL
                    gravity = Gravity.CENTER
                }
            val rows = game.pieceCount / 2
            repeat(rows) { row ->
                val line = LinearLayout(context).apply { orientation = HORIZONTAL }
                repeat(2) { col ->
                    val piece = if (bank) game.order[row * 2 + col] else row * 2 + col
                    val view = PieceView(context, game, piece, bank)
                    view.tag = if (bank) "puzzle_piece_$piece" else "puzzle_slot_$piece"
                    view.contentDescription = if (bank) "Pieza ${piece+1}" else "Lugar ${piece+1}"
                    view.isFocusable = true
                    val height = (panelWidth / rows) - dp(8)
                    line.addView(
                        view,
                        LayoutParams(panelWidth / 2 - dp(8), height).apply {
                            setMargins(dp(4), dp(4), dp(4), dp(4))
                        },
                    )
                    if (bank) {
                        sources.add(view)
                        view.setOnClickListener {
                            if (piece !in game.placed) {
                                selected = piece
                                message.text = "Ahora toca su lugar."
                                audio.say("puzzle_pick")
                                refresh()
                            }
                        }
                        view.setOnLongClickListener {
                            if (piece in game.placed) return@setOnLongClickListener false
                            selected = piece
                            refresh()
                            val data = ClipData.newPlainText("pieza", piece.toString())
                            if (Build.VERSION.SDK_INT >= 24)
                                it.startDragAndDrop(data, View.DragShadowBuilder(it), null, 0)
                            else
                                @Suppress("DEPRECATION")
                                it.startDrag(data, View.DragShadowBuilder(it), null, 0)
                            true
                        }
                    } else {
                        targets.add(view)
                        view.setOnClickListener {
                            selected?.let { attempt(it, piece) }
                                ?: run {
                                    message.text = "Primero toca una pieza."
                                    audio.say("puzzle_help")
                                }
                        }
                        view.setOnDragListener { _, event ->
                            when (event.action) {
                                DragEvent.ACTION_DRAG_STARTED ->
                                    event.clipDescription?.hasMimeType("text/plain") == true
                                DragEvent.ACTION_DROP -> {
                                    event.clipData
                                        ?.getItemAt(0)
                                        ?.text
                                        ?.toString()
                                        ?.toIntOrNull()
                                        ?.let { attempt(it, piece) }
                                    true
                                }
                                else -> true
                            }
                        }
                    }
                }
                outer.addView(line)
            }
            return outer
        }
        val target = grid(false)
        val bank = grid(true)
        panels.addView(target, LayoutParams(panelWidth, LayoutParams.WRAP_CONTENT))
        panels.addView(
            bank,
            LayoutParams(panelWidth, LayoutParams.WRAP_CONTENT).apply {
                if (wide) leftMargin = dp(12) else topMargin = dp(14)
            },
        )
        addView(panels)
        refresh()
    }

    private fun attempt(piece: Int, slot: Int) {
        if (game.finished) return
        if (game.place(piece, slot)) {
            selected = null
            message.text =
                if (game.finished) "¡Armaste el dibujo!" else "¡Encaja! Busca otra pieza."
            refresh()
            if (game.finished) onFinished() else audio.say("praise_1")
        } else {
            message.text = "Probemos en otro lugar."
            audio.say("puzzle_again")
        }
    }

    private fun refresh() {
        sources.forEach {
            it.selectedPiece = selected == it.piece
            it.isEnabled = it.piece !in game.placed
            it.invalidate()
        }
        targets.forEach { it.invalidate() }
    }

    private class PieceView(
        context: Context,
        val game: PuzzleGame,
        val piece: Int,
        val bank: Boolean,
    ) : View(context) {
        var selectedPiece = false
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        init {
            isClickable = true
            background =
                GradientDrawable().apply {
                    setColor(Color.WHITE)
                    cornerRadius = 16 * resources.displayMetrics.density
                }
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val placed = piece in game.placed
            paint.style = Paint.Style.FILL
            paint.color =
                if (selectedPiece) Palette.peach
                else if (placed && !bank) Palette.mint else Palette.sky
            val r = 14 * resources.displayMetrics.density
            canvas.drawRoundRect(0f, 0f, width.toFloat(), height.toFloat(), r, r, paint)
            canvas.save()
            val clip =
                Path().apply {
                    addRoundRect(5f, 5f, width - 5f, height - 5f, r, r, Path.Direction.CW)
                }
            canvas.clipPath(clip)
            val alpha = if (bank && placed) 35 else if (!bank && !placed) 35 else 255
            canvas.saveLayerAlpha(0f, 0f, width.toFloat(), height.toFloat(), alpha)
            val rows = game.pieceCount / 2
            val col = piece % 2
            val row = piece / 2
            Artwork.draw(
                canvas,
                game.art,
                RectF(
                    -col * width.toFloat(),
                    -row * height.toFloat(),
                    (2 - col) * width.toFloat(),
                    (rows - row) * height.toFloat(),
                ),
            )
            canvas.restore()
            canvas.restore()
            if (selectedPiece) {
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 6f
                paint.color = Palette.green
                canvas.drawRoundRect(3f, 3f, width - 3f, height - 3f, r, r, paint)
            }
        }
    }
}
