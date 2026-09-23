package com.example.sqlite.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "user_profiles",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["user_id"],
            childColumns = ["user_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class UserProfileEntity(
    @PrimaryKey
    @ColumnInfo(name = "user_id")
    var userId: Long = 0,

    @ColumnInfo(name = "display_name")
    var displayName: String = "",

    @ColumnInfo(name = "avatar_uri")
    var avatarUri: String? = null,

    @ColumnInfo(name = "total_xp")
    var totalXp: Int = 0,

    @ColumnInfo(name = "mana")
    var mana: Int = 20,

    @ColumnInfo(name = "total_questions")
    var totalQuestions: Int = 0,

    @ColumnInfo(name = "correct_answers")
    var correctAnswers: Int = 0,

    @ColumnInfo(name = "current_streak")
    var currentStreak: Int = 0,

    @ColumnInfo(name = "badges_json")
    var badgesJson: String? = null,

    @ColumnInfo(name = "reminder_time")
    var reminderTime: String? = null,

    @ColumnInfo(name = "notifications_enabled")
    var notificationsEnabled: Int = 1,

    @ColumnInfo(name = "last_active_date")
    var lastActiveDate: String? = null,

    @ColumnInfo(name = "study_motto")
    var studyMotto: String? = null
)

