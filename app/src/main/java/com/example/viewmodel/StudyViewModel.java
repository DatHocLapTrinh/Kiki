package com.example.viewmodel;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Base64;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import dagger.hilt.android.lifecycle.HiltViewModel;
import javax.inject.Inject;
import com.example.BuildConfig;
import com.example.model.GeminiModels;
import com.example.model.QAItem;
import com.example.model.QuestItem;
import com.example.network.RetrofitClient;
import com.example.repository.DataRepository;


import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import retrofit2.Response;

@HiltViewModel
public class StudyViewModel extends ViewModel {
    public final DataRepository repository;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    private final MutableLiveData<List<QAItem>> _history = new MutableLiveData<>(new ArrayList<>());
    public LiveData<List<QAItem>> getHistory() { return _history; }

    private final MutableLiveData<Boolean> _isEnglish = new MutableLiveData<>(false);
    public LiveData<Boolean> isEnglish() { return _isEnglish; }
    public void setEnglish(boolean isEnglish) { _isEnglish.setValue(isEnglish); }

    private final MutableLiveData<String> _userName = new MutableLiveData<>("Adventurer");
    public LiveData<String> getUserName() { return _userName; }

    private final MutableLiveData<Long> _currentUserId = new MutableLiveData<>(-1L);
    public LiveData<Long> getCurrentUserId() { return _currentUserId; }

    private final MutableLiveData<Boolean> _isGenerating = new MutableLiveData<>(false);
    public LiveData<Boolean> isGenerating() { return _isGenerating; }

    private final MutableLiveData<Integer> _xp = new MutableLiveData<>(0);
    public LiveData<Integer> getXp() { return _xp; }

    private final MutableLiveData<Integer> _level = new MutableLiveData<>(1);
    public LiveData<Integer> getLevel() { return _level; }

    private final MutableLiveData<String> _rankTitle = new MutableLiveData<>("Bronze Novice");
    public LiveData<String> getRankTitle() { return _rankTitle; }

    public String calculateRankTitle(int xp) {
        if (xp >= 15000) return "Grandmaster";
        if (xp >= 13000) return "Crystal Knight";
        if (xp >= 11000) return "Gold Wizard";
        if (xp >= 9000) return "Silver Apprentice";
        return "Bronze Novice";
    }

    @Inject
    public StudyViewModel(DataRepository repository) {
        this.repository = repository;
    }

    private final MutableLiveData<Integer> _streak = new MutableLiveData<>(0);
    public LiveData<Integer> getStreak() { return _streak; }

    private final MutableLiveData<Integer> _mana = new MutableLiveData<>(20);
    public LiveData<Integer> getMana() { return _mana; }

    private final MutableLiveData<Integer> _currentNode = new MutableLiveData<>(1);
    public LiveData<Integer> getCurrentNode() { return _currentNode; }

    private final MutableLiveData<List<com.example.sqlite.room.UserProfileEntity>> _leaderboard = new MutableLiveData<>(new ArrayList<>());
    public LiveData<List<com.example.sqlite.room.UserProfileEntity>> getLeaderboard() { return _leaderboard; }

    private final MutableLiveData<List<com.example.sqlite.room.DailyTaskEntity>> _dailyTasks = new MutableLiveData<>(new ArrayList<>());
    public LiveData<List<com.example.sqlite.room.DailyTaskEntity>> getDailyTasks() { return _dailyTasks; }

    private final MutableLiveData<Boolean> _dailyChestOpened = new MutableLiveData<>(false);
    public LiveData<Boolean> isDailyChestOpened() { return _dailyChestOpened; }

    public void fetchLeaderboard() {
        executor.execute(() -> {
            List<com.example.sqlite.room.UserProfileEntity> lb = repository.getLeaderboard();
            _leaderboard.postValue(lb);
        });
    }

    private final MutableLiveData<String> _selectedLevel = new MutableLiveData<>("High School");
    public LiveData<String> getSelectedLevel() { return _selectedLevel; }

    private final MutableLiveData<String> _selectedSubject = new MutableLiveData<>("Physics & Chemistry");
    public LiveData<String> getSelectedSubject() { return _selectedSubject; }

    public void setPreferences(String level, String subject) {
        _selectedLevel.setValue(level);
        _selectedSubject.setValue(subject);
    }

    private final MutableLiveData<List<String>> _availableChapters = new MutableLiveData<>(new ArrayList<>());
    public LiveData<List<String>> getAvailableChapters() { return _availableChapters; }

    private final MutableLiveData<List<Integer>> _chapterProgress = new MutableLiveData<>(new ArrayList<>());
    public LiveData<List<Integer>> getChapterProgress() { return _chapterProgress; }

    private final MutableLiveData<Boolean> _chaptersLoading = new MutableLiveData<>(false);
    public LiveData<Boolean> getChaptersLoading() { return _chaptersLoading; }

    private final MutableLiveData<String> _chaptersError = new MutableLiveData<>(null);
    public LiveData<String> getChaptersError() { return _chaptersError; }

    public void refreshChapters() {
        _chaptersLoading.postValue(true);
        _chaptersError.postValue(null);
        executor.execute(() -> {
            try {
                List<String> data = repository.getChaptersByPreference(_selectedLevel.getValue(), _selectedSubject.getValue());
                _availableChapters.postValue(data);

                Long userId = _currentUserId.getValue();
                List<Integer> progress = new ArrayList<>();
                for (String title : data) {
                    progress.add(userId == null || userId == -1L
                            ? 1
                            : repository.getCurrentLessonNode(userId, title));
                }
                _chapterProgress.postValue(progress);
            } catch (Exception exception) {
                _chaptersError.postValue("Không thể tải bản đồ học. Vui lòng thử lại.");
            } finally {
                _chaptersLoading.postValue(false);
            }
        });
    }

    private final MutableLiveData<String> _currentChapterTitle = new MutableLiveData<>("Genesis Core");
    public LiveData<String> getCurrentChapterTitle() { return _currentChapterTitle; }

    private final MutableLiveData<Integer> _currentChapterIndex = new MutableLiveData<>(1);
    public LiveData<Integer> getCurrentChapterIndex() { return _currentChapterIndex; }

    public void selectChapterByTitle(String title, int index) {
        executor.execute(() -> {
            long id = repository.getChapterIdByTitle(title);
            _currentChapterTitle.postValue(title);
            _currentChapterIndex.postValue(index);
            Long userId = _currentUserId.getValue();
            int savedNode = userId == null || userId == -1L
                    ? 1
                    : repository.getCurrentLessonNode(userId, title);
            _currentNode.postValue(savedNode);
            if (id != -1) loadQuestions(id, savedNode);
        });
    }

    private final MutableLiveData<List<QuestItem>> _questions = new MutableLiveData<>(new ArrayList<>());
    public LiveData<List<QuestItem>> getQuestions() { return _questions; }

    private final MutableLiveData<List<QuestItem>> _lastQuestResults = new MutableLiveData<>(new ArrayList<>());
    public LiveData<List<QuestItem>> getLastQuestResults() { return _lastQuestResults; }

    private List<QuestItem> _allChapterQuestions = new ArrayList<>();

    public void loadQuestions(long chapterId) {
        loadQuestions(chapterId, 1);
    }

    private void loadQuestions(long chapterId, int lessonIndex) {
        executor.execute(() -> {
            _allChapterQuestions = repository.getQuestionsByChapter(chapterId);
            startLesson(lessonIndex);
        });
    }

    public void startLesson(int lessonIndex) {
        int startIndex = (lessonIndex - 1) * 10;
        int endIndex = Math.min(startIndex + 10, _allChapterQuestions.size());
        if (startIndex >= 0 && startIndex < _allChapterQuestions.size()) {
            _questions.postValue(_allChapterQuestions.subList(startIndex, endIndex));
        } else {
            _questions.postValue(new ArrayList<>());
        }
    }

    public void completeCurrentLesson() {
        Long userId = _currentUserId.getValue();
        String chapterTitle = _currentChapterTitle.getValue();
        Integer currentNode = _currentNode.getValue();
        if (userId == null || userId == -1L || chapterTitle == null || currentNode == null) return;

        int nextNode = Math.min(currentNode + 1, 6);
        _currentNode.setValue(nextNode);
        executor.execute(() -> {
            repository.saveCurrentLessonNode(userId, chapterTitle, nextNode);
            refreshChapters();
        });
    }

    private final MutableLiveData<String> _questAnalysis = new MutableLiveData<>(null);
    public LiveData<String> getQuestAnalysis() { return _questAnalysis; }

    public void setStreak(int streak) { _streak.setValue(streak); }
    public void setMana(int mana) { _mana.setValue(mana); }
    public void setCurrentNode(int node) { _currentNode.setValue(node); }

    public void analyzeQuestResults(Context context, List<QuestItem> results) {
        _lastQuestResults.setValue(results);
        _questAnalysis.setValue(null);
        
        int correctCount = 0;
        for (QuestItem item : results) {
            if (item.getSelectedIndex() == item.getCorrectIndex()) {
                correctCount++;
            }
        }
        if (correctCount > 0) {
            addXp(correctCount * 20);
        }
        final int finalCorrectCount = correctCount;

        executor.execute(() -> {
            Long userId = _currentUserId.getValue();
            if (userId != null && userId != -1L) {
                recordDailyTaskProgress(userId, "LESSON_COMPLETE", 1);
                recordDailyTaskProgress(userId, "ANSWER_QUESTIONS", results.size());
                if (!results.isEmpty() && finalCorrectCount == results.size()) {
                    recordDailyTaskProgress(userId, "PERFECT_SCORE", 1);
                }
                refreshDailyTasks();
            }

            StringBuilder prompt = new StringBuilder("Here are the student's exercise results:\n");
            for (QuestItem item : results) {
                String selected = (item.getSelectedIndex() >= 0 && item.getSelectedIndex() < item.getOptions().size()) 
                    ? item.getOptions().get(item.getSelectedIndex()) : "Skipped";
                prompt.append("Question: ").append(item.getQuestion())
                      .append("\nStudent chose: ").append(selected)
                      .append("\nCorrect answer: ").append(item.getOptions().get(item.getCorrectIndex())).append("\n");
            }
            prompt.append("\nAct as an AI tutor, analyze weaknesses based on incorrect answers, explain briefly, and propose a study plan. DO NOT use markdown format, asterisks, hashes, or any special formatting symbols. Use plain text only.");
            
            String analysis = fetchAnswerFromGemini(context, prompt.toString(), null);
            _questAnalysis.postValue(analysis);
        });
    }

    public void onLoginSuccess(String email) {
        executor.execute(() -> {
            long userId = repository.getUserIdByEmail(email);
            if (userId != -1L) {
                _currentUserId.postValue(userId);
                _history.postValue(new ArrayList<>());
                com.example.sqlite.room.UserProfileEntity profile = repository.getUserProfile(userId);
                if (profile != null) {
                    _userName.postValue(profile.displayName);
                    _xp.postValue(profile.totalXp);
                    _level.postValue((profile.totalXp / 100) + 1);
                    _rankTitle.postValue(calculateRankTitle(profile.totalXp));
                    _mana.postValue(profile.mana);
                    _streak.postValue(profile.currentStreak);
                }
                fetchLeaderboard();
                refreshDailyTasks();
            }
        });
    }

    private String today() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
    }

    public void refreshDailyTasks() {
        Long userId = _currentUserId.getValue();
        if (userId == null || userId == -1L) {
            _dailyTasks.postValue(new ArrayList<>());
            _dailyChestOpened.postValue(false);
            return;
        }

        executor.execute(() -> {
            String date = today();
            List<com.example.sqlite.room.DailyTaskEntity> tasks = repository.getOrCreateDailyTasks(userId, date);
            _dailyTasks.postValue(tasks);
            _dailyChestOpened.postValue(repository.isDailyChestClaimed(userId, date));
        });
    }

    private void recordDailyTaskProgress(long userId, String taskType, int increment) {
        String date = today();
        repository.getOrCreateDailyTasks(userId, date);
        repository.updateDailyTaskProgress(userId, date, taskType, increment);
    }

    public void claimDailyTask(long taskId) {
        executor.execute(() -> {
            com.example.sqlite.room.DailyTaskEntity task = repository.getDailyTask(taskId);
            if (task != null && repository.claimDailyTask(taskId)) {
                addXp(task.rewardXp);
                refreshDailyTasks();
            }
        });
    }

    public void claimDailyChest() {
        Long userId = _currentUserId.getValue();
        if (userId == null || userId == -1L) return;

        executor.execute(() -> {
            String date = today();
            List<com.example.sqlite.room.DailyTaskEntity> tasks = repository.getOrCreateDailyTasks(userId, date);
            boolean allClaimed = !tasks.isEmpty();
            for (com.example.sqlite.room.DailyTaskEntity task : tasks) {
                if (!"CLAIMED".equals(task.status)) {
                    allClaimed = false;
                    break;
                }
            }
            if (allClaimed && repository.claimDailyChest(userId, date)) {
                addXp(500);
                _dailyChestOpened.postValue(true);
                refreshDailyTasks();
            }
        });
    }

    public void refreshHistory(long userId) {
        executor.execute(() -> {
            List<QAItem> data = repository.getAIHistory(userId);
            _history.postValue(data);
        });
    }

    public void askQuestion(Context context, String question, Uri imageUri, Runnable onSuccess) {
        if (question.isEmpty() && imageUri == null) return;
        _isGenerating.postValue(true);
        executor.execute(() -> {
            try {
                String answer = fetchAnswerFromGemini(context, question, imageUri);
                Long userId = _currentUserId.getValue();
                if (userId != null && userId != -1L) {
                    repository.saveAIQuestion(userId, question, imageUri != null ? imageUri.toString() : null, answer);
                    List<QAItem> data = repository.getAIHistory(userId);
                    _history.postValue(data);
                    repository.updateXP(userId, 20);
                    Integer currentXp = _xp.getValue();
                    if (currentXp == null) currentXp = 0;
                    int newXp = currentXp + 20;
                    _xp.postValue(newXp);
                    _level.postValue((newXp / 100) + 1);
                    _rankTitle.postValue(calculateRankTitle(newXp));
                } else {
                    // Chưa đăng nhập: vẫn hiển thị câu trả lời trong session
                    List<QAItem> current = _history.getValue();
                    List<QAItem> updated = new ArrayList<>();
                    updated.add(new QAItem(0, question, imageUri != null ? imageUri.toString() : null, answer, System.currentTimeMillis()));
                    if (current != null) updated.addAll(current);
                    _history.postValue(updated);
                }
            } finally {
                _isGenerating.postValue(false);
                if (onSuccess != null) onSuccess.run();
            }
        });
    }

    public void askFollowUpQuestion(Context context, String currentQuestion, String followUpType) {
        _isGenerating.postValue(true);
        executor.execute(() -> {
            try {
                String prompt = "";
                String title = "";
                if ("EXPLAIN".equals(followUpType)) {
                    prompt = "Based on the question: '" + currentQuestion + "'. Please explain it in more detail, step by step. Use '[STEP] ' prefix for each step. Reply in the same language as the question.";
                    title = "Giải thích thêm: " + currentQuestion;
                } else if ("SIMILAR".equals(followUpType)) {
                    prompt = "Based on the question: '" + currentQuestion + "'. Give me a similar practice exercise with a hint. Use '[STEP] ' prefix for each step. Reply in the same language as the question.";
                    title = "Bài tập tương tự: " + currentQuestion;
                }
                String answer = fetchAnswerFromGemini(context, prompt, null);
                Long userId = _currentUserId.getValue();
                if (userId != null && userId != -1L) {
                    repository.saveAIQuestion(userId, title, null, answer);
                    List<QAItem> data = repository.getAIHistory(userId);
                    _history.postValue(data);
                    repository.updateXP(userId, 10);
                } else {
                    List<QAItem> current = _history.getValue();
                    List<QAItem> updated = new ArrayList<>();
                    updated.add(new QAItem(0, title, null, answer, System.currentTimeMillis()));
                    if (current != null) updated.addAll(current);
                    _history.postValue(updated);
                }
            } finally {
                _isGenerating.postValue(false);
            }
        });
    }

    public void deepAnalysis(Context context, String question) {
        _isGenerating.postValue(true);
        executor.execute(() -> {
            try {
                String prompt = "Perform a deep academic analysis of this topic: '" + question + "'. Cover: 1) Core concept, 2) Why it works (theory), 3) Common mistakes, 4) Real-world applications. Use '[STEP] ' prefix for each section. Reply in the same language as the question.";
                String title = "[Phân tích sâu] " + question;
                String answer = fetchAnswerFromGemini(context, prompt, null);
                Long userId = _currentUserId.getValue();
                if (userId != null && userId != -1L) {
                    repository.saveAIQuestion(userId, title, null, answer);
                    List<QAItem> data = repository.getAIHistory(userId);
                    _history.postValue(data);
                } else {
                    List<QAItem> current = _history.getValue();
                    List<QAItem> updated = new ArrayList<>();
                    updated.add(new QAItem(0, title, null, answer, System.currentTimeMillis()));
                    if (current != null) updated.addAll(current);
                    _history.postValue(updated);
                }
            } finally {
                _isGenerating.postValue(false);
            }
        });
    }

    private final MutableLiveData<List<String>> _quizQuestions = new MutableLiveData<>(new ArrayList<>());
    public LiveData<List<String>> getQuizQuestions() { return _quizQuestions; }

    private final MutableLiveData<Boolean> _quizLoading = new MutableLiveData<>(false);
    public LiveData<Boolean> isQuizLoading() { return _quizLoading; }

    public void generateQuickQuiz(Context context, String question) {
        _quizLoading.postValue(true);
        executor.execute(() -> {
            try {
                String prompt = "Create exactly 3 multiple choice questions to test understanding of: '" + question + "'.\n" +
                    "Format each question EXACTLY like this (no extra text):\n" +
                    "Q: [question text]\n" +
                    "A: [option A]\n" +
                    "B: [option B]\n" +
                    "C: [option C]\n" +
                    "D: [option D]\n" +
                    "ANS: [correct letter A/B/C/D]\n" +
                    "---\n" +
                    "Reply in the same language as the topic.";
                String raw = fetchAnswerFromGemini(context, prompt, null);
                List<String> blocks = new ArrayList<>();
                for (String block : raw.split("---")) {
                    String trimmed = block.trim();
                    if (!trimmed.isEmpty()) blocks.add(trimmed);
                }
                _quizQuestions.postValue(blocks);
            } finally {
                _quizLoading.postValue(false);
            }
        });
    }

    public void addXp(int amount) {
        Integer currentXp = _xp.getValue();
        if (currentXp == null) currentXp = 0;
        int newXp = currentXp + amount;
        _xp.postValue(newXp);
        _level.postValue((newXp / 100) + 1);
        _rankTitle.postValue(calculateRankTitle(newXp));
        
        Long userId = _currentUserId.getValue();
        if (userId != null && userId != -1L) {
            executor.execute(() -> {
                repository.updateXP(userId, amount);
                fetchLeaderboard();
            });
        }
    }

    public void logout() {
        _currentUserId.setValue(-1L);
        _userName.setValue("Adventurer");
        _history.setValue(new ArrayList<>());
        _dailyTasks.setValue(new ArrayList<>());
        _dailyChestOpened.setValue(false);
    }

    private String fetchAnswerFromGemini(Context context, String prompt, Uri uri) {
        try {
            String actualPrompt = prompt.isEmpty() ? "Please explain this image in detail." : prompt;

            List<GeminiModels.Message> messages = new ArrayList<>();
            messages.add(new GeminiModels.Message(
                "system",
                "You are an efficient AI Study Mentor. Provide concise, direct Socratic hints. Step-by-step only if complex. Use '[STEP] ' prefix. Be fast and brief."
            ));
            messages.add(new GeminiModels.Message("user", actualPrompt));

            GeminiModels.GenerateContentRequest request = new GeminiModels.GenerateContentRequest(
                "llama-3.3-70b-versatile", messages, 0.7, 1024
            );

            Response<GeminiModels.GenerateContentResponse> response = RetrofitClient.getService().generateContent(request).execute();
            if (response.isSuccessful() && response.body() != null
                    && response.body().getChoices() != null
                    && !response.body().getChoices().isEmpty()) {
                return response.body().getChoices().get(0).getMessage().getContent();
            }

            String errorMsg = "API Error. ";
            if (response.errorBody() != null) {
                try { errorMsg += response.errorBody().string(); }
                catch (Exception e) { errorMsg += "Code: " + response.code(); }
            } else {
                errorMsg += "Status: " + response.code();
            }
            return errorMsg;
        } catch (Exception e) {
            e.printStackTrace();
            return "Connection/System Error: " + e.getMessage();
        }
    }

    private String uriToBase64(Context context, Uri uri) {
        try {
            InputStream is = context.getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(is);
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, os);
            return Base64.encodeToString(os.toByteArray(), Base64.NO_WRAP);
        } catch (Exception e) {
            return null;
        }
    }
}
