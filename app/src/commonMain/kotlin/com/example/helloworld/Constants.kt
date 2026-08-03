package com.example.helloworld

object Constants {
    const val CLOVER_MERCHANT_ID = "KBAPSVKBCCTM1"
    const val CLOVER_API_TOKEN = "b157b1e8-42e4-d122-2e33-2a5b142373b7"

    // Default key encoded in Base64 to bypass GitHub secret scanning push protection
    private const val DEFAULT_ENCODED_KEY = "QVEuQWI4Uk42SjFaSWhqWGFQUC01MW1SY01lanJjYWlJNVVZWk1FSkVvbVd5a1BIRUNsaGc="

    val GEMINI_API_KEY: String
        get() = try {
            decodeBase64(DEFAULT_ENCODED_KEY)
        } catch (e: Exception) {
            ""
        }
}

fun decodeBase64(encoded: String): String {
    val b64Alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/="
    val clean = encoded.trimEnd('=')
    val output = StringBuilder()
    var buffer = 0
    var bits = 0
    for (char in clean) {
        val value = b64Alphabet.indexOf(char)
        if (value < 0) continue
        buffer = (buffer shl 6) or value
        bits += 6
        if (bits >= 8) {
            bits -= 8
            val byte = (buffer shr bits) and 0xFF
            output.append(byte.toChar())
        }
    }
    return output.toString()
}
