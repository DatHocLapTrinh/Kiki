package com.example

import com.example.sqlite.room.ResearchTelemetryEntity
import com.example.util.ResearchTelemetryExporter
import org.junit.Assert.*
import org.junit.Test

class ResearchTelemetryExporterTest {

    @Test
    fun testExportToCsv_CorrectHeadersAndRows() {
        val entry1 = ResearchTelemetryEntity(
            id = 1,
            sessionId = "SES_001",
            userId = 100,
            timestamp = "2026-10-22T01:00:00",
            sloTopic = "Past Continuous Tense",
            challengeType = "MULTIPLE_CHOICE",
            itemIdentifier = "She was reading a book when I arrived.",
            responseLatencyMs = 2400L,
            isCorrect = true,
            usedHint = false,
            isDistracted = false,
            objectiveQuality = 5,
            easinessFactor = 2.6f,
            intervalDays = 3,
            retentionProbability = 0.9521f
        )

        val entry2 = ResearchTelemetryEntity(
            id = 2,
            sessionId = "SES_001",
            userId = 100,
            timestamp = "2026-10-22T01:01:30",
            sloTopic = "Vocabulary Vault",
            challengeType = "FLASHCARD",
            itemIdentifier = "ubiquitous, meaning everywhere",
            responseLatencyMs = 32000L,
            isCorrect = true,
            usedHint = false,
            isDistracted = true,
            objectiveQuality = 3,
            easinessFactor = 2.4f,
            intervalDays = 1,
            retentionProbability = 0.8200f
        )

        val csv = ResearchTelemetryExporter.exportToCsv(listOf(entry1, entry2))
        val lines = csv.trim().split("\n")

        assertEquals("Must have 1 header line + 2 data lines", 3, lines.size)
        assertTrue("Header must contain standard columns", lines[0].startsWith("session_id,user_id,timestamp"))
        assertTrue("Entry 1 must contain correct latency and quality", lines[1].contains("2400,1,0,0,5,2.60,3,0.9521"))
        assertTrue("Entry 2 must escape comma in item identifier and record distraction", lines[2].contains("\"ubiquitous, meaning everywhere\""))
        assertTrue("Entry 2 must record isDistracted = 1 and quality = 3", lines[2].contains("32000,1,0,1,3,2.40,1,0.8200"))
    }
}
