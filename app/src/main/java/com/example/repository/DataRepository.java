package com.example.repository;

import android.content.Context;
import androidx.room.Room;
import com.example.model.QAItem;
import com.example.model.QuestItem;
import com.example.sqlite.room.*;
import org.json.JSONArray;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DataRepository {

    private AppDatabase db;
    private AppDao dao;
    private Context context;
    private static final String DAILY_STATE_PREFS = "StudyMentorDailyState";
    private static final String MAP_PROGRESS_PREFS = "StudyMentorMapProgress";

    public DataRepository(Context context) {
        this.context = context.getApplicationContext();
        db = Room.databaseBuilder(context.getApplicationContext(), AppDatabase.class, "AIStudyMentorRoom.db")
                .fallbackToDestructiveMigration()
                .allowMainThreadQueries()
                .build();
        dao = db.appDao();
        seedInitialData();
    }

    private void seedInitialData() {
        new Thread(() -> {
            List<String> existing = dao.getActiveChapters();
            if (existing == null || existing.isEmpty()) {
                try {
                    java.io.InputStream is = context.getAssets().open("questions.json");
                    int size = is.available();
                    byte[] buffer = new byte[size];
                    is.read(buffer);
                    is.close();
                    String json = new String(buffer, "UTF-8");
                    org.json.JSONObject root = new org.json.JSONObject(json);

                    String[] levels = {"Middle School", "High School", "University / College"};
                    
                    for (String level : levels) {
                        for (java.util.Iterator<String> it = root.keys(); it.hasNext(); ) {
                            String subject = it.next();
                            org.json.JSONObject chaptersObj = root.getJSONObject(subject);

                            int cIndex = 1;
                            for (java.util.Iterator<String> itC = chaptersObj.keys(); itC.hasNext(); ) {
                                String chapterTitle = itC.next();
                                org.json.JSONArray questionsArr = chaptersObj.getJSONArray(chapterTitle);

                                ChapterEntity chapter = new ChapterEntity();
                                chapter.level = level;
                                chapter.subject = subject;
                                chapter.title = chapterTitle;
                                chapter.orderNo = cIndex++;
                                chapter.isActive = 1;
                                long chapterId = dao.insertChapter(chapter);

                                for (int i = 0; i < questionsArr.length(); i++) {
                                    org.json.JSONObject qObj = questionsArr.getJSONObject(i);
                                    String qText = qObj.getString("question");
                                    String optionsJson = qObj.getJSONArray("options").toString();
                                    int correctIdx = Integer.parseInt(qObj.getString("correctAnswer"));
                                    insertQuestion(chapterId, qText, optionsJson, correctIdx);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            List<UserProfileEntity> lb = dao.getLeaderboard();
            if (lb == null || lb.size() < 10) {
                String[] names = {"Alex", "Jordan", "Taylor", "Morgan", "Casey", "Riley", "Jamie", "Skyler", "Cameron", "Quinn"};
                int[] xps = {15200, 14500, 13100, 12500, 11000, 9500, 8000, 7500, 6000, 5000};
                for (int i = 0; i < 10; i++) {
                    UserEntity dummy = new UserEntity();
                    dummy.email = "dummy" + i + "@test.com";
                    dummy.passwordHash = "xxx";
                    long id = dao.insertUser(dummy);
                    if (id != -1) {
                        UserProfileEntity p = new UserProfileEntity();
                        p.userId = id;
                        p.displayName = names[i];
                        p.totalXp = xps[i];
                        dao.insertUserProfile(p);
                    }
                }
            }
        }).start();
    }



    private void insertQuestion(long chapterId, String text, String optionsJson, int correctIdx) {
        QuestionEntity q = new QuestionEntity();
        q.chapterId = chapterId;
        q.questionText = text;
        q.optionsJson = optionsJson;
        q.correctAnswer = String.valueOf(correctIdx);
        dao.insertQuestion(q);
    }

    public List<String> getChaptersByPreference(String level, String subject) {
        return dao.getChaptersByPreference(level, subject);
    }

    public long getChapterIdByTitle(String title) {
        Long id = dao.getChapterIdByTitle(title);
        return id != null ? id : -1;
    }

    public List<QuestItem> getQuestionsByChapter(long chapterId) {
        List<QuestionEntity> questions = dao.getQuestionsByChapter(chapterId);
        List<QuestItem> items = new ArrayList<>();
        for (QuestionEntity q : questions) {
            try {
                JSONArray array = new JSONArray(q.optionsJson);
                List<String> options = new ArrayList<>();
                for (int i = 0; i < array.length(); i++) {
                    options.add(array.getString(i));
                }
                items.add(new QuestItem(q.questionText, options, Integer.parseInt(q.correctAnswer)));
            } catch (Exception e) { e.printStackTrace(); }
        }
        return items;
    }

    public long registerUser(String name, String email, String passwordHash) {
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        Long existingId = dao.getUserIdByEmail(normalizedEmail);
        if (existingId != null) return -2;

        UserEntity user = new UserEntity();
        user.email = normalizedEmail;
        user.passwordHash = passwordHash;
        long userId = dao.insertUser(user);

        if (userId != -1) {
            UserProfileEntity profile = new UserProfileEntity();
            profile.userId = userId;
            profile.displayName = name;
            dao.insertUserProfile(profile);
        }
        return userId;
    }

    public UserEntity login(String email, String passwordHash) {
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        return dao.loginUser(normalizedEmail, passwordHash);
    }

    public long getUserIdByEmail(String email) {
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        Long id = dao.getUserIdByEmail(normalizedEmail);
        return id != null ? id : -1;
    }

    public UserProfileEntity getUserProfile(long userId) {
        return dao.getUserProfile(userId);
    }

    public List<String> getActiveChapters() {
        return dao.getActiveChapters();
    }

    public void saveAIQuestion(long userId, String question, String imageUri, String aiAnswer) {
        AIQuestionEntity ai = new AIQuestionEntity();
        ai.userId = userId;
        ai.questionText = question;
        ai.imageUri = imageUri;
        ai.aiAnswer = aiAnswer;
        ai.createdAt = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(new java.util.Date());
        dao.insertAIQuestion(ai);
    }

    public List<QAItem> getAIHistory(long userId) {
        List<AIQuestionEntity> list = dao.getAIHistory(userId);
        List<QAItem> items = new ArrayList<>();
        for (AIQuestionEntity ai : list) {
            items.add(new QAItem((int)ai.aiQuestionId, ai.questionText, ai.imageUri, ai.aiAnswer, 0));
        }
        return items;
    }

    public void updateXP(long userId, int xpGain) {
        dao.updateXP(userId, xpGain);
    }

    public void updateMana(long userId, int manaGain) {
        dao.updateMana(userId, manaGain);
    }

    public List<DailyTaskEntity> getOrCreateDailyTasks(long userId, String taskDate) {
        List<DailyTaskEntity> tasks = dao.getDailyTasks(userId, taskDate);
        if (tasks != null && !tasks.isEmpty()) return tasks;

        insertDailyTask(userId, taskDate, "LESSON_COMPLETE", 1, 50);
        insertDailyTask(userId, taskDate, "PERFECT_SCORE", 1, 100);
        insertDailyTask(userId, taskDate, "ANSWER_QUESTIONS", 10, 50);
        return dao.getDailyTasks(userId, taskDate);
    }

    private void insertDailyTask(long userId, String taskDate, String taskType, int targetValue, int rewardXp) {
        DailyTaskEntity task = new DailyTaskEntity();
        task.userId = userId;
        task.taskDate = taskDate;
        task.taskType = taskType;
        task.title = taskType;
        task.targetValue = targetValue;
        task.currentValue = 0;
        task.rewardXp = rewardXp;
        task.status = "IN_PROGRESS";
        dao.insertDailyTask(task);
    }

    public void updateDailyTaskProgress(long userId, String taskDate, String taskType, int increment) {
        List<DailyTaskEntity> tasks = dao.getDailyTasks(userId, taskDate);
        if (tasks == null) return;
        for (DailyTaskEntity task : tasks) {
            if (taskType.equals(task.taskType)) {
                dao.updateTaskProgress(task.taskId, increment);
                return;
            }
        }
    }

    public DailyTaskEntity getDailyTask(long taskId) {
        return dao.getDailyTask(taskId);
    }

    public boolean claimDailyTask(long taskId) {
        return dao.claimDailyTask(taskId) > 0;
    }

    private String chestKey(long userId, String taskDate) {
        return "chest_" + userId + "_" + taskDate;
    }

    public boolean isDailyChestClaimed(long userId, String taskDate) {
        return context.getSharedPreferences(DAILY_STATE_PREFS, Context.MODE_PRIVATE)
                .getBoolean(chestKey(userId, taskDate), false);
    }

    public synchronized boolean claimDailyChest(long userId, String taskDate) {
        android.content.SharedPreferences preferences = context.getSharedPreferences(
                DAILY_STATE_PREFS, Context.MODE_PRIVATE);
        String key = chestKey(userId, taskDate);
        if (preferences.getBoolean(key, false)) return false;
        return preferences.edit().putBoolean(key, true).commit();
    }

    private String mapNodeKey(long userId, String chapterTitle) {
        return "node_" + userId + "_" + chapterTitle;
    }

    public int getCurrentLessonNode(long userId, String chapterTitle) {
        return context.getSharedPreferences(MAP_PROGRESS_PREFS, Context.MODE_PRIVATE)
                .getInt(mapNodeKey(userId, chapterTitle), 1);
    }

    public void saveCurrentLessonNode(long userId, String chapterTitle, int node) {
        int safeNode = Math.max(1, Math.min(6, node));
        context.getSharedPreferences(MAP_PROGRESS_PREFS, Context.MODE_PRIVATE)
                .edit()
                .putInt(mapNodeKey(userId, chapterTitle), safeNode)
                .apply();
    }

    public List<UserProfileEntity> getLeaderboard() {
        return dao.getLeaderboard();
    }
}
