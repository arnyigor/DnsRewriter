package com.arny.dnsrewriter.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arny.dnsrewriter.data.remote.NextDnsApiClient
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch

class TestViewModel(
    private val nextDnsApiClient: NextDnsApiClient,
    private val ioDispatcher: CoroutineDispatcher // Внедряем IO Dispatcher
) : ViewModel() {

    fun testCreateProfile() {
        // Запускаем сетевой запрос в фоновом потоке
        viewModelScope.launch(ioDispatcher) {
            try {
                println("--- Начинаем тест создания профиля ---")
                val profile = nextDnsApiClient.createAnonymousProfile()
                println("--- УСПЕХ! ---")
                println("Config ID: ${profile.configId}")
                println("Cookies: ${profile.cookies}")
                println("-----------------")
            } catch (e: Exception) {
                println("--- ОШИБКА! ---")
                println("Класс ошибки: ${e.javaClass.simpleName}")
                println("Сообщение: ${e.message}")
                e.printStackTrace()
                println("-----------------")
            }
        }
    }
}