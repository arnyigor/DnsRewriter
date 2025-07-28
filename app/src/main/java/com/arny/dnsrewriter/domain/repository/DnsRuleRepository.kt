package com.arny.dnsrewriter.domain.repository

import com.arny.dnsrewriter.domain.model.DnsRule
import kotlinx.coroutines.flow.Flow

/**
 * Контракт для работы с хранилищем DNS-правил.
 */
interface DnsRuleRepository {

    /**
     * Получить поток со списком всех DNS-правил.
     * Flow используется для автоматического получения обновлений при изменении данных.
     */
    fun getRules(): Flow<List<DnsRule>>

    /**
     * Получить список активных правил (для VpnService).
     * Это suspend-функция, так как это будет однократный запрос.
     */
    suspend fun getActiveRules(): List<DnsRule>

    /**
     * Добавить новое правило в хранилище.
     */
    suspend fun addRule(rule: DnsRule)

    /**
     * Удалить правило из хранилища.
     */
    suspend fun deleteRule(rule: DnsRule)

    /**
     * Обновить существующее правило (например, для изменения флага isEnabled).
     */
    suspend fun updateRule(rule: DnsRule)

    /**
     * Заменить все правила в хранилище на новые.
     */
    suspend fun replaceAllRules(rules: List<DnsRule>)
}