import ApexLapDomain
import ApexLapLive
import Foundation

enum LiveStreamError: LocalizedError {
    case refused(Int)
    var errorDescription: String? {
        switch self {
        case let .refused(code): I18n.strings.openF1Refused(code)
        }
    }
}

/// OpenF1 live feed: MQTT over URLSessionWebSocketTask, OAuth2 token as MQTT password.
enum OpenF1LiveStream {
    static func events(username: String, token: String) -> AsyncThrowingStream<TimingEvent, Error> {
        AsyncThrowingStream { continuation in
            let task = URLSession.shared.webSocketTask(with: OpenF1Live.websocketURL, protocols: ["mqtt"])
            let worker = Task {
                var reader = MqttCodec.Reader()
                do {
                    task.resume()
                    try await task.send(.data(MqttCodec.connect(clientId: "apexlap-\(UUID().uuidString)", username: username, password: token, keepAlive: 60)))
                    let keepAlive = Task {
                        while !Task.isCancelled {
                            try await Task.sleep(nanoseconds: 30_000_000_000)
                            try await task.send(.data(MqttCodec.pingReq()))
                        }
                    }
                    defer { keepAlive.cancel() }
                    while !Task.isCancelled {
                        let message = try await task.receive()
                        guard case let .data(bytes) = message else { continue }
                        for packet in reader.feed(bytes) {
                            switch packet {
                            case let .connAck(code):
                                guard code == 0 else { throw LiveStreamError.refused(code) }
                                try await task.send(.data(MqttCodec.subscribe(packetId: 1, topics: OpenF1Live.topics)))
                            case let .publish(topic, payload, _, packetId):
                                if let packetId { try await task.send(.data(MqttCodec.pubAck(packetId))) }
                                if let event = OpenF1Live.parse(topic: topic, payload: payload, now: .now) { continuation.yield(event) }
                            default:
                                break
                            }
                        }
                    }
                    continuation.finish()
                } catch {
                    continuation.finish(throwing: error)
                }
            }
            continuation.onTermination = { _ in
                worker.cancel()
                task.send(.data(MqttCodec.disconnect())) { _ in }
                task.cancel(with: .normalClosure, reason: nil)
            }
        }
    }
}
