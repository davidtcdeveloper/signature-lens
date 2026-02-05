package com.signaturelens

import android.app.Application
import com.signaturelens.di.cameraModule
import com.signaturelens.di.coreModule
import com.signaturelens.di.viewModelModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

/**
 * Application class for SignatureLens.
 * Initializes Koin dependency injection on startup.
 */
class SignatureLensApp : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Koin DI
        startKoin {
            androidContext(this@SignatureLensApp)
            modules(cameraModule, viewModelModule, coreModule)
        }
    }
}
