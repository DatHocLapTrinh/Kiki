package com.example.sqlite.room;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;
import androidx.room.ForeignKey;

@Entity(tableName = "ai_questions",
        foreignKeys = @ForeignKey(entity = UserEntity.class,
                                  parentColumns = "user_id",
                                  childColumns = "user_id",
                                  onDelete = ForeignKey.CASCADE))
public class AIQuestionEntity {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "ai_question_id")
    public long aiQuestionId;

    @ColumnInfo(name = "user_id")
    public long userId;

    @ColumnInfo(name = "question_text")
    public String questionText;

    @ColumnInfo(name = "image_uri")
    public String imageUri;

    @ColumnInfo(name = "image_description")
    public String imageDescription;

    @ColumnInfo(name = "ai_answer")
    public String aiAnswer;

    @ColumnInfo(name = "status")
    public String status;

    @ColumnInfo(name = "created_at")
    public String createdAt;
}
