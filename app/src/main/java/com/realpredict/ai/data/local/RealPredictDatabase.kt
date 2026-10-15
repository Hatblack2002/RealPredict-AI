package com.realpredict.ai.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.realpredict.ai.data.local.dao.DrawDao
import com.realpredict.ai.data.local.entity.DrawEntity

@Database(
    entities = [DrawEntity::class],
    version = 1,
    exportSchema = true
)
abstract class RealPredictDatabase : RoomDatabase() {
    abstract fun drawDao(): DrawDao

    companion object {
        @Volatile
        private var INSTANCE: RealPredictDatabase? = null

        fun getDatabase(context: Context): RealPredictDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    RealPredictDatabase::class.java,
                    "realpredict_local.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
