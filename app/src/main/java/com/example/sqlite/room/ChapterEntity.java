package com.example.sqlite.room;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;
import androidx.annotation.NonNull;

@Entity(tableName = "chapters")
public class ChapterEntity {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "chapter_id")
    public long chapterId;

    @NonNull
    @ColumnInfo(name = "title")
    public String title;

    @ColumnInfo(name = "description")
    public String description;

    @ColumnInfo(name = "thumbnail_uri")
    public String thumbnailUri;

    @ColumnInfo(name = "order_no")
    public int orderNo;

    @ColumnInfo(name = "level")
    public String level;

    @ColumnInfo(name = "subject")
    public String subject;

    @ColumnInfo(name = "is_active")
    public int isActive = 1;
}
