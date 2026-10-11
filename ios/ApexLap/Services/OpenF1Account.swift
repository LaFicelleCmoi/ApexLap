import ApexLapLive
import Foundation
import Observation
import Security

/// OpenF1 sponsor account: username in UserDefaults, password in the Keychain (tokens expire hourly, so the
/// password is needed to renew them).
@MainActor
@Observable
final class OpenF1Account {
    static let shared = OpenF1Account()
    private nonisolated static let usernameKey = "openf1Username"
    private nonisolated static let service = "com.apexlap.openf1"

    private(set) var username: String? = UserDefaults.standard.string(forKey: usernameKey)
    let repository = OpenF1Repository()
    let tokens: OpenF1TokenProvider

    private init() {
        tokens = OpenF1TokenProvider { await OpenF1Account.storedCredentials() }
    }

    var isSignedIn: Bool { username != nil }

    /// Validates the credentials by requesting a token; nothing is kept on failure.
    func signIn(username: String, password: String) async throws {
        let user = username.trimmingCharacters(in: .whitespaces)
        _ = try await OpenF1Client().token(username: user, password: password)
        Self.savePassword(password, account: user)
        UserDefaults.standard.set(user, forKey: Self.usernameKey)
        self.username = user
        await tokens.invalidate()
    }

    func signOut() async {
        if let username { Self.deletePassword(account: username) }
        UserDefaults.standard.removeObject(forKey: Self.usernameKey)
        username = nil
        await tokens.invalidate()
    }

    nonisolated static func storedCredentials() async -> (username: String, password: String)? {
        guard let user = UserDefaults.standard.string(forKey: usernameKey), let password = loadPassword(account: user) else { return nil }
        return (user, password)
    }

    private nonisolated static func query(account: String) -> [String: Any] {
        [kSecClass as String: kSecClassGenericPassword, kSecAttrService as String: service, kSecAttrAccount as String: account]
    }

    private nonisolated static func savePassword(_ password: String, account: String) {
        deletePassword(account: account)
        var attributes = query(account: account)
        attributes[kSecValueData as String] = Data(password.utf8)
        attributes[kSecAttrAccessible as String] = kSecAttrAccessibleAfterFirstUnlock
        SecItemAdd(attributes as CFDictionary, nil)
    }

    private nonisolated static func loadPassword(account: String) -> String? {
        var request = query(account: account)
        request[kSecReturnData as String] = true
        request[kSecMatchLimit as String] = kSecMatchLimitOne
        var item: CFTypeRef?
        guard SecItemCopyMatching(request as CFDictionary, &item) == errSecSuccess, let data = item as? Data else { return nil }
        return String(data: data, encoding: .utf8)
    }

    private nonisolated static func deletePassword(account: String) {
        SecItemDelete(query(account: account) as CFDictionary)
    }
}
