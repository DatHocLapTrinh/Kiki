package com.example

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class JourneyStageMapperTest {

    @Test
    fun `first stage is current and later stages are locked`() {
        val stages = JourneyStageMapper.mapStages(listOf("A", "B", "C"), listOf(1, 1, 1))

        assertEquals(JourneyStageState.CURRENT, stages[0].state)
        assertEquals(JourneyStageState.LOCKED, stages[1].state)
        assertEquals(JourneyStageState.LOCKED, stages[2].state)
    }

    @Test
    fun `completed stage unlocks the next stage`() {
        val stages = JourneyStageMapper.mapStages(listOf("A", "B", "C"), listOf(6, 1, 1))

        assertEquals(JourneyStageState.COMPLETED, stages[0].state)
        assertEquals(JourneyStageState.CURRENT, stages[1].state)
        assertEquals(JourneyStageState.LOCKED, stages[2].state)
    }

    @Test
    fun `overall progress uses lessons and handles empty list`() {
        val stages = JourneyStageMapper.mapStages(listOf("A", "B"), listOf(3, 1))

        assertEquals(2, JourneyStageMapper.completedLessons(stages))
        assertEquals(10, JourneyStageMapper.totalLessons(stages))
        assertEquals(20, JourneyStageMapper.percentage(JourneyStageMapper.overallProgress(stages)))
        assertEquals(0f, JourneyStageMapper.overallProgress(emptyList()), 0f)
    }

    @Test
    fun `reward unlocks only after every stage is complete`() {
        val incomplete = JourneyStageMapper.mapStages(listOf("A", "B"), listOf(6, 5))
        val complete = JourneyStageMapper.mapStages(listOf("A", "B"), listOf(6, 6))

        assertFalse(JourneyStageMapper.rewardUnlocked(incomplete))
        assertTrue(JourneyStageMapper.rewardUnlocked(complete))
        assertEquals(16, JourneyStageMapper.earnedStars(complete))
    }
}
