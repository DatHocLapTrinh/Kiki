package com.example.sqlite.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "weak_points",
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
data class WeakPointEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "weak_id")
    var weakId: Long = 0,

    @ColumnInfo(name = "user_id")
    var userId: Long = 0,

    @ColumnInfo(name = "question")
    var question: String = "",

    @ColumnInfo(name = "options_json")
    var optionsJson: String = "",

    @ColumnInfo(name = "correct_index")
    var correctIndex: Int = 0,

    @ColumnInfo(name = "wrong_count")
    var wrongCount: Int = 1,

    @ColumnInfo(name = "last_failed_at")
    var lastFailedAt: String = ""
)
