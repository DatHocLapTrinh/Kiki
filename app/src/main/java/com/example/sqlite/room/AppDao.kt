package com.example.sqlite.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface AppDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserProfile(profile: UserProfileEntity): Long

    @Query("SELECT user_id FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserIdByEmail(email: String): Long?

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users WHERE email = :email AND password_hash = :passwordHash LIMIT 1")
    suspend fun loginUser(email: String, passwordHash: String): UserEntity?

    @Query("SELECT * FROM user_profiles WHERE user_id = :userId LIMIT 1")
    suspend fun getUserProfile(userId: Long): UserProfileEntity?

    @Query("UPDATE user_profiles SET total_xp = total_xp + :xpGain WHERE user_id = :userId")
    suspend fun updateXP(userId: Long, xpGain: Int)

    @Query("UPDATE user_profiles SET mana = mana + :manaGain WHERE user_id = :userId")
    suspend fun updateMana(userId: Long, manaGain: Int)

    @Query("SELECT * FROM user_profiles ORDER BY total_xp DESC LIMIT 10")
    suspend fun getLeaderboard(): List<UserProfileEntity>

    // --- Chapters ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapter(chapter: ChapterEntity): Long

    @Query("SELECT title FROM chapters WHERE level = :level AND subject = :subject ORDER BY order_no")
    suspend fun getChaptersByPreference(level: String, subject: String): List<String>

    @Query("SELECT chapter_id FROM chapters WHERE title = :title LIMIT 1")
    suspend fun getChapterIdByTitle(title: String): Long?

    @Query("SELECT title FROM chapters WHERE is_active = 1 ORDER BY order_no")
    suspend fun getActiveChapters(): List<String>

    @Query("DELETE FROM chapters")
    suspend fun clearChapters()

    @Query("DELETE FROM questions")
    suspend fun clearQuestions()

    // --- Questions ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestion(question: QuestionEntity): Long

    @Query("SELECT * FROM questions WHERE chapter_id = :chapterId")
    suspend fun getQuestionsByChapter(chapterId: Long): List<QuestionEntity>

    // --- Quiz Attempts ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuizAttempt(attempt: QuizAttemptEntity): Long

    // --- Daily Tasks ---
    @Query("SELECT * FROM daily_tasks WHERE user_id = :userId AND task_date = :taskDate")
    suspend fun getDailyTasks(userId: Long, taskDate: String): List<DailyTaskEntity>

    @Query("SELECT * FROM daily_tasks WHERE task_id = :taskId LIMIT 1")
    suspend fun getDailyTask(taskId: Long): DailyTaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailyTask(task: DailyTaskEntity): Long

    @Query(
        "UPDATE daily_tasks SET current_value = current_value + :increment, " +
        "status = CASE WHEN (current_value + :increment) >= target_value THEN 'COMPLETED' ELSE 'IN_PROGRESS' END, " +
        "completed_at = CASE WHEN (current_value + :increment) >= target_value THEN datetime('now') ELSE NULL END " +
        "WHERE task_id = :taskId AND status NOT IN ('COMPLETED', 'CLAIMED')"
    )
    suspend fun updateTaskProgress(taskId: Long, increment: Int): Int

    @Query("UPDATE daily_tasks SET status = 'CLAIMED' WHERE task_id = :taskId AND status = 'COMPLETED'")
    suspend fun claimDailyTask(taskId: Long): Int

    // --- AI Questions ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAIQuestion(aiQuestion: AIQuestionEntity): Long

    @Query("SELECT * FROM ai_questions WHERE user_id = :userId ORDER BY created_at DESC")
    suspend fun getAIHistory(userId: Long): List<AIQuestionEntity>

    @Query("DELETE FROM ai_questions WHERE ai_question_id = :aiQuestionId")
    suspend fun deleteAIQuestion(aiQuestionId: Long): Int

    @Query("DELETE FROM ai_questions WHERE user_id = :userId")
    suspend fun clearAIHistory(userId: Long): Int

    // --- Streak & Profile Customization ---
    @Query("UPDATE user_profiles SET current_streak = :streak, last_active_date = :lastActiveDate WHERE user_id = :userId")
    suspend fun updateStreak(userId: Long, streak: Int, lastActiveDate: String): Int

    @Query("UPDATE user_profiles SET display_name = :displayName, avatar_uri = :avatarUri, study_motto = :motto WHERE user_id = :userId")
    suspend fun updateProfileInfo(userId: Long, displayName: String, avatarUri: String?, motto: String?): Int

    // --- Vocabulary Notes & Bookmarks ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVocabulary(vocab: VocabularyEntity): Long

    @Query("SELECT * FROM vocabulary_notes WHERE user_id = :userId ORDER BY vocab_id DESC")
    suspend fun getVocabularyList(userId: Long): List<VocabularyEntity>

    @Query("UPDATE vocabulary_notes SET is_mastered = :isMastered WHERE vocab_id = :vocabId")
    suspend fun updateVocabularyMastered(vocabId: Long, isMastered: Boolean): Int

    @Query("DELETE FROM vocabulary_notes WHERE vocab_id = :vocabId")
    suspend fun deleteVocabulary(vocabId: Long): Int

    @Query("SELECT COUNT(*) FROM vocabulary_notes WHERE user_id = :userId AND word = :word")
    suspend fun isWordBookmarked(userId: Long, word: String): Int

    @Query("UPDATE user_profiles SET streak_shields = :shields WHERE user_id = :userId")
    suspend fun updateStreakShields(userId: Long, shields: Int): Int

    // --- Weak Points (Smart Mistake Bank) ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeakPoint(point: WeakPointEntity): Long

    @Query("SELECT * FROM weak_points WHERE user_id = :userId ORDER BY wrong_count DESC, last_failed_at DESC")
    suspend fun getWeakPoints(userId: Long): List<WeakPointEntity>

    @Query("SELECT * FROM weak_points WHERE user_id = :userId AND question = :question LIMIT 1")
    suspend fun getWeakPointByQuestion(userId: Long, question: String): WeakPointEntity?

    @Query("UPDATE weak_points SET wrong_count = wrong_count + 1, last_failed_at = :failedAt WHERE weak_id = :weakId")
    suspend fun incrementWeakPointCount(weakId: Long, failedAt: String): Int

    @Query("DELETE FROM weak_points WHERE weak_id = :weakId")
    suspend fun deleteWeakPoint(weakId: Long): Int

    @Query("DELETE FROM weak_points WHERE user_id = :userId")
    suspend fun clearWeakPoints(userId: Long): Int

    @Query("SELECT COUNT(*) FROM weak_points WHERE user_id = :userId")
    suspend fun getWeakPointsCount(userId: Long): Int
}


