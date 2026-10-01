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
    fun testChiefDoctorProgressAndUnlock() {
        val progressBadges = AchievementRegistry.getAllBadges(
            unlockedIds = emptySet(),
            curedWeakPoints = 6
        )
        val doctorInProgress = progressBadges.first { it.id == BadgeId.CHIEF_DOCTOR.id }
        assertFalse("Doctor should be locked with 6 cured points", doctorInProgress.isUnlocked)
        assertEquals(6, doctorInProgress.currentProgress)
        assertEquals(10, doctorInProgress.targetCount)
        assertEquals(0.6f, doctorInProgress.progressFraction, 0.01f)

        val completedBadges = AchievementRegistry.getAllBadges(
            unlockedIds = emptySet(),
            curedWeakPoints = 12
        )
        val doctorCompleted = completedBadges.first { it.id == BadgeId.CHIEF_DOCTOR.id }
        assertTrue("Doctor should be unlocked when >= 10 cured points", doctorCompleted.isUnlocked)
        assertEquals(10, doctorCompleted.currentProgress)
        assertEquals(1.0f, doctorCompleted.progressFraction, 0.01f)
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
        assertEquals(7, allBadges.size)

        for (badge in allBadges) {
            assertTrue("Badge titleVi should not be blank: ${badge.id}", badge.titleVi.isNotBlank())
            assertTrue("Badge titleEn should not be blank: ${badge.id}", badge.titleEn.isNotBlank())
            assertTrue("Badge descriptionVi should not be blank: ${badge.id}", badge.descriptionVi.isNotBlank())
            assertTrue("Badge descriptionEn should not be blank: ${badge.id}", badge.descriptionEn.isNotBlank())
            assertTrue("Target count should be > 0: ${badge.id}", badge.targetCount > 0)
            assertTrue("Progress fraction should be in 0..1: ${badge.id}", badge.progressFraction in 0f..1f)
        }
    }
}
