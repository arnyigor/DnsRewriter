package com.arny.dnsrewriter.domain.model

/**
 * Основная бизнес-модель правила DNS.
 *
 * @param id Уникальный идентификатор правила.
 * @param domain Доменное имя для перезаписи (например, "example.com").
 * @param ipAddress IP-адрес, на который будет указывать домен.
 * @param isEnabled Флаг, позволяющий временно включать/отключать правило.
 */
data class DnsRule(
    val id: Int = 0,
    val domain: String,
    val ipAddress: String,
    val isEnabled: Boolean = true
)