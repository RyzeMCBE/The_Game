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
    private var counting: CountingRound? = null
    private var countingPanel: CountingPanel? = null
    private var discovery: DiscoveryRound? = null
    private var sorting: SortingRound? = null
    private var tracing: TracingGame? = null
    private var albumCategory = "all"
    private var albumPage = 0
    private var chosenColor = "green"
    private var chosenBrush = 20f
    private var erasing = false

    private data class Retained(
        val painting: Painting?,
        val learning: LearningRound?,
        val memory: MemoryGame?,
        val puzzle: PuzzleGame?,
        val counting: CountingRound?,
        val discovery: DiscoveryRound?,
        val sorting: SortingRound?,
        val tracing: TracingGame?,
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
        DrawingCatalog.load(this)
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
        @Suppress("DEPRECATION")
        counting = retained?.counting ?: state?.getSerializable("counting") as? CountingRound
        @Suppress("DEPRECATION")
        discovery = retained?.discovery ?: state?.getSerializable("discovery") as? DiscoveryRound
        @Suppress("DEPRECATION")
        sorting = retained?.sorting ?: state?.getSerializable("sorting") as? SortingRound
        @Suppress("DEPRECATION")
        tracing = retained?.tracing ?: state?.getSerializable("tracing") as? TracingGame
        albumCategory = state?.getString("album_category") ?: "all"
        albumPage = state?.getInt("album_page") ?: 0
        chosenColor = state?.getString("paint_color")?.takeIf { it in Palette.colors } ?: "green"
        chosenBrush = state?.getFloat("paint_brush", 20f) ?: 20f
        erasing = state?.getBoolean("paint_eraser") ?: false
        selectedDrawing = state?.getString("drawing")
        root =
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                background = StorybookBackground()
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
            "album" -> showAlbum()
            "count_menu" -> showCountingMenu()
            "counting" -> showCounting(false)
            "discovery" -> showDiscovery(discovery?.activity ?: GameActivity.SHADOWS, false)
            "sorting" -> showSorting(false)
            "paths" -> showPaths(false)
            "animal_explore" -> showAnimalExplore()
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
        if (state?.getString("pending_reward") == GameActivity.PAINTING.name && screen == "paint")
            celebrate(GameActivity.PAINTING)
    }

    override fun onResume() {
        super.onResume()
        foreground = true
        audio.resume()
        countingPanel?.resumeInteraction()
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
        out.putSerializable("counting", counting)
        out.putSerializable("discovery", discovery)
        out.putSerializable("sorting", sorting)
        out.putSerializable("tracing", tracing)
        out.putString("album_category", albumCategory)
        out.putInt("album_page", albumPage)
        out.putString("paint_color", chosenColor)
        out.putFloat("paint_brush", chosenBrush)
        out.putBoolean("paint_eraser", erasing)
        out.putString("pending_reward", pendingReward?.name)
        super.onSaveInstanceState(out)
    }

    @Deprecated("Activity retained data is used only for the small native screen state")
    override fun onRetainNonConfigurationInstance(): Any =
        Retained(painting, learning, memory, puzzle, counting, discovery, sorting, tracing)

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
            "paint" -> showAlbum()
            "counting" -> showCountingMenu()
            "count_menu",
            "animal_explore" -> showLearn()
            "discovery",
            "sorting",
            "paths" -> showPlay()
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

    private fun rounded(fill: Int, radius: Int = 28) =
        GradientDrawable().apply {
            setColor(fill)
            cornerRadius = dp(radius).toFloat()
            setStroke(dp(1), Palette.outline)
        }

    /** Subtle watercolor gradients for cards; no bitmap assets or network. */
    private fun storyPanel(first: Int, second: Int = Palette.paper, radius: Int = 28) =
        GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(first, second)).apply {
            cornerRadius = dp(radius).toFloat()
            setStroke(dp(1), Palette.outline)
        }

    private fun clickable(view: View, fill: Int = Color.WHITE, action: () -> Unit): View {
        view.background = RippleDrawable(
            ColorStateList.valueOf(0x225c82ad), storyPanel(fill, Palette.paper), null
        )
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
        countingPanel = null
        root.removeAllViews()
        val now = SystemClock.elapsedRealtime()
        if (route == "parent" || route == "break") clock.pause(now)
        else if (foreground) clock.resume(now)
        val header =
            LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, dp(6), 0, dp(8))
            }
        if (route != "home")
            header.addView(
                button("Inicio", "home", Palette.sky) {
                        if (screen == "break")
                            gate {
                                clock.reset(SystemClock.elapsedRealtime())
                                store.elapsed = 0
                                showHome()
                            }
                        else showHome()
                    }
                    .apply { textSize = 14f },
                LinearLayout.LayoutParams(dp(67), dp(48)),
            )
        header.addView(
            text(if (route == "home") "El mundo de Noah" else "Noah y Milo", 18f, bold = true)
                .apply { gravity = Gravity.CENTER },
            LinearLayout.LayoutParams(0, dp(54), 1f),
        )
        header.addView(
            button("♪", "repeat", Palette.sky) { audio.say(*promptKeys) }
                .apply { contentDescription = "Escuchar a Milo" },
            LinearLayout.LayoutParams(dp(48), dp(48)),
        )
        header.addView(
            button("Familia", "family", Palette.mint) { gate { showParent() } }
                .apply { textSize = 14f },
            LinearLayout.LayoutParams(dp(73), dp(48)).apply { leftMargin = dp(6) },
        )
        root.addFull(header)
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
            val artId = when (route) {
                "album", "paint", "gallery", "drawing" -> "paint"
                "learn", "learning", "animal_explore" -> "rainbow"
                "count_menu", "counting" -> "apple"
                "paths" -> "duck"
                "sorting", "play", "puzzle", "memory", "discovery" -> "play"
                "break" -> "sleep"
                else -> "star"
            }
            val introduction = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(17), dp(15), dp(14), dp(15))
                background = storyPanel(Palette.sky, Palette.paper, 30)
                elevation = dp(2).toFloat()
                tag = "storybook_header"
            }
            introduction.addView(
                LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    addFull(text(title, 25f, bold = true))
                    if (subtitle.isNotEmpty()) {
                        gap(4)
                        addFull(text(subtitle, 15f, Palette.muted))
                    }
                },
                LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f),
            )
            introduction.addView(
                FrameLayout(this).apply {
                    background = rounded(Palette.butter, 22)
                    addView(
                        ArtView(this@MainActivity, artId),
                        FrameLayout.LayoutParams(dp(66), dp(66), Gravity.CENTER),
                    )
                },
                LinearLayout.LayoutParams(dp(76), dp(76)).apply { leftMargin = dp(8) },
            )
            body.addFull(introduction)
            body.gap(15)
        }
    }

    /** Friendly illustrated activity card reused by all game menus. */
    private fun card(
        title: String,
        description: String,
        art: String,
        fill: Int,
        tagName: String,
        vertical: Boolean = false,
        action: () -> Unit,
    ): View {
        val item = LinearLayout(this).apply {
            orientation = if (vertical) LinearLayout.VERTICAL else LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(12), dp(12), dp(12))
            minimumHeight = dp(if (vertical) 190 else 106)
            tag = tagName
            contentDescription = title + ". " + description
            elevation = dp(2).toFloat()
            clipToOutline = true
        }
        item.addView(
            FrameLayout(this).apply {
                background = storyPanel(Palette.paper, fill, 25)
                addView(
                    ArtView(this@MainActivity, art),
                    FrameLayout.LayoutParams(dp(76), dp(76), Gravity.CENTER),
                )
            },
            LinearLayout.LayoutParams(dp(if (vertical) 108 else 88), dp(if (vertical) 108 else 88))
                .apply { if (vertical) gravity = Gravity.CENTER_HORIZONTAL },
        )
        val labels = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(if (vertical) 0 else dp(13), 0, 0, 0)
            addFull(text(title, 22f, bold = true).apply {
                if (vertical) gravity = Gravity.CENTER
            })
            gap(3)
            addFull(text(description, 14f, Palette.muted).apply {
                if (vertical) gravity = Gravity.CENTER
            })
        }
        item.addView(labels, LinearLayout.LayoutParams(
            if (vertical) ViewGroup.LayoutParams.MATCH_PARENT else 0,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            if (vertical) 0f else 1f,
        ))
        if (!vertical) item.addView(
            text("›", 31f, Palette.ink, true).apply {
                gravity = Gravity.CENTER
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            },
            LinearLayout.LayoutParams(dp(21), dp(48)),
        )
        return clickable(item, fill, action)
    }

    /**
     * A home-only card: the artwork occupies its own illustrated medallion and
     * the whole surface remains a single accessible tap target.
     */
    private fun homeAdventure(
        title: String,
        description: String,
        art: String,
        fill: Int,
        tagName: String,
        vertical: Boolean,
        action: () -> Unit,
    ): View {
        val item =
            LinearLayout(this).apply {
                orientation = if (vertical) LinearLayout.VERTICAL else LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(14), dp(12), dp(14), dp(12))
                minimumHeight = dp(if (vertical) 190 else 106)
                tag = tagName
                contentDescription = "$title. $description"
                elevation = dp(2).toFloat()
                clipToOutline = true
            }
        val artSize = if (vertical) 104 else 88
        item.addView(
            HomeAdventureArtView(this, art),
            LinearLayout.LayoutParams(dp(artSize), dp(artSize)).apply {
                if (vertical) gravity = Gravity.CENTER_HORIZONTAL
            },
        )
        val labels = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = if (vertical) Gravity.CENTER else Gravity.CENTER_VERTICAL
            setPadding(if (vertical) 0 else dp(12), 0, 0, 0)
            addFull(
                text(title, if (vertical) 23f else 24f, bold = true).apply {
                    if (vertical) gravity = Gravity.CENTER
                }
            )
            addFull(
                text(description, if (vertical) 13f else 14f, Palette.muted).apply {
                    if (vertical) gravity = Gravity.CENTER
                }
            )
        }
        item.addView(
            labels,
            LinearLayout.LayoutParams(
                if (vertical) ViewGroup.LayoutParams.MATCH_PARENT else 0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                if (vertical) 0f else 1f,
            ),
        )
        if (!vertical) {
            item.addView(
                text("›", 34f, Palette.ink, bold = true).apply {
                    gravity = Gravity.CENTER
                    contentDescription = null
                    importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                },
                LinearLayout.LayoutParams(dp(24), dp(48)),
            )
        }
        return clickable(item, fill, action)
    }

    private fun showHome() {
        shell("home", "", "")
        val hero =
            FrameLayout(this).apply {
                background = rounded(Palette.sky, 36)
                elevation = dp(3).toFloat()
                clipToOutline = true
                tag = "home_hero"
                addView(
                    MiloHeroView(this@MainActivity),
                    FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(198)),
                )
                addView(
                    LinearLayout(this@MainActivity).apply {
                        orientation = LinearLayout.VERTICAL
                        gravity = Gravity.CENTER_VERTICAL
                        setPadding(dp(18), dp(7), 0, dp(7))
                        addFull(text("¡Hola, Noah!", 30f, bold = true))
                        gap(5)
                        addFull(text("¡Hoy tenemos una nueva aventura!", 16f, Palette.muted))
                        gap(10)
                        addFull(
                            text("PINTA  •  JUEGA  •  DESCUBRE", 11f, Palette.green, bold = true)
                        )
                    },
                    FrameLayout.LayoutParams(
                        dp(((resources.configuration.screenWidthDp - 32) * .58f).toInt().coerceAtLeast(150)),
                        dp(198),
                        Gravity.START or Gravity.CENTER_VERTICAL,
                    ),
                )
            }
        body.addFull(hero, 198)
        body.gap(17)
        body.addFull(text("Elige tu aventura", 23f, bold = true))
        body.gap(3)
        body.addFull(text("Tres mundos para explorar", 15f, Palette.muted))
        body.gap(13)
        val wide = resources.configuration.screenWidthDp >= 650
        val choices =
            listOf(
                homeAdventure("Pintar", "Mi taller de colores", "paint", Palette.peach, "paint", wide) {
                    showAlbum()
                },
                homeAdventure("Jugar", "Diversión y sorpresas", "play", Palette.lilac, "play", wide) {
                    showPlay()
                },
                homeAdventure(
                    "Aprender", "Descubro cosas nuevas", "rainbow",
                    Palette.mint, "learn", wide,
                ) {
                    showLearn()
                },
            )
        if (wide) body.addFull(columns(choices, 206))
        else
            choices.forEach {
                body.addFull(it)
                body.gap(11)
            }
        body.gap(5)
        body.addFull(
            button("Mis dibujos guardados", "gallery", Palette.sky) { showGallery() }.apply {
                textSize = 16f
                elevation = dp(2).toFloat()
            }
        )
        body.gap(12)
        body.addFull(
            text("Con Milo, cada día es una aventura ✦", 14f, Palette.muted).apply {
                gravity = Gravity.CENTER
            }
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
        body.gap()
        listOf(
                Triple(GameActivity.SHADOWS, "Encuentra la sombra", "cat"),
                Triple(GameActivity.SIZES, "Grande y pequeño", "elephant"),
                Triple(GameActivity.SORTING, "Cada cosa en su cesta", "play"),
                Triple(GameActivity.PATHS, "Caminos mágicos", "duck"),
            )
            .forEach { (activity, title, art) ->
                body.addFull(
                    card(
                        title,
                        when (activity) {
                            GameActivity.SHADOWS -> "Une el animal con su silueta"
                            GameActivity.SIZES -> "Descubre los tamaños"
                            GameActivity.SORTING -> "Toca una pieza y su cesta"
                            else -> "Sigue el camino con tu dedo"
                        },
                        art,
                        when (activity) {
                            GameActivity.SHADOWS -> Palette.sky
                            GameActivity.SIZES -> Palette.peach
                            GameActivity.SORTING -> Palette.mint
                            else -> Palette.lilac
                        },
                        activity.name.lowercase(),
                    ) {
                        showGame(activity)
                    }
                )
                body.gap()
            }
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
            card("Contamos con Milo", "Tres juegos del 1 al 10", "apple", Palette.mint, "numbers") {
                showCountingMenu()
            }
        )
        body.gap()
        body.addFull(
            card(
                "Conoce los animales",
                "15 amigos con sonidos reales",
                "elephant",
                Palette.sky,
                "animal_explore",
            ) {
                showAnimalExplore()
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
        if (kind == LearningKind.NUMBERS) {
            showCounting(fresh)
            return
        }
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
                        setPadding(dp(8), dp(10), dp(8), dp(10))
                        elevation = dp(2).toFloat()
                        tag = "answer_${item.id}"
                        contentDescription = item.label
                    }
                tile.addView(
                    FrameLayout(this).apply {
                        background = storyPanel(Palette.sky, Palette.paper, 24)
                        addView(
                            ArtView(this@MainActivity, item.art),
                            FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT,
                            ),
                        )
                    },
                    LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(102)),
                )
                tile.gap(8)
                tile.addFull(text(item.label, 17f, bold = true).apply { gravity = Gravity.CENTER })
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
                tile.background = storyPanel(
                    if (index in game.matched) Palette.mint
                    else if (visible) Palette.paper else Palette.lilac,
                    Palette.paper,
                    28,
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
                            setPadding(dp(7), dp(7), dp(7), dp(7))
                            elevation = dp(2).toFloat()
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
            GameActivity.NUMBERS -> showCounting(true)
            GameActivity.SHADOWS,
            GameActivity.SIZES -> showDiscovery(activity, true)
            GameActivity.SORTING -> showSorting(true)
            GameActivity.PATHS -> showPaths(true)
            GameActivity.PAINTING -> {
                painting = Painting(store.nextDrawing())
                store.saveDraft(painting!!)
                showPaint()
            }
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

    /** An illustrated, paged bookshelf; only the selected sheet enters the painting view. */
    private fun showAlbum() {
        if (painting == null) painting = store.readDraft()
        shell("album", "Mi álbum para pintar", "¡Elige un dibujo y dale vida con colores!")
        val filters = listOf("all" to "Todos") + DrawingCatalog.categories
        val filterRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(2), dp(4), dp(2), dp(4))
        }
        filters.forEach { (id, label) ->
            filterRow.addView(
                button(
                    label, "category_" + id,
                    if (id == albumCategory) Palette.butter else Palette.paper,
                ) {
                    albumCategory = id
                    albumPage = 0
                    showAlbum()
                }.apply {
                    textSize = 14f
                    minHeight = dp(46)
                    elevation = if (id == albumCategory) dp(3).toFloat() else 0f
                    isSelected = id == albumCategory
                },
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(48)).apply {
                    rightMargin = dp(9)
                },
            )
        }
        body.addFull(HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            addView(filterRow)
            tag = "album_filters"
        })
        body.gap(12)
        body.addFull(
            button("✦  ¡Sorpréndeme con un dibujo!", "surprise_drawing", Palette.peach) {
                openSheet(store.nextDrawing(), register = false)
            }.apply { elevation = dp(3).toFloat() }
        )
        val draft = painting!!
        if (draft.marks.isNotEmpty()) {
            body.gap(9)
            body.addFull(
                button("✎  Continuar mi dibujo", "continue_paint", Palette.sky) { showPaint() }
            )
        }
        body.gap(15)
        val pages = store.drawingOrder().mapNotNull(DrawingCatalog::find).filter {
            albumCategory == "all" || it.category == albumCategory
        }
        albumPage = albumPage.coerceIn(0, ((pages.size - 1).coerceAtLeast(0)) / 12)
        body.addFull(text("Escoge tu favorito", 21f, bold = true))
        body.gap(10)
        val sheetColors = listOf(Palette.sky, Palette.peach, Palette.mint, Palette.lilac)
        pages.drop(albumPage * 12).take(12).chunked(2).forEachIndexed { rowIndex, entries ->
            val cards = entries.mapIndexed { column, sheet ->
                val shade = sheetColors[(albumPage * 12 + rowIndex * 2 + column) % sheetColors.size]
                LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                    setPadding(dp(9), dp(10), dp(9), dp(10))
                    tag = "sheet_" + sheet.id
                    contentDescription = "Pintar " + sheet.name
                    addFull(
                        FrameLayout(this@MainActivity).apply {
                            background = storyPanel(Palette.paper, shade, 21)
                            setPadding(dp(5), dp(5), dp(5), dp(5))
                            addView(SheetView(this@MainActivity, sheet, colored = true),
                                FrameLayout.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                ))
                        },
                        119,
                    )
                    gap(7)
                    addFull(text(sheet.name, 16f, bold = true).apply {
                        gravity = Gravity.CENTER
                        maxLines = 2
                    })
                    addFull(text("✦ Para pintar", 12f, Palette.green).apply {
                        gravity = Gravity.CENTER
                    })
                    clickable(this, shade) { openSheet(sheet.id) }
                    elevation = dp(3).toFloat()
                    clipToOutline = true
                }
            }
            body.addFull(columns(cards), 224)
            body.gap(12)
        }
        val navigation = mutableListOf<View>()
        if (albumPage > 0) navigation.add(
            button("‹ Anteriores", "album_previous", Palette.paper) {
                albumPage--
                showAlbum()
            }
        )
        if ((albumPage + 1) * 12 < pages.size) navigation.add(
            button("Más dibujos ›", "album_more", Palette.sky) {
                albumPage++
                showAlbum()
            }
        )
        if (navigation.isNotEmpty()) {
            body.gap(3)
            body.addFull(columns(navigation))
        }
        body.gap(17)
        body.addFull(
            button("Hoja en blanco", "blank_page", Palette.mint) {
                openSheet("blank", register = false)
            }
        )
        body.gap(9)
        body.addFull(button("Mis pinturas guardadas", "gallery", Palette.lilac) { showGallery() })
        prompt("album_help")
    }

    private fun openSheet(id: String, register: Boolean = true) {
        painting?.let(store::saveDraft)
        if (register && DrawingCatalog.find(id) != null) store.chooseDrawing(id)
        painting = store.readSheetDraft(id)
        erasing = false
        showPaint()
    }

    /** The painting workspace never contains the album, a spinner or other sheets. */
    private fun showPaint() {
        if (painting == null) painting = store.readDraft()
        shell("paint", "")
        root.removeAllViews()
        body =
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                tag = "screen_paint"
                setPadding(0, dp(4), 0, dp(6))
            }
        root.addView(
            body,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ),
        )
        val heading =
            LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                background = storyPanel(Palette.sky, Palette.paper, 24)
                setPadding(dp(5), dp(4), dp(5), dp(4))
                elevation = dp(2).toFloat()
            }
        heading.addView(
            button("Álbum", "album_back", Palette.sky) { showAlbum() }.apply { textSize = 15f },
            LinearLayout.LayoutParams(dp(76), dp(48)),
        )
        heading.addView(
            text("¡A pintar!", 19f, bold = true).apply { gravity = Gravity.CENTER },
            LinearLayout.LayoutParams(0, dp(48), 1f),
        )
        // The completion button lives in the upper toolbar, away from the drawing hand.
        heading.addView(
            button("¡Listo! ✓", "save", Palette.mint) {
                paintView?.let { finishPainting(it) }
            }.apply { textSize = 15f; contentDescription = "Terminar y guardar mi pintura" },
            LinearLayout.LayoutParams(dp(106), dp(48)),
        )
        // Reserve extra vertical inset so the top completion button never clips in landscape.
        body.addFull(heading, 64)
        body.gap(7)
        val canvas =
            PaintCanvasView(this, painting!!).apply {
                paintColor = Palette.colors.getValue(chosenColor)
                brushSize = chosenBrush
                tool = if (erasing) "eraser" else "brush"
                onChanged = {
                    painting = this.painting
                    store.saveDraft(this.painting)
                }
                background = rounded(Palette.paper, 24)
                clipToOutline = true
            }
        paintView = canvas
        val workspace =
            LinearLayout(this).apply {
                orientation =
                    if (
                        resources.configuration.screenWidthDp >
                            resources.configuration.screenHeightDp
                    )
                        LinearLayout.HORIZONTAL
                    else LinearLayout.VERTICAL
            }
        body.addView(
            workspace,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f),
        )
        val landscape = workspace.orientation == LinearLayout.HORIZONTAL
        val paper =
            FrameLayout(this).apply {
                background = storyPanel(Palette.butter, Palette.paper, 27)
                elevation = dp(4).toFloat()
                setPadding(dp(4), dp(4), dp(4), dp(4))
                addView(
                    canvas,
                    FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    ),
                )
            }
        workspace.addView(
            paper,
            if (landscape)
                LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f).apply {
                    rightMargin = dp(10)
                }
            else
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f).apply {
                    bottomMargin = dp(8)
                },
        )
        val controls = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = storyPanel(Palette.sky, Palette.paper, 25)
            setPadding(dp(7), dp(8), dp(7), dp(8))
            elevation = dp(2).toFloat()
            tag = "paint_tools"
        }
        val swatches = mutableMapOf<String, FrameLayout>()
        fun refreshColors() {
            swatches.forEach { (key, v) ->
                v.background =
                    GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(Palette.colors.getValue(key))
                        setStroke(
                            dp(if (key == chosenColor && !erasing) 4 else 1),
                            if (key == chosenColor && !erasing) Palette.ink else 0x6696a2af,
                        )
                    }
                (v.getChildAt(0) as TextView).text = if (key == chosenColor && !erasing) "✓" else ""
                v.isSelected = key == chosenColor && !erasing
            }
        }
        Palette.colors.entries.chunked(if (landscape) 4 else 6).forEach { entries ->
            val row =
                entries.map { (key, color) ->
                    FrameLayout(this).apply {
                        tag = "paint_$key"
                        contentDescription = GameContent.colors.first { it.id == key }.label
                        isClickable = true
                        isFocusable = true
                        val light =
                            Color.red(color) * .299 +
                                Color.green(color) * .587 +
                                Color.blue(color) * .114 > 155
                        addView(
                            text("", 25f, if (light) Palette.ink else Color.WHITE, true).apply {
                                gravity = Gravity.CENTER
                            },
                            FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT,
                            ),
                        )
                        setOnClickListener {
                            chosenColor = key
                            erasing = false
                            canvas.paintColor = color
                            canvas.tool = "brush"
                            refreshColors()
                            audio.say(key)
                        }
                        swatches[key] = this
                    }
                }
            controls.addFull(columns(row, 48))
            controls.gap(5)
        }
        refreshColors()
        val brushButtons = mutableListOf<Button>()
        fun refreshBrush() {
            brushButtons.forEachIndexed { i, v ->
                v.background =
                    rounded(
                        if (listOf(10f, 20f, 34f)[i] == chosenBrush && !erasing) Palette.mint
                        else Color.WHITE,
                        18,
                    )
            }
        }
        val tools =
            listOf(10f, 20f, 34f).mapIndexed { index, size ->
                button("●", "brush_$index") {
                        chosenBrush = size
                        erasing = false
                        canvas.brushSize = size
                        canvas.tool = "brush"
                        refreshBrush()
                        refreshColors()
                    }
                    .apply {
                        textSize = listOf(13f, 20f, 27f)[index]
                        setPadding(dp(6), dp(2), dp(6), dp(2))
                        contentDescription =
                            listOf("Pincel fino", "Pincel mediano", "Pincel grueso")[index]
                        brushButtons.add(this)
                    }
            } +
                listOf(
                    button("Borrar", "eraser") {
                            erasing = true
                            canvas.tool = "eraser"
                            refreshColors()
                            refreshBrush()
                        }
                        .apply {
                            textSize = 12f
                            contentDescription = "Borrador"
                        },
                    button("↶", "undo") { canvas.undo() }
                        .apply {
                            textSize = 27f
                            setPadding(dp(6), dp(2), dp(6), dp(2))
                            contentDescription = "Deshacer último trazo"
                        },
                )
        if (landscape) controls.addFull(columns(tools.take(3), 48))
        else controls.addFull(columns(tools, 48))
        refreshBrush()
        if (landscape) {
            workspace.addView(
                ScrollView(this).apply {
                    isVerticalScrollBarEnabled = false
                    addView(controls)
                },
                LinearLayout.LayoutParams(dp(216), ViewGroup.LayoutParams.MATCH_PARENT),
            )
            // Keep the eraser and undo visible; completion is above the canvas.
            body.gap(6)
            body.addFull(columns(listOf(tools[3], tools[4]), 52))
        } else {
            workspace.addFull(controls)
        }
        prompt(if (painting!!.template == "blank") "paint_help" else "border_help")
    }

    private fun finishPainting(canvas: PaintCanvasView) {
        if (saving) return
        saving = true
        val token = generation
        body.findViewWithTag<View>("save")?.isEnabled = false
        store.saveDraft(canvas.painting)
        store.savePicture(canvas.bitmap()) pictureSaved@{ success ->
            saving = false
            if (isDestroyed || isFinishing || token != generation || screen != "paint")
                return@pictureSaved
            body.findViewWithTag<View>("save")?.isEnabled = true
            if (success) {
                store.record("painting")
                celebrate(GameActivity.PAINTING)
            } else {
                Toast.makeText(this, "No se pudo guardar. Intenta otra vez.", Toast.LENGTH_SHORT)
                    .show()
                audio.say("save_error")
            }
        }
    }

    private fun showCountingMenu() {
        shell("count_menu", "Contamos con Milo", "Toca, reparte y descubre del 1 al 10.")
        body.addFull(text("Elegimos cuántos contar", 18f, bold = true))
        body.gap(8)
        body.addFull(
            columns(
                listOf(3, 5, 10).map { maximum ->
                    button(
                        "1–$maximum",
                        "range_$maximum",
                        if (store.countMaximum == maximum) Palette.mint else Color.WHITE,
                    ) {
                        store.countRange = maximum
                        showCountingMenu()
                    }
                }
            )
        )
        body.gap()
        listOf(
                Triple(CountingMode.TOUCH, "Cuenta tocando", "Toca las manzanas una por una"),
                Triple(CountingMode.FEED, "Dale de comer a Milo", "Pon la cantidad que te pida"),
                Triple(CountingMode.MATCH, "Encuentra el número", "Cuenta y elige el número"),
            )
            .forEach { (mode, title, description) ->
                body.addFull(
                    card(
                        title,
                        description,
                        if (mode == CountingMode.FEED) "bear" else "apple",
                        if (mode == CountingMode.FEED) Palette.peach else Palette.sky,
                        "count_mode_${mode.name.lowercase()}",
                    ) {
                        showCounting(true, mode)
                    }
                )
                body.gap()
            }
        body.addFull(
            button("Avanzar a mi ritmo", "range_auto", Palette.lilac) {
                store.countRange = 0
                showCountingMenu()
            }
        )
        prompt("count_touch")
    }

    private fun showCounting(fresh: Boolean, mode: CountingMode? = null) {
        if (fresh || counting == null)
            counting =
                CountingRound.create(
                    mode ?: CountingMode.entries[store.successes("numbers") % 3],
                    store.countMaximum,
                    store.difficulty,
                    previous = counting?.target,
                )
        val game = counting!!
        shell(
            "counting",
            when (game.mode) {
                CountingMode.TOUCH -> "Vamos a contar, Noah"
                CountingMode.FEED -> "${game.target} manzanas para Milo"
                CountingMode.MATCH -> "¿Cuántas manzanas hay?"
            },
        )
        val panel =
            CountingPanel(this, game, audio) {
                store.record("numbers")
                celebrate(GameActivity.NUMBERS, "total_${game.target}")
            }
        panel.tag = "count_panel"
        countingPanel = panel
        body.addFull(panel)
        when (game.mode) {
            CountingMode.TOUCH -> prompt("count_touch")
            CountingMode.FEED -> prompt("give_${game.target}")
            CountingMode.MATCH -> prompt("count_match")
        }
        if (game.completed) celebrate(GameActivity.NUMBERS, "total_${game.target}")
    }

    private fun showDiscovery(activity: GameActivity, fresh: Boolean) {
        if (fresh || discovery?.activity != activity) discovery = DiscoveryRound.create(activity)
        val game = discovery!!
        shell(
            "discovery",
            if (activity == GameActivity.SHADOWS) "Encuentra su sombra"
            else if (game.target == "big") "Toca el animal grande" else "Toca el animal pequeño",
        )
        body.tag = "screen_${activity.name.lowercase()}"
        body.addFull(
            DiscoveryPanel(this, game, audio) {
                    store.record(activity.name.lowercase())
                    celebrate(activity)
                }
                .apply { tag = "discovery_panel" }
        )
        prompt(if (activity == GameActivity.SHADOWS) "shadow_help" else "ask_${game.target}")
        if (game.completed) celebrate(activity)
    }

    private fun showSorting(fresh: Boolean) {
        if (fresh || sorting == null)
            sorting = SortingRound.create(store.successes("sorting") % 2 == 1)
        val game = sorting!!
        shell(
            "sorting",
            "Cada cosa en su cesta",
            if (game.byShape) "Vamos a ordenar las formas" else "Vamos a ordenar los colores",
        )
        body.addFull(
            SortingPanel(this, game, audio) {
                    store.record("sorting")
                    celebrate(GameActivity.SORTING)
                }
                .apply { tag = "sorting_panel" }
        )
        prompt("sorting_help")
        if (game.completed) celebrate(GameActivity.SORTING)
    }

    private fun showPaths(fresh: Boolean) {
        if (fresh || tracing == null) tracing = TracingGame(store.successes("paths") % 4)
        val game = tracing!!
        shell("paths", "Caminos mágicos", "Ayuda al patito siguiendo el camino.")
        val token = generation
        body.addFull(
            FingerPathView(this, game) {
                if (generation == token) {
                    store.record("paths")
                    celebrate(GameActivity.PATHS)
                }
            }.apply {
                background = storyPanel(Palette.sky, Palette.paper, 28)
                elevation = dp(2).toFloat()
            }
        )
        body.gap()
        body.addFull(
            text("Empieza en el círculo verde", 18f, Palette.green, true).apply {
                gravity = Gravity.CENTER
            }
        )
        prompt("paths_help")
        if (game.completed) celebrate(GameActivity.PATHS)
    }

    private fun showAnimalExplore() {
        shell("animal_explore", "Mis amigos animales", "Toca un amigo y escucha su sonido.")
        GameContent.animals.chunked(3).forEach { items ->
            val cards =
                items.map { item ->
                    LinearLayout(this).apply {
                        orientation = LinearLayout.VERTICAL
                        gravity = Gravity.CENTER
                        tag = "explore_${item.id}"
                        contentDescription = "Escuchar ${item.label}"
                        addFull(
                            FrameLayout(this@MainActivity).apply {
                                background = storyPanel(Palette.sky, Palette.paper, 24)
                                addView(ArtView(this@MainActivity, item.art),
                                    FrameLayout.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                    ))
                            },
                            90,
                        )
                        gap(5)
                        addFull(
                            text(item.label, 16f, bold = true).apply { gravity = Gravity.CENTER }
                        )
                        elevation = dp(2).toFloat()
                        clickable(this, Palette.paper) { audio.say(item.voice, "sound_${item.id}") }
                    }
                }
            body.addFull(columns(cards), 137)
            body.gap()
        }
        prompt("animal_explore")
    }

    /** Saved pictures remain private; the gallery is now a small picture museum. */
    private fun showGallery() {
        shell("gallery", "Mis obras de arte", "¡Mira todas las creaciones de Noah!")
        val files = store.gallery()
        if (files.isEmpty()) {
            body.addFull(
                FrameLayout(this).apply {
                    background = storyPanel(Palette.peach, Palette.paper, 32)
                    addView(
                        ArtView(this@MainActivity, "paint"),
                        FrameLayout.LayoutParams(dp(160), dp(160), Gravity.CENTER),
                    )
                    elevation = dp(2).toFloat()
                },
                188,
            )
            body.gap(12)
            body.addFull(
                text("¡Tu museo está esperando su primera obra!", 20f, Palette.muted).apply {
                    gravity = Gravity.CENTER
                }
            )
            body.gap(14)
            body.addFull(button("¡Vamos a pintar!", "paint", Palette.peach) { showAlbum() })
        } else {
            galleryPage = galleryPage.coerceIn(0, (files.size - 1) / 12)
            body.addFull(text("Tu colección de colores ✦", 21f, bold = true))
            body.gap(11)
            val shades = listOf(Palette.peach, Palette.sky, Palette.mint, Palette.lilac)
            files.drop(galleryPage * 12).take(12).chunked(2).forEachIndexed { rowIndex, entries ->
                val cells = entries.mapIndexed { column, file ->
                    val color = shades[(galleryPage * 12 + rowIndex * 2 + column) % shades.size]
                    LinearLayout(this).apply {
                        orientation = LinearLayout.VERTICAL
                        gravity = Gravity.CENTER
                        setPadding(dp(9), dp(10), dp(9), dp(10))
                        tag = "drawing_" + file.name
                        contentDescription = "Ver dibujo del " +
                            DateFormat.getDateInstance().format(Date(file.lastModified()))
                        val image = ImageView(this@MainActivity).apply {
                            setImageBitmap(BitmapFactory.decodeFile(
                                file.path,
                                BitmapFactory.Options().apply { inSampleSize = 4 },
                            ))
                            scaleType = ImageView.ScaleType.FIT_CENTER
                            background = rounded(Palette.paper, 18)
                            setPadding(dp(5), dp(5), dp(5), dp(5))
                            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                        }
                        addFull(image, 116)
                        gap(7)
                        addFull(
                            text("Mi obra ✦", 16f, bold = true).apply { gravity = Gravity.CENTER }
                        )
                        addFull(
                            text(
                                DateFormat.getDateInstance(DateFormat.MEDIUM)
                                    .format(Date(file.lastModified())),
                                12f, Palette.muted,
                            ).apply { gravity = Gravity.CENTER }
                        )
                        clickable(this, color) { showDrawing(file.name) }
                        elevation = dp(3).toFloat()
                    }
                }
                body.addFull(columns(cells), 198)
                body.gap(12)
            }
            val paging = mutableListOf<View>()
            if (galleryPage > 0) paging.add(
                button("‹ Anteriores", "gallery_previous", Palette.paper) {
                    galleryPage--
                    showGallery()
                }
            )
            if ((galleryPage + 1) * 12 < files.size) paging.add(
                button("Más dibujos ›", "gallery_more", Palette.sky) {
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
        shell("drawing", "¡Mi obra de arte!", "Una creación especial de Noah")
        body.addFull(
            FrameLayout(this).apply {
                background = storyPanel(Palette.butter, Palette.paper, 30)
                setPadding(dp(10), dp(10), dp(10), dp(10))
                elevation = dp(3).toFloat()
                val picture = ImageView(this@MainActivity).apply {
                    setImageBitmap(BitmapFactory.decodeFile(file.path))
                    adjustViewBounds = true
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    background = rounded(Color.WHITE, 20)
                    contentDescription = "Tu dibujo guardado"
                    tag = "saved_drawing"
                }
                addView(
                    picture,
                    FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT),
                )
            },
        )
        body.gap(15)
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
                "Sombras" to "shadows",
                "Tamaños" to "sizes",
                "Clasificar" to "sorting",
                "Caminos" to "paths",
                "Pinturas" to "painting",
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
            button("Créditos de dibujos y sonidos", "credits", Palette.sky) {
                val content = assets.open("credits.txt").bufferedReader().use { it.readText() }
                AlertDialog.Builder(this)
                    .setTitle("Créditos")
                    .setView(
                        ScrollView(this).apply {
                            addView(
                                text(content, 15f).apply {
                                    setPadding(dp(20), dp(12), dp(20), dp(12))
                                }
                            )
                        }
                    )
                    .setPositiveButton("Cerrar", null)
                    .show()
            }
        )
        body.gap(12)
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
