package com.example.sqlite.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "quiz_attempts",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["user_id"],
            childColumns = ["user_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ChapterEntity::class,
            parentColumns = ["chapter_id"],
            childColumns = ["chapter_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["user_id"]),
        Index(value = ["chapter_id"])
    ]
)
data class QuizAttemptEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "attempt_id")
    var attemptId: Long = 0,

    @ColumnInfo(name = "user_id")
    var userId: Long = 0,

    @ColumnInfo(name = "chapter_id")
    var chapterId: Long = 0,

    @ColumnInfo(name = "answers_json")
    var answersJson: String? = null,

    @ColumnInfo(name = "total_questions")
    var totalQuestions: Int = 0,

    @ColumnInfo(name = "correct_answers")
    var correctAnswers: Int = 0,

    @ColumnInfo(name = "score")
    var score: Int = 0,

    @ColumnInfo(name = "status")
    var status: String? = "COMPLETED",

    @ColumnInfo(name = "started_at")
    var startedAt: String? = null,

    @ColumnInfo(name = "completed_at")
    var completedAt: String? = null
)
