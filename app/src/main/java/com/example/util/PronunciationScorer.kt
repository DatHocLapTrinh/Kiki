package com.example.util

import java.util.Locale

enum class WordAccuracy {
    EXCELLENT, // >= 80% (Green)
    GOOD,      // 55% - 79% (Amber/Yellow)
    POOR       // < 55% (Red/Missed)
}

data class ScoredWord(
    val originalWord: String,
    val spokenWord: String?,
    val score: Int, // 0 - 100
    val accuracy: WordAccuracy
)

data class PronunciationResult(
    val targetSentence: String,
    val spokenSentence: String,
    val overallScore: Int, // 0 - 100
    val scoredWords: List<ScoredWord>,
    val isPassed: Boolean, // overallScore >= 70
    val feedbackMessage: String
)

object PronunciationScorer {

    fun cleanToken(word: String): String {
        return word.lowercase(Locale.ROOT)
            .trim()
            .trim('.', ',', '!', '?', ';', ':', '"', '“', '”', '\'', '’', '(', ')')
    }

    fun levenshteinDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }
        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j

        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,      // deletion
                    dp[i][j - 1] + 1,      // insertion
                    dp[i - 1][j - 1] + cost // substitution
                )
            }
        }
        return dp[s1.length][s2.length]
    }

    fun calculateWordScore(target: String, spoken: String): Int {
        val cleanTarget = cleanToken(target)
        val cleanSpoken = cleanToken(spoken)

        if (cleanTarget.isEmpty() && cleanSpoken.isEmpty()) return 100
        if (cleanTarget.isEmpty() || cleanSpoken.isEmpty()) return 0
        if (cleanTarget == cleanSpoken) return 100

        val maxLen = maxOf(cleanTarget.length, cleanSpoken.length)
        val dist = levenshteinDistance(cleanTarget, cleanSpoken)
        val similarity = (1.0f - dist.toFloat() / maxLen.toFloat()).coerceIn(0f, 1f)
        return (similarity * 100).toInt()
    }

    fun evaluate(targetSentence: String, spokenSentence: String): PronunciationResult {
        val targetTokens = targetSentence.split(Regex("\\s+")).filter { it.isNotBlank() }
        val spokenTokens = spokenSentence.split(Regex("\\s+")).filter { it.isNotBlank() }

        if (spokenTokens.isEmpty()) {
            val emptyScored = targetTokens.map {
                ScoredWord(originalWord = it, spokenWord = null, score = 0, accuracy = WordAccuracy.POOR)
            }
            return PronunciationResult(
                targetSentence = targetSentence,
                spokenSentence = "",
                overallScore = 0,
                scoredWords = emptyScored,
                isPassed = false,
                feedbackMessage = "Chưa phát hiện giọng nói. Hãy bấm micro và thử lại nhé!"
            )
        }

        val usedSpokenIndices = mutableSetOf<Int>()
        val scoredWords = mutableListOf<ScoredWord>()

        for ((tIdx, tWord) in targetTokens.withIndex()) {
            val cleanT = cleanToken(tWord)
            var bestSpokenIdx: Int? = null
            var bestScore = -1

            // Window-based search around corresponding position
            val searchRadius = 3
            val minIdx = (tIdx - searchRadius).coerceAtLeast(0)
            val maxIdx = (tIdx + searchRadius).coerceAtMost(spokenTokens.size - 1)

            for (sIdx in minIdx..maxIdx) {
                if (usedSpokenIndices.contains(sIdx)) continue
                val sWord = spokenTokens[sIdx]
                val score = calculateWordScore(cleanT, cleanToken(sWord))
                if (score > bestScore) {
                    bestScore = score
                    bestSpokenIdx = sIdx
                }
            }

            // Fallback global search if window didn't yield an acceptable match
            if (bestScore < 50) {
                for (sIdx in spokenTokens.indices) {
                    if (usedSpokenIndices.contains(sIdx)) continue
                    val sWord = spokenTokens[sIdx]
                    val score = calculateWordScore(cleanT, cleanToken(sWord))
                    if (score > bestScore) {
                        bestScore = score
                        bestSpokenIdx = sIdx
                    }
                }
            }

            if (bestSpokenIdx != null && bestScore >= 35) {
                usedSpokenIndices.add(bestSpokenIdx)
                val matchedSpoken = spokenTokens[bestSpokenIdx]
                val accuracy = when {
                    bestScore >= 80 -> WordAccuracy.EXCELLENT
                    bestScore >= 55 -> WordAccuracy.GOOD
                    else -> WordAccuracy.POOR
                }
                scoredWords.add(
                    ScoredWord(
                        originalWord = tWord,
                        spokenWord = matchedSpoken,
                        score = bestScore,
                        accuracy = accuracy
                    )
                )
            } else {
                scoredWords.add(
                    ScoredWord(
                        originalWord = tWord,
                        spokenWord = null,
                        score = 0,
                        accuracy = WordAccuracy.POOR
                    )
                )
            }
        }

        val avgScore = if (scoredWords.isNotEmpty()) {
            scoredWords.map { it.score }.average().toInt()
        } else {
            0
        }

        val isPassed = avgScore >= 70
        val feedback = when {
            avgScore >= 90 -> "Xuất sắc! Phát âm chuẩn như người bản xứ ⭐"
            avgScore >= 80 -> "Rất tốt! Ngữ điệu tự nhiên và chuẩn xác 👍"
            avgScore >= 70 -> "Đạt chuẩn! Bạn có thể nghe lại các từ đỏ để hoàn thiện hơn 👏"
            avgScore >= 50 -> "Khá tốt! Hãy chú ý phát âm rõ các âm đuôi hơn nhé 💪"
            else -> "Đừng nản lòng! Bấm nghe lại câu mẫu và thử nói lại nào 🎯"
        }

        return PronunciationResult(
            targetSentence = targetSentence,
            spokenSentence = spokenSentence,
            overallScore = avgScore,
            scoredWords = scoredWords,
            isPassed = isPassed,
            feedbackMessage = feedback
        )
    }
}
