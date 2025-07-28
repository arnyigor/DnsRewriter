package com.arny.dnsrewriter

import android.app.Application
import com.arny.dnsrewriter.di.appModule
import com.arny.dnsrewriter.di.dataModule
import com.arny.dnsrewriter.di.domainModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class DnsApp : Application() {

    override fun onCreate() {
        super.onCreate()

        startKoin {
            // Логгер для Koin. Очень полезен для отладки DI.
            // В релизной сборке можно установить Level.NONE
            androidLogger(Level.DEBUG)

            // Предоставляем Android-контекст для Koin (нужен для Room).
            androidContext(this@DnsApp)

            // Перечисляем все наши модули.
            modules(listOf(domainModule, dataModule, appModule))
        }
    }
}