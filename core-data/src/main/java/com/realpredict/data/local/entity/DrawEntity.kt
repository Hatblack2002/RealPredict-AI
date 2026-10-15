package com.realpredict.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

/**
 * Tabla de sorteos históricos (migrada desde :app — sin duplicación).
 * Los números se conservan como String "00"-"99", nunca como Int.
 */
@Entity(
    tableName = "draws",
    indices = [
        Index(value = ["drawDate", "drawType"], unique = true)
    ]
)
data class DrawEntity(
    @PrimaryKey
    val id: String,
    val drawDate: String,
    val drawType: String,
    val firstNumber: String,   // String "00".."99"
    val secondNumber: String,
    val thirdNumber: String,
    val sourceId: String,
    val sourceRow: Int?,
    val verificationStatus: String,
    val createdAt: String
)
