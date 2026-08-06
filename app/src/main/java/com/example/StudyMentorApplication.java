package com.example;

import android.app.Application;
import dagger.hilt.android.HiltAndroidApp;

@HiltAndroidApp
public class StudyMentorApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
    }
}
