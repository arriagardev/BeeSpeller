package com.example.beespeller.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.beespeller.model.Word
import com.example.beespeller.model.AiHint

@Database(entities = [Word::class, AiHint::class], version = 4, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun wordDao(): WordDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "bee_speller_database"
                )
                .fallbackToDestructiveMigration() // Reset DB for schema change
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
