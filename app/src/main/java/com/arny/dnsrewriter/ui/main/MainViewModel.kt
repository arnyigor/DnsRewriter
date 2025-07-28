package com.arny.dnsrewriter.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arny.dnsrewriter.domain.model.DnsRule
import com.arny.dnsrewriter.domain.usecase.*
import com.arny.dnsrewriter.service.VpnStateLogger
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(
    private val getDnsRulesUseCase: GetDnsRulesUseCase,
    private val addDnsRuleUseCase: AddDnsRuleUseCase,
    private val updateDnsRuleUseCase: UpdateDnsRuleUseCase,
    private val deleteDnsRuleUseCase: DeleteDnsRuleUseCase,
    private val parseAndImportRulesUseCase: ParseAndImportRulesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainScreenState())
    val uiState: StateFlow<MainScreenState> = _uiState.asStateFlow()

    init {
        // Подписываемся на поток правил из базы данных.
        // Любое изменение в БД автоматически прилетит сюда и обновит UI.
        viewModelScope.launch {
            getDnsRulesUseCase().collect { rules ->
                _uiState.update { it.copy(rules = rules, isLoading = false) }
            }
        }

        // Подписываемся на логи
        viewModelScope.launch {
            VpnStateLogger.logFlow.collect { log ->
                // Добавляем новый лог в начало списка, ограничиваем размер
                _uiState.update { state ->
                    val updatedLogs = (listOf(log) + state.logs).take(100)
                    state.copy(logs = updatedLogs)
                }
            }
        }
    }

    // Центральная функция для обработки всех действий пользователя
    fun onEvent(event: MainScreenEvent) {
        when (event) {
            is MainScreenEvent.ToggleRule -> onToggleRule(event.rule)
            is MainScreenEvent.DeleteRule -> onDeleteRule(event.rule)
            MainScreenEvent.ToggleVpnService -> onToggleVpn()
            is MainScreenEvent.ImportFile -> importRules(event.fileContent)
            // Обработка диалога
            MainScreenEvent.AddRuleClicked -> _uiState.update { it.copy(showAddRuleDialog = true) }
            MainScreenEvent.DismissAddRuleDialog -> clearAndDismissDialog()
            is MainScreenEvent.OnDomainChanged -> _uiState.update { it.copy(newRuleDomain = event.domain) }
            is MainScreenEvent.OnIpChanged -> _uiState.update { it.copy(newRuleIp = event.ip) }
            MainScreenEvent.ConfirmAddRule -> onConfirmAddRule()
        }
    }

    private fun importRules(content: String) {
        viewModelScope.launch {
            parseAndImportRulesUseCase(content)
            // Можно добавить лог или snackbar об успешном импорте
        }
    }

    private fun onToggleRule(rule: DnsRule) {
        viewModelScope.launch {
            updateDnsRuleUseCase(rule.copy(isEnabled = !rule.isEnabled))
        }
    }

    private fun onDeleteRule(rule: DnsRule) {
        viewModelScope.launch {
            deleteDnsRuleUseCase(rule)
        }
    }

    private fun onConfirmAddRule() {
        viewModelScope.launch {
            val domain = _uiState.value.newRuleDomain.trim()
            val ip = _uiState.value.newRuleIp.trim()
            if (domain.isNotBlank() && ip.isNotBlank()) {
                addDnsRuleUseCase(DnsRule(domain = domain, ipAddress = ip))
                clearAndDismissDialog()
            }
        }
    }

    private fun onToggleVpn() {
        // Эту логику мы реализуем позже, когда будет готов VpnService.
        // Она будет запускать или останавливать сервис.
        val isCurrentlyRunning = _uiState.value.isVpnRunning
        _uiState.update { it.copy(isVpnRunning = !isCurrentlyRunning) }
        // Здесь будет вызов: context.startService(...) или context.stopService(...)
        println("VPN Toggled. New state: ${!isCurrentlyRunning}")
    }

    private fun clearAndDismissDialog() {
        _uiState.update {
            it.copy(
                showAddRuleDialog = false,
                newRuleDomain = "",
                newRuleIp = ""
            )
        }
    }
}