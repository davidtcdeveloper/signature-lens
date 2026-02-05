package com.signaturelens.di

import com.signaturelens.ui.screen.PreviewViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

/**
 * Koin module for ViewModel dependencies.
 */
val viewModelModule = module {
    viewModel { PreviewViewModel(get()) }
}
