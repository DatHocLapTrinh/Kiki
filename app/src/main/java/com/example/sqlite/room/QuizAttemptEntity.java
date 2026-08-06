package com.example.sqlite.room;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;
import androidx.room.ForeignKey;

@Entity(tableName = "quiz_attempts",
        foreignKeys = {
            @ForeignKey(entity = UserEntity.class,
                        parentColumns = "user_id",
                        childColumns = "user_id",
                        onDelete = ForeignKey.CASCADE),
            @ForeignKey(entity = ChapterEntity.class,
                        parentColumns = "chapter_id",
                        childColumns = "chapter_id",
                        onDelete = ForeignKey.CASCADE)
        })
public class QuizAttemptEntity {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "attempt_id")
    public long attemptId;

    @ColumnInfo(name = "user_id")
    public long userId;

    @ColumnInfo(name = "chapter_id")
    public long chapterId;

    @ColumnInfo(name = "answers_json")
    public String answersJson;

    @ColumnInfo(name = "total_questions")
    public int totalQuestions;

    @ColumnInfo(name = "correct_answers")
    public int correctAnswers;

    @ColumnInfo(name = "score")
    public int score;

    @ColumnInfo(name = "status")
    public String status;

    @ColumnInfo(name = "started_at")
    public String startedAt;

    @ColumnInfo(name = "completed_at")
    public String completedAt;
}
