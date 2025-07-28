package com.arny.dnsrewriter.di

import androidx.room.Room
import com.arny.dnsrewriter.data.local.AppDatabase
import com.arny.dnsrewriter.data.repository.DnsRuleRepositoryImpl
import com.arny.dnsrewriter.domain.repository.DnsRuleRepository
import com.arny.dnsrewriter.domain.usecase.AddDnsRuleUseCase
import com.arny.dnsrewriter.domain.usecase.DeleteDnsRuleUseCase
import com.arny.dnsrewriter.domain.usecase.GetActiveRulesUseCase
import com.arny.dnsrewriter.domain.usecase.GetDnsRulesUseCase
import com.arny.dnsrewriter.domain.usecase.ParseAndImportRulesUseCase
import com.arny.dnsrewriter.domain.usecase.UpdateDnsRuleUseCase
import com.arny.dnsrewriter.ui.main.MainViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

/**
 * Koin-модуль для Domain слоя.
 * Предоставляет UseCases. Используем `factory`, так как UseCase'ы обычно
 * не имеют состояния и могут создаваться каждый раз заново.
 */
val domainModule = module {
    factory { GetDnsRulesUseCase(get()) }
    factory { GetActiveRulesUseCase(get()) }
    factory { AddDnsRuleUseCase(get()) }
    factory { UpdateDnsRuleUseCase(get()) }
    factory { DeleteDnsRuleUseCase(get()) }
    factory { ParseAndImportRulesUseCase(get()) }
}

/**
 * Koin-модуль для Data слоя.
 * Предоставляет репозитории и источники данных (БД). Используем `single`
 * для создания единственного экземпляра на все время жизни приложения.
 */
val dataModule = module {
    // Предоставляем реализацию репозитория, связывая интерфейс с реализацией.
    // Когда кто-то запросит DnsRuleRepository, Koin предоставит DnsRuleRepositoryImpl.
    single<DnsRuleRepository> { DnsRuleRepositoryImpl(get()) }

    // Предоставляем DAO. Koin сначала создаст AppDatabase (get()), а затем вызовет .dnsRuleDao()
    single { get<AppDatabase>().dnsRuleDao() }

    // Предоставляем саму базу данных. androidContext() приходит из Koin'а.
    single {
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java,
            "dns_rewriter_db.db"
        ).build()
    }
}

/**
 * Koin-модуль для Presentation (app) слоя.
 * Предоставляет ViewModel'и.
 */
val appModule = module {
    // Используем специальный `viewModel` билдер, который правильно
    // управляет жизненным циклом ViewModel.
    viewModel {
        MainViewModel(
            getDnsRulesUseCase = get(),
            addDnsRuleUseCase = get(),
            updateDnsRuleUseCase = get(),
            deleteDnsRuleUseCase = get(),
            parseAndImportRulesUseCase = get()
        )
    }
}