package com.arny.dnsrewriter.domain.usecase

import com.arny.dnsrewriter.domain.model.DnsRule
import com.arny.dnsrewriter.domain.repository.DnsRuleRepository

class ParseAndImportRulesUseCase(
    private val repository: DnsRuleRepository
) {
    suspend operator fun invoke(fileContent: String) {
        val rules = fileContent.lines()
            .map { it.trim() }
            // Игнорируем комментарии и пустые строки
            .filter { it.isNotEmpty() && !it.startsWith("#") }
            .mapNotNull { line ->
                // Разделяем по одному или нескольким пробелам/табам
                val parts = line.split(Regex("\\s+"))
                if (parts.size >= 2) {
                    val ip = parts[0]
                    val domain = parts[1]

                    // Валидация IP и домена
                    if (isValidIpAddress(ip) && isValidDomain(domain)) {
                        DnsRule(domain = domain, ipAddress = ip, isEnabled = true)
                    } else {
                        null // Игнорируем строки с невалидными данными
                    }
                } else {
                    null // Игнорируем некорректные строки
                }
            }

        if (rules.isNotEmpty()) {
            repository.replaceAllRules(rules)
        }
    }

    private fun isValidIpAddress(ip: String): Boolean {
        return when {
            // IPv4 валидация (включая 0.0.0.0)
            ip.contains(".") -> {
                val octets = ip.split(".")
                if (octets.size != 4) return false

                octets.all { octet ->
                    try {
                        val value = octet.toInt()
                        value in 0..255 &&
                                (value == 0 || octet == value.toString()) // Разрешаем 0, но проверяем ведущие нули для других чисел
                    } catch (_: NumberFormatException) {
                        false
                    }
                }
            }
            // IPv6 валидация (упрощенная)
            ip.contains(":") -> {
                try {
                    java.net.InetAddress.getByName(ip) != null
                } catch (_: Exception) {
                    false
                }
            }

            else -> false
        }
    }

    private fun isValidDomain(domain: String): Boolean {
        return domain.isNotBlank() &&
                domain.length <= 253 &&
                domain.matches(Regex("^[a-zA-Z\\d]([a-zA-Z\\d\\-.]*[a-zA-Z\\d])?$")) &&
                !domain.startsWith("-") &&
                !domain.endsWith("-") &&
                !domain.startsWith(".") &&
                !domain.endsWith(".") &&
                domain.split(".").all { part ->
                    part.isNotBlank() &&
                            part.length <= 63 &&
                            (
                                    part.matches(Regex("^[a-zA-Z\\d][a-zA-Z\\d-]*[a-zA-Z\\d]$")) ||
                                            part.matches(Regex("^[a-zA-Z\\d]+$")) ||
                                            part.matches(Regex("^[a-zA-Z][a-zA-Z\\d-]*$"))
                                    )
                }
    }
}