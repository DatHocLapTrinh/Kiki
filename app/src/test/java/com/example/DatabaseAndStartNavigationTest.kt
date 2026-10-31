package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.sqlite.room.AppDatabase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DatabaseAndStartNavigationTest {

    @Test
    fun testDatabaseInitializesAndOpensSuccessfully() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .fallbackToDestructiveMigration(dropAllTables = true)
            .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
            .allowMainThreadQueries()
            .build()

        assertNotNull(db)
        val dao = db.appDao()
        assertNotNull(dao)
        val profile = dao.getAnyUserProfile()
        db.close()
    }
}
