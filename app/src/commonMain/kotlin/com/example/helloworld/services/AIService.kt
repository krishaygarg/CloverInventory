package com.example.helloworld.services

import com.example.helloworld.Constants
import com.example.helloworld.models.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json

data class AIInsight(
    val title: String,
    val description: String,
    val suggestedItemId: String?,
    val suggestedDiscount: Double,
    val type: InsightType,
    val suggestedCombo: FlashCombo? = null
)

enum class InsightType {
    RECOMMENDED, TRENDING, CLEARANCE, COMBO
}

fun formatPriceHelper(cents: Long): String {
    val dollars = cents / 100
    val remainder = cents % 100
    return "$dollars.${remainder.toString().padStart(2, '0')}"
}

/**
 * Direct Gemini API Service for Clover Inventory App.
 * Uses Google Gemini 3.1 Flash Lite with automatic 429 rate limit retries.
 * ZERO fallbacks - raw errors are captured and exposed for debugging.
 */
class AIService(
    private var apiKey: String = Constants.GEMINI_API_KEY,
    private var apiKey2: String = Constants.GEMINI_API_KEY_2
) {
    private val descriptionCache = mutableMapOf<String, String>()

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }

    private val client = HttpClient {
        install(ContentNegotiation) {
            json(json)
        }
    }

    var apiError: String? = null
        private set

    val isAiReady: Boolean get() = apiKey.isNotBlank() && apiKey != "YOUR_GEMINI_API_KEY"

    fun updateApiKey(key: String) {
        this.apiKey = key.trim()
        this.apiError = null
    }

    private fun extractRetryDelaySeconds(errorJson: String): Int? {
        val regex = Regex(""""retryDelay":\s*"(\d+)s"""")
        val match = regex.find(errorJson)
        return match?.groupValues?.get(1)?.toIntOrNull()
    }

private data class AuthConfig(val name: String, val key: String, val urlTemplate: String, val useBearer: Boolean)

    private suspend fun callGeminiApi(prompt: String): String {
        if (!isAiReady) {
            val error = "Gemini API key is missing. Set GEMINI_API_KEY in Constants.kt or click ENTER GEMINI API KEY."
            apiError = error
            throw IllegalStateException(error)
        }

        val modelsToTry = listOf("gemini-2.5-flash-lite", "gemini-1.5-flash", "gemini-1.5-flash-lite")
        val request = GeminiRequest(
            contents = listOf(
                GeminiContent(
                    role = "user",
                    parts = listOf(GeminiPart(text = prompt))
                )
            )
        )

        val errorsSummary = mutableListOf<String>()

        // Build auth configs for each non-blank key
        val keys = listOfNotNull(
            apiKey.trim().takeIf { it.isNotBlank() },
            apiKey2.trim().takeIf { it.isNotBlank() }
        )
        val authConfigs = keys.flatMap { k ->
            listOf(
                AuthConfig("query_key", k, "https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent?key=$k", useBearer = false),
                AuthConfig("header_bearer", k, "https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent", useBearer = true)
            )
        }

        for (model in modelsToTry) {
            for (auth in authConfigs) {
                val url = auth.urlTemplate.replace("{model}", model)
                var retryCount = 0
                val maxRetries = 1

                while (retryCount <= maxRetries) {
                    try {
                        val response = client.post(url) {
                            contentType(ContentType.Application.Json)
                            if (auth.useBearer) {
                                header("Authorization", "Bearer ${auth.key}")
                            }
                            setBody(request)
                        }

                        if (response.status == HttpStatusCode.OK) {
                            val geminiResponse: GeminiResponse = response.body()
                            val text = geminiResponse.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text
                            if (!text.isNullOrBlank()) {
                                apiError = null
                                return text
                            } else {
                                errorsSummary.add("Gemini ($model) empty candidate text.")
                                break
                            }
                        } else if (response.status == HttpStatusCode.TooManyRequests || response.status.value == 429) {
                            val errBody = response.bodyAsText()
                            val delaySeconds = extractRetryDelaySeconds(errBody) ?: (3 * (1 shl retryCount))
                            if (retryCount < maxRetries) {
                                retryCount++
                                delay(delaySeconds * 1000L)
                                continue
                            } else {
                                errorsSummary.add("Gemini ($model) 429 Rate Limit: $errBody")
                                break
                            }
                        } else {
                            val errBody = response.bodyAsText()
                            val httpErr = "Gemini ($model via ${auth.name}) HTTP ${response.status.value}: $errBody"
                            println(httpErr)
                            errorsSummary.add(httpErr)
                            break
                        }
                    } catch (e: Exception) {
                        val excErr = "Gemini ($model via ${auth.name}) Error: ${e.message}"
                        println(excErr)
                        errorsSummary.add(excErr)
                        break
                    }
                }
            }
        }

        val finalError = errorsSummary.distinct().take(2).joinToString(" | ")
        apiError = finalError
        throw IllegalStateException(finalError)
    }

    suspend fun getMerchantInsights(items: List<FlashItem>): List<AIInsight> {
        if (items.isEmpty()) return emptyList()

        val itemsSummary = items.joinToString("\n") { "- ID: ${it.id}, Name: ${it.name}, Price: $${formatPriceHelper(it.price)}" }
        val prompt = """
            You are an expert merchant inventory advisor for cafes and quick-service restaurants.
            Analyze these store inventory items:
            $itemsSummary

            PAIRING RULES FOR COMBO:
            1. Create a logical, highly popular combo bundle using EXACT IDs from the item list above.
            2. Match a Main Entree (e.g. Burger, Sandwich, Toast) with its natural complement (e.g. French Fries, Beverage, Side), OR match a Beverage (e.g. Coffee, Latte, Tea) with a Bakery item (e.g. Croissant, Muffin).
            3. NEVER pair arbitrary or incompatible items (such as Ice Cream with French Fries) when a primary main dish (like a Burger) or complementary beverage is present!
            4. comboItemIds MUST contain the exact string IDs of the paired items from the list above.

            Provide 3 merchant insights in JSON array format:
            1. COMBO: A sensible, high-value combo pairing (15% bundle discount).
            2. TRENDING: Recommend a popular item category to highlight (12% promotional discount).
            3. CLEARANCE: Select a slower-moving or high-margin item to accelerate sales turnover (20% discount).

            Return ONLY a raw JSON array (no markdown code block syntax, no extra text).
            JSON Schema:
            [
              {
                "title": "Perfect Pairing",
                "description": "Bundle [Item 1] & [Item 2] for 15% off.",
                "suggestedItemId": null,
                "suggestedDiscount": 0.15,
                "type": "COMBO",
                "comboName": "[Item 1] & [Item 2]",
                "comboItemIds": ["exact_id_1", "exact_id_2"],
                "comboPrice": 850
              },
              {
                "title": "Trending: [Category Name]",
                "description": "High demand in [Category Name]. Discount [Item Name] by 12% to drive sales volume.",
                "suggestedItemId": "exact_item_id",
                "suggestedDiscount": 0.12,
                "type": "TRENDING"
              },
              {
                "title": "Inventory Acceleration",
                "description": "Discount [Item Name] by 20% to accelerate inventory turnover.",
                "suggestedItemId": "exact_item_id",
                "suggestedDiscount": 0.20,
                "type": "CLEARANCE"
              }
            ]
        """.trimIndent()

        return try {
            val responseText = callGeminiApi(prompt)
            val cleanJson = responseText.trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

            val rawInsights: List<GeminiInsightJson> = json.decodeFromString(cleanJson)
            rawInsights.map { raw ->
                val type = when (raw.type.uppercase()) {
                    "COMBO" -> InsightType.COMBO
                    "TRENDING" -> InsightType.TRENDING
                    "CLEARANCE" -> InsightType.CLEARANCE
                    else -> InsightType.RECOMMENDED
                }

                val combo = if (type == InsightType.COMBO && raw.comboItemIds.isNotEmpty()) {
                    val comboItems = items.filter { it.id in raw.comboItemIds }
                    val originalSum = comboItems.sumOf { it.price }
                    val finalPrice = if (raw.comboPrice > 0) raw.comboPrice else (originalSum * 0.85).toLong()
                    FlashCombo(
                        id = "combo_${raw.comboItemIds.joinToString("_")}",
                        name = raw.comboName ?: comboItems.joinToString(" & ") { it.name },
                        itemIds = raw.comboItemIds,
                        bundlePrice = finalPrice,
                        description = raw.description
                    )
                } else null

                AIInsight(
                    title = raw.title,
                    description = raw.description,
                    suggestedItemId = raw.suggestedItemId,
                    suggestedDiscount = raw.suggestedDiscount,
                    type = type,
                    suggestedCombo = combo
                )
            }
        } catch (e: Exception) {
            println("Gemini API Merchant Insights Failed: ${e.message}")
            apiError = e.message
            emptyList()
        }
    }

    suspend fun generateDescription(itemName: String, itemId: String): String {
        descriptionCache[itemId]?.let { return it }

        val prompt = "Write a 1-sentence, factual, fluff-free description of $itemName (max 10 words). No marketing adjectives."
        return try {
            val responseText = callGeminiApi(prompt)
            val cleanDesc = responseText.trim().substringBefore("\n")
            descriptionCache[itemId] = cleanDesc
            cleanDesc
        } catch (e: Exception) {
            println("Gemini API Description Failed for $itemName: ${e.message}")
            val errText = "Gemini Error: ${e.message}"
            descriptionCache[itemId] = errText
            errText
        }
    }
}
