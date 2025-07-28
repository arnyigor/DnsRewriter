package com.arny.dnsrewriter.domain.model

data class NextDnsProfile(
    val configId: String,
    // Сессионные куки в виде одной строки "key1=value1; key2=value2;"
    val cookies: String,
    // DNS-адреса, которые пользователь будет вводить в настройках
    val dnsServers: List<String> = listOf("dns.nextdns.io") // Значение по умолчанию
)