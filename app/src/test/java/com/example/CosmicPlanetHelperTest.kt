package com.example

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CosmicPlanetHelperTest {

    @Test
    fun `planet diameters strictly progress from smallest to largest in each cycle`() {
        val d0 = CosmicPlanetHelper.getPlanetDiameter(0)
        val d1 = CosmicPlanetHelper.getPlanetDiameter(1)
        val d2 = CosmicPlanetHelper.getPlanetDiameter(2)
        val d3 = CosmicPlanetHelper.getPlanetDiameter(3)
        val d4 = CosmicPlanetHelper.getPlanetDiameter(4)
        val d5 = CosmicPlanetHelper.getPlanetDiameter(5)
        val d6 = CosmicPlanetHelper.getPlanetDiameter(6)
        val d7 = CosmicPlanetHelper.getPlanetDiameter(7)

        assertEquals(74.dp, d0)
        assertEquals(80.dp, d1)
        assertEquals(88.dp, d2)
        assertEquals(96.dp, d3)
        assertEquals(104.dp, d4)
        assertEquals(112.dp, d5)
        assertEquals(120.dp, d6)
        assertEquals(132.dp, d7)

        assertTrue("Stage 0 < Stage 1", d0.value < d1.value)
        assertTrue("Stage 1 < Stage 2", d1.value < d2.value)
        assertTrue("Stage 2 < Stage 3", d2.value < d3.value)
        assertTrue("Stage 3 < Stage 4", d3.value < d4.value)
        assertTrue("Stage 4 < Stage 5", d4.value < d5.value)
        assertTrue("Stage 5 < Stage 6", d5.value < d6.value)
        assertTrue("Stage 6 < Stage 7", d6.value < d7.value)
    }

    @Test
    fun `planet types map correctly through cosmic stages`() {
        assertEquals(CosmicPlanetType.TERRA_GAIA, CosmicPlanetHelper.getPlanetTypeForStage(0))
        assertEquals(CosmicPlanetType.MOON_GRAY, CosmicPlanetHelper.getPlanetTypeForStage(1))
        assertEquals(CosmicPlanetType.SATURN_MONARCH, CosmicPlanetHelper.getPlanetTypeForStage(2))
        assertEquals(CosmicPlanetType.JUPITER_STORM, CosmicPlanetHelper.getPlanetTypeForStage(3))
        assertEquals(CosmicPlanetType.MARS_CRIMSON, CosmicPlanetHelper.getPlanetTypeForStage(4))
        assertEquals(CosmicPlanetType.URANUS_ICE, CosmicPlanetHelper.getPlanetTypeForStage(5))
        assertEquals(CosmicPlanetType.NEPTUNE_STORM, CosmicPlanetHelper.getPlanetTypeForStage(6))
        assertEquals(CosmicPlanetType.SOLAR_SUN, CosmicPlanetHelper.getPlanetTypeForStage(7))

        // Cycle repeats seamlessly for AI-generated higher stages
        assertEquals(CosmicPlanetType.TERRA_GAIA, CosmicPlanetHelper.getPlanetTypeForStage(8))
        assertEquals(CosmicPlanetType.SOLAR_SUN, CosmicPlanetHelper.getPlanetTypeForStage(15))

        // Final stage override when totalStages is provided
        assertEquals(CosmicPlanetType.SOLAR_SUN, CosmicPlanetHelper.getPlanetTypeForStage(6, totalStages = 7))
    }

    @Test
    fun `planet names support bilingual display`() {
        val sun = CosmicPlanetType.SOLAR_SUN
        assertEquals("Mặt Trời (Đỉnh Cao Thái Dương)", CosmicPlanetHelper.getPlanetTypeName(sun, isEnglish = false))
        assertEquals("The Sun (Solar Apex)", CosmicPlanetHelper.getPlanetTypeName(sun, isEnglish = true))

        val uranus = CosmicPlanetType.URANUS_ICE
        assertEquals("Sao Thiên Vương", CosmicPlanetHelper.getPlanetTypeName(uranus, isEnglish = false))
        assertEquals("Uranus (Cyan Ice Giant)", CosmicPlanetHelper.getPlanetTypeName(uranus, isEnglish = true))

        val neptune = CosmicPlanetType.NEPTUNE_STORM
        assertEquals("Sao Hải Vương", CosmicPlanetHelper.getPlanetTypeName(neptune, isEnglish = false))
        assertEquals("Neptune (Great Dark Spot)", CosmicPlanetHelper.getPlanetTypeName(neptune, isEnglish = true))
    }
}
