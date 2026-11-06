package com.example

import com.example.model.CosmicPassConfig
import com.example.model.CosmicPassTier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CosmicPassSubscriptionTest {

    @Test
    fun testCosmicPassTiersConfig() {
        val tiers = CosmicPassTier.values()
        assertEquals(3, tiers.size)

        val annualTier = CosmicPassTier.ANNUAL
        assertTrue("Annual tier should be marked as best value", annualTier.isBestValue)
        assertNotNull(annualTier.savingsBadge)
        assertTrue(annualTier.savingsBadge!!.contains("TIẾT KIỆM"))

        val monthlyTier = CosmicPassTier.MONTHLY
        assertFalse("Monthly tier is not marked as best value", monthlyTier.isBestValue)

        val starBundle = CosmicPassTier.STAR_BUNDLE
        assertEquals("NẠP NHANH", starBundle.savingsBadge)
    }

    @Test
    fun testCosmicPassBenefitsCoverage() {
        val benefits = CosmicPassConfig.benefits
        assertTrue("Must have at least 5 benefits", benefits.size >= 5)

        val hasUnlimitedHearts = benefits.any { it.vipAdvantage.contains("VÔ HẠN TIM") }
        assertTrue("Should include Unlimited Hearts benefit", hasUnlimitedHearts)

        val hasUnlimitedAI = benefits.any { it.vipAdvantage.contains("KHÔNG GIỚI HẠN") }
        assertTrue("Should include Unlimited AI Mentor benefit", hasUnlimitedAI)

        val hasAutoStreak = benefits.any { it.vipAdvantage.contains("TỰ ĐỘNG") }
        assertTrue("Should include Auto Streak benefit", hasAutoStreak)

        val hasEnergyExp = benefits.any { it.vipAdvantage.contains("EXP") }
        assertTrue("Should include Energy & EXP boost benefit", hasEnergyExp)
    }
}
