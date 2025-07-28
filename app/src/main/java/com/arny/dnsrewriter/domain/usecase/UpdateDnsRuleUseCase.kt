package com.arny.dnsrewriter.domain.usecase

import com.arny.dnsrewriter.domain.model.DnsRule
import com.arny.dnsrewriter.domain.repository.DnsRuleRepository
class UpdateDnsRuleUseCase(private val repository: DnsRuleRepository) {
    suspend operator fun invoke(rule: DnsRule) = repository.updateRule(rule)
}