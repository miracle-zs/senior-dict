package com.seniordict.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        CharacterEntity::class,
        CharComponentEntity::class,
        SearchHistoryEntity::class,
        FavoriteEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class SeniorDictDatabase : RoomDatabase() {
    abstract fun dictDao(): SeniorDictDao

    companion object {
        @Volatile
        private var INSTANCE: SeniorDictDatabase? = null

        fun getDatabase(context: Context): SeniorDictDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SeniorDictDatabase::class.java,
                    "senior_dict.db"
                )
                .createFromAsset("senior_dict.db")
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
