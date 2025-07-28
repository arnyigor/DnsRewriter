package com.arny.dnsrewriter.data.repository

import com.arny.dnsrewriter.data.local.DnsRuleDao
import com.arny.dnsrewriter.data.local.toDomainModel
import com.arny.dnsrewriter.data.local.toEntity
import com.arny.dnsrewriter.domain.model.DnsRule
import com.arny.dnsrewriter.domain.repository.DnsRuleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DnsRuleRepositoryImpl(
    private val dao: DnsRuleDao
) : DnsRuleRepository {

    override suspend fun replaceAllRules(rules: List<DnsRule>) {
        val entities = rules.map { it.toEntity() }
        dao.replaceAll(entities)
    }

    override fun getRules(): Flow<List<DnsRule>> {
        return dao.getRules().map { entities ->
            entities.map { it.toDomainModel() } // Преобразуем Entity в Domain Model
        }
    }

    override suspend fun getActiveRules(): List<DnsRule> {
        return dao.getActiveRules().map { it.toDomainModel() }
    }

    override suspend fun addRule(rule: DnsRule) {
        dao.insert(rule.toEntity()) // Преобразуем Domain Model в Entity
    }

    override suspend fun deleteRule(rule: DnsRule) {
        dao.delete(rule.toEntity())
    }

    override suspend fun updateRule(rule: DnsRule) {
        dao.update(rule.toEntity())
    }
}