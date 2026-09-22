package com.example.di

import android.content.Context
import androidx.room.Room
import com.example.network.GroqApiService
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

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "AIStudyMentorRoom.db"
        )
            .fallbackToDestructiveMigration(dropAllTables = true)
            // Đã loại bỏ allowMainThreadQueries() để bảo vệ luồng UI
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
}
