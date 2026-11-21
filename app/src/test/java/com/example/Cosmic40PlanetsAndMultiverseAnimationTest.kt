package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.repository.DataRepository
import com.example.sqlite.room.AppDatabase
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class Cosmic40PlanetsAndMultiverseAnimationTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var repository: DataRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = DataRepository(db.appDao(), context)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testCosmicDefaultStagesHasExact40PlanetsAcross4Sections() {
        val stages = DataRepository.COSMIC_DEFAULT_STAGES
        assertEquals(40, stages.size)

        // Section 1: Planets 1 - 10 (Beginner A0-A1)
        assertTrue(stages[0].first.contains("Chặng 1: Sao Thủy"))
        assertTrue(stages[9].first.contains("Chặng 10: Mặt Trời"))

        // Section 2: Planets 11 - 20 (Elementary A2)
        assertTrue(stages[10].first.contains("Chặng 11: Trạm Không Gian ISS"))
        assertTrue(stages[19].first.contains("Chặng 20: Trái Tim Thiên Hà"))

        // Section 3: Planets 21 - 30 (Intermediate B1-B2)
        assertTrue(stages[20].first.contains("Chặng 21: Tinh Vân Con Cua"))
        assertTrue(stages[29].first.contains("Chặng 30: Đỉnh Cao Tinh Vân"))

        // Section 4: Planets 31 - 40 (Advanced C1)
        assertTrue(stages[30].first.contains("Chặng 31: Hố Sâu Không Gian"))
        assertTrue(stages[39].first.contains("Chặng 40: Điểm Kỳ Dị Toàn Năng"))
    }

    @Test
    fun testGetChaptersByPreferenceAlwaysReturnsAll40Planets() = runTest {
        val chapters = repository.getChaptersByPreference("Beginner", "English")
        assertEquals(40, chapters.size)
        assertEquals(stagesTitle(1), chapters[0])
        assertEquals(stagesTitle(40), chapters[39])
    }

    @Test
    fun testDatabaseAutoHealsIfChaptersCountIsLessThan40() = runTest {
        repository.ensureSeeded()
        val dao = db.appDao()

        // Giả lập tình huống cơ sở dữ liệu cũ bị thiếu chỉ còn 5 hoặc 10 chặng
        dao.clearChapters()
        val partial = DataRepository.COSMIC_DEFAULT_STAGES.take(10)
        partial.forEachIndexed { idx, (title, desc) ->
            val entity = com.example.sqlite.room.ChapterEntity(
                title = title,
                description = desc,
                level = "Beginner",
                subject = "English",
                orderNo = idx,
                isActive = 1
            )
            dao.insertChapter(entity)
        }
        assertEquals(10, dao.getActiveChapters().size)

        // Khi ViewModel gọi getChaptersByPreference, hệ thống phải tự động phục hồi đủ 40 chặng!
        val healedChapters = repository.getChaptersByPreference("Beginner", "English")
        assertEquals(40, healedChapters.size)
        assertEquals(40, dao.getActiveChapters().size)
    }

    @Test
    fun testResetWithAiMicrolearningRoadmapPreservesAll40CosmicPlanets() = runTest {
        repository.ensureSeeded()
        val dao = db.appDao()

        // Gọi resetWithAiMicrolearningRoadmap (khi hoàn thành chẩn đoán cá nhân hóa)
        val dummyAiStages = listOf(
            Pair("Chặng 1: Giao Tiếp Khởi Đầu", "Làm quen câu chào"),
            Pair("Chặng 2: Hỏi Thăm Bản Thân", "Hỏi thăm bạn bè")
        )
        val firstChapterId = repository.resetWithAiMicrolearningRoadmap(
            level = "Beginner",
            stages = dummyAiStages,
            firstChapterUnits = null,
            questionsPerLesson = 5
        )
        assertTrue(firstChapterId > 0)

        // Kiểm tra toàn bộ 40 hành tinh vũ trụ vẫn được bảo toàn trọn vẹn 100%, không bị xóa mất!
        val activeChapters = dao.getActiveChapters()
        assertEquals(40, activeChapters.size)
    }

    @Test
    fun testMultiverseGatewayPositioningCalculation() {
        val chapterTitles = (1..40).map { "Planet $it" }
        val nodeProgress = List(40) { 1 }
        val stages = JourneyStageMapper.mapStages(chapterTitles, nodeProgress)

        assertEquals(40, stages.size)
        val totalSectionBreaks = if (stages.isEmpty()) 0 else (stages.lastIndex / 10)
        assertEquals(3, totalSectionBreaks) // Dividers between section 1-2, 2-3, 3-4

        val stageStepValue = 255f
        val sectionBreakValue = 88f
        val rewardTopValue = (stages.size * stageStepValue + totalSectionBreaks * sectionBreakValue + 10f)
        val multiverseTopValue = rewardTopValue + 130f

        // Cổng Đa Vũ Trụ Tri Thức Kiki phải nằm ở mốc kết thúc của toàn bộ 40 hành tinh (> 10,000 dp)
        assertTrue("Multiverse gateway must be after all 40 planets", multiverseTopValue > 10000f)
    }

    private fun stagesTitle(planetNumber: Int): String {
        return DataRepository.COSMIC_DEFAULT_STAGES[planetNumber - 1].first
    }
}
