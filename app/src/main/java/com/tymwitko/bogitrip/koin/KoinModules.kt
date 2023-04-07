package com.tymwitko.bogitrip.koin

import com.tymwitko.bogitrip.model.TtsManager
import com.tymwitko.bogitrip.model.TurnByTurnNavigator
import com.tymwitko.bogitrip.viewmodels.MapViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    viewModel { MapViewModel() }
    single { TurnByTurnNavigator() }
    single { TtsManager(androidContext()) }
}