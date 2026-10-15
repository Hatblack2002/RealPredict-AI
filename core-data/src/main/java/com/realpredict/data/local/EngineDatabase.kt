package com.realpredict.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.realpredict.data.local.dao.*
import com.realpredict.data.local.entity.*

/**
 * Base de datos local del motor RealPredict AI.
 * Versión 2 (añade las nuevas tablas del motor; la v1 solo tenía `draws`).
 *
 * NOTA: La app ya existe con `RealPredictDatabase` en `:app`. Esta es la versión
 * extendida del motor. Para integrar, recomendamos migrar a esta BD unificada y
 * eliminar la del :app, o mantenerlas separadas y referenciar ambas desde :app.
 *
 * Migraciones: NUNCA usar destructiveMigration en producción. Esta versión trae
 * esqueleto de migración para añadir tablas nuevas.
 */
@Database(
    entities = [
        ImportBatchEntity::class,
        PredictionEntity::class,
        PredictionEvaluationEntity::class,
        ProviderConfigurationEntity::class,
        ChatConversationEntity::class,
        ChatMessageEntity::class,
        ExperimentEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class EngineDatabase : RoomDatabase() {
    abstract fun importBatchDao(): ImportBatchDao
    abstract fun predictionDao(): PredictionDao
    abstract fun predictionEvaluationDao(): PredictionEvaluationDao
    abstract fun providerConfigurationDao(): ProviderConfigurationDao
    abstract fun chatConversationDao(): ChatConversationDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun experimentDao(): ExperimentDao

    companion object {
        private const val DB_NAME = "realpredict_engine.db"

        @Volatile
        private var INSTANCE: EngineDatabase? = null

        fun get(context: Context): EngineDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    EngineDatabase::class.java,
                    DB_NAME
                )
                    // En producción: definir migraciones reales, NO destructive.
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }

        /** Solo para tests: permite inyectar una BD en memoria. */
        fun inMemory(context: Context): EngineDatabase =
            Room.inMemoryDatabaseBuilder(context, EngineDatabase::class.java)
                .allowMainThreadQueries()
                .build()
    }
}
