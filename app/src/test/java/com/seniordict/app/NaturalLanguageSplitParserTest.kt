package com.seniordict.app

import com.seniordict.app.data.FakeSeniorDictDao
import com.seniordict.app.data.db.CharacterEntity
import com.seniordict.app.data.repository.DictRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class NaturalLanguageSplitParserTest {

    private lateinit var dao: FakeSeniorDictDao
    private lateinit var repository: DictRepository

    @Before
    fun setUp() {
        dao = FakeSeniorDictDao()
        repository = DictRepository(dao)

        // Seed test characters
        dao.addTestCharacter(
            CharacterEntity(
                id = 1,
                char = "柏",
                pinyin = "bǎi",
                pinyinRaw = "bai",
                audioKey = "bai3",
                radicals = "木",
                strokeCount = 9,
                definition = "柏树，常绿乔木",
                examples = "柏树、圆柏"
            ),
            parts = listOf("木", "白")
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
                definition = "能治病防病的物质",
                examples = "中药、草药"
            ),
            parts = listOf("艹", "约")
        )

        dao.addTestCharacter(
            CharacterEntity(
                id = 3,
                char = "江",
                pinyin = "jiāng",
                pinyinRaw = "jiang",
                audioKey = "jiang1",
                radicals = "氵",
                strokeCount = 6,
                definition = "大河的通称",
                examples = "长江、江河"
            ),
            parts = listOf("氵", "工")
        )
    }

    @Test
    fun `test direct single character lookup`() = runTest {
        val results = repository.naturalLanguageSplitQuery("柏")
        assertEquals(1, results.size)
        assertEquals("柏", results.first().char)
    }

    @Test
    fun `test colloquial spoken radical queries with conversational filler`() = runTest {
        // "左边木右边白" -> parts: 木, 白
        val results1 = repository.naturalLanguageSplitQuery("左边木右边白")
        assertEquals("Should find 柏", 1, results1.size)
        assertEquals("柏", results1.first().char)

        // "木字旁一个白是个什么字" -> parts: 木, 白
        val results2 = repository.naturalLanguageSplitQuery("木字旁一个白是个什么字")
        assertEquals("Should filter conversational words and find 柏", 1, results2.size)
        assertEquals("柏", results2.first().char)
    }

    @Test
    fun `test radical spoken aliases`() = runTest {
        // "草字头加约" -> parts: 艹, 约 -> 药
        val results1 = repository.naturalLanguageSplitQuery("草字头加约")
        assertEquals("Should map 草字头 to 艹 and find 药", 1, results1.size)
        assertEquals("药", results1.first().char)

        // "三点水加个工怎么念" -> parts: 氵, 工 -> 江
        val results2 = repository.naturalLanguageSplitQuery("三点水加个工怎么念")
        assertEquals("Should map 三点水 to 氵 and find 江", 1, results2.size)
        assertEquals("江", results2.first().char)
    }

    @Test
    fun `test reverse order tolerance`() = runTest {
        // "白 木" -> reversed lookup checks 白,木 then 木,白
        val results = repository.naturalLanguageSplitQuery("白 木")
        assertEquals("Should tolerate reverse order and find 柏", 1, results.size)
        assertEquals("柏", results.first().char)
    }

    @Test
    fun `test empty or noise-only queries return emptyList`() = runTest {
        val emptyResult = repository.naturalLanguageSplitQuery("")
        assertTrue(emptyResult.isEmpty())

        val noiseResult = repository.naturalLanguageSplitQuery("是个什么字怎么念")
        assertTrue(noiseResult.isEmpty())
    }
}
