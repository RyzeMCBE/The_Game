package com.ryzemc.pequenomundo

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

/** Small native activity panels; all decisions and progress live in serializable models. */
open class GamePanel(context: Context) : LinearLayout(context) {
    init {
        orientation = VERTICAL
    }

    protected fun dp(n: Int) = (n * resources.displayMetrics.density + .5f).toInt()

    protected fun label(value: String, size: Float = 21f) =
        TextView(context).apply {
            text = value
            textSize = size
            setTextColor(Palette.ink)
            gravity = Gravity.CENTER
            typeface = Typeface.create("sans-serif-rounded", Typeface.BOLD)
        }

    protected fun surface(view: View, color: Int = Color.WHITE, action: () -> Unit): View {
        view.background =
            GradientDrawable().apply {
                setColor(color)
                cornerRadius = dp(24).toFloat()
                setStroke(dp(2), 0x2290b4bf)
            }
        view.isClickable = true
        view.isFocusable = true
        view.setOnClickListener { action() }
        return view
    }

    protected fun full(view: View, height: Int = ViewGroup.LayoutParams.WRAP_CONTENT) {
        addView(
            view,
            LayoutParams(LayoutParams.MATCH_PARENT, if (height >= 0) dp(height) else height).apply {
                bottomMargin = dp(12)
            },
        )
    }

    protected fun row(views: List<View>, height: Int): View =
        LinearLayout(context).apply {
            gravity = Gravity.CENTER
            for ((i, v) in views.withIndex()) addView(
                v,
                LayoutParams(0, dp(height), 1f).apply { if (i > 0) leftMargin = dp(8) },
            )
        }

    protected fun tile(
        art: String,
        text: String,
        tagName: String,
        artHeight: Int = 100,
        action: () -> Unit,
    ): LinearLayout =
        LinearLayout(context).apply {
            orientation = VERTICAL
            gravity = Gravity.CENTER
            tag = tagName
            contentDescription = text
            setPadding(dp(4), dp(8), dp(4), dp(8))
            addView(ArtView(context, art), LayoutParams(LayoutParams.MATCH_PARENT, dp(artHeight)))
            if (text.isNotEmpty())
                addView(
                    label(text, 18f),
                    LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT),
                )
            surface(this, Color.WHITE, action)
        }
}

class CountingPanel(
    context: Context,
    val game: CountingRound,
    private val audio: AudioController,
    private val finish: () -> Unit,
) : GamePanel(context) {
    private var busy = false
    private val counter = label("${game.counted.size}", 42f)
    private val fruitButtons = mutableListOf<View>()

    init {
        if (game.mode == CountingMode.MATCH) {
            full(
                ArtView(context, "apples_${game.target}", "${game.target} manzanas para contar"),
                210,
            )
            full(
                row(
                    game.options.map { n ->
                        Button(context).apply {
                            text = "$n"
                            textSize = 48f
                            isAllCaps = false
                            setTextColor(Palette.ink)
                            tag = "count_answer_$n"
                            contentDescription = "Número $n"
                            surface(this, Palette.sky) {
                                if (game.answer(n)) finish() else audio.say("try_again")
                            }
                        }
                    },
                    104,
                )
            )
        } else {
            if (game.mode == CountingMode.FEED)
                full(ArtView(context, "bear", "Milo espera sus manzanas"), 108)
            counter.tag = "count_total"
            counter.accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
            full(counter, 58)
            val columns = if (game.totalItems > 5) 5 else minOf(game.totalItems, 3)
            (0 until game.totalItems).chunked(columns).forEach { indices ->
                full(
                    row(
                        indices.map { index ->
                            tile(game.art, "", "count_item_$index", 62) {
                                    if (!busy)
                                        game.tap(index)?.let { value ->
                                            refresh()
                                            if (game.completed) finish()
                                            else {
                                                busy = true
                                                audio.say(
                                                    "count_$value",
                                                    onFinished = { busy = false },
                                                )
                                            }
                                        }
                                }
                                .also { fruitButtons.add(it) }
                        },
                        86,
                    )
                )
            }
            full(
                label(
                    if (game.mode == CountingMode.FEED) "Dale ${game.target} manzanas a Milo"
                    else "Toca cada manzana una vez",
                    18f,
                )
            )
            refresh()
        }
    }

    private fun refresh() {
        counter.text =
            if (game.mode == CountingMode.FEED) "${game.counted.size} / ${game.target}"
            else "${game.counted.size}"
        fruitButtons.forEachIndexed { i, v ->
            v.alpha = if (i in game.counted) .30f else 1f
            v.isEnabled = i !in game.counted && !game.completed
            v.contentDescription =
                if (i in game.counted) "Manzana contada ${game.counted.indexOf(i)+1}"
                else "Contar manzana ${i+1}"
        }
    }

    fun resumeInteraction() {
        busy = false
    }
}

class DiscoveryPanel(
    context: Context,
    val game: DiscoveryRound,
    private val audio: AudioController,
    finish: () -> Unit,
) : GamePanel(context) {
    init {
        if (game.activity == GameActivity.SHADOWS)
            full(ArtView(context, "page_${game.target}", "Animal que busca su sombra"), 190)
        val choices =
            game.options.map { id ->
                if (game.activity == GameActivity.SIZES) {
                    LinearLayout(context).apply {
                        orientation = VERTICAL
                        gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                        tag = "discovery_$id"
                        contentDescription = if (id == "big") "Animal grande" else "Animal pequeño"
                        addView(
                            ArtView(context, "page_${game.art}"),
                            LayoutParams(
                                LayoutParams.MATCH_PARENT,
                                dp(if (id == "big") 190 else 88),
                            ),
                        )
                        surface(this, Color.WHITE) {
                            if (game.answer(id)) finish() else audio.say("try_again")
                        }
                    }
                } else
                    tile("shadow_$id", "", "discovery_$id", 110) {
                        if (game.answer(id)) finish() else audio.say("try_again")
                    }
            }
        full(row(choices, if (game.activity == GameActivity.SIZES) 225 else 145))
    }
}

class SortingPanel(
    context: Context,
    val game: SortingRound,
    private val audio: AudioController,
    finish: () -> Unit,
) : GamePanel(context) {
    private val pieces = mutableListOf<View>()
    private val prompt = label("Toca una pieza", 19f)

    init {
        full(prompt, 50)
        full(
            row(
                game.items.mapIndexed { i, id ->
                    tile(if (game.byShape) id else "color_$id", "", "sort_item_$i", 75) {
                            if (game.select(i)) {
                                prompt.text = "Ahora toca su cesta"
                                refresh()
                            }
                        }
                        .also { pieces.add(it) }
                },
                106,
            )
        )
        full(label(if (game.byShape) "Cada forma en su cesta" else "Cada color en su cesta", 19f))
        full(
            row(
                game.bins.map { id ->
                    tile(if (game.byShape) id else "color_$id", "Cesta", "sort_bin_$id", 95) {
                            if (game.place(id)) {
                                prompt.text = "¡En su lugar!"
                                refresh()
                                if (game.completed) finish() else audio.say(id)
                            } else if (game.selected >= 0) audio.say("try_again")
                        }
                        .apply {
                            contentDescription =
                                "Cesta ${if(game.byShape)GameContent.shapes.first { it.id==id }.label else GameContent.colors.first { it.id==id }.label}"
                        }
                },
                155,
            )
        )
        refresh()
    }

    private fun refresh() {
        pieces.forEachIndexed { i, v ->
            v.visibility = if (i in game.placed) View.INVISIBLE else View.VISIBLE
            v.scaleX = if (i == game.selected) .88f else 1f
            v.scaleY = v.scaleX
        }
    }
}
