package com.apexlap.data.openf1

import java.io.ByteArrayOutputStream

/**
 * Minimal MQTT 3.1.1 client codec — just what OpenF1's live feed needs: CONNECT with username/password,
 * SUBSCRIBE at QoS 0, receiving PUBLISH (QoS 0/1, acknowledged), keep-alive pings and DISCONNECT.
 * Spec: https://docs.oasis-open.org/mqtt/mqtt/v3.1.1/mqtt-v3.1.1.html
 */
object MqttCodec {

    sealed interface Packet {
        data class ConnAck(val returnCode: Int) : Packet
        data class SubAck(val packetId: Int, val grantedQos: List<Int>) : Packet
        data class Publish(val topic: String, val payload: ByteArray, val qos: Int, val packetId: Int?) : Packet
        data object PingResp : Packet
        data class Other(val type: Int) : Packet
    }

    fun connect(clientId: String, username: String, password: String, keepAliveSeconds: Int): ByteArray {
        val body = ByteArrayOutputStream().apply {
            writeString("MQTT")
            write(4) // protocol level 3.1.1
            write(0b1100_0010) // username + password + clean session
            write(keepAliveSeconds shr 8 and 0xFF)
            write(keepAliveSeconds and 0xFF)
            writeString(clientId)
            writeString(username)
            writeString(password)
        }.toByteArray()
        return packet(0x10, body)
    }

    fun subscribe(packetId: Int, topics: List<String>): ByteArray {
        val body = ByteArrayOutputStream().apply {
            write(packetId shr 8 and 0xFF)
            write(packetId and 0xFF)
            topics.forEach {
                writeString(it)
                write(0) // QoS 0
            }
        }.toByteArray()
        return packet(0x82, body)
    }

    fun pubAck(packetId: Int): ByteArray = byteArrayOf(0x40, 2, (packetId shr 8 and 0xFF).toByte(), (packetId and 0xFF).toByte())

    fun pingReq(): ByteArray = byteArrayOf(0xC0.toByte(), 0)

    fun disconnect(): ByteArray = byteArrayOf(0xE0.toByte(), 0)

    internal fun encodeRemainingLength(length: Int): ByteArray {
        require(length in 0..268_435_455) { "MQTT packet too large: $length" }
        val out = ByteArrayOutputStream()
        var value = length
        do {
            var digit = value % 128
            value /= 128
            if (value > 0) digit = digit or 0x80
            out.write(digit)
        } while (value > 0)
        return out.toByteArray()
    }

    private fun packet(header: Int, body: ByteArray): ByteArray =
        ByteArrayOutputStream().apply {
            write(header)
            write(encodeRemainingLength(body.size))
            write(body)
        }.toByteArray()

    private fun ByteArrayOutputStream.writeString(value: String) {
        val bytes = value.toByteArray(Charsets.UTF_8)
        write(bytes.size shr 8 and 0xFF)
        write(bytes.size and 0xFF)
        write(bytes)
    }

    /**
     * Reassembles packets from a byte stream: a WebSocket frame may carry several MQTT packets,
     * or only part of one.
     */
    class Reader {
        private var buffer = ByteArray(0)

        fun feed(bytes: ByteArray): List<Packet> {
            buffer += bytes
            val packets = mutableListOf<Packet>()
            while (true) {
                val (packet, consumed) = tryDecode(buffer) ?: break
                packets += packet
                buffer = buffer.copyOfRange(consumed, buffer.size)
            }
            return packets
        }
    }

    /** Returns the first complete packet in [data] and how many bytes it used, or null if incomplete. */
    internal fun tryDecode(data: ByteArray): Pair<Packet, Int>? {
        if (data.size < 2) return null
        var multiplier = 1
        var length = 0
        var index = 1
        while (true) {
            if (index >= data.size) return null
            val digit = data[index].toInt() and 0xFF
            length += (digit and 0x7F) * multiplier
            multiplier *= 128
            index++
            if (digit and 0x80 == 0) break
            if (index > 4) throw IllegalStateException("Malformed MQTT remaining length")
        }
        val total = index + length
        if (data.size < total) return null
        val header = data[0].toInt() and 0xFF
        val body = data.copyOfRange(index, total)
        val packet = when (header shr 4) {
            2 -> Packet.ConnAck(returnCode = body.getOrNull(1)?.toInt()?.and(0xFF) ?: -1)
            9 -> Packet.SubAck(body.u16(0), body.drop(2).map { it.toInt() and 0xFF })
            3 -> {
                val qos = header shr 1 and 0x03
                val topicLength = body.u16(0)
                val topic = String(body, 2, topicLength, Charsets.UTF_8)
                var offset = 2 + topicLength
                val packetId = if (qos > 0) body.u16(offset).also { offset += 2 } else null
                Packet.Publish(topic, body.copyOfRange(offset, body.size), qos, packetId)
            }
            13 -> Packet.PingResp
            else -> Packet.Other(header shr 4)
        }
        return packet to total
    }

    private fun ByteArray.u16(offset: Int): Int = (this[offset].toInt() and 0xFF shl 8) or (this[offset + 1].toInt() and 0xFF)
}
