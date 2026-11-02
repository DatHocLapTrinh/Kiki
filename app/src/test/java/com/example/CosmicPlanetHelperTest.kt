package com.example

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CosmicPlanetHelperTest {

    @Test
    fun `planet diameters are properly assigned in cosmic stages`() {
        assertEquals(70.dp, CosmicPlanetHelper.getPlanetDiameter(0))  // Sao Thủy
        assertEquals(78.dp, CosmicPlanetHelper.getPlanetDiameter(1))  // Sao Kim
        assertEquals(84.dp, CosmicPlanetHelper.getPlanetDiameter(2))  // Trái Đất
        assertEquals(74.dp, CosmicPlanetHelper.getPlanetDiameter(3))  // Mặt Trăng
        assertEquals(88.dp, CosmicPlanetHelper.getPlanetDiameter(4))  // Sao Hỏa
        assertEquals(106.dp, CosmicPlanetHelper.getPlanetDiameter(5)) // Sao Mộc
        assertEquals(96.dp, CosmicPlanetHelper.getPlanetDiameter(6))  // Sao Thổ
        assertEquals(110.dp, CosmicPlanetHelper.getPlanetDiameter(7)) // Sao Thiên Vương
        assertEquals(118.dp, CosmicPlanetHelper.getPlanetDiameter(8)) // Sao Hải Vương
        assertEquals(134.dp, CosmicPlanetHelper.getPlanetDiameter(9)) // Mặt Trời

        // Final stage override always gives apex size
        assertEquals(134.dp, CosmicPlanetHelper.getPlanetDiameter(4, totalStages = 5))
    }

    @Test
    fun `planet types map correctly through cosmic stages in solar system order`() {
        assertEquals(CosmicPlanetType.MERCURY_ROCK, CosmicPlanetHelper.getPlanetTypeForStage(0))
        assertEquals(CosmicPlanetType.VENUS_SULFUR, CosmicPlanetHelper.getPlanetTypeForStage(1))
        assertEquals(CosmicPlanetType.TERRA_GAIA, CosmicPlanetHelper.getPlanetTypeForStage(2))
        assertEquals(CosmicPlanetType.MOON_GRAY, CosmicPlanetHelper.getPlanetTypeForStage(3))
        assertEquals(CosmicPlanetType.MARS_CRIMSON, CosmicPlanetHelper.getPlanetTypeForStage(4))
        assertEquals(CosmicPlanetType.JUPITER_STORM, CosmicPlanetHelper.getPlanetTypeForStage(5))
        assertEquals(CosmicPlanetType.SATURN_MONARCH, CosmicPlanetHelper.getPlanetTypeForStage(6))
        assertEquals(CosmicPlanetType.URANUS_ICE, CosmicPlanetHelper.getPlanetTypeForStage(7))
        assertEquals(CosmicPlanetType.NEPTUNE_STORM, CosmicPlanetHelper.getPlanetTypeForStage(8))
        assertEquals(CosmicPlanetType.SOLAR_SUN, CosmicPlanetHelper.getPlanetTypeForStage(9))

        // Cycle repeats seamlessly for AI-generated higher stages
        assertEquals(CosmicPlanetType.MERCURY_ROCK, CosmicPlanetHelper.getPlanetTypeForStage(10))
        assertEquals(CosmicPlanetType.SOLAR_SUN, CosmicPlanetHelper.getPlanetTypeForStage(19))

        // Final stage override when totalStages is provided
        assertEquals(CosmicPlanetType.SOLAR_SUN, CosmicPlanetHelper.getPlanetTypeForStage(6, totalStages = 7))
    }

    @Test
    fun `planet names support bilingual display`() {
        val mercury = CosmicPlanetType.MERCURY_ROCK
        assertEquals("Sao Thủy (Khoáng Thạch Lam)", CosmicPlanetHelper.getPlanetTypeName(mercury, isEnglish = false))
        assertEquals("Mercury (MESSENGER Rayed)", CosmicPlanetHelper.getPlanetTypeName(mercury, isEnglish = true))

        val venus = CosmicPlanetType.VENUS_SULFUR
        assertEquals("Sao Kim (Mây Vàng Ngọc Trai)", CosmicPlanetHelper.getPlanetTypeName(venus, isEnglish = false))
        assertEquals("Venus (Golden Cloud Swirl)", CosmicPlanetHelper.getPlanetTypeName(venus, isEnglish = true))

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
