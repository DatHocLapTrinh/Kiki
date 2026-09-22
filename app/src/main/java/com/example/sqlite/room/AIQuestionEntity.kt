package com.example.sqlite.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "ai_questions",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["user_id"],
            childColumns = ["user_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["user_id"])]
)
data class AIQuestionEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "ai_question_id")
    var aiQuestionId: Long = 0,

    @ColumnInfo(name = "user_id")
    var userId: Long = 0,

    @ColumnInfo(name = "question_text")
    var questionText: String = "",

    @ColumnInfo(name = "image_uri")
    var imageUri: String? = null,

    @ColumnInfo(name = "image_description")
    var imageDescription: String? = null,

    @ColumnInfo(name = "ai_answer")
    var aiAnswer: String = "",

    @ColumnInfo(name = "status")
    var status: String? = null,

    @ColumnInfo(name = "created_at")
    var createdAt: String? = null
)
