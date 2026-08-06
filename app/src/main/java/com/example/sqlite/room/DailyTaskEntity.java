package com.example.sqlite.room;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;
import androidx.room.ForeignKey;

@Entity(tableName = "daily_tasks",
        foreignKeys = @ForeignKey(entity = UserEntity.class,
                                  parentColumns = "user_id",
                                  childColumns = "user_id",
                                  onDelete = ForeignKey.CASCADE))
public class DailyTaskEntity {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "task_id")
    public long taskId;

    @ColumnInfo(name = "user_id")
    public long userId;

    @ColumnInfo(name = "task_date")
    public String taskDate;

    @ColumnInfo(name = "task_type")
    public String taskType;

    @ColumnInfo(name = "title")
    public String title;

    @ColumnInfo(name = "target_value")
    public int targetValue;

    @ColumnInfo(name = "current_value")
    public int currentValue = 0;

    @ColumnInfo(name = "reward_xp")
    public int rewardXp;

    @ColumnInfo(name = "status")
    public String status;

    @ColumnInfo(name = "completed_at")
    public String completedAt;
}
