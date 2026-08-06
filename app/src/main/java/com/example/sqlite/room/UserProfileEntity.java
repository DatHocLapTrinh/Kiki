package com.example.sqlite.room;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;
import androidx.room.ForeignKey;

@Entity(tableName = "user_profiles",
        foreignKeys = @ForeignKey(entity = UserEntity.class,
                                  parentColumns = "user_id",
                                  childColumns = "user_id",
                                  onDelete = ForeignKey.CASCADE))
public class UserProfileEntity {
    @PrimaryKey
    @ColumnInfo(name = "user_id")
    public long userId;

    @ColumnInfo(name = "display_name")
    public String displayName;

    @ColumnInfo(name = "avatar_uri")
    public String avatarUri;

    @ColumnInfo(name = "total_xp")
    public int totalXp = 0;

    @ColumnInfo(name = "mana")
    public int mana = 20;

    @ColumnInfo(name = "total_questions")
    public int totalQuestions = 0;

    @ColumnInfo(name = "correct_answers")
    public int correctAnswers = 0;

    @ColumnInfo(name = "current_streak")
    public int currentStreak = 0;

    @ColumnInfo(name = "badges_json")
    public String badgesJson;

    @ColumnInfo(name = "reminder_time")
    public String reminderTime;

    @ColumnInfo(name = "notifications_enabled")
    public int notificationsEnabled = 1;
}
