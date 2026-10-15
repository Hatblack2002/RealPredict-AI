package com.realpredict.ai.data.local.dao

import androidx.room.*
import com.realpredict.ai.data.local.entity.DrawEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DrawDao {
    @Query("SELECT * FROM draws ORDER BY drawDate DESC")
    fun getAllDrawsFlow(): Flow<List<DrawEntity>>

    @Query("SELECT * FROM draws ORDER BY drawDate DESC")
    suspend fun getAllDraws(): List<DrawEntity>

    @Query("SELECT * FROM draws WHERE drawDate = :date AND drawType = :type LIMIT 1")
    suspend fun getDrawByDate(date: String, type: String = "QUINIELA_REAL_1PM"): DrawEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDraws(draws: List<DrawEntity>)

    @Query("SELECT COUNT(*) FROM draws")
    suspend fun countDraws(): Int

    @Query("DELETE FROM draws")
    suspend fun clearAll()
}
