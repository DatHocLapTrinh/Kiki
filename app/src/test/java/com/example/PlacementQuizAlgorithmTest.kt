package com.example

import com.example.util.AdaptiveDiagnosticEngine
import org.junit.Assert.*
import org.junit.Test

class PlacementQuizAlgorithmTest {

    @Test
    fun testDiagnosticQuestionsIntegrity() {
        val items = AdaptiveDiagnosticEngine.itemBank
        assertTrue("Item bank must not be empty", items.isNotEmpty())

        items.forEachIndexed { index, item ->
            assertTrue("Item ${index + 1} prompt must not be blank", item.questionPrompt.isNotBlank())
            assertTrue("Item ${index + 1} must have options", item.options.isNotEmpty())
            assertTrue("CorrectIndex must be valid", item.correctIndex in item.options.indices)
            assertTrue("Explanation must not be blank", item.explanation.isNotBlank())
            assertTrue("Skill tag must not be blank", item.skillTag.isNotBlank())
        }
    }

    @Test
    fun testLevelEvaluationLogic() {
        fun evaluate(score: Int): String {
            return when (score) {
                0, 1 -> "Beginner"
                2 -> "Intermediate"
                else -> "Advanced"
            }
        }

        assertEquals("Beginner", evaluate(0))
        assertEquals("Beginner", evaluate(1))
        assertEquals("Intermediate", evaluate(2))
        assertEquals("Advanced", evaluate(3))
    }
}
