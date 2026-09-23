package com.example.sqlite.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "vocabulary_notes",
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
data class VocabularyEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "vocab_id")
    var vocabId: Long = 0,

    @ColumnInfo(name = "user_id")
    var userId: Long = 0,

    @ColumnInfo(name = "word")
    var word: String = "",

    @ColumnInfo(name = "phonetic")
    var phonetic: String = "",

    @ColumnInfo(name = "meaning")
    var meaning: String = "",

    @ColumnInfo(name = "example")
    var example: String = "",

    @ColumnInfo(name = "is_mastered")
    var isMastered: Boolean = false,

    @ColumnInfo(name = "created_at")
    var createdAt: String = ""
)
