package com.apexlap.testing

import kotlinx.serialization.json.Json

val testJson = Json { ignoreUnknownKeys = true; explicitNulls = false }

fun fixture(name: String): String =
    requireNotNull(object {}.javaClass.classLoader?.getResource(name)) { "Missing fixture $name" }.readText()

inline fun <reified T> decodeFixture(name: String): T = testJson.decodeFromString(fixture(name))
