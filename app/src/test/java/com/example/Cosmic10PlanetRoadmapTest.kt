package com.example

import com.example.model.LearnerPersonaProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Cosmic10PlanetRoadmapTest {

    @Test
    fun testTenPlanetsInSolarSystemOrder() {
        val planets = listOf(
            "Sao Thủy",
            "Sao Kim",
            "Trái Đất",
            "Mặt Trăng",
            "Sao Hỏa",
            "Sao Mộc",
            "Sao Thổ",
            "Sao Thiên Vương",
            "Sao Hải Vương",
            "Mặt Trời"
        )
        assertEquals(10, planets.size)

        // Verify CosmicPlanetHelper maps all 10 stages properly
        for (i in 0 until 10) {
            val planetType = CosmicPlanetHelper.getPlanetTypeForStage(i, 10)
            val vietnameseName = CosmicPlanetHelper.getPlanetTypeName(planetType, isEnglish = false)
            assertTrue("Stage $i (${planets[i]}) should match $vietnameseName", vietnameseName.contains(planets[i]))
        }
    }
}

