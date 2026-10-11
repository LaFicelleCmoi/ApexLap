import Foundation

/// Minimal MQTT 3.1.1 client codec for OpenF1's live feed (same scope as the Android `MqttCodec`).
public enum MqttCodec {
    public enum Packet: Equatable, Sendable {
        case connAck(returnCode: Int)
        case subAck(packetId: Int)
        case publish(topic: String, payload: Data, qos: Int, packetId: Int?)
        case pingResp
        case other(type: Int)
    }

    public static func connect(clientId: String, username: String, password: String, keepAlive: Int) -> Data {
        var body = Data()
        body.appendString("MQTT")
        body.append(4) // protocol level 3.1.1
        body.append(0b1100_0010) // username + password + clean session
        body.append(UInt8(keepAlive >> 8 & 0xFF))
        body.append(UInt8(keepAlive & 0xFF))
        body.appendString(clientId)
        body.appendString(username)
        body.appendString(password)
        return packet(0x10, body)
    }

    public static func subscribe(packetId: Int, topics: [String]) -> Data {
        var body = Data([UInt8(packetId >> 8 & 0xFF), UInt8(packetId & 0xFF)])
        for topic in topics {
            body.appendString(topic)
            body.append(0)
        }
        return packet(0x82, body)
    }

    public static func pubAck(_ packetId: Int) -> Data { Data([0x40, 2, UInt8(packetId >> 8 & 0xFF), UInt8(packetId & 0xFF)]) }
    public static func pingReq() -> Data { Data([0xC0, 0]) }
    public static func disconnect() -> Data { Data([0xE0, 0]) }

    static func encodeRemainingLength(_ length: Int) -> Data {
        var out = Data()
        var value = length
        repeat {
            var digit = UInt8(value % 128)
            value /= 128
            if value > 0 { digit |= 0x80 }
            out.append(digit)
        } while value > 0
        return out
    }

    private static func packet(_ header: UInt8, _ body: Data) -> Data {
        var out = Data([header])
        out.append(encodeRemainingLength(body.count))
        out.append(body)
        return out
    }

    /// Returns the first complete packet and how many bytes it used, or nil when more bytes are needed.
    static func decode(_ data: [UInt8]) -> (Packet, Int)? {
        guard data.count >= 2 else { return nil }
        var multiplier = 1, length = 0, index = 1
        while true {
            guard index < data.count, index <= 4 else { return nil }
            let digit = Int(data[index])
            length += (digit & 0x7F) * multiplier
            multiplier *= 128
            index += 1
            if digit & 0x80 == 0 { break }
        }
        let total = index + length
        guard data.count >= total else { return nil }
        let header = Int(data[0])
        let body = Array(data[index..<total])
        func u16(_ offset: Int) -> Int { Int(body[offset]) << 8 | Int(body[offset + 1]) }
        let packet: Packet
        switch header >> 4 {
        case 2: packet = .connAck(returnCode: body.count > 1 ? Int(body[1]) : -1)
        case 9: packet = .subAck(packetId: u16(0))
        case 3:
            let qos = header >> 1 & 0x03
            let topicLength = u16(0)
            let topic = String(decoding: body[2..<(2 + topicLength)], as: UTF8.self)
            var offset = 2 + topicLength
            var packetId: Int?
            if qos > 0 {
                packetId = u16(offset)
                offset += 2
            }
            packet = .publish(topic: topic, payload: Data(body[offset...]), qos: qos, packetId: packetId)
        case 13: packet = .pingResp
        default: packet = .other(type: header >> 4)
        }
        return (packet, total)
    }

    /// Reassembles packets split across (or packed into) WebSocket frames.
    public struct Reader: Sendable {
        private var buffer: [UInt8] = []
        public init() {}

        public mutating func feed(_ bytes: Data) -> [Packet] {
            buffer.append(contentsOf: bytes)
            var packets: [Packet] = []
            while let (packet, used) = MqttCodec.decode(buffer) {
                packets.append(packet)
                buffer.removeFirst(used)
            }
            return packets
        }
    }
}

private extension Data {
    mutating func appendString(_ value: String) {
        let bytes = Array(value.utf8)
        append(UInt8(bytes.count >> 8 & 0xFF))
        append(UInt8(bytes.count & 0xFF))
        append(contentsOf: bytes)
    }
}
