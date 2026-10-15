package com.realpredict.ai

import android.app.Application
import com.realpredict.data.local.EngineDatabase
import com.realpredict.security.CryptoManager

/**
 * Application principal. Inicializa el motor: base de datos unificada
 * (EngineDatabase de :core-data) y gestor criptográfico (CryptoManager de :core-security).
 *
 * Los antiguos RealPredictDatabase y KeystoreManager legacy se eliminaron del :app
 * para evitar duplicación con los módulos del motor.
 */
class RealPredictApp : Application() {

    lateinit var database: EngineDatabase
        private set

    lateinit var cryptoManager: CryptoManager
        private set

    override fun onCreate() {
        super.onCreate()
        database = EngineDatabase.get(this)
        cryptoManager = CryptoManager(this)
    }
}
