package com.arny.dnsrewriter.di

import com.arny.dnsrewriter.data.local.ProfileStorage
import com.arny.dnsrewriter.data.local.SecurePrefs
import com.arny.dnsrewriter.data.remote.NextDnsApiClient
import com.arny.dnsrewriter.ui.main.TestViewModel
import kotlinx.coroutines.Dispatchers
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

val coroutinesModule = module {
    single(named("IO")) { Dispatchers.IO }
    single(named("Default")) { Dispatchers.Default }
    single(named("Main")) { Dispatchers.Main }
}

/**
 * Koin-модуль для Domain слоя.
 * Предоставляет UseCases. Используем `factory`, так как UseCase'ы обычно
 * не имеют состояния и могут создаваться каждый раз заново.
 */
val domainModule = module {
}

/**
 * Koin-модуль для Data слоя.
 * Предоставляет репозитории и источники данных (БД). Используем `single`
 * для создания единственного экземпляра на все время жизни приложения.
 */
val dataModule = module {
    single { NextDnsApiClient() }
    single {
        ProfileStorage(SecurePrefs(androidContext()).prefs)
    }
}

/**
 * Koin-модуль для Presentation (app) слоя.
 * Предоставляет ViewModel'и.
 */
val appModule = module {
    // Используем специальный `viewModel` билдер, который правильно
    // управляет жизненным циклом ViewModel.
    viewModel { TestViewModel(get(), get(named("IO"))) }
}