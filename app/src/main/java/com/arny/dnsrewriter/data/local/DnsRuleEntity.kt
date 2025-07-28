package com.arny.dnsrewriter.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.arny.dnsrewriter.domain.model.DnsRule

@Entity(tableName = "dns_rules")
data class DnsRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val domain: String,
    val ipAddress: String,
    val isEnabled: Boolean
)

// --- ВАЖНО: Функции-мапперы ---
// Они нужны для преобразования между слоями Data и Domain.
// Остальная часть приложения никогда не должна видеть DnsRuleEntity.

fun DnsRuleEntity.toDomainModel(): DnsRule = DnsRule(
    id = this.id,
    domain = this.domain,
    ipAddress = this.ipAddress,
    isEnabled = this.isEnabled
)

fun DnsRule.toEntity(): DnsRuleEntity = DnsRuleEntity(
    id = this.id,
    domain = this.domain,
    ipAddress = this.ipAddress,
    isEnabled = this.isEnabled
)