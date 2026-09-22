package com.example.sqlite.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chapters")
data class ChapterEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "chapter_id")
    var chapterId: Long = 0,

    @ColumnInfo(name = "title")
    var title: String = "",

    @ColumnInfo(name = "description")
    var description: String? = null,

    @ColumnInfo(name = "thumbnail_uri")
    var thumbnailUri: String? = null,

    @ColumnInfo(name = "order_no")
    var orderNo: Int = 0,

    @ColumnInfo(name = "level")
    var level: String? = null,

    @ColumnInfo(name = "subject")
    var subject: String? = null,

    @ColumnInfo(name = "is_active")
    var isActive: Int = 1
)
