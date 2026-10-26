package com.ryzemc.pequenomundo

import android.app.Activity
import android.app.AlertDialog
import android.content.res.ColorStateList
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.*
import android.window.OnBackInvokedDispatcher
import java.text.DateFormat
import java.util.Date
import kotlin.random.Random

class MainActivity : Activity() {
    private lateinit var store: LocalStore
    private lateinit var audio: AudioController
    private lateinit var clock: PlayClock
    private lateinit var root: LinearLayout
    private lateinit var stage: FrameLayout
    private lateinit var body: LinearLayout
    private val handler = Handler(Looper.getMainLooper())
    private var foreground = false
    private var screen = "home"
    private var generation = 0
    private var promptKeys = arrayOf("welcome")
    private var painting: Painting? = null
    private var paintView: PaintCanvasView? = null
    private var learning: LearningRound? = null
    private var memory: MemoryGame? = null
    private var puzzle: PuzzleGame? = null
    private var selectedDrawing: String? = null
    private var saving = false
    private var galleryPage = 0
    private var pendingReward: GameActivity? = null
    private var rewardVoice: String? = null
    private var rewardOverlay: FrameLayout? = null
    private var rewardAnimation: CelebrationView? = null
    private var rewardGate: RewardGate? = null

    private data class Retained(
        val painting: Painting?,
        val learning: LearningRound?,
        val memory: MemoryGame?,
        val puzzle: PuzzleGame?,
    )

    private val ticker =
        object : Runnable {
            override fun run() {
                if (!foreground) return
                if (
                    screen != "parent" &&
                        screen != "break" &&
                        clock.expired(SystemClock.elapsedRealtime(), store.breakMinutes)
                )
                    showBreak()
                handler.postDelayed(this, 1000)
            }
        }

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        store = LocalStore(this)
        audio = AudioController(this, store)
        clock = PlayClock(store.elapsed)
        val retained = lastNonConfigurationInstance as? Retained
        painting = retained?.painting
        @Suppress("DEPRECATION")
        learning = retained?.learning ?: state?.getSerializable("learning") as? LearningRound
        @Suppress("DEPRECATION")
        memory = retained?.memory ?: state?.getSerializable("memory") as? MemoryGame
        @Suppress("DEPRECATION")
        puzzle = retained?.puzzle ?: state?.getSerializable("puzzle") as? PuzzleGame
        selectedDrawing = state?.getString("drawing")
        root =
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundColor(Palette.background)
                if (Build.VERSION.SDK_INT >= 29) isForceDarkAllowed = false
            }
        stage =
            FrameLayout(this).apply { if (Build.VERSION.SDK_INT >= 29) isForceDarkAllowed = false }
        stage.addView(
            root,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ),
        )
        setContentView(stage)
        if (Build.VERSION.SDK_INT >= 33)
            onBackInvokedDispatcher.registerOnBackInvokedCallback(
                OnBackInvokedDispatcher.PRIORITY_DEFAULT
            ) {
                navigateBack()
            }
        root.setOnApplyWindowInsetsListener { view, insets ->
            if (Build.VERSION.SDK_INT >= 30) {
                val bars =
                    insets.getInsets(
                        WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout()
                    )
                view.setPadding(dp(16) + bars.left, bars.top, dp(16) + bars.right, bars.bottom)
            } else {
                @Suppress("DEPRECATION")
                view.setPadding(
                    dp(16) + insets.systemWindowInsetLeft,
                    insets.systemWindowInsetTop,
                    dp(16) + insets.systemWindowInsetRight,
                    insets.systemWindowInsetBottom,
                )
            }
            insets
        }
        root.requestApplyInsets()
        // Parent access is deliberately not restored after rotation/process recreation.
        when (state?.getString("screen")) {
            "paint" -> showPaint()
            "play" -> showPlay()
            "learn" -> showLearn()
            "learning" -> learning?.let { showLearning(it.kind, false) } ?: showLearn()
            "memory" -> showMemory(false)
            "puzzle" -> showPuzzle(false)
            "gallery" -> showGallery()
            "drawing" -> selectedDrawing?.let { showDrawing(it) } ?: showGallery()
            "break" -> showBreak()
            else -> showHome()
        }
    }

    override fun onResume() {
        super.onResume()
        foreground = true
        audio.resume()
        if (screen != "parent" && screen != "break") clock.resume(SystemClock.elapsedRealtime())
        handler.post(ticker)
        if (pendingReward != null) startCelebration() else audio.say(*promptKeys)
    }

    override fun onPause() {
        foreground = false
        handler.removeCallbacks(ticker)
        clock.pause(SystemClock.elapsedRealtime())
        store.elapsed = clock.elapsed(SystemClock.elapsedRealtime())
        painting?.let(store::saveDraft)
        cancelCelebration(clearPending = false)
        audio.pause()
        super.onPause()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        cancelCelebration(clearPending = false)
        audio.pause()
        store.close()
        super.onDestroy()
    }

    override fun onSaveInstanceState(out: Bundle) {
        out.putString("screen", screen)
        out.putSerializable("learning", learning)
        out.putSerializable("memory", memory)
        out.putSerializable("puzzle", puzzle)
        out.putString("drawing", selectedDrawing)
        super.onSaveInstanceState(out)
    }

    @Deprecated("Activity retained data is used only for the small native screen state")
    override fun onRetainNonConfigurationInstance(): Any =
        Retained(painting, learning, memory, puzzle)

    @Deprecated("Android back dispatch delegates here on supported Android versions")
    override fun onBackPressed() {
        navigateBack()
    }

    private fun navigateBack() {
        if (rewardOverlay != null) {
            showHome()
            return
        }
        when (screen) {
            "home" -> finish()
            "learning" -> showLearn()
            "memory",
            "puzzle" -> showPlay()
            "drawing" -> showGallery()
            "break" ->
                gate {
                    clock.reset(SystemClock.elapsedRealtime())
                    store.elapsed = 0
                    showHome()
                }
            else -> showHome()
        }
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density + .5f).toInt()

    private fun rounded(fill: Int, radius: Int = 23) =
        GradientDrawable().apply {
            setColor(fill)
            cornerRadius = dp(radius).toFloat()
        }

    private fun clickable(view: View, fill: Int = Color.WHITE, action: () -> Unit): View {
        view.background = RippleDrawable(ColorStateList.valueOf(0x2234715C), rounded(fill), null)
        view.isClickable = true
        view.isFocusable = true
        view.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
        view.setOnClickListener { action() }
        view.accessibilityDelegate =
            object : View.AccessibilityDelegate() {
                override fun onInitializeAccessibilityNodeInfo(
                    host: View,
                    info: AccessibilityNodeInfo,
                ) {
                    super.onInitializeAccessibilityNodeInfo(host, info)
                    info.className = "android.widget.Button"
                }
            }
        return view
    }

    private fun text(
        value: String,
        size: Float = 18f,
        color: Int = Palette.ink,
        bold: Boolean = false,
    ) =
        TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            typeface =
                Typeface.create("sans-serif-rounded", if (bold) Typeface.BOLD else Typeface.NORMAL)
            setLineSpacing(dp(2).toFloat(), 1f)
        }

    private fun button(
        value: String,
        tagName: String = "",
        fill: Int = Color.WHITE,
        action: () -> Unit,
    ) =
        Button(this).apply {
            text = value
            tag = tagName
            isAllCaps = false
            textSize = 17f
            setTextColor(Palette.ink)
            minHeight = dp(56)
            minimumWidth = dp(48)
            setPadding(dp(12), dp(9), dp(12), dp(9))
            clickable(this, fill, action)
        }

    private fun LinearLayout.gap(height: Int = 12) {
        addView(View(this@MainActivity), LinearLayout.LayoutParams(1, dp(height)))
    }

    private fun LinearLayout.addFull(
        view: View,
        height: Int = ViewGroup.LayoutParams.WRAP_CONTENT,
    ) {
        addView(
            view,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                if (height >= 0) dp(height) else height,
            ),
        )
    }

    private fun columns(views: List<View>, height: Int = ViewGroup.LayoutParams.WRAP_CONTENT) =
        LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            views.forEachIndexed { i, v ->
                addView(
                    v,
                    LinearLayout.LayoutParams(0, if (height >= 0) dp(height) else height, 1f)
                        .apply { if (i > 0) leftMargin = dp(8) },
                )
            }
        }

    private fun prompt(vararg keys: String) {
        promptKeys = arrayOf(*keys)
        audio.say(*keys)
    }

    private fun shell(route: String, title: String, subtitle: String = "") {
        cancelCelebration()
        if (screen == "paint") painting?.let(store::saveDraft)
        screen = route
        generation++
        paintView = null
        root.removeAllViews()
        val now = SystemClock.elapsedRealtime()
        if (route == "parent" || route == "break") clock.pause(now)
        else if (foreground) clock.resume(now)
        val header =
            LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, dp(10), 0, dp(4))
            }
        header.addView(
            text("El mundo de Noah", 19f, bold = true).apply { gravity = Gravity.CENTER_VERTICAL },
            LinearLayout.LayoutParams(0, dp(56), 1f),
        )
        header.addView(
            button("Familia", "family", Palette.mint) { gate { showParent() } },
            LinearLayout.LayoutParams(dp(84), dp(48)),
        )
        root.addFull(header)
        val nav =
            LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, dp(4), 0, dp(8))
            }
        if (route != "home")
            nav.addView(
                button("Inicio", "home") {
                    if (screen == "break")
                        gate {
                            clock.reset(SystemClock.elapsedRealtime())
                            store.elapsed = 0
                            showHome()
                        }
                    else showHome()
                },
                LinearLayout.LayoutParams(dp(80), dp(48)),
            )
        nav.addView(View(this), LinearLayout.LayoutParams(0, 1, 1f))
        nav.addView(
            button("Escuchar a Milo", "repeat", Palette.sky) { audio.say(*promptKeys) },
            LinearLayout.LayoutParams(dp(170), dp(48)),
        )
        root.addFull(nav)
        val scroll =
            ScrollView(this).apply {
                isFillViewport = true
                isVerticalScrollBarEnabled = false
            }
        val frame =
            object : FrameLayout(this) {
                override fun onMeasure(w: Int, h: Int) {
                    if (childCount > 0)
                        getChildAt(0).layoutParams.width =
                            minOf(View.MeasureSpec.getSize(w), dp(720))
                    super.onMeasure(w, h)
                }
            }
        body =
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(0, dp(8), 0, dp(24))
                tag = "screen_$route"
            }
        val bodyWidth = dp((resources.configuration.screenWidthDp - 32).coerceAtMost(720))
        frame.addView(
            body,
            FrameLayout.LayoutParams(
                bodyWidth,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.TOP or Gravity.CENTER_HORIZONTAL,
            ),
        )
        scroll.addView(
            frame,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ),
        )
        root.addView(scroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        if (title.isNotEmpty()) {
            body.addFull(text(title, 29f, bold = true))
            body.gap(4)
        }
        if (subtitle.isNotEmpty()) {
            body.addFull(text(subtitle, 17f, Palette.muted))
            body.gap()
        }
    }

    private fun card(
        title: String,
        description: String,
        art: String,
        fill: Int,
        tagName: String,
        vertical: Boolean = false,
        action: () -> Unit,
    ): View {
        val card =
            LinearLayout(this).apply {
                orientation = if (vertical) LinearLayout.VERTICAL else LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(15), dp(12), dp(15), dp(12))
                minimumHeight = dp(if (vertical) 195 else 104)
                tag = tagName
                contentDescription = "$title. $description"
            }
        card.addView(
            ArtView(this, art),
            LinearLayout.LayoutParams(dp(if (vertical) 94 else 76), dp(if (vertical) 94 else 76))
                .apply { gravity = Gravity.CENTER_HORIZONTAL },
        )
        val labels =
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(if (vertical) 0 else dp(15), 0, 0, 0)
                addFull(
                    text(title, 24f, bold = true).apply { if (vertical) gravity = Gravity.CENTER }
                )
                addFull(text(description, 15f).apply { if (vertical) gravity = Gravity.CENTER })
            }
        card.addView(
            labels,
            LinearLayout.LayoutParams(
                if (vertical) ViewGroup.LayoutParams.MATCH_PARENT else 0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                if (vertical) 0f else 1f,
            ),
        )
        return clickable(card, fill, action)
    }

    private fun showHome() {
        shell("home", "", "")
        val hero =
            LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                background = rounded(Palette.sky)
                setPadding(dp(16), dp(10), dp(12), dp(10))
            }
        val words =
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                addFull(text("¡Hola, Noah!", 30f, bold = true))
                addFull(text("¿A qué jugamos hoy?", 17f, Palette.muted))
            }
        hero.addView(words, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        hero.addView(
            ArtView(this, "bear", "Milo, el osito"),
            LinearLayout.LayoutParams(dp(98), dp(110)),
        )
        body.addFull(hero)
        body.gap(16)
        val wide = resources.configuration.screenWidthDp >= 650
        val choices =
            listOf(
                card("Pintar", "Mi taller de colores", "paint", Palette.peach, "paint", wide) {
                    showPaint()
                },
                card("Jugar", "Formas, parejas y piezas", "play", Palette.lilac, "play", wide) {
                    showPlay()
                },
                card(
                    "Aprender",
                    "Pequeños descubrimientos",
                    "rainbow",
                    Palette.mint,
                    "learn",
                    wide,
                ) {
                    showLearn()
                },
            )
        if (wide) body.addFull(columns(choices))
        else
            choices.forEach {
                body.addFull(it)
                body.gap(12)
            }
        body.gap(4)
        body.addFull(button("Mis dibujos", "gallery", Palette.sky) { showGallery() })
        body.gap(12)
        body.addFull(
            text("Soy Milo. ¡Vamos, Noah!", 16f, Palette.muted).apply { gravity = Gravity.CENTER }
        )
        prompt("welcome")
    }

    private fun showPlay() {
        shell("play", "Vamos a jugar", "Elige una aventura pequeña.")
        body.addFull(
            card("Formas", "Cada forma en su lugar", "play", Palette.peach, "shapes") {
                showLearning(LearningKind.SHAPES, true)
            }
        )
        body.gap()
        body.addFull(
            card("Parejas", "Encuentra dos animales iguales", "cat", Palette.lilac, "memory") {
                showMemory(true)
            }
        )
        body.gap()
        body.addFull(
            card(
                "Rompecabezas",
                "Arma el dibujo con piezas grandes",
                "bear",
                Palette.mint,
                "puzzle",
            ) {
                showPuzzle(true)
            }
        )
        prompt("games_help")
    }

    private fun showLearn() {
        shell("learn", "Pequeños descubrimientos", "Escucha, toca y explora.")
        body.addFull(
            card("Colores", "Rojo, azul y muchos más", "rainbow", Palette.peach, "colors") {
                showLearning(LearningKind.COLORS, true)
            }
        )
        body.gap()
        body.addFull(
            card("Números", "Contamos del 1 al 5", "apple", Palette.mint, "numbers") {
                showLearning(LearningKind.NUMBERS, true)
            }
        )
        body.gap()
        body.addFull(
            card("Animales", "Escucha sus sonidos", "cow", Palette.lilac, "animals") {
                showLearning(LearningKind.ANIMALS, true)
            }
        )
        prompt("learn_help")
    }

    private fun showLearning(kind: LearningKind, fresh: Boolean) {
        if (fresh || learning?.kind != kind)
            learning =
                LearningRound.create(
                    kind,
                    store.difficulty,
                    previousId = learning?.takeIf { it.kind == kind }?.target?.id,
                )
        val round = learning!!
        val question =
            when (kind) {
                LearningKind.NUMBERS -> "¿Cuántas manzanas hay?"
                LearningKind.COLORS -> "Busca el color ${round.target.label.lowercase()}."
                LearningKind.SHAPES -> "Toca el ${round.target.label.lowercase()}."
                LearningKind.ANIMALS -> "¿Qué animal escuchas?"
            }
        shell("learning", question)
        body.tag = "screen_learning_${kind.name.lowercase()}"
        val art = if (kind == LearningKind.NUMBERS) "apples_${round.target.id}" else "bear"
        body.addFull(
            ArtView(
                this,
                art,
                if (kind == LearningKind.NUMBERS) "${round.target.id} manzanas"
                else "Milo te acompaña",
            ),
            if (kind == LearningKind.NUMBERS) 172 else 115,
        )
        body.gap()
        val feedback =
            text(
                    if (round.completed) "¡Lo descubriste!" else "Vamos pasito a pasito.",
                    19f,
                    Palette.green,
                    true,
                )
                .apply {
                    tag = "feedback"
                    gravity = Gravity.CENTER
                    accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
                }
        val choices =
            round.options.map { item ->
                val tile =
                    LinearLayout(this).apply {
                        orientation = LinearLayout.VERTICAL
                        gravity = Gravity.CENTER
                        setPadding(dp(6), dp(9), dp(6), dp(9))
                        tag = "answer_${item.id}"
                        contentDescription = item.label
                    }
                tile.addView(
                    ArtView(this, item.art),
                    LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(104)),
                )
                tile.addFull(text(item.label, 19f, bold = true).apply { gravity = Gravity.CENTER })
                clickable(
                        tile,
                        if (round.completed && item.id == round.target.id) Palette.mint
                        else Color.WHITE,
                    ) {
                        if (round.completed) return@clickable
                        if (round.answer(item.id)) {
                            store.record(kind.name.lowercase())
                            feedback.text = "¡Muy bien, Noah! Una estrella para ti."
                            tile.background = rounded(Palette.mint)
                            body.findViewWithTag<View>("answers")?.let { group ->
                                if (group is ViewGroup)
                                    for (i in 0 until group.childCount) group
                                        .getChildAt(i)
                                        .isEnabled = false
                            }
                            celebrate(GameActivity.from(kind), item.voice)
                        } else {
                            feedback.text = "Probemos otra vez, despacito."
                            audio.say("try_again")
                        }
                    }
                    .apply { isEnabled = !round.completed }
            }
        body.addFull(columns(choices).apply { tag = "answers" })
        body.gap(18)
        body.addFull(feedback)
        body.gap(12)
        when (kind) {
            LearningKind.NUMBERS -> prompt("numbers_help")
            LearningKind.ANIMALS -> prompt("ask_${round.target.id}", "sound_${round.target.id}")
            else -> prompt("ask_${round.target.id}")
        }
        if (round.completed) celebrate(GameActivity.from(kind), round.target.voice)
    }

    private fun showMemory(fresh: Boolean) {
        if (fresh || memory == null) memory = MemoryGame.create(if (store.difficulty == 2) 2 else 3)
        val game = memory!!
        shell("memory", "Busca las parejas", "Toca dos tarjetas para descubrir los animales.")
        val feedback =
            text(
                    if (game.finished) "¡Encontraste todas las parejas!"
                    else "Busquemos dos iguales.",
                    19f,
                    Palette.green,
                    true,
                )
                .apply {
                    gravity = Gravity.CENTER
                    tag = "feedback"
                    accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
                }
        val cards = mutableListOf<LinearLayout>()
        fun refresh() {
            cards.forEachIndexed { index, tile ->
                val visible = index in game.matched || index in game.faceUp
                (tile.getChildAt(0) as ArtView).apply {
                    art = if (visible) game.cards[index] else "star"
                    invalidate()
                }
                tile.background =
                    rounded(
                        if (index in game.matched) Palette.mint
                        else if (visible) Color.WHITE else Palette.lilac
                    )
                tile.contentDescription =
                    if (visible)
                        "${GameContent.animals.first {it.art==game.cards[index]}.label}, tarjeta ${index+1}"
                    else "Tarjeta ${index+1}, boca abajo"
                tile.isEnabled = index !in game.matched
            }
        }
        val token = generation
        fun concealLater() {
            handler.postDelayed(
                {
                    if (generation == token) {
                        game.concealMismatch()
                        refresh()
                    }
                },
                1250,
            )
        }
        for (row in 0 until game.cards.size / 2) {
            val views =
                (0..1).map { col ->
                    val index = row * 2 + col
                    val tile =
                        LinearLayout(this).apply {
                            gravity = Gravity.CENTER
                            orientation = LinearLayout.VERTICAL
                            tag = "memory_$index"
                            addView(
                                ArtView(this@MainActivity, "star"),
                                LinearLayout.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    dp(116),
                                ),
                            )
                        }
                    cards.add(tile)
                    clickable(tile, Palette.lilac) {
                        when (game.tap(index)) {
                            MemoryResult.REVEALED -> {
                                audio.say(game.cards[index], "sound_${game.cards[index]}")
                            }
                            MemoryResult.MATCH -> {
                                feedback.text = "¡Encontraste una pareja!"
                                audio.say("memory_match")
                            }
                            MemoryResult.MISMATCH -> {
                                feedback.text = "Miremos de nuevo, sin apuro."
                                audio.say("memory_again")
                                concealLater()
                            }
                            MemoryResult.FINISHED -> {
                                store.record("memory")
                                feedback.text = "¡Encontraste todas las parejas!"
                                celebrate(GameActivity.MEMORY)
                            }
                            else -> Unit
                        }
                        refresh()
                    }
                }
            body.addFull(columns(views))
            body.gap(12)
        }
        body.addFull(feedback)
        body.gap()
        refresh()
        if (game.waiting) concealLater()
        prompt("memory_help")
        if (game.finished) celebrate(GameActivity.MEMORY)
    }

    private fun showPuzzle(fresh: Boolean) {
        if (fresh || puzzle == null)
            puzzle =
                PuzzleGame(
                    if (store.difficulty == 2) 2 else 4,
                    listOf("bear", "fish", "house", "flower").random(),
                )
        val game = puzzle!!
        shell("puzzle", "Arma el dibujo")
        body.addFull(
            PuzzlePanel(this, game, audio) {
                store.record("puzzle")
                celebrate(GameActivity.PUZZLE)
            }
        )
        body.gap()
        prompt("puzzle_help")
        if (game.finished) celebrate(GameActivity.PUZZLE)
    }

    private fun showGame(activity: GameActivity) {
        when (activity) {
            GameActivity.MEMORY -> showMemory(true)
            GameActivity.PUZZLE -> showPuzzle(true)
            else -> showLearning(LearningKind.valueOf(activity.name), true)
        }
    }

    private fun celebrate(activity: GameActivity, answerVoice: String? = null) {
        if (pendingReward == activity && rewardOverlay != null) return
        pendingReward = activity
        rewardVoice = answerVoice
        if (foreground) startCelebration()
    }

    private fun startCelebration() {
        val activity = pendingReward ?: return
        val compact = resources.configuration.screenHeightDp < 500
        cancelCelebration(clearPending = false)
        val token = generation
        val overlay =
            FrameLayout(this).apply {
                tag = "celebration"
                isClickable = true
                setBackgroundColor(Palette.background)
                setPadding(root.paddingLeft, root.paddingTop, root.paddingRight, root.paddingBottom)
            }
        val art = CelebrationView(this)
        overlay.addView(
            art,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ),
        )
        val heading =
            text("¡Muy bien, Noah!", if (compact) 24f else 29f, Palette.green, true).apply {
                tag = "celebration_title"
                gravity = Gravity.CENTER
                accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
            }
        overlay.addView(
            heading,
            FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.TOP or Gravity.CENTER_HORIZONTAL,
                )
                .apply { topMargin = dp(if (compact) 14 else 84) },
        )
        overlay.addView(
            text("Una estrellita para ti", 19f, Palette.ink).apply { gravity = Gravity.CENTER },
            FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.TOP or Gravity.CENTER_HORIZONTAL,
                )
                .apply { topMargin = dp(if (compact) 52 else 132) },
        )
        overlay.addView(
            text("¡Seguimos jugando!", 19f, Palette.muted).apply { gravity = Gravity.CENTER },
            FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL,
                )
                .apply { bottomMargin = dp(if (compact) 8 else 32) },
        )
        overlay.addView(
            button("Inicio", "celebration_home", Palette.mint) { showHome() },
            FrameLayout.LayoutParams(dp(92), dp(52), Gravity.TOP or Gravity.START).apply {
                topMargin = dp(12)
            },
        )
        root.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
        stage.addView(
            overlay,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ),
        )
        rewardOverlay = overlay
        rewardAnimation = art
        val gate = RewardGate()
        rewardGate = gate
        fun active() =
            foreground &&
                generation == token &&
                rewardOverlay === overlay &&
                pendingReward == activity
        fun proceed(ready: Boolean) {
            if (ready && active()) showGame(activity.next())
        }
        fun presentStar() {
            if (!active()) return
            art.start { if (active()) proceed(gate.animationFinished()) }
            audio.say("praise_3", onFinished = { if (active()) proceed(gate.audioFinished()) })
            audio.playCelebrationChime()
        }
        val answer = rewardVoice
        if (answer == null) presentStar() else audio.say(answer, onFinished = { presentStar() })
    }

    private fun cancelCelebration(clearPending: Boolean = true) {
        rewardAnimation?.cancel()
        rewardGate?.cancel()
        rewardGate = null
        audio.stopEffect()
        rewardAnimation = null
        rewardOverlay?.let { stage.removeView(it) }
        rewardOverlay = null
        root.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_AUTO
        if (clearPending) {
            pendingReward = null
            rewardVoice = null
        }
    }

    private fun showPaint() {
        if (painting == null) painting = store.readDraft()
        shell("paint", "Mi taller de colores", "Pinta con tu dedo y hazlo a tu manera.")
        val canvas = PaintCanvasView(this, painting!!)
        paintView = canvas
        canvas.tool = if (painting!!.template == "blank") "brush" else "fill"
        canvas.onChanged = {
            painting = canvas.painting
            store.saveDraft(canvas.painting)
        }
        val templates = listOf("Hoja libre", "Casita", "Flor", "Osito", "Pez")
        val chooser =
            Spinner(this).apply {
                tag = "templates"
                contentDescription = "Elegir dibujo"
                adapter =
                    ArrayAdapter(
                        this@MainActivity,
                        android.R.layout.simple_spinner_dropdown_item,
                        templates,
                    )
                setSelection(Painting.templates.indexOf(canvas.painting.template))
            }
        body.addFull(chooser, 52)
        body.gap(8)
        body.addFull(canvas)
        body.gap(12)
        val palette =
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
            }
        val selections = mutableListOf<TextView>()
        Palette.colors.entries.chunked(4).forEach { entries ->
            val line =
                LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER
                }
            entries.forEach { (name, color) ->
                val swatch =
                    FrameLayout(this).apply {
                        contentDescription =
                            when (name) {
                                "red" -> "Rojo"
                                "yellow" -> "Amarillo"
                                "blue" -> "Azul"
                                "green" -> "Verde"
                                "purple" -> "Morado"
                                "orange" -> "Naranja"
                                else -> "Marrón"
                            }
                        tag = "paint_$name"
                        background = rounded(color, 50)
                        minimumHeight = dp(48)
                    }
                val mark = text("", 20f, Palette.ink, true).apply { gravity = Gravity.CENTER }
                swatch.addView(
                    mark,
                    FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    ),
                )
                selections.add(mark)
                clickable(swatch, color) {
                    canvas.paintColor = color
                    selections.forEach { it.text = "" }
                    mark.text = "•"
                    audio.say(name)
                }
                line.addView(
                    swatch,
                    LinearLayout.LayoutParams(0, dp(48), 1f).apply {
                        if (line.childCount > 0) leftMargin = dp(8)
                    },
                )
                if (name == "red") mark.text = "•"
            }
            palette.addFull(line)
            palette.gap(8)
        }
        body.addFull(palette)
        body.gap(4)
        val toolLabel =
            text(
                    if (canvas.tool == "fill") "Toca una zona para colorear."
                    else "Pincel grueso: pinta con el dedo.",
                    16f,
                    Palette.muted,
                )
                .apply { gravity = Gravity.CENTER }
        body.addFull(
            columns(
                listOf(
                    button("Pincel", "brush") {
                        canvas.tool = "brush"
                        toolLabel.text = "Pincel grueso: pinta con el dedo."
                        audio.say("paint_help")
                    },
                    button("Rellenar", "fill") {
                        canvas.tool = "fill"
                        toolLabel.text = "Toca una zona para colorear."
                        audio.say("fill_help")
                    },
                    button("Sellos", "stamps") {
                        canvas.tool = "stamp"
                        toolLabel.text = "Toca tu hoja para poner un sello."
                        audio.say("stamp_help")
                    },
                )
            )
        )
        body.gap(8)
        body.addFull(toolLabel)
        val stamps =
            listOf("star", "flower", "bear").map { art ->
                ArtView(this, art).apply {
                    contentDescription =
                        when (art) {
                            "star" -> "Sello de estrella"
                            "flower" -> "Sello de flor"
                            else -> "Sello de osito"
                        }
                    clickable(this, Palette.sky) {
                        canvas.tool = "stamp"
                        canvas.stamp = art
                        toolLabel.text = "Toca tu hoja para poner un sello."
                        audio.say("stamp_help")
                    }
                }
            }
        body.gap(8)
        body.addFull(columns(stamps, 54))
        body.gap(12)
        body.addFull(
            columns(
                listOf(
                    button("Deshacer", "undo") { canvas.undo() },
                    button("Guardar dibujo", "save", Palette.mint) { savePicture(canvas) },
                )
            )
        )
        body.gap(10)
        body.addFull(button("Ver mis dibujos", "gallery", Palette.sky) { showGallery() })
        chooser.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent: AdapterView<*>?) = Unit

                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long,
                ) {
                    val template = Painting.templates[position]
                    if (template != canvas.painting.template) {
                        if (canvas.painting.marks.isNotEmpty()) savePicture(canvas, true)
                        canvas.changeTemplate(template)
                        painting = canvas.painting
                        toolLabel.text =
                            if (template == "blank") "Pincel grueso: pinta con el dedo."
                            else "Toca una zona para colorear."
                        audio.say(if (template == "blank") "paint_help" else "fill_help")
                    }
                }
            }
        prompt(if (canvas.tool == "fill") "fill_help" else "paint_help")
    }

    private fun savePicture(canvas: PaintCanvasView, silent: Boolean = false) {
        if (saving && !silent) return
        if (!silent) {
            saving = true
            body.findViewWithTag<View>("save")?.isEnabled = false
        }
        store.savePicture(canvas.bitmap()) pictureSaved@{ success ->
            if (!silent) saving = false
            if (isDestroyed || isFinishing) return@pictureSaved
            if (!silent) {
                body.findViewWithTag<View>("save")?.isEnabled = true
                Toast.makeText(
                        this,
                        if (success) "Dibujo guardado" else "No se pudo guardar. Intenta otra vez.",
                        Toast.LENGTH_SHORT,
                    )
                    .show()
                if (screen == "paint") audio.say(if (success) "saved" else "save_error")
            }
        }
    }

    private fun showGallery() {
        shell("gallery", "Mis dibujos", "Un pequeño museo de tus creaciones.")
        val files = store.gallery()
        if (files.isEmpty()) {
            body.addFull(ArtView(this, "paint"), 180)
            body.addFull(
                text("Aquí aparecerán los dibujos que guardes.", 19f, Palette.muted).apply {
                    gravity = Gravity.CENTER
                }
            )
            body.gap()
            body.addFull(button("Vamos a pintar", "paint", Palette.peach) { showPaint() })
        } else {
            galleryPage = galleryPage.coerceIn(0, (files.size - 1) / 12)
            files.drop(galleryPage * 12).take(12).forEach { file ->
                val row =
                    LinearLayout(this).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                        setPadding(dp(10), dp(8), dp(10), dp(8))
                        tag = "drawing_${file.name}"
                        contentDescription =
                            "Ver dibujo del ${DateFormat.getDateInstance().format(Date(file.lastModified()))}"
                    }
                val image =
                    ImageView(this).apply {
                        setImageBitmap(
                            BitmapFactory.decodeFile(
                                file.path,
                                BitmapFactory.Options().apply { inSampleSize = 4 },
                            )
                        )
                        scaleType = ImageView.ScaleType.FIT_CENTER
                        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                    }
                row.addView(image, LinearLayout.LayoutParams(dp(110), dp(85)))
                row.addView(
                    text(
                        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                            .format(Date(file.lastModified())),
                        16f,
                    ),
                    LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f),
                )
                clickable(row, Color.WHITE) { showDrawing(file.name) }
                body.addFull(row)
                body.gap(10)
            }
            val paging = mutableListOf<View>()
            if (galleryPage > 0)
                paging.add(
                    button("Anteriores") {
                        galleryPage--
                        showGallery()
                    }
                )
            if ((galleryPage + 1) * 12 < files.size)
                paging.add(
                    button("Más dibujos") {
                        galleryPage++
                        showGallery()
                    }
                )
            if (paging.isNotEmpty()) body.addFull(columns(paging))
        }
        prompt("gallery_help")
    }

    private fun showDrawing(name: String) {
        val file = store.gallery().firstOrNull { it.name == name } ?: return showGallery()
        selectedDrawing = name
        shell("drawing", "Mi obra de arte")
        val image =
            ImageView(this).apply {
                setImageBitmap(BitmapFactory.decodeFile(file.path))
                adjustViewBounds = true
                scaleType = ImageView.ScaleType.FIT_CENTER
                contentDescription = "Tu dibujo guardado"
                tag = "saved_drawing"
            }
        body.addFull(image)
        body.gap()
        body.addFull(button("Volver a mis dibujos", "back_gallery", Palette.sky) { showGallery() })
        promptKeys = emptyArray()
    }

    private fun gate(onSuccess: () -> Unit) {
        val a = Random.nextInt(6, 10)
        val b = Random.nextInt(6, 10)
        val input =
            EditText(this).apply {
                inputType = InputType.TYPE_CLASS_NUMBER
                hint = "Respuesta"
                tag = "gate_answer"
                setSingleLine()
                setPadding(dp(22), dp(12), dp(22), dp(12))
            }
        val dialog =
            AlertDialog.Builder(this)
                .setTitle("Para adultos")
                .setMessage("¿Cuánto es $a + $b?")
                .setView(input)
                .setNegativeButton("Volver", null)
                .setPositiveButton("Entrar", null)
                .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                if (input.text.toString().toIntOrNull() == a + b) {
                    dialog.dismiss()
                    onSuccess()
                } else input.error = "Revisa la respuesta"
            }
        }
        dialog.show()
    }

    private fun showParent() {
        shell("parent", "Un espacio para ti", "Acompaña sus pequeños descubrimientos.")
        promptKeys = emptyArray()
        audio.stopVoice()
        val voice =
            Switch(this).apply {
                text = "Voz e indicaciones"
                textSize = 18f
                setTextColor(Palette.ink)
                isChecked = store.voice
                minimumHeight = dp(64)
                tag = "voice_setting"
                setOnCheckedChangeListener { _, checked ->
                    store.voice = checked
                    if (!checked) audio.stopVoice()
                }
            }
        val music =
            Switch(this).apply {
                text = "Música suave"
                textSize = 18f
                setTextColor(Palette.ink)
                isChecked = store.music
                minimumHeight = dp(64)
                setOnCheckedChangeListener { _, checked ->
                    store.music = checked
                    audio.updateMusic()
                }
            }
        body.addFull(voice)
        body.addFull(music)
        body.gap()
        body.addFull(text("Dificultad", 19f, bold = true))
        val difficulty =
            Spinner(this).apply {
                adapter =
                    ArrayAdapter(
                        this@MainActivity,
                        android.R.layout.simple_spinner_dropdown_item,
                        listOf(
                            "Inicio: 2 opciones, 2 parejas, 2 piezas",
                            "Más retos: 3 opciones, 3 parejas, 4 piezas",
                        ),
                    )
                setSelection(store.difficulty - 2)
            }
        body.addFull(difficulty, 60)
        difficulty.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent: AdapterView<*>?) = Unit

                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long,
                ) {
                    if (store.difficulty != position + 2) {
                        store.difficulty = position + 2
                        learning = null
                        memory = null
                        puzzle = null
                    }
                }
            }
        body.gap()
        body.addFull(text("Recordatorio de descanso", 19f, bold = true))
        val times = listOf(5, 10, 15, 20)
        val timer =
            Spinner(this).apply {
                adapter =
                    ArrayAdapter(
                        this@MainActivity,
                        android.R.layout.simple_spinner_dropdown_item,
                        times.map { "$it minutos de juego" },
                    )
                setSelection(times.indexOf(store.breakMinutes).coerceAtLeast(0))
            }
        body.addFull(timer, 60)
        timer.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent: AdapterView<*>?) = Unit

                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long,
                ) {
                    if (store.breakMinutes != times[position]) {
                        store.breakMinutes = times[position]
                        clock.reset(SystemClock.elapsedRealtime())
                        store.elapsed = 0
                    }
                }
            }
        body.gap()
        body.addFull(text("Sus descubrimientos", 21f, bold = true))
        body.gap(8)
        listOf(
                "Colores" to "colors",
                "Formas" to "shapes",
                "Números" to "numbers",
                "Animales" to "animals",
                "Parejas" to "memory",
                "Rompecabezas" to "puzzle",
            )
            .forEach { (label, key) ->
                body.addFull(
                    text(
                        "$label: ${store.successes(key)} actividades completadas",
                        17f,
                        Palette.muted,
                    )
                )
            }
        body.gap()
        body.addFull(
            text("Sus dibujos y avances se guardan en este dispositivo.", 16f, Palette.muted)
        )
        body.gap(16)
        body.addFull(
            button("Empezar una nueva sesión", "new_session", Palette.mint) {
                clock.reset(SystemClock.elapsedRealtime())
                store.elapsed = 0
                showHome()
            }
        )
    }

    private fun showBreak() {
        shell("break", "Hora de descansar", "Milo también necesita una pausa.")
        body.addFull(ArtView(this, "sleep", "Milo descansa"), 220)
        body.gap()
        body.addFull(
            text("Vamos a estirarnos, mirar alrededor y jugar un rato fuera de la pantalla.", 22f)
                .apply { gravity = Gravity.CENTER }
        )
        body.gap(24)
        body.addFull(
            button("Volver con un adulto", "resume_session", Palette.mint) {
                gate {
                    clock.reset(SystemClock.elapsedRealtime())
                    store.elapsed = 0
                    showHome()
                }
            }
        )
        prompt("break_time")
    }
}
