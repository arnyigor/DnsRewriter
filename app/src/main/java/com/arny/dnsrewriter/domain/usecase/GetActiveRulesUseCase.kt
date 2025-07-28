package com.arny.dnsrewriter.domain.usecase


import com.arny.dnsrewriter.domain.repository.DnsRuleRepository
class GetActiveRulesUseCase(private val repository: DnsRuleRepository) {
    suspend operator fun invoke() = repository.getActiveRules()
}
