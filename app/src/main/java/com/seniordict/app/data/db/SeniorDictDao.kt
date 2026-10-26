package com.seniordict.app.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

data class HistoryItem(
    val id: Long,
    val char: String,
    val timestamp: Long,
    val source: String,
    val pinyin: String?,
    val definition: String?,
    val examples: String?
)

data class FavoriteItem(
    val char: String,
    val savedAt: Long,
    val pinyin: String?,
    val definition: String?,
    val examples: String?
)

@Dao
interface SeniorDictDao {
    @Query("SELECT * FROM characters WHERE char = :character LIMIT 1")
    suspend fun getCharacter(character: String): CharacterEntity?

    @Query("SELECT * FROM characters WHERE pinyin_raw = :pinyinRaw LIMIT 20")
    suspend fun getCharactersByPinyin(pinyinRaw: String): List<CharacterEntity>

    @Query("""
        SELECT c.* FROM characters c
        INNER JOIN char_components cc ON c.char = cc.char
        WHERE cc.parts = :parts
        LIMIT 10
    """)
    suspend fun getCharactersByParts(parts: String): List<CharacterEntity>

    @Query("""
        SELECT c.* FROM characters c
        INNER JOIN char_components cc ON c.char = cc.char
        WHERE cc.parts LIKE '%' || :keyword || '%'
        LIMIT 20
    """)
    suspend fun searchCharactersByComponentKeyword(keyword: String): List<CharacterEntity>

    @Query("SELECT * FROM characters ORDER BY stroke_count ASC LIMIT 50")
    suspend fun getSampleCharacters(): List<CharacterEntity>

    // --- Search History ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: SearchHistoryEntity)

    @Query("""
        SELECT h.id, h.char, h.timestamp, h.source, c.pinyin, c.definition, c.examples
        FROM search_history h
        LEFT JOIN characters c ON h.char = c.char
        ORDER BY h.timestamp DESC
        LIMIT 100
    """)
    fun getRecentHistoryFlow(): Flow<List<HistoryItem>>

    @Query("""
        SELECT h.id, h.char, h.timestamp, h.source, c.pinyin, c.definition, c.examples
        FROM search_history h
        LEFT JOIN characters c ON h.char = c.char
        ORDER BY h.timestamp DESC
        LIMIT 100
    """)
    suspend fun getRecentHistory(): List<HistoryItem>

    @Query("DELETE FROM search_history WHERE id = :id")
    suspend fun deleteHistoryById(id: Long)

    @Query("DELETE FROM search_history WHERE char = :character")
    suspend fun deleteHistoryByChar(character: String)

    @Query("DELETE FROM search_history")
    suspend fun clearHistory()

    // --- Favorites (生字本) ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE char = :character")
    suspend fun removeFavorite(character: String)

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE char = :character LIMIT 1)")
    suspend fun isFavorite(character: String): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE char = :character LIMIT 1)")
    fun isFavoriteFlow(character: String): Flow<Boolean>

    @Query("""
        SELECT f.char, f.saved_at AS savedAt, c.pinyin, c.definition, c.examples
        FROM favorites f
        LEFT JOIN characters c ON f.char = c.char
        ORDER BY f.saved_at DESC
    """)
    fun getAllFavoritesFlow(): Flow<List<FavoriteItem>>

    @Query("""
        SELECT f.char, f.saved_at AS savedAt, c.pinyin, c.definition, c.examples
        FROM favorites f
        LEFT JOIN characters c ON f.char = c.char
        ORDER BY f.saved_at DESC
    """)
    suspend fun getAllFavorites(): List<FavoriteItem>
}
