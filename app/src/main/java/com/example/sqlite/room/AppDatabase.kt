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
        WeakPointEntity::class,
        ResearchTelemetryEntity::class
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

        fun createResearchTelemetryTableIfNotExists(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `research_telemetry` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `session_id` TEXT NOT NULL,
                    `user_id` INTEGER NOT NULL,
                    `timestamp` TEXT NOT NULL,
                    `slo_topic` TEXT NOT NULL,
                    `challenge_type` TEXT NOT NULL,
                    `item_identifier` TEXT NOT NULL,
                    `response_latency_ms` INTEGER NOT NULL,
                    `is_correct` INTEGER NOT NULL,
                    `used_hint` INTEGER NOT NULL,
                    `is_distracted` INTEGER NOT NULL,
                    `objective_quality` INTEGER NOT NULL,
                    `easiness_factor` REAL NOT NULL,
                    `interval_days` INTEGER NOT NULL,
                    `retention_probability` REAL NOT NULL,
                    FOREIGN KEY(`user_id`) REFERENCES `users`(`user_id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_research_telemetry_user_id` ON `research_telemetry` (`user_id`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_research_telemetry_session_id` ON `research_telemetry` (`session_id`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_research_telemetry_timestamp` ON `research_telemetry` (`timestamp`)")
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                addColumnIfNotExists(db, "user_profiles", "stars", "INTEGER NOT NULL DEFAULT 0")
                addColumnIfNotExists(db, "user_profiles", "hearts", "INTEGER NOT NULL DEFAULT 5")
                createResearchTelemetryTableIfNotExists(db)
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                addColumnIfNotExists(db, "user_profiles", "stars", "INTEGER NOT NULL DEFAULT 0")
                addColumnIfNotExists(db, "user_profiles", "hearts", "INTEGER NOT NULL DEFAULT 5")
                createResearchTelemetryTableIfNotExists(db)
            }
        }

        val MIGRATION_4_6 = object : Migration(4, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                addColumnIfNotExists(db, "user_profiles", "stars", "INTEGER NOT NULL DEFAULT 0")
                addColumnIfNotExists(db, "user_profiles", "hearts", "INTEGER NOT NULL DEFAULT 5")
                createResearchTelemetryTableIfNotExists(db)
            }
        }

        val ALL_MIGRATIONS = arrayOf(MIGRATION_4_5, MIGRATION_5_6, MIGRATION_4_6)
    }
}
