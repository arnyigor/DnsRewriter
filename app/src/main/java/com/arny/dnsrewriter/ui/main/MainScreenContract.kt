package com.arny.dnsrewriter.ui.main

import com.arny.dnsrewriter.domain.model.DnsRule

/**
 * Контракт для главного экрана, описывающий его состояние и события.
 */

// 1. Состояние экрана (State)
//    Это data class, который содержит все данные, необходимые для отрисовки UI.
data class MainScreenState(
    val isLoading: Boolean = true,
    val rules: List<DnsRule> = emptyList(),
    val logs: List<String> = emptyList(),
    val isVpnRunning: Boolean = false,

    // Состояние для диалога добавления нового правила
    val showAddRuleDialog: Boolean = false,
    val newRuleDomain: String = "",
    val newRuleIp: String = ""
)

// 2. События (Events)
//    Это sealed interface, который перечисляет все возможные действия пользователя.
sealed interface MainScreenEvent {
    // Событие для кнопки запуска/остановки VPN
    data object ToggleVpnService : MainScreenEvent

    data class ImportFile(val fileContent: String) : MainScreenEvent

    // События для управления правилами
    data class ToggleRule(val rule: DnsRule) : MainScreenEvent
    data class DeleteRule(val rule: DnsRule) : MainScreenEvent

    // События для диалога добавления правила
    data object AddRuleClicked : MainScreenEvent
    data object DismissAddRuleDialog : MainScreenEvent
    data class OnDomainChanged(val domain: String) : MainScreenEvent
    data class OnIpChanged(val ip: String) : MainScreenEvent
    data object ConfirmAddRule : MainScreenEvent
}