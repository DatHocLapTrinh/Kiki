package com.example.sqlite.room

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        UserEntity::class,
        UserProfileEntity::class,
        ChapterEntity::class,
        QuestionEntity::class,
        QuizAttemptEntity::class,
        DailyTaskEntity::class,
        AIQuestionEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao
}
