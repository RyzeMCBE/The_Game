package com.ryzemc.pequenomundo

import java.io.Serializable
import kotlin.random.Random

enum class LearningKind {
    COLORS,
    SHAPES,
    NUMBERS,
    ANIMALS,
}

/** The guided route advances only after an entire activity has been completed. */
enum class GameActivity {
    COLORS,
    SHAPES,
    NUMBERS,
    ANIMALS,
    MEMORY,
    PUZZLE,
    SHADOWS,
    SIZES,
    SORTING,
    PATHS,
    PAINTING;

    fun next(): GameActivity =
        if (this == PAINTING) PAINTING else guided[(guided.indexOf(this) + 1) % guided.size]

    companion object {
        val guided
            get() = entries.filter { it != PAINTING }

        fun from(kind: LearningKind): GameActivity = valueOf(kind.name)
    }
}

/** Joins independent audio and animation completions, with exactly one transition. */
class RewardGate {
    private var audioDone = false
    private var animationDone = false
    private var closed = false

    fun audioFinished(): Boolean {
        audioDone = true
        return consumeIfReady()
    }

    fun animationFinished(): Boolean {
        animationDone = true
        return consumeIfReady()
    }

    fun cancel() {
        closed = true
    }

    private fun consumeIfReady(): Boolean {
        if (closed || !audioDone || !animationDone) return false
        closed = true
        return true
    }
}

data class LearningItem(val id: String, val label: String, val art: String, val voice: String) :
    Serializable

object GameContent {
    val colors =
        listOf(
                "red" to "Rojo",
                "yellow" to "Amarillo",
                "blue" to "Azul",
                "green" to "Verde",
                "purple" to "Morado",
                "orange" to "Naranja",
                "pink" to "Rosa",
                "lightblue" to "Celeste",
                "brown" to "Marrón",
                "white" to "Blanco",
                "black" to "Negro",
                "gray" to "Gris",
            )
            .map { (id, label) -> LearningItem(id, label, "color_$id", id) }
    val shapes =
        listOf("circle" to "Círculo", "square" to "Cuadrado", "triangle" to "Triángulo").map {
            (id, label) ->
            LearningItem(id, label, id, id)
        }
    val numbers = (1..10).map { LearningItem("$it", "$it", "number_$it", "count_$it") }
    val animals =
        listOf(
                "cow" to "Vaca",
                "dog" to "Perro",
                "cat" to "Gato",
                "bird" to "Pajarito",
                "duck" to "Pato",
                "pig" to "Cerdo",
                "sheep" to "Oveja",
                "goat" to "Cabra",
                "horse" to "Caballo",
                "hen" to "Gallina",
                "lion" to "León",
                "elephant" to "Elefante",
                "monkey" to "Mono",
                "frog" to "Rana",
                "owl" to "Búho",
            )
            .map { (id, label) -> LearningItem(id, label, id, id) }

    fun items(kind: LearningKind): List<LearningItem> =
        when (kind) {
            LearningKind.COLORS -> colors
            LearningKind.SHAPES -> shapes
            LearningKind.NUMBERS -> numbers
            LearningKind.ANIMALS -> animals
        }
}

/** The remaining bag and recent history are persisted, including across app upgrades. */
class DrawingRotation(
    private val categories: Map<String, String>,
    remainingIds: List<String>? = null,
    recentIds: List<String> = emptyList(),
    random: Random = Random.Default,
) {
    val remaining =
        (remainingIds ?: categories.keys.shuffled(random))
            .filter { it in categories }
            .distinct()
            .toMutableList()
    val recent = recentIds.filter { it in categories }.takeLast(20).toMutableList()

    fun choose(id: String): String {
        require(id in categories)
        remaining.remove(id)
        recent.remove(id)
        recent.add(id)
        while (recent.size > 20) recent.removeAt(0)
        return id
    }

    fun next(random: Random = Random.Default): String {
        require(categories.isNotEmpty())
        if (remaining.isEmpty()) remaining.addAll(categories.keys.shuffled(random))
        val lastCategory = recent.lastOrNull()?.let(categories::get)
        val fresh = remaining.filter { it !in recent }.ifEmpty { remaining.toList() }
        val candidate = fresh.firstOrNull { categories[it] != lastCategory } ?: fresh.first()
        return choose(candidate)
    }
}

enum class CountingMode {
    TOUCH,
    FEED,
    MATCH,
}

class CountingRound(
    val mode: CountingMode,
    val target: Int,
    val options: List<Int>,
    val art: String = "apple",
) : Serializable {
    init {
        require(target in 1..10 && target in options && options.distinct().size == options.size)
    }

    val counted = mutableSetOf<Int>()
    var completed = false
        private set

    val totalItems
        get() = if (mode == CountingMode.FEED) 10 else target

    fun tap(index: Int): Int? {
        if (
            completed ||
                mode == CountingMode.MATCH ||
                index !in 0 until totalItems ||
                !counted.add(index)
        )
            return null
        if (counted.size == target) completed = true
        return counted.size
    }

    fun answer(number: Int): Boolean {
        if (completed || mode != CountingMode.MATCH || number != target || number !in options)
            return false
        completed = true
        return true
    }

    companion object {
        fun create(
            mode: CountingMode,
            maximum: Int,
            choices: Int,
            random: Random = Random.Default,
            previous: Int? = null,
        ): CountingRound {
            val limit = maximum.coerceIn(3, 10)
            val target = (1..limit).filter { it != previous }.random(random)
            val others =
                (1..limit).filter { it != target }.shuffled(random).take(choices.coerceIn(2, 3) - 1)
            return CountingRound(mode, target, (others + target).shuffled(random))
        }
    }
}

data class DiscoveryRound(
    val activity: GameActivity,
    val target: String,
    val options: List<String>,
    val art: String,
    var completed: Boolean = false,
) : Serializable {
    fun answer(id: String): Boolean {
        if (completed || id != target || id !in options) return false
        completed = true
        return true
    }

    companion object {
        fun create(activity: GameActivity, random: Random = Random.Default): DiscoveryRound {
            val animals =
                listOf(
                    "cat",
                    "elephant",
                    "giraffe",
                    "butterfly",
                    "fish",
                    "turtle",
                    "rabbit",
                    "bird",
                )
            val art = animals.random(random)
            return if (activity == GameActivity.SIZES)
                DiscoveryRound(
                    activity,
                    listOf("big", "small").random(random),
                    listOf("big", "small").shuffled(random),
                    art,
                )
            else {
                require(activity == GameActivity.SHADOWS)
                val options = animals.shuffled(random).take(3)
                DiscoveryRound(activity, options.random(random), options, options.first())
            }
        }
    }
}

class SortingRound(val byShape: Boolean, val bins: List<String>, val items: List<String>) :
    Serializable {
    init {
        require(bins.size == 2 && items.size == 4 && items.all { it in bins })
    }

    val placed = mutableSetOf<Int>()
    var selected = -1
    val completed
        get() = placed.size == items.size

    fun select(index: Int): Boolean {
        if (index !in items.indices || index in placed || completed) return false
        selected = index
        return true
    }

    fun place(bin: String): Boolean {
        if (selected !in items.indices || selected in placed || items[selected] != bin) return false
        placed.add(selected)
        selected = -1
        return true
    }

    companion object {
        fun create(byShape: Boolean, random: Random = Random.Default): SortingRound {
            val bins =
                (if (byShape) listOf("circle", "square", "triangle")
                    else listOf("red", "yellow", "blue", "green", "purple", "orange"))
                    .shuffled(random)
                    .take(2)
            return SortingRound(byShape, bins, (bins + bins).shuffled(random))
        }
    }
}

class TracingGame(val route: Int) : Serializable {
    val points: List<Pair<Float, Float>> =
        (0..20).map { i ->
            val t = i / 20f
            val y =
                when (route % 4) {
                    0 -> 225f
                    1 -> 225f + 95f * kotlin.math.sin(t * Math.PI * 2).toFloat()
                    2 -> 290f - 140f * kotlin.math.sin(t * Math.PI).toFloat()
                    else -> 160f + 130f * t
                }
            (60f + 480f * t) to y
        }
    var reached = 0
        private set

    val completed
        get() = reached == points.size

    fun touch(x: Float, y: Float): Boolean {
        if (completed) return false
        val (tx, ty) = points[reached]
        if (kotlin.math.hypot(x - tx, y - ty) > 38f) return false
        reached++
        return true
    }
}

data class LearningRound(
    val kind: LearningKind,
    val target: LearningItem,
    val options: List<LearningItem>,
    var completed: Boolean = false,
) : Serializable {
    fun answer(id: String): Boolean {
        if (completed || options.none { it.id == id } || target.id != id) return false
        completed = true
        return true
    }

    companion object {
        fun create(
            kind: LearningKind,
            choiceCount: Int,
            random: Random = Random.Default,
            previousId: String? = null,
        ): LearningRound {
            val pool = GameContent.items(kind)
            val target = pool.filter { it.id != previousId }.random(random)
            val others =
                pool
                    .filter { it.id != target.id }
                    .shuffled(random)
                    .take(choiceCount.coerceIn(2, 3) - 1)
            return LearningRound(kind, target, (others + target).shuffled(random))
        }
    }
}

enum class MemoryResult {
    IGNORED,
    REVEALED,
    MATCH,
    MISMATCH,
    FINISHED,
}

class MemoryGame private constructor(val cards: List<String>) : Serializable {
    val matched = mutableSetOf<Int>()
    val faceUp = mutableListOf<Int>()
    val finished: Boolean
        get() = matched.size == cards.size

    val waiting: Boolean
        get() = faceUp.size == 2 && cards[faceUp[0]] != cards[faceUp[1]]

    fun tap(index: Int): MemoryResult {
        if (index !in cards.indices || finished || waiting || index in matched || index in faceUp)
            return MemoryResult.IGNORED
        faceUp.add(index)
        if (faceUp.size == 1) return MemoryResult.REVEALED
        if (cards[faceUp[0]] != cards[faceUp[1]]) return MemoryResult.MISMATCH
        matched.addAll(faceUp)
        faceUp.clear()
        return if (finished) MemoryResult.FINISHED else MemoryResult.MATCH
    }

    fun concealMismatch() {
        if (waiting) faceUp.clear()
    }

    companion object {
        fun create(pairs: Int, random: Random = Random.Default): MemoryGame {
            val selected =
                GameContent.animals.map { it.art }.shuffled(random).take(pairs.coerceIn(2, 3))
            return MemoryGame((selected + selected).shuffled(random))
        }
    }
}

class PuzzleGame(val pieceCount: Int, val art: String, random: Random = Random.Default) :
    Serializable {
    init {
        require(pieceCount == 2 || pieceCount == 4)
    }

    val order = (0 until pieceCount).shuffled(random)
    val placed = mutableSetOf<Int>()
    val finished: Boolean
        get() = placed.size == pieceCount

    fun place(piece: Int, slot: Int): Boolean {
        if (piece !in 0 until pieceCount || piece in placed || piece != slot) return false
        placed.add(piece)
        return true
    }
}

/** Counts foreground play time only. A paused app never consumes a child's session. */
class PlayClock(initialMillis: Long = 0) {
    private var accumulated = initialMillis.coerceAtLeast(0)
    private var startedAt: Long? = null

    fun resume(now: Long) {
        if (startedAt == null) startedAt = now
    }

    fun pause(now: Long) {
        startedAt?.let { accumulated += (now - it).coerceAtLeast(0) }
        startedAt = null
    }

    fun elapsed(now: Long): Long =
        accumulated + (startedAt?.let { (now - it).coerceAtLeast(0) } ?: 0)

    fun reset(now: Long) {
        accumulated = 0
        if (startedAt != null) startedAt = now
    }

    fun expired(now: Long, minutes: Int): Boolean = minutes > 0 && elapsed(now) >= minutes * 60_000L
}
