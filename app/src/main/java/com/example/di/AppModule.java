package com.example.di;

import android.content.Context;
import androidx.room.Room;
import com.example.repository.DataRepository;
import com.example.sqlite.room.AppDatabase;
import com.example.sqlite.room.AppDao;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;
import javax.inject.Singleton;

@Module
@InstallIn(SingletonComponent.class)
public class AppModule {

    @Provides
    @Singleton
    public AppDatabase provideAppDatabase(@ApplicationContext Context context) {
        return Room.databaseBuilder(context, AppDatabase.class, "AIStudyMentorRoom.db")
                .fallbackToDestructiveMigration()
                .allowMainThreadQueries()
                .build();
    }

    @Provides
    @Singleton
    public AppDao provideAppDao(AppDatabase database) {
        return database.appDao();
    }

    @Provides
    @Singleton
    public DataRepository provideDataRepository(@ApplicationContext Context context) {
        return new DataRepository(context);
    }
}
