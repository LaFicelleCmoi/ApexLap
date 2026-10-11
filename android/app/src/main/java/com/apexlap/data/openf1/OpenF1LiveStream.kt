package com.apexlap.data.openf1

import com.apexlap.domain.live.TimingEvent
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import okio.ByteString.Companion.toByteString
import java.io.IOException
import java.time.Instant
import java.util.UUID

class OpenF1AuthException(message: String) : IOException(message)

/**
 * Live timing push feed: MQTT over a WebSocket to wss://mqtt.openf1.org:8084/mqtt, authenticated with the
 * OAuth2 token as MQTT password. Each topic mirrors a REST endpoint (v1/position, v1/laps…).
 */
class OpenF1LiveStream(
    private val client: OkHttpClient,
    private val json: Json,
    private val now: () -> Instant = Instant::now,
) {
    fun events(username: String, token: String): Flow<TimingEvent> = callbackFlow {
        val reader = MqttCodec.Reader()
        val request = Request.Builder()
            .url(OpenF1Api.LIVE_WEBSOCKET_URL)
            .header("Sec-WebSocket-Protocol", "mqtt")
            .build()

        val socket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                webSocket.send(MqttCodec.connect("apexlap-${UUID.randomUUID()}", username, token, KEEP_ALIVE_SECONDS).toByteString())
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                reader.feed(bytes.toByteArray()).forEach { packet ->
                    when (packet) {
                        is MqttCodec.Packet.ConnAck ->
                            if (packet.returnCode == 0) {
                                webSocket.send(MqttCodec.subscribe(1, TOPICS).toByteString())
                            } else {
                                close(OpenF1AuthException("Connexion OpenF1 refusée (code ${packet.returnCode})"))
                            }
                        is MqttCodec.Packet.Publish -> {
                            packet.packetId?.let { webSocket.send(MqttCodec.pubAck(it).toByteString()) }
                            parse(packet.topic, packet.payload.decodeToString())?.let { trySend(it) }
                        }
                        else -> Unit
                    }
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                close(if (response?.code == 401) OpenF1AuthException("Jeton OpenF1 refusé") else t)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                close()
            }
        })

        val keepAlive = launch {
            while (true) {
                delay(KEEP_ALIVE_SECONDS * 1000L / 2)
                socket.send(MqttCodec.pingReq().toByteString())
            }
        }
        awaitClose {
            keepAlive.cancel()
            socket.send(MqttCodec.disconnect().toByteString())
            socket.close(1000, null)
        }
    }

    internal fun parse(topic: String, payload: String): TimingEvent? = runCatching {
        when (topic) {
            "v1/position" -> json.decodeFromString<OpenF1PositionDto>(payload).toEvent()
            "v1/intervals" -> json.decodeFromString<OpenF1IntervalDto>(payload).toEvent()
            "v1/laps" -> json.decodeFromString<OpenF1LapDto>(payload).toEvent(fallback = now())
            "v1/stints" -> json.decodeFromString<OpenF1StintDto>(payload).toEvent(now())
            "v1/race_control" -> json.decodeFromString<OpenF1RaceControlDto>(payload).toEvent()
            "v1/pit" -> json.decodeFromString<OpenF1PitDto>(payload).toEvent()
            else -> null
        }
    }.getOrNull()

    companion object {
        const val KEEP_ALIVE_SECONDS = 60
        val TOPICS = listOf("v1/position", "v1/intervals", "v1/laps", "v1/stints", "v1/race_control", "v1/pit")
    }
}
