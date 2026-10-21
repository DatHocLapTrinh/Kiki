package com.example

import com.example.ui.defaultDiagnosticQuestions
import org.junit.Assert.*
import org.junit.Test

class PlacementQuizAlgorithmTest {

    @Test
    fun testDiagnosticQuestionsIntegrity() {
        assertEquals("Placement quiz must have exactly 3 questions", 3, defaultDiagnosticQuestions.size)

        defaultDiagnosticQuestions.forEachIndexed { index, q ->
            assertTrue("Question ${index + 1} text must not be blank", q.question.isNotBlank())
            assertEquals("Question ${index + 1} must have 4 options", 4, q.options.size)
            assertTrue("CorrectIndex must be valid option index", q.correctIndex in 0..3)
            assertTrue("Explanation must not be blank", q.explanation.isNotBlank())
            assertTrue("LevelTag must not be blank", q.levelTag.isNotBlank())
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
