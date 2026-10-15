package com.realpredict.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.realpredict.data.local.dao.*
import com.realpredict.data.local.entity.*

/**
 * Base de datos UNIFICADA del motor RealPredict AI.
 * Reemplaza a la `RealPredictDatabase` legacy que estaba en `:app`.
 *
 * Versión 1, schema exportado (exportSchema = true) para auditoría.
 *
 * Tablas incluidas:
 *  - draws (migrada desde :app)
 *  - import_batches
 *  - predictions (inmutable: solo @Insert)
 *  - prediction_evaluations
 *  - provider_configurations
 *  - chat_conversations
 *  - chat_messages
 *  - experiments
 *
 * Migraciones: NUNCA usar destructiveMigration en producción.
 * Para desarrollo inicial está bien; antes del release a Play Store,
 * definir migraciones reales con Migration object.
 */
@Database(
    entities = [
        DrawEntity::class,
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
    abstract fun drawDao(): DrawDao
    abstract fun importBatchDao(): ImportBatchDao
    abstract fun predictionDao(): PredictionDao
    abstract fun predictionEvaluationDao(): PredictionEvaluationDao
    abstract fun providerConfigurationDao(): ProviderConfigurationDao
    abstract fun chatConversationDao(): ChatConversationDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun experimentDao(): ExperimentDao

    companion object {
        private const val DB_NAME = "realpredict.db"

        @Volatile
        private var INSTANCE: EngineDatabase? = null

        fun get(context: Context): EngineDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    EngineDatabase::class.java,
                    DB_NAME
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }

        /** Para tests: BD en memoria. */
        fun inMemory(context: Context): EngineDatabase =
            Room.inMemoryDatabaseBuilder(context, EngineDatabase::class.java)
                .allowMainThreadQueries()
                .build()
    }
}
