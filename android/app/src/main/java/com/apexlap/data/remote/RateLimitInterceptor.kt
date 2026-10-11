package com.apexlap.data.remote

import okhttp3.Interceptor
import okhttp3.Response
import java.io.InterruptedIOException

/**
 * Jolpica allows ~4 requests/second (500/hour) per client and answers HTTP 429 beyond that.
 * Requests are spaced by [minIntervalMillis] across all threads, and a 429 is retried with
 * backoff (honouring `Retry-After` when present).
 */
class RateLimitInterceptor(
    private val minIntervalMillis: Long = 300,
    private val maxRetries: Int = 3,
    private val sleeper: (Long) -> Unit = Thread::sleep,
    private val clock: () -> Long = System::currentTimeMillis,
) : Interceptor {

    private val lock = Any()
    private var nextSlot = 0L

    override fun intercept(chain: Interceptor.Chain): Response {
        var attempt = 0
        while (true) {
            awaitSlot()
            val response = chain.proceed(chain.request())
            if (response.code != 429 || attempt >= maxRetries) return response
            val delay = response.header("Retry-After")?.toLongOrNull()?.times(1_000)
                ?: (1_000L shl attempt)
            response.close()
            pause(delay)
            attempt++
        }
    }

    private fun awaitSlot() {
        val wait = synchronized(lock) {
            val now = clock()
            val slot = maxOf(now, nextSlot)
            nextSlot = slot + minIntervalMillis
            slot - now
        }
        if (wait > 0) pause(wait)
    }

    private fun pause(millis: Long) {
        try {
            sleeper(millis)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            throw InterruptedIOException("Interrupted while rate limiting").apply { initCause(e) }
        }
    }
}
