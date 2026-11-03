package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.LeagueSettlementResult
import com.example.model.LeagueTier
import com.example.model.LeagueZone
import com.example.sync.LeagueManager
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Calendar
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
class LeagueRankingAndSettlementTest {

    private lateinit var context: Context
    private lateinit var leagueManager: LeagueManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        val prefs = context.getSharedPreferences("StudyMentorLeaguePrefs", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        leagueManager = LeagueManager(context)
    }

    @Test
    fun test5TrophyResourcesAreMappedProperly() {
        assertEquals(R.drawable.trophy_bronze, LeagueTier.BRONZE.trophyResId)
        assertEquals(R.drawable.trophy_silver, LeagueTier.SILVER.trophyResId)
        assertEquals(R.drawable.trophy_gold, LeagueTier.GOLD.trophyResId)
        assertEquals(R.drawable.trophy_platinum, LeagueTier.PLATINUM.trophyResId)
        assertEquals(R.drawable.trophy_diamond, LeagueTier.DIAMOND.trophyResId)
    }

    @Test
    fun testTierPromotionProgression() {
        assertEquals(LeagueTier.SILVER, LeagueTier.BRONZE.nextTier())
        assertEquals(LeagueTier.GOLD, LeagueTier.SILVER.nextTier())
        assertEquals(LeagueTier.PLATINUM, LeagueTier.GOLD.nextTier())
        assertEquals(LeagueTier.DIAMOND, LeagueTier.PLATINUM.nextTier())
        assertEquals(LeagueTier.DIAMOND, LeagueTier.DIAMOND.nextTier()) // Capped at Diamond
    }

    @Test
    fun testTierDemotionProgression() {
        assertEquals(LeagueTier.PLATINUM, LeagueTier.DIAMOND.previousTier())
        assertEquals(LeagueTier.GOLD, LeagueTier.PLATINUM.previousTier())
        assertEquals(LeagueTier.SILVER, LeagueTier.GOLD.previousTier())
        assertEquals(LeagueTier.BRONZE, LeagueTier.SILVER.previousTier())
        assertEquals(LeagueTier.BRONZE, LeagueTier.BRONZE.previousTier()) // Capped at Bronze
    }

    @Test
    fun testTierFromStringResolvesCorrectly() {
        assertEquals(LeagueTier.BRONZE, LeagueTier.fromString("BRONZE"))
        assertEquals(LeagueTier.SILVER, LeagueTier.fromString("Silver"))
        assertEquals(LeagueTier.GOLD, LeagueTier.fromString("vàng"))
        assertEquals(LeagueTier.PLATINUM, LeagueTier.fromString("bạch kim"))
        assertEquals(LeagueTier.DIAMOND, LeagueTier.fromString("diamond"))
        assertEquals(LeagueTier.BRONZE, LeagueTier.fromString("unknown_val"))
    }

    @Test
    fun testLeagueZoneRulesForTop5AndSafeAndDemotion() {
        // Top 1 to 5 always PROMOTION
        for (rank in 1..5) {
            assertEquals(LeagueZone.PROMOTION, leagueManager.determineZone(rank, LeagueTier.SILVER))
        }

        // Top 6 to 15 always SAFE
        for (rank in 6..15) {
            assertEquals(LeagueZone.SAFE, leagueManager.determineZone(rank, LeagueTier.SILVER))
        }

        // Rank 16+ is DEMOTION for Silver, Gold, Platinum, Diamond
        assertEquals(LeagueZone.DEMOTION, leagueManager.determineZone(16, LeagueTier.SILVER))
        assertEquals(LeagueZone.DEMOTION, leagueManager.determineZone(20, LeagueTier.GOLD))
        assertEquals(LeagueZone.DEMOTION, leagueManager.determineZone(25, LeagueTier.DIAMOND))

        // Bronze rank 16+ is SAFE (cannot fall below Bronze)
        assertEquals(LeagueZone.SAFE, leagueManager.determineZone(16, LeagueTier.BRONZE))
        assertEquals(LeagueZone.SAFE, leagueManager.determineZone(25, LeagueTier.BRONZE))
    }

    @Test
    fun testWeeklyXpAccumulation() {
        assertEquals(0, leagueManager.getWeeklyXp())
        leagueManager.addWeeklyXp(50)
        assertEquals(50, leagueManager.getWeeklyXp())
        leagueManager.addWeeklyXp(120)
        assertEquals(170, leagueManager.getWeeklyXp())
    }

    @Test
    fun testTimeUntilMondayAndFormatting() {
        val now = Calendar.getInstance()
        val millis = leagueManager.getMillisUntilNextMonday(now)
        assertTrue(millis > 0)

        val formattedDays = leagueManager.formatRemainingTime(259200000L) // 3 days
        assertTrue(formattedDays.contains("ngày"))

        val formattedHours = leagueManager.formatRemainingTime(7200000L) // 2 hours
        assertTrue(formattedHours.contains("giờ"))
    }

    @Test
    fun testMondaySettlementPromotesTop5User() {
        // Initial setup in Bronze
        leagueManager.setCurrentTier(LeagueTier.BRONZE)
        leagueManager.addWeeklyXp(300)

        // Mock that last settled week was an older week
        val prefs = context.getSharedPreferences("StudyMentorLeaguePrefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("key_last_settled_week", "2026-W40")
            .putInt("key_last_recorded_rank", 3) // Finished in Top 5 (Rank 3)
            .apply()

        // Trigger Monday settlement
        val settlement = leagueManager.checkAndSettleWeeklyLeague()
        assertNotNull(settlement)
        assertTrue(settlement is LeagueSettlementResult.Promoted)

        val promoted = settlement as LeagueSettlementResult.Promoted
        assertEquals(LeagueTier.BRONZE, promoted.oldTier)
        assertEquals(LeagueTier.SILVER, promoted.newTier)
        assertEquals(3, promoted.finalRank)
        assertEquals(LeagueTier.SILVER, leagueManager.getCurrentTier())
        assertEquals(0, leagueManager.getWeeklyXp()) // Weekly XP resets for the new week
    }

    @Test
    fun testMondaySettlementMaintainsSafeUser() {
        leagueManager.setCurrentTier(LeagueTier.GOLD)
        val prefs = context.getSharedPreferences("StudyMentorLeaguePrefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("key_last_settled_week", "2026-W40")
            .putInt("key_last_recorded_rank", 10) // Finished in Safe Zone (Rank 10)
            .apply()

        val settlement = leagueManager.checkAndSettleWeeklyLeague()
        assertNotNull(settlement)
        assertTrue(settlement is LeagueSettlementResult.Maintained)

        val maintained = settlement as LeagueSettlementResult.Maintained
        assertEquals(LeagueTier.GOLD, maintained.currentTier)
        assertEquals(LeagueTier.GOLD, leagueManager.getCurrentTier())
        assertEquals(0, leagueManager.getWeeklyXp())
    }

    @Test
    fun testMondaySettlementDemotesLowRankUser() {
        leagueManager.setCurrentTier(LeagueTier.SILVER)
        val prefs = context.getSharedPreferences("StudyMentorLeaguePrefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("key_last_settled_week", "2026-W40")
            .putInt("key_last_recorded_rank", 18) // Finished in Demotion Zone (Rank 18)
            .apply()

        val settlement = leagueManager.checkAndSettleWeeklyLeague()
        assertNotNull(settlement)
        assertTrue(settlement is LeagueSettlementResult.Demoted)

        val demoted = settlement as LeagueSettlementResult.Demoted
        assertEquals(LeagueTier.SILVER, demoted.oldTier)
        assertEquals(LeagueTier.BRONZE, demoted.newTier)
        assertEquals(18, demoted.finalRank)
        assertEquals(LeagueTier.BRONZE, leagueManager.getCurrentTier())
        assertEquals(0, leagueManager.getWeeklyXp())
    }
}

