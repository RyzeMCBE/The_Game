package com.ryzemc.pequenomundo

import kotlin.random.Random
import org.junit.Assert.*
import org.junit.Test

class GameEngineTest {
    @Test
    fun roundsHaveOneAnswerAndRespectDifficulty() {
        for (kind in LearningKind.entries) for (count in 2..3) for (seed in 0..99) {
            val round = LearningRound.create(kind, count, Random(seed))
            assertEquals(count, round.options.size)
            assertEquals(count, round.options.map { it.id }.toSet().size)
            assertEquals(1, round.options.count { it.id == round.target.id })
            assertFalse(round.answer("missing"))
            round.options
                .filter { it.id != round.target.id }
                .forEach { assertFalse(round.answer(it.id)) }
            assertTrue(round.answer(round.target.id))
            assertFalse("one reward per round", round.answer(round.target.id))
        }
    }

    @Test
    fun roundsVaryAndDoNotRepeatThePreviousTarget() {
        for (kind in LearningKind.entries) {
            val seen = (0..100).map { LearningRound.create(kind, 3, Random(it)).target.id }.toSet()
            assertEquals(GameContent.items(kind).size, seen.size)
            for (previous in seen) assertNotEquals(
                previous,
                LearningRound.create(kind, 2, Random(7), previous).target.id,
            )
        }
    }

    @Test
    fun memoryWaitsForMismatchAndCanBeCompleted() {
        val game = MemoryGame.create(3, Random(5))
        assertEquals(6, game.cards.size)
        assertTrue(game.cards.groupingBy { it }.eachCount().values.all { it == 2 })
        val different = game.cards.indices.first { game.cards[it] != game.cards[0] }
        assertEquals(MemoryResult.REVEALED, game.tap(0))
        assertEquals(MemoryResult.IGNORED, game.tap(0))
        assertEquals(MemoryResult.MISMATCH, game.tap(different))
        assertEquals(
            MemoryResult.IGNORED,
            game.tap(game.cards.indices.first { it != 0 && it != different }),
        )
        game.concealMismatch()
        game.cards
            .withIndex()
            .groupBy { it.value }
            .values
            .forEachIndexed { index, pair ->
                assertEquals(MemoryResult.REVEALED, game.tap(pair[0].index))
                assertEquals(
                    if (index == 2) MemoryResult.FINISHED else MemoryResult.MATCH,
                    game.tap(pair[1].index),
                )
            }
        assertTrue(game.finished)
        assertEquals(MemoryResult.IGNORED, game.tap(0))
    }

    @Test
    fun puzzlesOnlyAcceptTheMatchingSlotAndRewardOnce() {
        for (count in listOf(2, 4)) {
            val game = PuzzleGame(count, "bear", Random(8))
            assertEquals((0 until count).toSet(), game.order.toSet())
            assertFalse(game.place(-1, -1))
            assertFalse(game.place(0, 1))
            for (piece in game.order) {
                assertTrue(game.place(piece, piece))
                assertFalse(game.place(piece, piece))
            }
            assertTrue(game.finished)
        }
    }

    @Test
    fun clockExcludesBackgroundAndResetsWithoutLosingRunningState() {
        val clock = PlayClock()
        clock.resume(0)
        clock.resume(50)
        clock.pause(120_000)
        assertEquals(120_000L, clock.elapsed(900_000))
        clock.resume(900_000)
        assertTrue(clock.expired(1_080_000, 5))
        clock.reset(1_080_000)
        assertFalse(clock.expired(1_080_000, 5))
        assertEquals(10_000L, clock.elapsed(1_090_000))
        clock.pause(1_090_000)
        assertEquals(10_000L, clock.elapsed(2_000_000))
    }
}
