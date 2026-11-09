package com.example

import com.example.model.AchievementRegistry
import com.example.model.BadgeId
import org.junit.Assert.*
import org.junit.Test

class AchievementBadgeTest {

    @Test
    fun testNightOwlUnlockCondition() {
        val lockedBadges = AchievementRegistry.getAllBadges(
            unlockedIds = emptySet(),
            hasNightOwl = false
        )
        val nightOwlLocked = lockedBadges.first { it.id == BadgeId.NIGHT_OWL.id }
        assertFalse("Night Owl should be locked by default", nightOwlLocked.isUnlocked)
        assertEquals(0, nightOwlLocked.currentProgress)

        val unlockedBadges = AchievementRegistry.getAllBadges(
            unlockedIds = emptySet(),
            hasNightOwl = true
        )
        val nightOwlUnlocked = unlockedBadges.first { it.id == BadgeId.NIGHT_OWL.id }
        assertTrue("Night Owl should be unlocked when completed after 22:00", nightOwlUnlocked.isUnlocked)
        assertEquals(1, nightOwlUnlocked.currentProgress)
    }

    @Test
    fun testStreakMilestonesProgressAndUnlock() {
        val streak20 = AchievementRegistry.getAllBadges(
            unlockedIds = emptySet(),
            streak = 20
        )
        val streak7 = streak20.first { it.id == BadgeId.PERSISTENT_SCHOLAR.id }
        val streak30 = streak20.first { it.id == BadgeId.STREAK_30.id }
        val streak100 = streak20.first { it.id == BadgeId.STREAK_100.id }
        val streak300 = streak20.first { it.id == BadgeId.STREAK_300.id }

        assertTrue("Streak 7 should be unlocked with 20 days", streak7.isUnlocked)
        assertEquals(7, streak7.currentProgress)

        assertFalse("Streak 30 should be locked with 20 days", streak30.isUnlocked)
        assertEquals(20, streak30.currentProgress)
        assertEquals(30, streak30.targetCount)

        assertFalse("Streak 100 should be locked with 20 days", streak100.isUnlocked)
        assertFalse("Streak 300 should be locked with 20 days", streak300.isUnlocked)

        val streak150 = AchievementRegistry.getAllBadges(
            unlockedIds = emptySet(),
            streak = 150
        )
        assertTrue("Streak 30 should be unlocked with 150 days", streak150.first { it.id == BadgeId.STREAK_30.id }.isUnlocked)
        assertTrue("Streak 100 should be unlocked with 150 days", streak150.first { it.id == BadgeId.STREAK_100.id }.isUnlocked)
        assertFalse("Streak 300 should be locked with 150 days", streak150.first { it.id == BadgeId.STREAK_300.id }.isUnlocked)
    }

    @Test
    fun testSuperMemoryProgressAndUnlock() {
        val memoryBadges = AchievementRegistry.getAllBadges(
            unlockedIds = emptySet(),
            masteredVocabCount = 20
        )
        val memoryInProgress = memoryBadges.first { it.id == BadgeId.SUPER_MEMORY.id }
        assertFalse("Super Memory should be locked with 20 mastered words", memoryInProgress.isUnlocked)
        assertEquals(20, memoryInProgress.currentProgress)
        assertEquals(30, memoryInProgress.targetCount)

        val memoryCompleted = AchievementRegistry.getAllBadges(
            unlockedIds = emptySet(),
            masteredVocabCount = 35
        ).first { it.id == BadgeId.SUPER_MEMORY.id }
        assertTrue("Super Memory should be unlocked when >= 30 mastered words", memoryCompleted.isUnlocked)
        assertEquals(30, memoryCompleted.currentProgress)
    }

    @Test
    fun testPersistentScholarUnlock() {
        val streak5 = AchievementRegistry.getAllBadges(
            unlockedIds = emptySet(),
            streak = 5
        ).first { it.id == BadgeId.PERSISTENT_SCHOLAR.id }
        assertFalse("Persistent Scholar should be locked with 5-day streak", streak5.isUnlocked)
        assertEquals(5, streak5.currentProgress)

        val streak7 = AchievementRegistry.getAllBadges(
            unlockedIds = emptySet(),
            streak = 7
        ).first { it.id == BadgeId.PERSISTENT_SCHOLAR.id }
        assertTrue("Persistent Scholar should be unlocked with 7-day streak", streak7.isUnlocked)
        assertEquals(7, streak7.currentProgress)
    }

    @Test
    fun testAllBadgesIntegrity() {
        val allBadges = AchievementRegistry.getAllBadges(unlockedIds = emptySet())
        assertEquals(9, allBadges.size)

        for (badge in allBadges) {
            assertTrue("Badge titleVi should not be blank: ${badge.id}", badge.titleVi.isNotBlank())
            assertTrue("Badge titleEn should not be blank: ${badge.id}", badge.titleEn.isNotBlank())
            assertTrue("Badge descriptionVi should not be blank: ${badge.id}", badge.descriptionVi.isNotBlank())
            assertTrue("Badge descriptionEn should not be blank: ${badge.id}", badge.descriptionEn.isNotBlank())
            assertTrue("Target count should be > 0: ${badge.id}", badge.targetCount > 0)
            assertTrue("Progress fraction should be in 0..1: ${badge.id}", badge.progressFraction in 0f..1f)
        }
    }

    @Test
    fun testAllBadgesMapToUniqueDrawables() {
        val allBadges = AchievementRegistry.getAllBadges(unlockedIds = emptySet())
        val mappedDrawables = allBadges.map { resolveBadgeTrophyResource(it.id, it.iconKey) }
        assertEquals(9, mappedDrawables.size)
        // All 9 badges must have distinct non-repeating drawables
        assertEquals(9, mappedDrawables.toSet().size)
        for (res in mappedDrawables) {
            assertTrue("Drawable res should be positive", res > 0)
        }
    }

    @Test
    fun testAllStagesMasteryUnlock() {
        val locked = AchievementRegistry.getAllBadges(unlockedIds = emptySet(), allStagesCompleted = false)
            .first { it.id == BadgeId.ALL_STAGES_MASTERY.id }
        assertFalse(locked.isUnlocked)

        val unlocked = AchievementRegistry.getAllBadges(unlockedIds = emptySet(), allStagesCompleted = true)
            .first { it.id == BadgeId.ALL_STAGES_MASTERY.id }
        assertTrue(unlocked.isUnlocked)
    }
}
