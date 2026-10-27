package com.seniordict.app.data

import com.seniordict.app.data.db.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeSeniorDictDao : SeniorDictDao {

    val charactersMap = mutableMapOf<String, CharacterEntity>()
    val componentsMap = mutableMapOf<String, MutableList<String>>() // parts -> list of chars
    val displayPartsMap = mutableMapOf<String, String>()
    val historyList = mutableListOf<SearchHistoryEntity>()
    val favoritesSet = mutableSetOf<String>()

    private val historyFlow = MutableStateFlow<List<SearchHistoryEntity>>(emptyList())
    private val favoritesFlow = MutableStateFlow<Set<String>>(emptySet())

    private var nextHistoryId = 1L

    fun addTestCharacter(entity: CharacterEntity, parts: List<String> = emptyList()) {
        charactersMap[entity.char] = entity
        if (parts.isNotEmpty()) {
            val key = parts.joinToString(",")
            componentsMap.getOrPut(key) { mutableListOf() }.add(entity.char)
        }
    }

    override suspend fun getCharacter(character: String): CharacterEntity? {
        return charactersMap[character]
    }

    override suspend fun getCharactersByPinyin(pinyinRaw: String): List<CharacterEntity> {
        return charactersMap.values.filter { it.pinyinRaw.equals(pinyinRaw, ignoreCase = true) }
    }

    override suspend fun searchCharactersByPinyinPrefix(prefix: String): List<CharacterEntity> {
        return charactersMap.values.filter { it.pinyinRaw.startsWith(prefix, ignoreCase = true) }
    }

    override suspend fun searchCharactersByCharOrRadical(prefix: String): List<CharacterEntity> {
        return charactersMap.values.filter { it.char.startsWith(prefix) || it.radicals == prefix }
    }

    override suspend fun getCharComponentDisplay(character: String): String? {
        return displayPartsMap[character]
    }

    override suspend fun getCharactersByParts(parts: String): List<CharacterEntity> {
        val chars = componentsMap[parts] ?: return emptyList()
        return chars.mapNotNull { charactersMap[it] }
    }

    override suspend fun searchCharactersByComponentKeyword(keyword: String): List<CharacterEntity> {
        return componentsMap.entries
            .filter { it.key.contains(keyword) }
            .flatMap { it.value }
            .distinct()
            .mapNotNull { charactersMap[it] }
    }

    override suspend fun getSampleCharacters(): List<CharacterEntity> {
        return charactersMap.values.toList()
    }

    // --- Search History ---
    override suspend fun insertHistory(history: SearchHistoryEntity) {
        val entityWithId = if (history.id == 0L) history.copy(id = nextHistoryId++) else history
        historyList.add(0, entityWithId)
        historyFlow.value = historyList.toList()
    }

    override fun getRecentHistoryFlow(): Flow<List<HistoryItem>> {
        return historyFlow.map { list ->
            list.map { h ->
                val charEntity = charactersMap[h.char]
                HistoryItem(
                    id = h.id,
                    char = h.char,
                    timestamp = h.timestamp,
                    source = h.source,
                    pinyin = charEntity?.pinyin,
                    definition = charEntity?.definition,
                    examples = charEntity?.examples
                )
            }
        }
    }

    override suspend fun getRecentHistory(): List<HistoryItem> {
        return historyList.map { h ->
            val charEntity = charactersMap[h.char]
            HistoryItem(
                id = h.id,
                char = h.char,
                timestamp = h.timestamp,
                source = h.source,
                pinyin = charEntity?.pinyin,
                definition = charEntity?.definition,
                examples = charEntity?.examples
            )
        }
    }

    override suspend fun deleteHistoryById(id: Long) {
        historyList.removeAll { it.id == id }
        historyFlow.value = historyList.toList()
    }

    override suspend fun deleteHistoryByChar(character: String) {
        historyList.removeAll { it.char == character }
        historyFlow.value = historyList.toList()
    }

    override suspend fun clearHistory() {
        historyList.clear()
        historyFlow.value = emptyList()
    }

    // --- Favorites ---
    override suspend fun addFavorite(favorite: FavoriteEntity) {
        favoritesSet.add(favorite.char)
        favoritesFlow.value = favoritesSet.toSet()
    }

    override suspend fun removeFavorite(character: String) {
        favoritesSet.remove(character)
        favoritesFlow.value = favoritesSet.toSet()
    }

    override suspend fun isFavorite(character: String): Boolean {
        return favoritesSet.contains(character)
    }

    override fun isFavoriteFlow(character: String): Flow<Boolean> {
        return favoritesFlow.map { it.contains(character) }
    }

    override fun getAllFavoritesFlow(): Flow<List<FavoriteItem>> {
        return favoritesFlow.map { set ->
            set.map { char ->
                val charEntity = charactersMap[char]
                FavoriteItem(
                    char = char,
                    savedAt = System.currentTimeMillis(),
                    pinyin = charEntity?.pinyin,
                    definition = charEntity?.definition,
                    examples = charEntity?.examples
                )
            }
        }
    }

    override suspend fun getAllFavorites(): List<FavoriteItem> {
        return favoritesSet.map { char ->
            val charEntity = charactersMap[char]
            FavoriteItem(
                char = char,
                savedAt = System.currentTimeMillis(),
                pinyin = charEntity?.pinyin,
                definition = charEntity?.definition,
                examples = charEntity?.examples
            )
        }
    }
}
