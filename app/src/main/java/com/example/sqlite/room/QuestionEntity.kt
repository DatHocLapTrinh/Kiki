package com.example.sqlite.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "questions",
    foreignKeys = [
        ForeignKey(
            entity = ChapterEntity::class,
            parentColumns = ["chapter_id"],
            childColumns = ["chapter_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["chapter_id"])]
)
data class QuestionEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "question_id")
    var questionId: Long = 0,

    @ColumnInfo(name = "chapter_id")
    var chapterId: Long = 0,

    @ColumnInfo(name = "question_text")
    var questionText: String = "",

    @ColumnInfo(name = "question_type")
    var questionType: String? = null,

    @ColumnInfo(name = "options_json")
    var optionsJson: String = "",

    @ColumnInfo(name = "correct_answer")
    var correctAnswer: String = "0",

    @ColumnInfo(name = "explanation")
    var explanation: String? = null,

    @ColumnInfo(name = "points")
    var points: Int = 0,

    @ColumnInfo(name = "difficulty")
    var difficulty: String? = null
)
