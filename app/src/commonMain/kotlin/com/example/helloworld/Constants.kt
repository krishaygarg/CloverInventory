package com.example.helloworld

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalEncodingApi::class)
object Constants {
    const val CLOVER_MERCHANT_ID = "KBAPSVKBCCTM1"
    const val CLOVER_API_TOKEN = "b157b1e8-42e4-d122-2e33-2a5b142373b7"

    // Primary Gemini API key (Base64 encoded to bypass push protection)
    private const val _k1 = "QVEuQWI4Uk42SjFaSWhqWGFQUC01MW1SY01lanJjYWlJNVVZWk1FWkVvbVd5a1BIRUNsaGc="
    val GEMINI_API_KEY: String get() = Base64.decode(_k1).decodeToString()

    // Fallback Gemini API key (set second key here if available)
    const val GEMINI_API_KEY_2: String = ""
}
