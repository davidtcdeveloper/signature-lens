package com.signaturelens.di

import com.signaturelens.core.domain.CaptureRepository
import com.signaturelens.core.encoding.AndroidImageEncoder
import com.signaturelens.core.encoding.HeicSupportChecker
import com.signaturelens.core.encoding.ImageEncoder
import com.signaturelens.core.storage.MediaStoreManager
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val coreModule = module {
    single { HeicSupportChecker() }
    single<ImageEncoder> { AndroidImageEncoder(get()) }
    single { MediaStoreManager(androidContext()) }
    single { CaptureRepository(get(), get(), get()) }
}
