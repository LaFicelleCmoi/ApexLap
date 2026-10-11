package com.apexlap.data.openf1

import com.apexlap.domain.usecase.Clock
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.longOrNull
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/** Exchanges the sponsor credentials for an OAuth2 token (valid one hour) and renews it before expiry. */
@Singleton
class OpenF1TokenProvider @Inject constructor(
    private val api: OpenF1Api,
    private val credentials: OpenF1Credentials,
    private val clock: Clock,
) {
    private val mutex = Mutex()
    private var cached: Pair<String, Instant>? = null

    /** Null when no sponsor account is configured. */
    suspend fun token(): String? = mutex.withLock {
        cached?.let { (token, expiry) -> if (clock.now().isBefore(expiry.minusSeconds(120))) return token }
        val account = credentials.account() ?: return null
        val response = api.token(account.username, account.password)
        val lifetime = (response.expiresIn as? JsonPrimitive)?.let { it.longOrNull ?: it.content.toLongOrNull() } ?: 3600
        cached = response.accessToken to clock.now().plusSeconds(lifetime)
        response.accessToken
    }

    fun invalidate() {
        cached = null
    }
}
