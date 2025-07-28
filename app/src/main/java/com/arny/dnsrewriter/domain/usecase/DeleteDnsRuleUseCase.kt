package com.arny.dnsrewriter.domain.usecase

import com.arny.dnsrewriter.domain.model.DnsRule
import com.arny.dnsrewriter.domain.repository.DnsRuleRepository
class DeleteDnsRuleUseCase(private val repository: DnsRuleRepository) {
    suspend operator fun invoke(rule: DnsRule) = repository.deleteRule(rule)
}