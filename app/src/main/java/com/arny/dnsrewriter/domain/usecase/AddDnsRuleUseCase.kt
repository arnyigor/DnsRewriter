package com.arny.dnsrewriter.domain.usecase

import com.arny.dnsrewriter.domain.model.DnsRule
import com.arny.dnsrewriter.domain.repository.DnsRuleRepository

class AddDnsRuleUseCase(private val repository: DnsRuleRepository) {
    suspend operator fun invoke(rule: DnsRule) {
        // Здесь можно добавить логику валидации, например,
        // проверить, что домен и IP не пустые.
        if (rule.domain.isNotBlank() && rule.ipAddress.isNotBlank()) {
            repository.addRule(rule)
        }
    }
}