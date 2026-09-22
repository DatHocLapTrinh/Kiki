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
}
