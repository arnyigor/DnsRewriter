package com.arny.dnsrewriter.service

import android.util.Log
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.text.SimpleDateFormat
import java.util.*

object VpnStateLogger {
    private val _logFlow = MutableSharedFlow<String>(replay = 100) // Храним последние 100 логов
    val logFlow = _logFlow.asSharedFlow()

    fun log(message: String) {
        val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val value = "[$timestamp] $message"
        _logFlow.tryEmit(value)
        Log.d("VpnStateLogger", value)
    }
}