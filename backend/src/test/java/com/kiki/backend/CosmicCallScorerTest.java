package com.kiki.backend;

import com.kiki.backend.service.CosmicCallScorer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CosmicCallScorerTest {

    private CosmicCallScorer scorer;

    @BeforeEach
    void setUp() {
        scorer = new CosmicCallScorer();
    }

    @Test
    void testEmptyOrSilence_ReturnsZeroScore() {
        assertTrue(scorer.isEmptyOrSilence(""));
        assertTrue(scorer.isEmptyOrSilence("   "));
        assertTrue(scorer.isEmptyOrSilence("..."));
        assertTrue(scorer.isEmptyOrSilence(null));

        assertEquals(0, scorer.calibrateScore(75, "What is your name?", ""));
        assertEquals(0, scorer.calibrateScore(80, "Where do you live?", "   "));
        assertEquals(0, scorer.computeDeterministicScore("What is your name?", ""));
    }

    @Test
    void testGibberishOrPureNoise_ReturnsLowScore() {
        assertTrue(scorer.isGibberishOrPureNoise("uh um ah"));
        assertTrue(scorer.isGibberishOrPureNoise("blabla..."));

        assertEquals(15, scorer.calibrateScore(70, "What is your name?", "uh um blabla"));
    }

    @Test
    void testOffTopicAnswer_CapsScoreAt25EvenIfAiReturned72() {
        String question = "What is your name and how are you today?";
        String wrongAnswer = "the weather is rain coffee hotel";

        assertTrue(scorer.checkOffTopic(question.toLowerCase(), wrongAnswer.toLowerCase()));

        // AI cố tình chấm 72 điểm nhưng scorer phát hiện trả lời sai/lạc đề hoàn toàn -> hạ xuống <= 25 điểm
        int calibrated = scorer.calibrateScore(72, question, wrongAnswer);
        assertTrue(calibrated <= 25, "Score must be <= 25 for off-topic/wrong answer, but was " + calibrated);
    }

    @Test
    void testSingleWordAnswer_CapsScoreAt50() {
        int score = scorer.calibrateScore(85, "What is your favorite hobby to relax?", "music");
        assertTrue(score <= 50, "Single word answer must be capped at 50 points, but was " + score);
    }

    @Test
    void testDontKnowOrEvasion_CapsScoreAt25() {
        assertTrue(scorer.isDontKnowOrEvasion("no I don't know either"));
        assertTrue(scorer.isDontKnowOrEvasion("i don't know"));
        assertTrue(scorer.isDontKnowOrEvasion("no i dont know"));
        assertTrue(scorer.isDontKnowOrEvasion("i have no idea"));
        assertTrue(scorer.isDontKnowOrEvasion("I'm not sure"));

        // Người dùng nói "no I don't know either", AI chấm 78 điểm -> Scorer phải hạ xuống <= 25 điểm
        int calibrated = scorer.calibrateScore(78, "What do you like to do on weekends?", "no I don't know either");
        assertTrue(calibrated <= 25, "Score must be <= 25 for 'no I don't know either', but was " + calibrated);
    }

    @Test
    void testRelevantAndFluentAnswer_MaintainsHighScore() {
        String question = "What is your name?";
        String answer = "Hello Kiki, my name is Truong";

        assertFalse(scorer.checkOffTopic(question.toLowerCase(), answer.toLowerCase()));

        int score = scorer.calibrateScore(90, question, answer);
        assertEquals(90, score);
    }
}
