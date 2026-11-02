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

        assertEquals(72.dp, d0)
        assertEquals(80.dp, d1)
        assertEquals(90.dp, d2)
        assertEquals(102.dp, d3)
        assertEquals(114.dp, d4)
        assertEquals(128.dp, d5)

        assertTrue("Stage 1 must be smaller than Stage 2", d0.value < d1.value)
        assertTrue("Stage 2 must be smaller than Stage 3", d1.value < d2.value)
        assertTrue("Stage 3 must be smaller than Stage 4", d2.value < d3.value)
        assertTrue("Stage 4 must be smaller than Stage 5", d3.value < d4.value)
        assertTrue("Stage 5 must be smaller than Stage 6", d4.value < d5.value)
    }

    @Test
    fun `planet types map correctly through cosmic stages`() {
        assertEquals(CosmicPlanetType.TERRA_GAIA, CosmicPlanetHelper.getPlanetTypeForStage(0))
        assertEquals(CosmicPlanetType.MOON_GRAY, CosmicPlanetHelper.getPlanetTypeForStage(1))
        assertEquals(CosmicPlanetType.SATURN_MONARCH, CosmicPlanetHelper.getPlanetTypeForStage(2))
        assertEquals(CosmicPlanetType.JUPITER_STORM, CosmicPlanetHelper.getPlanetTypeForStage(3))
        assertEquals(CosmicPlanetType.MARS_CRIMSON, CosmicPlanetHelper.getPlanetTypeForStage(4))
        assertEquals(CosmicPlanetType.SOLAR_SUN, CosmicPlanetHelper.getPlanetTypeForStage(5))

        // Cycle repeats seamlessly for AI-generated higher stages
        assertEquals(CosmicPlanetType.TERRA_GAIA, CosmicPlanetHelper.getPlanetTypeForStage(6))
        assertEquals(CosmicPlanetType.SOLAR_SUN, CosmicPlanetHelper.getPlanetTypeForStage(11))
    }

    @Test
    fun `planet names support bilingual display`() {
        val sun = CosmicPlanetType.SOLAR_SUN
        assertEquals("Mặt Trời (Đỉnh Cao Thái Dương)", CosmicPlanetHelper.getPlanetTypeName(sun, isEnglish = false))
        assertEquals("The Sun (Solar Apex)", CosmicPlanetHelper.getPlanetTypeName(sun, isEnglish = true))
    }
}
