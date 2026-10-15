package com.realpredict.ai

import android.app.Application
import com.realpredict.ai.di.AppContainer
import com.realpredict.ai.seed.SeedDataLoader
import com.realpredict.data.local.EngineDatabase
import com.realpredict.security.CryptoManager

/**
 * Application principal. Inicializa:
 *  - AppContainer (ServiceLocator) con todos los servicios del motor.
 *  - SeedDataLoader: inserta los 100 sorteos verificados de la Quiniela Real La Una
 *    (2026-07-07 a 2026-10-14) al primer arranque, en un scope de IO separado
 *    para no bloquear el inicio de la UI.
 *
 * Los antiguos RealPredictDatabase y KeystoreManager legacy se eliminaron de :app
 * para evitar duplicación con los módulos del motor (:core-data, :core-security).
 */
class RealPredictApp : Application() {

    lateinit var database: EngineDatabase
        private set

    lateinit var cryptoManager: CryptoManager
        private set

    override fun onCreate() {
        super.onCreate()
        // Inicializa el ServiceLocator con el contexto de aplicación
        AppContainer.init(this)

        // Singletons accesibles directamente desde Application (legacy compat)
        database = AppContainer.database
        cryptoManager = AppContainer.cryptoManager

        // Carga semilla (100 sorteos reales) solo si la BD está vacía
        SeedDataLoader(AppContainer.idGenerator, AppContainer.timeProvider)
            .seedIfEmpty(this)
    }
}
