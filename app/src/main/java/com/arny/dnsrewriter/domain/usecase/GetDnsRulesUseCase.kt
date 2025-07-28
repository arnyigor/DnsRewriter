package com.arny.dnsrewriter.domain.usecase

import com.arny.dnsrewriter.domain.repository.DnsRuleRepository

class GetDnsRulesUseCase(private val repository: DnsRuleRepository) {
    operator fun invoke() = repository.getRules()
}