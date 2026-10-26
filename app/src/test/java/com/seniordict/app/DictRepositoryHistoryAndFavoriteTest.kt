package com.seniordict.app

import com.seniordict.app.data.FakeSeniorDictDao
import com.seniordict.app.data.db.CharacterEntity
import com.seniordict.app.data.repository.DictRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class DictRepositoryHistoryAndFavoriteTest {

    private lateinit var dao: FakeSeniorDictDao
    private lateinit var repository: DictRepository

    @Before
    fun setUp() {
        dao = FakeSeniorDictDao()
        repository = DictRepository(dao)

        dao.addTestCharacter(
            CharacterEntity(
                id = 1,
                char = "柏",
                pinyin = "bǎi",
                pinyinRaw = "bai",
                audioKey = "bai3",
                radicals = "木",
                strokeCount = 9,
                definition = "柏树",
                examples = "柏树"
            )
        )
        dao.addTestCharacter(
            CharacterEntity(
                id = 2,
                char = "药",
                pinyin = "yào",
                pinyinRaw = "yao",
                audioKey = "yao4",
                radicals = "艹",
                strokeCount = 9,
                definition = "药物",
                examples = "中药"
            )
        )
    }

    @Test
    fun `test search history deduplication moves recent query to top`() = runTest {
        // Look up 柏 from CAMERA
        repository.recordHistory("柏", "CAMERA")
        val history1 = dao.getRecentHistory()
        assertEquals(1, history1.size)
        assertEquals("柏", history1[0].char)
        assertEquals("CAMERA", history1[0].source)

        // Look up 药 from GALLERY
        repository.recordHistory("药", "GALLERY")
        val history2 = dao.getRecentHistory()
        assertEquals(2, history2.size)
        assertEquals("药", history2[0].char)
        assertEquals("柏", history2[1].char)

        // Look up 柏 again -> should delete old entry and move 柏 to top!
        repository.recordHistory("柏", "CAMERA")
        val history3 = dao.getRecentHistory()
        assertEquals("History size must remain 2 (deduplicated)", 2, history3.size)
        assertEquals("柏 must be moved to index 0", "柏", history3[0].char)
        assertEquals("药 must be at index 1", "药", history3[1].char)
    }

    @Test
    fun `test toggleFavorite adds and removes character`() = runTest {
        assertFalse("Initially not favorite", repository.isFavorite("柏"))

        // First toggle: adds favorite
        val state1 = repository.toggleFavorite("柏")
        assertTrue("Should return true when added", state1)
        assertTrue("isFavorite should return true", repository.isFavorite("柏"))

        // Second toggle: removes favorite
        val state2 = repository.toggleFavorite("柏")
        assertFalse("Should return false when removed", state2)
        assertFalse("isFavorite should return false", repository.isFavorite("柏"))
    }

    @Test
    fun `test clearHistory removes all history records`() = runTest {
        repository.recordHistory("柏", "CAMERA")
        repository.recordHistory("药", "GALLERY")
        assertEquals(2, dao.getRecentHistory().size)

        repository.clearHistory()
        assertEquals(0, dao.getRecentHistory().size)
    }
}
