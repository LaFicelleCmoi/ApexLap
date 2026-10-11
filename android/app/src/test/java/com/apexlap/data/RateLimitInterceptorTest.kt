package com.apexlap.data

import com.apexlap.data.remote.RateLimitInterceptor
import okhttp3.Call
import okhttp3.Connection
import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.concurrent.TimeUnit

class RateLimitInterceptorTest {

    private var now = 0L
    private val sleeps = mutableListOf<Long>()
    private val interceptor = RateLimitInterceptor(
        minIntervalMillis = 300,
        maxRetries = 2,
        sleeper = { sleeps += it; now += it },
        clock = { now },
    )

    @Test
    fun `consecutive requests are spaced by the minimum interval`() {
        val chain = FakeChain(listOf(200, 200, 200))
        repeat(3) { interceptor.intercept(chain) }
        assertEquals(listOf(300L, 300L), sleeps)
    }

    @Test
    fun `429 is retried with Retry-After then exponential backoff`() {
        val chain = FakeChain(listOf(429, 429, 200), retryAfter = mapOf(0 to "2"))
        val response = interceptor.intercept(chain)
        assertEquals(200, response.code)
        assertEquals(3, chain.calls)
        // 2 s from Retry-After, then the 300 ms spacing is already covered, then 2 s backoff (attempt 1).
        assertEquals(listOf(2_000L, 2_000L), sleeps)
    }

    @Test
    fun `gives up after max retries and returns the 429`() {
        val chain = FakeChain(listOf(429, 429, 429, 429))
        assertEquals(429, interceptor.intercept(chain).code)
        assertEquals(3, chain.calls)
    }

    private class FakeChain(private val codes: List<Int>, private val retryAfter: Map<Int, String> = emptyMap()) : Interceptor.Chain {
        var calls = 0
        private val request = Request.Builder().url("https://api.jolpi.ca/ergast/f1/2026.json").build()

        override fun proceed(request: Request): Response {
            val index = calls++
            return Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(codes[index])
                .message("")
                .apply { retryAfter[index]?.let { header("Retry-After", it) } }
                .body("{}".toResponseBody())
                .build()
        }

        override fun request() = request
        override fun call(): Call = throw UnsupportedOperationException()
        override fun connection(): Connection? = null
        override fun connectTimeoutMillis() = 0
        override fun readTimeoutMillis() = 0
        override fun writeTimeoutMillis() = 0
        override fun withConnectTimeout(timeout: Int, unit: TimeUnit) = this
        override fun withReadTimeout(timeout: Int, unit: TimeUnit) = this
        override fun withWriteTimeout(timeout: Int, unit: TimeUnit) = this
    }
}
