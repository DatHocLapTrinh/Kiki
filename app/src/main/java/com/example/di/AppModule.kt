package com.example.di

import android.content.Context
import androidx.room.Room
import com.example.network.GeminiApiService
import com.example.network.GroqApiService
import com.example.network.KikiBackendApiService
import com.example.sqlite.room.AppDao
import com.example.sqlite.room.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    private val MIGRATION_4_5 = object : androidx.room.migration.Migration(4, 5) {
        override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
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
    }

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "AIStudyMentorRoom.db"
        )
            .addMigrations(MIGRATION_4_5)
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }

    @Provides
    @Singleton
    fun provideAppDao(database: AppDatabase): AppDao {
        return database.appDao()
    }

    @Provides
    @Singleton
    fun provideGroqApiService(): GroqApiService {
        return GroqApiService.create()
    }

    @Provides
    @Singleton
    fun provideGeminiApiService(): GeminiApiService {
        return GeminiApiService.create()
    }

    @Provides
    @Singleton
    fun provideKikiBackendApiService(): KikiBackendApiService {
        return KikiBackendApiService.create()
    }
}
