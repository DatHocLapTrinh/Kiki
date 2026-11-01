package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.sqlite.room.AppDatabase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
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
            .addMigrations(*AppDatabase.ALL_MIGRATIONS)
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

    @Test
    fun testMigrationsAreProperlyConfigured() {
        assertEquals(6, AppDatabase.ALL_MIGRATIONS.size)
        assertEquals(4, AppDatabase.MIGRATION_4_5.startVersion)
        assertEquals(5, AppDatabase.MIGRATION_4_5.endVersion)
        assertEquals(5, AppDatabase.MIGRATION_5_6.startVersion)
        assertEquals(6, AppDatabase.MIGRATION_5_6.endVersion)
        assertEquals(4, AppDatabase.MIGRATION_4_6.startVersion)
        assertEquals(6, AppDatabase.MIGRATION_4_6.endVersion)
        assertEquals(6, AppDatabase.MIGRATION_6_7.startVersion)
        assertEquals(7, AppDatabase.MIGRATION_6_7.endVersion)
        assertEquals(5, AppDatabase.MIGRATION_5_7.startVersion)
        assertEquals(7, AppDatabase.MIGRATION_5_7.endVersion)
        assertEquals(4, AppDatabase.MIGRATION_4_7.startVersion)
        assertEquals(7, AppDatabase.MIGRATION_4_7.endVersion)
    }
}
