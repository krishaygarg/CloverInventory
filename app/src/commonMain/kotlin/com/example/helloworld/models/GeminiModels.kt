package com.example.helloworld.models

import kotlinx.serialization.Serializable

@Serializable
data class GeminiPart(
    val text: String
)

@Serializable
data class GeminiContent(
    val role: String = "user",
    val parts: List<GeminiPart>
)

@Serializable
data class GeminiRequest(
    val contents: List<GeminiContent>
)

@Serializable
data class GeminiCandidateContent(
    val parts: List<GeminiPart> = emptyList()
)

@Serializable
data class GeminiCandidate(
    val content: GeminiCandidateContent? = null
)

@Serializable
data class GeminiResponse(
    val candidates: List<GeminiCandidate> = emptyList()
)

@Serializable
data class GeminiInsightJson(
    val title: String = "",
    val description: String = "",
    val suggestedItemId: String? = null,
    val suggestedDiscount: Double = 0.15,
    val type: String = "RECOMMENDED",
    val comboName: String? = null,
    val comboItemIds: List<String> = emptyList(),
    val comboPrice: Long = 0L
)
