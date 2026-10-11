package com.apexlap.data.remote

import okhttp3.Interceptor
import okhttp3.Response
import java.io.InterruptedIOException

/**
 * Client-side rate limiting. Jolpica allows ~4 requests/second; OpenF1's free tier 3/second and 30/minute.
 * Requests are spaced by [minIntervalMillis] across all threads, optionally capped to [maxPerMinute] in a
 * sliding window, and a 429 is retried with backoff (honouring `Retry-After` when present).
 */
class RateLimitInterceptor(
    private val minIntervalMillis: Long = 300,
    private val maxPerMinute: Int? = null,
    private val maxRetries: Int = 3,
    private val sleeper: (Long) -> Unit = Thread::sleep,
    private val clock: () -> Long = System::currentTimeMillis,
) : Interceptor {

    private val lock = Any()
    private var nextSlot = 0L
    private val recent = ArrayDeque<Long>()

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
            var slot = maxOf(now, nextSlot)
            maxPerMinute?.let { limit ->
                while (recent.isNotEmpty() && recent.first() <= slot - 60_000) recent.removeFirst()
                if (recent.size >= limit) slot = maxOf(slot, recent.first() + 60_000)
                recent.addLast(slot)
            }
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
