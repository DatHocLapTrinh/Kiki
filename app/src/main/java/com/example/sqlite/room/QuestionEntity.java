package com.example.sqlite.room;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;
import androidx.room.ForeignKey;
import androidx.annotation.NonNull;

@Entity(tableName = "questions",
        foreignKeys = @ForeignKey(entity = ChapterEntity.class,
                                  parentColumns = "chapter_id",
                                  childColumns = "chapter_id",
                                  onDelete = ForeignKey.CASCADE))
public class QuestionEntity {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "question_id")
    public long questionId;

    @ColumnInfo(name = "chapter_id")
    public long chapterId;

    @NonNull
    @ColumnInfo(name = "question_text")
    public String questionText;

    @ColumnInfo(name = "question_type")
    public String questionType;

    @ColumnInfo(name = "options_json")
    public String optionsJson;

    @ColumnInfo(name = "correct_answer")
    public String correctAnswer;

    @ColumnInfo(name = "explanation")
    public String explanation;

    @ColumnInfo(name = "points")
    public int points = 0;

    @ColumnInfo(name = "difficulty")
    public String difficulty;
}
