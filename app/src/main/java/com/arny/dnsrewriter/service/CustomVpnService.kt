package com.arny.dnsrewriter.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import com.arny.dnsrewriter.R
import com.arny.dnsrewriter.domain.usecase.GetActiveRulesUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.xbill.DNS.ARecord
import org.xbill.DNS.Address
import org.xbill.DNS.DClass
import org.xbill.DNS.Flags
import org.xbill.DNS.Message
import org.xbill.DNS.Section
import org.xbill.DNS.Type
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.ClosedByInterruptException
import java.nio.channels.FileChannel
import kotlin.concurrent.thread

class CustomVpnService : VpnService(), KoinComponent {

    private val TAG = "CustomVpnService"
    private val NOTIFICATION_ID = 1
    private val NOTIFICATION_CHANNEL_ID = "DnsVpnChannel"

    // Внедряем UseCase с помощью Koin
    private val getActiveRulesUseCase: GetActiveRulesUseCase by inject()
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var vpnInterface: ParcelFileDescriptor? = null
    private var vpnThread: Thread? = null

    @Volatile
    private var isThreadRunning = false
    private var dnsRules: Map<String, String> = emptyMap()

    companion object {
        const val ACTION_START = "com.arny.dnsrewriter.START_VPN"
        const val ACTION_STOP = "com.arny.dnsrewriter.STOP_VPN"

        // StateFlow для отслеживания статуса сервиса извне (например, из ViewModel)
        private val _isRunning = MutableStateFlow(false)
        val isRunning = _isRunning.asStateFlow()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return when (intent?.action) {
            ACTION_START -> {
                startVpn()
                START_STICKY // Перезапускать сервис, если система его убьет
            }

            ACTION_STOP -> {
                stopVpn()
                START_NOT_STICKY // Не перезапускать после остановки
            }

            else -> START_NOT_STICKY
        }
    }

    private fun startVpn() {
        if (isThreadRunning) { // Проверяем наш флаг
            Log.d(TAG, "VPN уже запущен.")
            return
        }

        isThreadRunning = true // Устанавливаем флаг
        _isRunning.value = true // Уведомляем UI
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())

        serviceScope.launch {
            loadRules()
            setupVpnInterface()
            Log.d(TAG, "VPN запущен с ${dnsRules.size} правилами.")
            VpnStateLogger.log("VPN запущен. Активных правил: ${dnsRules.size}")

            // Запускаем поток
            vpnThread = thread(start = true) { runVpnLogic() }
        }
    }

    private fun stopVpn() {
        Log.d(TAG, "Инициирована остановка VPN...")
        VpnStateLogger.log("Остановка VPN...")

        isThreadRunning = false // Сбрасываем флаг. Цикл в runVpnLogic теперь завершится.
        vpnThread?.interrupt() // По-прежнему вызываем interrupt(), чтобы прервать блокирующие вызовы I/O.

        // НЕ закрываем vpnInterface здесь. Это сделает runVpnLogic.

        stopForeground(true)
        stopSelf()
        _isRunning.value = false
    }

    private suspend fun loadRules() {
        dnsRules = getActiveRulesUseCase()
            .associate { it.domain.lowercase() to it.ipAddress }
    }

    private fun setupVpnInterface() {
        val builder = Builder()
            .setSession(getString(R.string.app_name))
            .addAddress("10.0.0.1", 24) // Виртуальный IP-адрес для туннеля
            .addRoute("0.0.0.0", 0)       // Перехватывать весь трафик
            .addDnsServer("8.8.8.8")       // Upstream DNS-сервер для запросов, которых нет в наших правилах
            .setMtu(1400)

        vpnInterface = builder.establish()
    }


    private fun runVpnLogic() {
        val vpnInputChannel: FileChannel = FileInputStream(vpnInterface!!.fileDescriptor).channel
        val vpnOutputChannel: FileChannel = FileOutputStream(vpnInterface!!.fileDescriptor).channel

        // Используем один и тот же буфер для экономии памяти
        val packet = ByteBuffer.allocate(32767)

        while (isThreadRunning) {
            try {
                packet.clear()
                val length = vpnInputChannel.read(packet)
                if (length > 0) {
                    packet.flip() // Готовим буфер к чтению (limit=length, position=0)

                    // --- КОРРЕКТНАЯ ПРОВЕРКА ВЕРСИИ IP ---
                    if (packet.remaining() > 0) {
                        // 1. Читаем первый байт прямо из буфера по индексу 0, не сдвигая позицию
                        val firstByte = packet.get(0)

                        // 2. Преобразуем Byte в Int и обнуляем старшие биты, чтобы получить беззнаковое значение
                        val firstByteAsUnsignedInt = firstByte.toInt() and 0xFF

                        // 3. Выполняем сдвиг для получения версии IP. `ushr` теперь применяется к Int.
                        val ipVersion = firstByteAsUnsignedInt ushr 4

                        if (ipVersion == 4) {
                            // Это IPv4 пакет, передаем его на обработку
                            // Создаем массив байтов только здесь, когда он действительно нужен
                            val originalPacketBytes = ByteArray(packet.remaining())
                            packet.get(originalPacketBytes)

                            handleIPv4Packet(originalPacketBytes, vpnOutputChannel)
                        }
                        // Все не-IPv4 пакеты просто игнорируются и не пропускаются
                    }
                }
            } catch (e: ClosedByInterruptException) {
                Log.d(TAG, "Канал закрыт по прерыванию, выходим из цикла.")
                break
            } catch (e: IOException) {
                Log.e(TAG, "Ошибка I/O, выходим из цикла", e)
                break
            } catch (e: Exception) {
                Log.e(TAG, "Неизвестная ошибка в цикле VPN", e)
            }
        }
        Log.d(TAG, "VPN поток завершен.")
    }

    private fun handleIPv4Packet(packetData: ByteArray, vpnOutputChannel: FileChannel) {
        val ipHeaderLength = getIpHeaderLengthIfDnsPacket(packetData)

        if (ipHeaderLength != null) {
            // Это DNS-запрос, обрабатываем его детально
            handleDnsPacket(packetData, ipHeaderLength, vpnOutputChannel)
        } else {
            // Это другой IPv4-трафик (TCP, ICMP и т.д.), просто пропускаем его
            try {
                vpnOutputChannel.write(ByteBuffer.wrap(packetData))
            } catch (e: IOException) {
                Log.e(TAG, "Ошибка при пропуске не-DNS пакета", e)
            }
        }
    }

    /**
     * Обрабатывает пакет, который уже был идентифицирован как DNS-запрос.
     * Парсит его, проверяет наличие правила и либо формирует поддельный ответ,
     * либо пропускает пакет дальше к реальному DNS-серверу.
     *
     * @param packetData Байты оригинального IP-пакета с DNS-запросом.
     * @param ipHeaderLength Длина IP-заголовка.
     * @param vpnOutputChannel Канал для записи ответных или пропущенных пакетов.
     */
    private fun handleDnsPacket(packetData: ByteArray, ipHeaderLength: Int, vpnOutputChannel: FileChannel) {
        try {
            val dnsDataOffset = ipHeaderLength + 8
            val dnsRequest = Message(packetData.sliceArray(dnsDataOffset until packetData.size))
            val question = dnsRequest.question ?: return

            val domain = question.name.toString(true).lowercase()
            val type = question.type
            val ruleIp = dnsRules[domain]

            if (type == Type.A && ruleIp != null) {
                // Этот лог оставляем, он важен для пользователя
                VpnStateLogger.log("ПЕРЕЗАПИСЬ: $domain -> $ruleIp")

                val dnsResponse = Message(dnsRequest.header.id).apply {
                    header.setFlag(Flags.QR.toInt())
                    header.setFlag(Flags.AA.toInt())
                    addRecord(question, Section.QUESTION)
                    addRecord(ARecord(question.name, DClass.IN, 3600L, Address.getByName(ruleIp)), Section.ANSWER)
                }

                val responsePacket = buildDnsResponsePacket(packetData, ipHeaderLength, dnsResponse.toWire())
                vpnOutputChannel.write(ByteBuffer.wrap(responsePacket))

            } else {
                // Просто пропускаем пакет дальше.
                vpnOutputChannel.write(ByteBuffer.wrap(packetData))
            }
        } catch (e: Exception) {
            // Системный лог для разработчика оставляем, но в логгер для UI не пишем
            Log.w(TAG, "Не удалось обработать DNS-пакет, пропускаем...", e)
            try {
                vpnOutputChannel.write(ByteBuffer.wrap(packetData))
            } catch (writeErr: IOException) {
                Log.e(TAG, "Критическая ошибка: не удалось пропустить пакет после сбоя", writeErr)
            }
        }
    }


    /**
     * Создает ответный IP/UDP пакет путем модификации оригинального запроса.
     * Это более надежный способ, чем создание пакета с нуля.
     */
    private fun buildDnsResponsePacket(
        requestPacket: ByteArray,
        ipHeaderLength: Int,
        dnsPayload: ByteArray
    ): ByteArray {
        val udpPayloadSize = 8 + dnsPayload.size // 8 байт UDP-заголовок + DNS-данные
        val ipPacketSize = ipHeaderLength + udpPayloadSize

        // 1. Создаем буфер нужного размера
        val responseBuffer = ByteBuffer.allocate(ipPacketSize)

        // 2. Копируем оригинальный IP-заголовок
        responseBuffer.put(requestPacket, 0, ipHeaderLength)

        // 3. Модифицируем IP-заголовок в новом буфере

        // Меняем местами IP-адреса (Source: 12-15, Dest: 16-19)
        val sourceIp = requestPacket.copyOfRange(12, 16)
        val destIp = requestPacket.copyOfRange(16, 20)
        responseBuffer.position(12)
        responseBuffer.put(destIp)
        responseBuffer.put(sourceIp)

        // Обновляем общую длину пакета (смещение 2)
        responseBuffer.position(2)
        responseBuffer.putShort(ipPacketSize.toShort())

        // Обнуляем чек-сумму (смещение 10) перед пересчетом
        responseBuffer.position(10)
        responseBuffer.putShort(0)

        // --- ВАЖНЫЙ ФИКС: Создаем временный массив ТОЛЬКО для расчета чек-суммы ---
        val ipHeaderForChecksum = responseBuffer.array().copyOfRange(0, ipHeaderLength)
        val ipChecksum = calculateChecksum(ipHeaderForChecksum, 0, ipHeaderLength)
        responseBuffer.position(10)
        responseBuffer.putShort(ipChecksum)

        // --- 4. Создаем UDP-заголовок ---
        responseBuffer.position(ipHeaderLength) // Переходим к месту начала UDP-заголовка

        // Читаем порты из оригинального пакета
        val originalUdpHeader = ByteBuffer.wrap(requestPacket, ipHeaderLength, 8)
        val sourcePort = originalUdpHeader.short
        val destPort = originalUdpHeader.short

        // Меняем порты местами
        responseBuffer.putShort(destPort) // Новый source port
        responseBuffer.putShort(sourcePort) // Новый dest port

        // Обновляем длину UDP (заголовок + данные)
        responseBuffer.putShort(udpPayloadSize.toShort())

        // UDP Checksum (можно оставить 0)
        responseBuffer.putShort(0)

        // --- 5. Добавляем DNS-ответ ---
        responseBuffer.put(dnsPayload)

        return responseBuffer.array()
    }

    /**
     * Универсальный расчет 16-битной чек-суммы по алгоритму RFC 1071.
     */
    private fun calculateChecksum(data: ByteArray, offset: Int, length: Int): Short {
        var sum = 0
        var i = offset
        // Суммируем 16-битные слова
        while (i < offset + length - 1) {
            val word = ((data[i].toInt() and 0xFF) shl 8) or (data[i + 1].toInt() and 0xFF)
            sum += word
            i += 2
        }
        // Если остался один байт, добавляем его
        if (length % 2 != 0) {
            sum += (data[i].toInt() and 0xFF) shl 8
        }
        // "Заворачиваем" переполнения
        while (sum ushr 16 > 0) {
            sum = (sum and 0xFFFF) + (sum ushr 16)
        }
        // Инвертируем результат
        return sum.inv().toShort()
    }

    /**
     * Проверяет, является ли пакет DNS-запросом (UDP, порт 53).
     * @return Длину IP-заголовка, если это DNS-пакет, иначе null.
     */
    private fun getIpHeaderLengthIfDnsPacket(data: ByteArray): Int? {
        if (data.size < 20) return null // Минимальный размер IP-заголовка

        // Длина IP заголовка в 32-битных словах (младшие 4 бита первого байта)
        val ipHeaderLength = (data[0].toInt() and 0x0F) * 4
        if (data.size < ipHeaderLength + 4) return null // Пакет слишком мал

        val protocol = data[9].toInt()
        if (protocol != 17) return null // 17 = UDP

        // Смещение до поля Destination Port в UDP-заголовке
        val dstPortOffset = ipHeaderLength + 2
        val dstPort =
            ((data[dstPortOffset].toInt() and 0xFF) shl 8) or (data[dstPortOffset + 1].toInt() and 0xFF)

        return if (dstPort == 53) ipHeaderLength else null
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy сервиса. Закрываем ресурсы.")
        // Гарантированно ждем завершения потока
        try {
            vpnThread?.join(500) // Ждем до 500 мс
        } catch (e: InterruptedException) {
            Log.w(TAG, "Прервано ожидание завершения vpnThread")
        }
        vpnThread = null

        try {
            vpnInterface?.close()
            Log.d(TAG, "Интерфейс VPN закрыт.")
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при закрытии интерфейса VPN", e)
        }
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        // Каналы уведомлений нужны только для API 26 (Android O) и выше.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "DNS Rewriter Service"
            val descriptionText = "Показывает, что VPN для перезаписи DNS активен"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("DNS Rewriter Активен")
            .setContentText("Нажмите для управления.")
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Замените на свою иконку
            .setPriority(NotificationCompat.PRIORITY_LOW) // Приоритет для старых версий
            .build()
    }
}