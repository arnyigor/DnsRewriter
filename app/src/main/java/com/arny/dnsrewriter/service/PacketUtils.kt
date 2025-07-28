package com.arny.dnsrewriter.service

import java.nio.ByteBuffer

object PacketUtils {

    fun getProtocol(packet: ByteBuffer): Int {
        return packet.get(9).toInt() and 0xFF
    }

    fun getSourceIp(packet: ByteBuffer): String {
        return ipToString(packet, 12)
    }

    fun getDestinationIp(packet: ByteBuffer): String {
        return ipToString(packet, 16)
    }

    fun getSourcePort(packet: ByteBuffer, ipHeaderLength: Int): Int {
        return packet.getShort(ipHeaderLength).toInt() and 0xFFFF
    }

    fun getDestinationPort(packet: ByteBuffer, ipHeaderLength: Int): Int {
        return packet.getShort(ipHeaderLength + 2).toInt() and 0xFFFF
    }

    private fun ipToString(packet: ByteBuffer, offset: Int): String {
        return buildString {
            append(packet.get(offset).toInt() and 0xFF)
            append(".")
            append(packet.get(offset + 1).toInt() and 0xFF)
            append(".")
            append(packet.get(offset + 2).toInt() and 0xFF)
            append(".")
            append(packet.get(offset + 3).toInt() and 0xFF)
        }
    }
}