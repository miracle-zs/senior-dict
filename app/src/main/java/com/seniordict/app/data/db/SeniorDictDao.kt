package com.seniordict.app.data.db

import androidx.room.Dao
import androidx.room.Query

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
}
