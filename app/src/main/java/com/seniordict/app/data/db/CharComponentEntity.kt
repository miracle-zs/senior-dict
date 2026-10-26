package com.seniordict.app.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "char_components")
data class CharComponentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    
    @ColumnInfo(name = "char")
    val char: String,
    
    @ColumnInfo(name = "parts")
    val parts: String,
    
    @ColumnInfo(name = "display_parts")
    val displayParts: String
)
