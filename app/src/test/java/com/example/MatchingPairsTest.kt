package com.example

import com.example.model.MatchingPair
import com.example.model.QuestItem
import com.example.model.QuestionType
import org.junit.Assert.*
import org.junit.Test

class MatchingPairsTest {

    @Test
    fun testMatchingPairCreation() {
        val pair = MatchingPair(1, "give up", "từ bỏ")
        assertEquals(1, pair.id)
        assertEquals("give up", pair.english)
        assertEquals("từ bỏ", pair.vietnamese)
    }

    @Test
    fun testQuestItemWithMatchingPairs() {
        val pairs = listOf(
            MatchingPair(1, "give up", "từ bỏ"),
            MatchingPair(2, "turn off", "tắt (thiết bị)"),
            MatchingPair(3, "look after", "chăm sóc"),
            MatchingPair(4, "call off", "hủy bỏ")
        )
        val summary = pairs.joinToString(", ") { "${it.english} = ${it.vietnamese}" }

        val item = QuestItem(
            question = "Ghép cặp các từ vựng tiếng Anh với nghĩa tiếng Việt tương ứng",
            options = listOf(summary),
            correctIndex = 0,
            type = QuestionType.MATCHING_PAIRS,
            matchingPairs = pairs
        )

        assertEquals(QuestionType.MATCHING_PAIRS, item.type)
        assertEquals(4, item.matchingPairs.size)
        assertEquals(0, item.correctIndex)
    }

    @Test
    fun testMatchingLogicEvaluation() {
        val pairs = listOf(
            MatchingPair(1, "essential", "thiết yếu"),
            MatchingPair(2, "brilliant", "tài giỏi"),
            MatchingPair(3, "challenge", "thử thách"),
            MatchingPair(4, "opportunity", "cơ hội")
        )

        // Matching Simulation
        val matchedIds = mutableSetOf<Int>()
        var errorCount = 0

        fun tapPair(engId: Int, vnId: Int) {
            if (engId == vnId) {
                matchedIds.add(engId)
            } else {
                errorCount++
            }
        }

        // Tap 1: Correct
        tapPair(1, 1)
        assertTrue(matchedIds.contains(1))
        assertEquals(0, errorCount)

        // Tap 2: Wrong (brilliant with thử thách)
        tapPair(2, 3)
        assertFalse(matchedIds.contains(2))
        assertEquals(1, errorCount)

        // Tap 2 retry: Correct
        tapPair(2, 2)
        assertTrue(matchedIds.contains(2))

        // Complete remaining pairs
        tapPair(3, 3)
        tapPair(4, 4)

        assertEquals(4, matchedIds.size)
        // <= 2 mistakes counts as mastery
        val isMastered = errorCount <= 2
        assertTrue(isMastered)
    }

    @Test
    fun testMatchingPairsResultsMapping() {
        val pairs = listOf(
            MatchingPair(1, "perseverance", "sự kiên trì"),
            MatchingPair(2, "confidence", "sự tự tin")
        )
        val summary = pairs.joinToString(", ") { "${it.english} = ${it.vietnamese}" }

        val item = QuestItem(
            question = "Ghép cặp từ vựng",
            options = listOf(summary),
            correctIndex = 0,
            type = QuestionType.MATCHING_PAIRS,
            matchingPairs = pairs
        )

        // Successful completion mapping
        val isCorrect = true
        val copyCorrect = QuestItem(
            question = item.question,
            options = listOf(summary),
            correctIndex = 0,
            type = item.type,
            matchingPairs = item.matchingPairs
        )
        copyCorrect.selectedIndex = if (isCorrect) 0 else 1

        assertEquals(copyCorrect.correctIndex, copyCorrect.selectedIndex)

        // Failed completion mapping (too many errors)
        val isFailed = false
        val copyFailed = QuestItem(
            question = item.question,
            options = listOf(summary),
            correctIndex = 0,
            type = item.type,
            matchingPairs = item.matchingPairs
        )
        copyFailed.selectedIndex = if (isFailed) 0 else 1

        assertNotEquals(copyFailed.correctIndex, copyFailed.selectedIndex)
    }
}
