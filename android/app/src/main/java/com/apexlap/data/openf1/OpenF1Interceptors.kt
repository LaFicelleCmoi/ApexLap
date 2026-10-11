package com.apexlap.data.openf1

import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody

/** OpenF1 answers 404 `{"detail":"No results found."}` for empty queries; turn that into an empty array. */
class OpenF1EmptyResultInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        if (response.code != 404) return response
        val body = response.peekBody(1024).string()
        if (!body.contains("No results found")) return response
        response.close()
        return response.newBuilder()
            .code(200)
            .message("OK")
            .body("[]".toResponseBody("application/json".toMediaType()))
            .build()
    }
}
