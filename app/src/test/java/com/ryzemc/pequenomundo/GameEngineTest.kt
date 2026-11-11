package com.ryzemc.pequenomundo

import kotlin.random.Random
import org.junit.Assert.*
import org.junit.Test

class GameEngineTest {
    @Test
    fun guidedRouteVisitsEveryActivityAndWraps() {
        val route = generateSequence(GameActivity.COLORS) { it.next() }.take(11).toList()
        assertEquals(
            listOf(
                GameActivity.COLORS,
                GameActivity.SHAPES,
                GameActivity.NUMBERS,
                GameActivity.ANIMALS,
                GameActivity.MEMORY,
                GameActivity.PUZZLE,
                GameActivity.SHADOWS,
                GameActivity.SIZES,
                GameActivity.SORTING,
                GameActivity.PATHS,
                GameActivity.COLORS,
            ),
            route,
        )
        for (kind in LearningKind.entries) assertEquals(kind.name, GameActivity.from(kind).name)
    }

    @Test
    fun rewardWaitsForBothCompletionsAndAdvancesOnlyOnce() {
        for (audioFirst in listOf(true, false)) {
            val gate = RewardGate()
            assertFalse(if (audioFirst) gate.audioFinished() else gate.animationFinished())
            assertTrue(if (audioFirst) gate.animationFinished() else gate.audioFinished())
            assertFalse(gate.audioFinished())
            assertFalse(gate.animationFinished())
        }
    }

    @Test
    fun cancelledRewardNeverAdvances() {
        val gate = RewardGate()
        assertFalse(gate.audioFinished())
        gate.cancel()
        assertFalse(gate.animationFinished())
        assertFalse(gate.audioFinished())
    }

    @Test
    fun drawingBagVisitsAll200AcrossRestartsAndAvoidsRecentAtTheNextCycle() {
        val categories = (0 until 200).associate { "sheet_$it" to "category_${it%6}" }
        var rotation = DrawingRotation(categories, random = Random(42))
        val seen = mutableListOf<String>()
        repeat(200) { step ->
            val id = rotation.next(Random(step))
            assertFalse("A closed app must not restart the bag", id in seen)
            seen.add(id)
            rotation =
                DrawingRotation(categories, rotation.remaining.toList(), rotation.recent.toList())
        }
        assertEquals(200, seen.distinct().size)
        assertFalse(rotation.next(Random(8)) in seen.takeLast(20))
        assertTrue(
            "Alternate categories while there are alternatives",
            seen.zipWithNext().count { categories[it.first] != categories[it.second] } > 175,
        )
    }

    @Test
    fun manuallyChoosingASheetRemovesItFromTheAutomaticBag() {
        val categories = (0 until 200).associate { "sheet_$it" to "category_${it%6}" }
        val rotation = DrawingRotation(categories, random = Random(4))
        assertEquals("sheet_42", rotation.choose("sheet_42"))
        assertEquals("sheet_42", rotation.choose("sheet_42"))
        val automatic = (0 until 199).map { rotation.next(Random(it)) }
        assertEquals(199, automatic.distinct().size)
        assertFalse(
            "Manual repeats remain possible, automatic repeats do not",
            "sheet_42" in automatic,
        )
    }

    @Test
    fun touchingAndFeedingCountEachObjectOnceAndStopAtTheRequestedAmount() {
        for (mode in listOf(CountingMode.TOUCH, CountingMode.FEED)) for (target in 1..10) {
            val game = CountingRound(mode, target, listOf(target))
            assertNull(game.tap(-1))
            assertNull(game.tap(10))
            for (i in 0 until target) {
                assertEquals(i + 1, game.tap(i))
                assertNull(game.tap(i))
                assertEquals(i == target - 1, game.completed)
            }
            assertNull(game.tap(9))
            assertEquals(target, game.counted.size)
        }
    }

    @Test
    fun numberMatchingAndDifficultyCoverOneThroughTen() {
        for (maximum in listOf(3, 5, 10)) {
            val seen = mutableSetOf<Int>()
            repeat(300) { seed ->
                val game = CountingRound.create(CountingMode.MATCH, maximum, 3, Random(seed))
                seen.add(game.target)
                assertTrue(game.options.all { it in 1..maximum })
                assertEquals(3, game.options.distinct().size)
                assertFalse(game.answer(-1))
                game.options.filter { it != game.target }.forEach { assertFalse(game.answer(it)) }
                assertTrue(game.answer(game.target))
                assertFalse(game.answer(game.target))
            }
            assertEquals((1..maximum).toSet(), seen)
        }
    }

    @Test
    fun shadowsAndSizesHaveOneAnswerAndOneReward() {
        for (activity in listOf(GameActivity.SHADOWS, GameActivity.SIZES)) repeat(40) { seed ->
            val game = DiscoveryRound.create(activity, Random(seed))
            assertFalse(game.answer("missing"))
            game.options.filter { it != game.target }.forEach { assertFalse(game.answer(it)) }
            assertTrue(game.answer(game.target))
            assertFalse(game.answer(game.target))
        }
    }

    @Test
    fun sortingRequiresSelectingAndPlacingEveryPieceInItsOwnBasket() {
        for (shapes in listOf(true, false)) {
            val game = SortingRound.create(shapes, Random(17))
            assertFalse(game.place(game.bins.first()))
            assertFalse(game.select(-1))
            for (i in game.items.indices) {
                assertTrue(game.select(i))
                assertFalse(game.place(game.bins.first { it != game.items[i] }))
                assertTrue(game.place(game.items[i]))
                assertFalse(game.select(i))
            }
            assertTrue(game.completed)
            assertFalse(game.place(game.bins.first()))
        }
    }

    @Test
    fun tracingMustFollowTheWholePathInsteadOfJumpingToTheFinish() {
        repeat(4) { route ->
            val game = TracingGame(route)
            val end = game.points.last()
            assertFalse(game.touch(end.first, end.second))
            assertFalse(game.touch(-100f, -100f))
            for (p in game.points) assertTrue(game.touch(p.first, p.second))
            assertTrue(game.completed)
            assertFalse(game.touch(end.first, end.second))
        }
    }

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
            val random = Random(42)
            val seen =
                (0 until 1000).map { LearningRound.create(kind, 3, random).target.id }.toSet()
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
