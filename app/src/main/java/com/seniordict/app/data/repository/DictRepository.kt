package com.seniordict.app.data.repository

import com.seniordict.app.data.db.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class DictRepository(private val dao: SeniorDictDao) {

    // Common radical spoken aliases
    private val radicalAliases = mapOf(
        "草字头" to listOf("艹", "草"),
        "三点水" to listOf("氵", "水"),
        "单人旁" to listOf("亻", "人"),
        "双人旁" to listOf("彳"),
        "提手旁" to listOf("扌", "手"),
        "木字旁" to listOf("木"),
        "口字旁" to listOf("口"),
        "金字旁" to listOf("钅", "金"),
        "火字旁" to listOf("火", "灬"),
        "土字旁" to listOf("土"),
        "日字旁" to listOf("日"),
        "月字旁" to listOf("月", "肉"),
        "目字旁" to listOf("目"),
        "石字旁" to listOf("石"),
        "示字旁" to listOf("礻", "示"),
        "衣字旁" to listOf("衤", "衣"),
        "言字旁" to listOf("讠", "言"),
        "足字旁" to listOf("𧾷", "足"),
        "绞丝旁" to listOf("纟", "糸"),
        "女字旁" to listOf("女"),
        "子字旁" to listOf("子"),
        "宝盖头" to listOf("宀"),
        "秃宝盖" to listOf("冖"),
        "人字头" to listOf("人"),
        "雨字头" to listOf("雨"),
        "竹字头" to listOf("⺮", "竹"),
        "反犬旁" to listOf("犭", "犬"),
        "立刀旁" to listOf("刂", "刀"),
        "力字旁" to listOf("力"),
        "走之底" to listOf("辶"),
        "走之旁" to listOf("辶"),
        "建字底" to listOf("廴"),
        "包字头" to listOf("勹"),
        "同字框" to listOf("冂"),
        "国字框" to listOf("囗"),
        "门字框" to listOf("门"),
        "风字框" to listOf("风"),
        "病字头" to listOf("疒"),
        "病字旁" to listOf("疒")
    )

    suspend fun lookupCharacter(char: String): CharacterEntity? = withContext(Dispatchers.IO) {
        if (char.isBlank()) return@withContext null
        dao.getCharacter(char.trim())
    }

    suspend fun searchByPinyin(rawPinyin: String): List<CharacterEntity> = withContext(Dispatchers.IO) {
        if (rawPinyin.isBlank()) return@withContext emptyList()
        dao.getCharactersByPinyin(rawPinyin.trim().lowercase())
    }

    suspend fun queryByParts(parts: List<String>): List<CharacterEntity> = withContext(Dispatchers.IO) {
        if (parts.isEmpty()) return@withContext emptyList()
        val key = parts.joinToString(",")
        var results = dao.getCharactersByParts(key)
        if (results.isEmpty() && parts.size == 2) {
            // Try reverse order e.g. "白,木"
            val reverseKey = "${parts[1]},${parts[0]}"
            results = dao.getCharactersByParts(reverseKey)
        }
        results
    }

    suspend fun naturalLanguageSplitQuery(input: String): List<CharacterEntity> = withContext(Dispatchers.IO) {
        var cleanInput = input.trim()
        if (cleanInput.isBlank()) return@withContext emptyList()

        // 1. If it's a single character search
        if (cleanInput.length == 1) {
            val single = dao.getCharacter(cleanInput)
            return@withContext if (single != null) listOf(single) else emptyList()
        }

        // 2. Identify radical alias
        val detectedParts = mutableListOf<String>()
        for ((alias, radicals) in radicalAliases) {
            if (cleanInput.contains(alias)) {
                detectedParts.add(radicals.first())
                cleanInput = cleanInput.replace(alias, "")
            }
        }

        // 3. Remove conversational connector words
        val noiseWords = listOf(
            "左边", "右边", "上边", "下边", "上面", "下面", "左面", "右面",
            "中间", "里面", "外面", "一个", "半个", "是个", "什么字",
            "怎么念", "怎么读", "加上", "加", "和", "跟", "与", "旁边", "旁边是", "边是", "旁是"
        )
        for (noise in noiseWords) {
            cleanInput = cleanInput.replace(noise, "")
        }

        // Extract remaining single Chinese characters as parts
        val remainingChars = cleanInput.filter { it in '\u4e00'..'\u9fa5' }
        for (ch in remainingChars) {
            detectedParts.add(ch.toString())
        }

        if (detectedParts.isEmpty()) {
            return@withContext emptyList()
        }

        // Exact parts query
        val directResults = queryByParts(detectedParts)
        if (directResults.isNotEmpty()) {
            return@withContext directResults
        }

        // Fallback: search each part in keyword
        val fallbackSet = mutableSetOf<CharacterEntity>()
        for (p in detectedParts) {
            val list = dao.searchCharactersByComponentKeyword(p)
            fallbackSet.addAll(list)
        }
        fallbackSet.take(15)
    }

    // --- Search History ---
    suspend fun recordHistory(char: String, source: String = "CAMERA") = withContext(Dispatchers.IO) {
        val trimmed = char.trim()
        if (trimmed.isBlank()) return@withContext
        dao.deleteHistoryByChar(trimmed)
        dao.insertHistory(SearchHistoryEntity(char = trimmed, source = source))
    }

    fun getRecentHistoryFlow(): Flow<List<HistoryItem>> = dao.getRecentHistoryFlow()

    suspend fun deleteHistory(id: Long) = withContext(Dispatchers.IO) {
        dao.deleteHistoryById(id)
    }

    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        dao.clearHistory()
    }

    // --- Favorites (生字本) ---
    suspend fun toggleFavorite(char: String): Boolean = withContext(Dispatchers.IO) {
        val isFav = dao.isFavorite(char)
        if (isFav) {
            dao.removeFavorite(char)
            false
        } else {
            dao.addFavorite(FavoriteEntity(char = char))
            true
        }
    }

    suspend fun isFavorite(char: String): Boolean = withContext(Dispatchers.IO) {
        dao.isFavorite(char)
    }

    fun isFavoriteFlow(char: String): Flow<Boolean> = dao.isFavoriteFlow(char)

    fun getAllFavoritesFlow(): Flow<List<FavoriteItem>> = dao.getAllFavoritesFlow()
}
