package com.apexlap.live

import com.apexlap.data.openf1.MqttCodec
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MqttCodecTest {

    @Test
    fun `remaining length uses the MQTT variable-length encoding`() {
        assertArrayEquals(byteArrayOf(0), MqttCodec.encodeRemainingLength(0))
        assertArrayEquals(byteArrayOf(127), MqttCodec.encodeRemainingLength(127))
        assertArrayEquals(byteArrayOf(0x80.toByte(), 0x01), MqttCodec.encodeRemainingLength(128))
        assertArrayEquals(byteArrayOf(0xFF.toByte(), 0x7F), MqttCodec.encodeRemainingLength(16_383))
        assertArrayEquals(byteArrayOf(0x80.toByte(), 0x80.toByte(), 0x01), MqttCodec.encodeRemainingLength(16_384))
    }

    @Test
    fun `connect carries protocol 3_1_1, credentials flags and keep-alive`() {
        val packet = MqttCodec.connect("c", "me@x.fr", "token", keepAliveSeconds = 60)
        assertEquals(0x10, packet[0].toInt())
        // remaining length, then "MQTT" protocol name.
        assertEquals("MQTT", String(packet, 4, 4))
        assertEquals(4, packet[8].toInt())
        assertEquals(0b1100_0010, packet[9].toInt() and 0xFF)
        assertEquals(60, packet[11].toInt())
        val text = String(packet)
        assertTrue(text.contains("me@x.fr") && text.contains("token"))
        assertEquals(packet.size - 2, packet[1].toInt())
    }

    @Test
    fun `subscribe lists topics at QoS 0`() {
        val packet = MqttCodec.subscribe(7, listOf("v1/laps"))
        assertEquals(0x82, packet[0].toInt() and 0xFF)
        assertEquals(7, packet[3].toInt())
        assertEquals("v1/laps", String(packet, 6, 7))
        assertEquals(0, packet.last().toInt())
    }

    private fun publish(topic: String, payload: String, qos: Int = 0, id: Int = 0): ByteArray {
        val body = mutableListOf<Byte>()
        body += (topic.length shr 8).toByte(); body += topic.length.toByte()
        body += topic.toByteArray().toList()
        if (qos > 0) { body += (id shr 8).toByte(); body += id.toByte() }
        body += payload.toByteArray().toList()
        return byteArrayOf((0x30 or (qos shl 1)).toByte()) + MqttCodec.encodeRemainingLength(body.size) + body.toByteArray()
    }

    @Test
    fun `reader decodes connack, QoS 1 publish and pingresp`() {
        val reader = MqttCodec.Reader()
        val packets = reader.feed(byteArrayOf(0x20, 2, 0, 0) + publish("v1/position", """{"a":1}""", qos = 1, id = 42) + byteArrayOf(0xD0.toByte(), 0))
        assertEquals(MqttCodec.Packet.ConnAck(0), packets[0])
        val pub = packets[1] as MqttCodec.Packet.Publish
        assertEquals("v1/position", pub.topic)
        assertEquals("""{"a":1}""", pub.payload.decodeToString())
        assertEquals(42, pub.packetId)
        assertEquals(MqttCodec.Packet.PingResp, packets[2])
    }

    @Test
    fun `reader reassembles a packet split across frames`() {
        val reader = MqttCodec.Reader()
        val payload = "x".repeat(300) // forces a two-byte remaining length
        val bytes = publish("v1/laps", payload)
        assertTrue(reader.feed(bytes.copyOfRange(0, 1)).isEmpty())
        assertTrue(reader.feed(bytes.copyOfRange(1, 150)).isEmpty())
        val packets = reader.feed(bytes.copyOfRange(150, bytes.size))
        assertEquals(payload, (packets.single() as MqttCodec.Packet.Publish).payload.decodeToString())
    }

    @Test
    fun `pubAck echoes the packet id`() {
        assertArrayEquals(byteArrayOf(0x40, 2, 0x01, 0x02), MqttCodec.pubAck(258))
    }
}
