package com.example

import com.example.model.ChapterUnlockEvent
import com.example.sqlite.room.WeakPointEntity
import org.json.JSONArray
import org.junit.Assert.*
import org.junit.Test

class ChapterUnlockAndGenerationTest {

    @Test
    fun testStageUnlockingProgression() {
        val chapters = listOf(
            "English Fundamentals",
            "Vocabulary & Daily Idioms",
            "Syntax & Sentence Mastery"
        )

        // Case 1: In progress on Chapter 1 (node = 4 out of 5)
        val progress1 = listOf(4, 1, 1)
        val stages1 = JourneyStageMapper.mapStages(chapters, progress1)
        assertEquals(JourneyStageState.CURRENT, stages1[0].state)
        assertEquals(JourneyStageState.LOCKED, stages1[1].state)
        assertEquals(JourneyStageState.LOCKED, stages1[2].state)
        assertFalse(stages1[0].state == JourneyStageState.COMPLETED)

        // Case 2: Fresh completion of Chapter 1 (node = 6) -> Chapter 2 is unlocked and becomes CURRENT
        val progress2 = listOf(6, 1, 1)
        val stages2 = JourneyStageMapper.mapStages(chapters, progress2)
        assertEquals(JourneyStageState.COMPLETED, stages2[0].state)
        assertEquals(JourneyStageState.CURRENT, stages2[1].state)
        assertEquals(JourneyStageState.LOCKED, stages2[2].state)

        // Case 3: All 3 chapters completed
        val progress3 = listOf(6, 6, 6)
        val stages3 = JourneyStageMapper.mapStages(chapters, progress3)
        assertTrue(stages3.all { it.state == JourneyStageState.COMPLETED })
        assertTrue(JourneyStageMapper.rewardUnlocked(stages3))
    }

    @Test
    fun testTopicProgressionLogic() {
        val syllabus = listOf(
            "English Fundamentals",
            "Vocabulary & Daily Idioms",
            "Syntax & Sentence Mastery",
            "Conversational Fluency",
            "Academic & IELTS Mastery",
            "Professional & Business Communication",
            "Travel & Cultural Encounters",
            "Advanced Discourse & Critical Thinking",
            "Nuanced Collocations & Phrasal Verbs",
            "Mastery Summit & Fluency Capstone"
        )

        fun determineNextTopic(existingChapters: List<String>, level: String): String {
            for (topic in syllabus) {
                if (!existingChapters.any { it.contains(topic, ignoreCase = true) }) {
                    return topic
                }
            }
            return "Adaptive Mastery: Level $level Stage ${existingChapters.size + 1}"
        }

        // When user has completed the first 5 chapters
        val existing5 = syllabus.take(5)
        val nextTopic = determineNextTopic(existing5, "Intermediate")
        assertEquals("Professional & Business Communication", nextTopic)

        // When user has completed all 10 chapters
        val nextAfterAll = determineNextTopic(syllabus, "Advanced")
        assertEquals("Adaptive Mastery: Level Advanced Stage 11", nextAfterAll)
    }

    @Test
    fun testWeakKeywordsExtraction() {
        fun extractWeakKeywords(weakPoints: List<WeakPointEntity>): List<String> {
            val keywords = mutableListOf<String>()
            val stringPattern = java.util.regex.Pattern.compile("\"((?:\\\\.|[^\"\\\\])*)\"")
            for (wp in weakPoints) {
                try {
                    val matcher = stringPattern.matcher(wp.optionsJson)
                    val options = mutableListOf<String>()
                    while (matcher.find()) {
                        val raw = matcher.group(1) ?: ""
                        options.add(raw.replace("\\\"", "\"").replace("\\\\", "\\").trim())
                    }
                    if (wp.correctIndex in 0 until options.size) {
                        val word = options[wp.correctIndex]
                        if (word.isNotBlank() && !keywords.contains(word)) {
                            keywords.add(word)
                        }
                    } else {
                        val candidate = wp.question.split(" ").firstOrNull { it.length > 4 }
                        if (candidate != null && !keywords.contains(candidate)) {
                            keywords.add(candidate)
                        }
                    }
                } catch (_: Exception) {
                    val candidate = wp.question.split(" ").firstOrNull { it.length > 4 }
                    if (candidate != null && !keywords.contains(candidate)) {
                        keywords.add(candidate)
                    }
                }
                if (keywords.size >= 5) break
            }
            return keywords
        }

        val weakPoints = listOf(
            WeakPointEntity(
                userId = 1L,
                question = "She ______ to school by bus every morning.",
                optionsJson = """["go", "goes", "going", "is go"]""",
                correctIndex = 1
            ),
            WeakPointEntity(
                userId = 1L,
                question = "They ______ English for two hours yesterday.",
                optionsJson = """["study", "studied", "studying", "studies"]""",
                correctIndex = 1
            ),
            WeakPointEntity(
                userId = 1L,
                question = "Duplicate test with goes",
                optionsJson = """["go", "goes", "going"]""",
                correctIndex = 1
            ),
            WeakPointEntity(
                userId = 1L,
                question = "We ______ to Paris next summer.",
                optionsJson = """["will travel", "traveled", "travels"]""",
                correctIndex = 0
            )
        )

        val extracted = extractWeakKeywords(weakPoints)
        assertEquals(3, extracted.size)
        assertEquals("goes", extracted[0])
        assertEquals("studied", extracted[1])
        assertEquals("will travel", extracted[2])
    }

    @Test
    fun testChapterUnlockEventModel() {
        val event = ChapterUnlockEvent(
            completedChapterTitle = "English Fundamentals",
            nextChapterTitle = "Vocabulary & Daily Idioms",
            isAiGenerated = false,
            weakPointsAddressedCount = 0
        )
        assertEquals("English Fundamentals", event.completedChapterTitle)
        assertEquals("Vocabulary & Daily Idioms", event.nextChapterTitle)
        assertFalse(event.isAiGenerated)

        val aiEvent = ChapterUnlockEvent(
            completedChapterTitle = "Academic & IELTS Mastery",
            nextChapterTitle = "Professional & Business Communication",
            isAiGenerated = true,
            weakPointsAddressedCount = 3
        )
        assertTrue(aiEvent.isAiGenerated)
        assertEquals(3, aiEvent.weakPointsAddressedCount)
    }
}
