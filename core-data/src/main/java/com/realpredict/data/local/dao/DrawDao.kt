package com.realpredict.data.local.dao

import androidx.room.*
import com.realpredict.data.local.entity.DrawEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DrawDao {
    @Query("SELECT * FROM draws ORDER BY drawDate DESC")
    fun observeAll(): Flow<List<DrawEntity>>

    @Query("SELECT * FROM draws ORDER BY drawDate DESC")
    suspend fun getAllDraws(): List<DrawEntity>

    @Query("SELECT * FROM draws WHERE drawDate = :date AND drawType = :type LIMIT 1")
    suspend fun getDrawByDate(date: String, type: String = "QUINIELA_REAL_1PM"): DrawEntity?

    @Query("SELECT * FROM draws WHERE drawDate < :dateExclusive AND drawType = :type ORDER BY drawDate ASC")
    suspend fun getAllBefore(dateExclusive: String, type: String = "QUINIELA_REAL_1PM"): List<DrawEntity>

    @Query("SELECT * FROM draws WHERE drawDate BETWEEN :start AND :end ORDER BY drawDate ASC")
    suspend fun getRange(start: String, end: String): List<DrawEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDraws(draws: List<DrawEntity>)

    @Query("UPDATE draws SET verificationStatus = :status WHERE id = :drawId")
    suspend fun updateVerificationStatus(drawId: String, status: String)

    @Query("SELECT COUNT(*) FROM draws")
    suspend fun countDraws(): Int

    @Query("DELETE FROM draws")
    suspend fun clearAll()
}
