package com.realpredict.ai

import android.app.Application
import com.realpredict.ai.data.local.RealPredictDatabase
import com.realpredict.ai.data.security.KeystoreManager

class RealPredictApp : Application() {

    lateinit var database: RealPredictDatabase
        private set

    lateinit var keystoreManager: KeystoreManager
        private set

    override fun onCreate() {
        super.onCreate()
        database = RealPredictDatabase.getDatabase(this)
        keystoreManager = KeystoreManager(this)
    }
}
