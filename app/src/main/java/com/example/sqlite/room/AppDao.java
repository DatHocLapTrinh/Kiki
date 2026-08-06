package com.example.sqlite.room;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface AppDao {
    @Insert
    long insertUser(UserEntity user);

    @Insert
    long insertUserProfile(UserProfileEntity profile);

    @Query("SELECT user_id FROM users WHERE email = :email LIMIT 1")
    Long getUserIdByEmail(String email);

    @Query("SELECT * FROM users WHERE email = :email AND password_hash = :passwordHash LIMIT 1")
    UserEntity loginUser(String email, String passwordHash);

    @Query("SELECT * FROM user_profiles WHERE user_id = :userId LIMIT 1")
    UserProfileEntity getUserProfile(long userId);

    @Query("UPDATE user_profiles SET total_xp = total_xp + :xpGain WHERE user_id = :userId")
    void updateXP(long userId, int xpGain);

    @Query("UPDATE user_profiles SET mana = mana + :manaGain WHERE user_id = :userId")
    void updateMana(long userId, int manaGain);

    @Query("SELECT * FROM user_profiles ORDER BY total_xp DESC LIMIT 10")
    List<UserProfileEntity> getLeaderboard();

    // --- Chapters ---
    @Insert
    long insertChapter(ChapterEntity chapter);

    @Query("SELECT title FROM chapters WHERE level = :level AND subject = :subject ORDER BY order_no")
    List<String> getChaptersByPreference(String level, String subject);

    @Query("SELECT chapter_id FROM chapters WHERE title = :title LIMIT 1")
    Long getChapterIdByTitle(String title);

    @Query("SELECT title FROM chapters WHERE is_active = 1 ORDER BY order_no")
    List<String> getActiveChapters();

    // --- Questions ---
    @Insert
    long insertQuestion(QuestionEntity question);

    @Query("SELECT * FROM questions WHERE chapter_id = :chapterId")
    List<QuestionEntity> getQuestionsByChapter(long chapterId);

    // --- Quiz Attempts ---
    @Insert
    long insertQuizAttempt(QuizAttemptEntity attempt);

    // --- Daily Tasks ---
    @Query("SELECT * FROM daily_tasks WHERE user_id = :userId AND task_date = :taskDate")
    List<DailyTaskEntity> getDailyTasks(long userId, String taskDate);

    @Query("SELECT * FROM daily_tasks WHERE task_id = :taskId LIMIT 1")
    DailyTaskEntity getDailyTask(long taskId);

    @Insert
    long insertDailyTask(DailyTaskEntity task);

    @Query("UPDATE daily_tasks SET current_value = current_value + :increment, " +
           "status = CASE WHEN (current_value + :increment) >= target_value THEN 'COMPLETED' ELSE 'IN_PROGRESS' END, " +
           "completed_at = CASE WHEN (current_value + :increment) >= target_value THEN datetime('now') ELSE NULL END " +
           "WHERE task_id = :taskId AND status NOT IN ('COMPLETED', 'CLAIMED')")
    int updateTaskProgress(long taskId, int increment);

    @Query("UPDATE daily_tasks SET status = 'CLAIMED' WHERE task_id = :taskId AND status = 'COMPLETED'")
    int claimDailyTask(long taskId);

    // --- AI Questions ---
    @Insert
    long insertAIQuestion(AIQuestionEntity aiQuestion);

    @Query("SELECT * FROM ai_questions WHERE user_id = :userId ORDER BY created_at DESC")
    List<AIQuestionEntity> getAIHistory(long userId);
}
