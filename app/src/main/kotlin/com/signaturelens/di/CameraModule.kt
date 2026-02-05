package com.signaturelens.di

import com.signaturelens.camera.CameraRepository
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val cameraModule = module {
    single { CameraRepository(androidContext()) }
}
