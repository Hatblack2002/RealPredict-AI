package com.realpredict.ai.di

import android.content.Context
import com.realpredict.ai.viewmodel.HomeViewModel
import com.realpredict.ai.viewmodel.HistoryViewModel
import com.realpredict.ai.viewmodel.ImportViewModel
import com.realpredict.ai.viewmodel.StatisticsViewModel
import com.realpredict.common.DefaultDispatcherProvider
import com.realpredict.common.DefaultTimeProvider
import com.realpredict.common.DispatcherProvider
import com.realpredict.common.IdGenerator
import com.realpredict.common.TimeProvider
import com.realpredict.common.UuidIdGenerator
import com.realpredict.data.importer.CsvImporter
import com.realpredict.data.importer.XlsxImporter
import com.realpredict.data.local.EngineDatabase
import com.realpredict.data.repository.DrawRepositoryImpl
import com.realpredict.domain.repository.DrawRepository
import com.realpredict.security.CryptoManager
import com.realpredict.statistics.StatisticsEngine

/**
 * ServiceLocator simple sin framework de DI (Sección 1 — aplicación local-first).
 *
 * Inicializado en [com.realpredict.ai.RealPredictApp.onCreate] y accesible desde
 * cualquier ViewModel vía [get]. No tocar el orden de inicialización: las dependencias
 * tienen restricciones (EngineDatabase primero, repositorios después).
 *
 * No incluye singletons de IA ni de chat: esos se inicializan on-demand desde
 * ApiKeysScreen una vez que el usuario configura sus claves.
 */
object AppContainer {

    private lateinit var appContext: Context

    // Servicios compartidos
    val dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider()
    val timeProvider: TimeProvider = DefaultTimeProvider()
    val idGenerator: IdGenerator = UuidIdGenerator()

    // Lazies para que se inicialicen solo cuando se necesitan (después de appContext)
    val database: EngineDatabase by lazy { EngineDatabase.get(appContext) }
    val cryptoManager: CryptoManager by lazy { CryptoManager(appContext) }
    val drawRepository: DrawRepository by lazy {
        DrawRepositoryImpl(database.drawDao(), idGenerator, timeProvider)
    }
    val statisticsEngine: StatisticsEngine by lazy { StatisticsEngine() }
    val csvImporter: CsvImporter by lazy { CsvImporter(idGenerator, timeProvider) }
    val xlsxImporter: XlsxImporter by lazy { XlsxImporter(idGenerator, timeProvider) }

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    // ---- Factory functions para ViewModels ----

    fun homeViewModel(): HomeViewModel = HomeViewModel(drawRepository, dispatcherProvider)

    fun statisticsViewModel(): StatisticsViewModel =
        StatisticsViewModel(drawRepository, statisticsEngine, dispatcherProvider)

    fun historyViewModel(): HistoryViewModel = HistoryViewModel(drawRepository, dispatcherProvider)

    fun importViewModel(): ImportViewModel =
        ImportViewModel(drawRepository, csvImporter, xlsxImporter, idGenerator, timeProvider, dispatcherProvider)
}
