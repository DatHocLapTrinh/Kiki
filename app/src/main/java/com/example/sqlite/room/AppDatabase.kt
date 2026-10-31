package com.example.sqlite.room

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        UserEntity::class,
        UserProfileEntity::class,
        ChapterEntity::class,
        QuestionEntity::class,
        QuizAttemptEntity::class,
        DailyTaskEntity::class,
        AIQuestionEntity::class,
        VocabularyEntity::class,
        WeakPointEntity::class
    ],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao

    companion object {
        fun addColumnIfNotExists(
            db: SupportSQLiteDatabase,
            tableName: String,
            columnName: String,
            columnDef: String
        ) {
            var columnExists = false
            val cursor = db.query("PRAGMA table_info($tableName)")
            try {
                val nameColumnIndex = cursor.getColumnIndex("name")
                while (cursor.moveToNext()) {
                    if (nameColumnIndex != -1 && cursor.getString(nameColumnIndex) == columnName) {
                        columnExists = true
                        break
                    }
                }
            } finally {
                cursor.close()
            }
            if (!columnExists) {
                db.execSQL("ALTER TABLE $tableName ADD COLUMN $columnName $columnDef")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                addColumnIfNotExists(db, "user_profiles", "stars", "INTEGER NOT NULL DEFAULT 0")
                addColumnIfNotExists(db, "user_profiles", "hearts", "INTEGER NOT NULL DEFAULT 5")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                addColumnIfNotExists(db, "user_profiles", "stars", "INTEGER NOT NULL DEFAULT 0")
                addColumnIfNotExists(db, "user_profiles", "hearts", "INTEGER NOT NULL DEFAULT 5")
            }
        }

        val MIGRATION_4_6 = object : Migration(4, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                addColumnIfNotExists(db, "user_profiles", "stars", "INTEGER NOT NULL DEFAULT 0")
                addColumnIfNotExists(db, "user_profiles", "hearts", "INTEGER NOT NULL DEFAULT 5")
            }
        }

        val ALL_MIGRATIONS = arrayOf(MIGRATION_4_5, MIGRATION_5_6, MIGRATION_4_6)
    }
}
