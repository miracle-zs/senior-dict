package com.seniordict.app.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "characters")
data class CharacterEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    
    @ColumnInfo(name = "char")
    val char: String,
    
    @ColumnInfo(name = "pinyin")
    val pinyin: String,
    
    @ColumnInfo(name = "pinyin_raw")
    val pinyinRaw: String,
    
    @ColumnInfo(name = "audio_key")
    val audioKey: String,
    
    @ColumnInfo(name = "radicals")
    val radicals: String?,
    
    @ColumnInfo(name = "stroke_count")
    val strokeCount: Int,
    
    @ColumnInfo(name = "definition")
    val definition: String?,
    
    @ColumnInfo(name = "examples")
    val examples: String?
)
