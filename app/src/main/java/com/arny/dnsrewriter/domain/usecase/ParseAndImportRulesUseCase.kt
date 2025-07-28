package com.arny.dnsrewriter.domain.usecase


import com.arny.dnsrewriter.domain.model.DnsRule
import com.arny.dnsrewriter.domain.repository.DnsRuleRepository

class ParseAndImportRulesUseCase(
    private val repository: DnsRuleRepository
) {
    suspend operator fun invoke(fileContent: String) {
        val rules = fileContent.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") }
            .mapNotNull { line ->
                // Разделяем по одному или нескольким пробелам/табам
                val parts = line.split(Regex("\\s+"))
                if (parts.size >= 2) {
                    val ip = parts[0]
                    val domain = parts[1] // Это оригинальный домен, может содержать *.

                    // Простая, но достаточная валидация
                    if (isValidIpAddress(ip) && domain.isNotBlank()) {
                        // В базу данных сохраняем домен как есть, с `*.`
                        DnsRule(domain = domain, ipAddress = ip, isEnabled = true)
                    } else {
                        null
                    }
                } else {
                    null
                }
            }

        if (rules.isNotEmpty()) {
            repository.replaceAllRules(rules)
        }
    }

    private fun isValidIpAddress(ip: String): Boolean {
        return android.util.Patterns.IP_ADDRESS.matcher(ip).matches()
    }
}