package com.example.sqlite.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "daily_tasks",
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
data class DailyTaskEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "task_id")
    var taskId: Long = 0,

    @ColumnInfo(name = "user_id")
    var userId: Long = 0,

    @ColumnInfo(name = "task_date")
    var taskDate: String = "",

    @ColumnInfo(name = "task_type")
    var taskType: String = "",

    @ColumnInfo(name = "title")
    var title: String = "",

    @ColumnInfo(name = "target_value")
    var targetValue: Int = 0,

    @ColumnInfo(name = "current_value")
    var currentValue: Int = 0,

    @ColumnInfo(name = "reward_xp")
    var rewardXp: Int = 0,

    @ColumnInfo(name = "status")
    var status: String = "IN_PROGRESS",

    @ColumnInfo(name = "completed_at")
    var completedAt: String? = null
)
