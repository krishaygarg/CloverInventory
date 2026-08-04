package com.example.helloworld.services

import com.example.helloworld.models.CloverItemResponse
import com.example.helloworld.models.FlashItem
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

class InventoryService(
    private val merchantId: String,
    private val apiToken: String
) {
    private val jsonDecoder = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }

    private val client = HttpClient {
        install(ContentNegotiation) {
            json(jsonDecoder)
        }
    }

    private val baseUrl = "https://apisandbox.dev.clover.com"

    private suspend fun cloverGet(path: String): String? {
        val urls = listOf(
            "/api/clover" to path,
            "https://api.allorigins.win/raw?url=" to "https://apisandbox.dev.clover.com$path",
            baseUrl to path,
            "https://corsproxy.io/?" to "$baseUrl$path"
        )
        for ((base, p) in urls) {
            try {
                val fullUrl = if (base.startsWith("/")) "$p" else if (base.contains("url=")) "$base$p" else "$base$p"
                val targetUrl = if (base == "/api/clover") "/api/clover$path" else fullUrl
                val response = client.get(targetUrl) {
                    header(HttpHeaders.Authorization, "Bearer $apiToken")
                    header(HttpHeaders.Accept, "application/json")
                }
                if (response.status == HttpStatusCode.OK) {
                    val body = response.bodyAsText()
                    if (body.trimStart().startsWith("{")) return body
                }
            } catch (t: Throwable) {
                println("Clover GET $base$p: ${t.message}")
            }
        }
        return null
    }

    suspend fun getInventory(): List<FlashItem> {
        val path = "/v3/merchants/$merchantId/items"
        val body = cloverGet(path)

        if (body != null) {
            return try {
                val itemResponse: CloverItemResponse = jsonDecoder.decodeFromString(body)
                itemResponse.elements.map {
                    FlashItem(
                        id = it.id ?: "",
                        name = it.name ?: "Unknown Item",
                        price = it.price ?: 0L
                    )
                }.also { println("Loaded ${it.size} items from Clover API") }
            } catch (t: Throwable) {
                println("Parse error: ${t.message}")
                getDemoItems()
            }
        }

        println("Clover API unreachable. Serving demo inventory.")
        return getDemoItems()
    }

    suspend fun addItem(name: String, priceCents: Long): Boolean {
        val request = com.example.helloworld.models.CloverAddItemRequest(name, priceCents)
        val path = "/v3/merchants/$merchantId/items"
        val urls = listOf(
            "/api/clover$path",
            "$baseUrl$path",
            "https://corsproxy.io/?$baseUrl$path"
        )
        for (url in urls) {
            try {
                val response = client.post(url) {
                    header(HttpHeaders.Authorization, "Bearer $apiToken")
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }
                if (response.status == HttpStatusCode.OK || response.status == HttpStatusCode.Created) return true
            } catch (t: Throwable) {
                println("AddItem $url: ${t.message}")
            }
        }
        return false
    }

    suspend fun deleteItem(itemId: String): Boolean {
        val path = "/v3/merchants/$merchantId/items/$itemId"
        val urls = listOf(
            "/api/clover$path",
            "$baseUrl$path",
            "https://corsproxy.io/?$baseUrl$path"
        )
        for (url in urls) {
            try {
                val response = client.delete(url) {
                    header(HttpHeaders.Authorization, "Bearer $apiToken")
                }
                if (response.status == HttpStatusCode.OK || response.status == HttpStatusCode.NoContent) return true
            } catch (t: Throwable) {
                println("DeleteItem $url: ${t.message}")
            }
        }
        return false
    }

    private fun getDemoItems(): List<FlashItem> {
        return listOf(
            FlashItem("1", "Chocolate Croissant", 350),
            FlashItem("2", "Blueberry Muffin", 275),
            FlashItem("3", "Flat White", 450),
            FlashItem("4", "Iced Latte", 500),
            FlashItem("5", "Avocado Toast", 1200)
        )
    }
}
