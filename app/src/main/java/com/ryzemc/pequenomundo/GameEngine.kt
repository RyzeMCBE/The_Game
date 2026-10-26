package com.ryzemc.pequenomundo

import java.io.Serializable
import kotlin.random.Random

enum class LearningKind {
    COLORS,
    SHAPES,
    NUMBERS,
    ANIMALS,
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
            )
            .map { (id, label) -> LearningItem(id, label, "color_$id", id) }
    val shapes =
        listOf("circle" to "Círculo", "square" to "Cuadrado", "triangle" to "Triángulo").map {
            (id, label) ->
            LearningItem(id, label, id, id)
        }
    val numbers = (1..5).map { LearningItem("$it", "$it", "number_$it", "number_$it") }
    val animals =
        listOf(
                "cow" to "Vaca",
                "dog" to "Perro",
                "cat" to "Gato",
                "bird" to "Pajarito",
                "duck" to "Pato",
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
