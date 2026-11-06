package com.example

import com.example.ui.CosmicSectionConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CosmicSectionSelectorCrashPreventionTest {

    private fun createMockStages(count: Int, completedCount: Int = 0): List<JourneyStageUiModel> {
        return (0 until count).map { index ->
            val state = when {
                index < completedCount -> JourneyStageState.COMPLETED
                index == completedCount -> JourneyStageState.CURRENT
                else -> JourneyStageState.LOCKED
            }
            JourneyStageUiModel(
                id = "planet_$index",
                sequence = index,
                title = "Planet $index",
                description = "Cosmic Planet $index",
                completedLessons = if (index < completedCount) 5 else 0,
                totalLessons = 5,
                progress = if (index < completedCount) 1f else 0f,
                state = state,
                visualType = PlanetVisualType.CYAN,
                destinationId = null,
                rewardStars = 10,
                isRecommended = (index == completedCount)
            )
        }
    }

    @Test
    fun testSectionOneAlwaysUnlockedAndNeverThrowsNegativeDrop() {
        // Test with empty list
        val emptyStages = emptyList<JourneyStageUiModel>()
        val unlockedEmpty = CosmicSectionConfig.isSectionUnlocked(1, emptyStages)
        assertTrue("Section 1 must always be unlocked even when stages is empty", unlockedEmpty)

        // Test with 40 stages, 0 completed
        val stages = createMockStages(40, completedCount = 0)
        val unlockedNormal = CosmicSectionConfig.isSectionUnlocked(1, stages)
        assertTrue("Section 1 must always be unlocked", unlockedNormal)

        // Test getAllSections with empty stages
        val sectionsEmpty = CosmicSectionConfig.getAllSections(emptyStages)
        assertEquals(4, sectionsEmpty.size)
        assertEquals(1, sectionsEmpty[0].sectionNumber)
        assertEquals(0, sectionsEmpty[0].completedCount)
        assertEquals(10, sectionsEmpty[0].totalPlanets)

        // Test getAllSections with 40 stages
        val sections40 = CosmicSectionConfig.getAllSections(stages)
        assertEquals(4, sections40.size)
    }

    @Test
    fun testSectionTwoUnlockLogic() {
        // Case A: 0 completed in Section 1 -> Section 2 locked
        val stages0 = createMockStages(40, completedCount = 4)
        assertFalse("Section 2 should be locked when Section 1 has only 4 completed", 
            CosmicSectionConfig.isSectionUnlocked(2, stages0))

        // Case B: All 10 completed in Section 1 -> Section 2 unlocked
        val stages10 = createMockStages(40, completedCount = 10)
        assertTrue("Section 2 should be unlocked when Section 1 has 10 completed",
            CosmicSectionConfig.isSectionUnlocked(2, stages10))

        // Case C: First stage of Section 2 (index 10) is CURRENT -> Section 2 unlocked
        val stagesAtSection2 = (0 until 40).map { index ->
            val state = when {
                index < 10 -> JourneyStageState.COMPLETED
                index == 10 -> JourneyStageState.CURRENT
                else -> JourneyStageState.LOCKED
            }
            JourneyStageUiModel(
                id = "planet_$index",
                sequence = index,
                title = "Planet $index",
                description = "Cosmic Planet $index",
                completedLessons = if (index < 10) 5 else 0,
                totalLessons = 5,
                progress = if (index < 10) 1f else 0f,
                state = state,
                visualType = PlanetVisualType.PURPLE,
                destinationId = null,
                rewardStars = 10,
                isRecommended = (index == 10)
            )
        }
        assertTrue("Section 2 should be unlocked when index 10 is CURRENT",
            CosmicSectionConfig.isSectionUnlocked(2, stagesAtSection2))
    }

    @Test
    fun testSectionUnlockWithPartialOrEmptyListDoesNotCrash() {
        val emptyStages = emptyList<JourneyStageUiModel>()
        for (sec in 1..5) {
            val unlocked = CosmicSectionConfig.isSectionUnlocked(sec, emptyStages)
            if (sec == 1) {
                assertTrue("Section 1 unlocked", unlocked)
            } else {
                assertFalse("Section $sec locked with empty stages", unlocked)
            }
        }

        val partialStages = createMockStages(5, completedCount = 2)
        for (sec in 1..4) {
            val unlocked = CosmicSectionConfig.isSectionUnlocked(sec, partialStages)
            if (sec == 1) {
                assertTrue("Section 1 unlocked", unlocked)
            } else {
                assertFalse("Section $sec locked with partial stages", unlocked)
            }
        }
    }
}
