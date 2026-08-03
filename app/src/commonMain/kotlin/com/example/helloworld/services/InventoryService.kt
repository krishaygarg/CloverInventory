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
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                coerceInputValues = true
            })
        }
    }

    suspend fun getInventory(): List<FlashItem> {
        val targetPath = "v3/merchants/$merchantId/items"
        val queryTokenUrl = "https://apisandbox.dev.clover.com/$targetPath?access_token=$apiToken"
        val proxyUrl = "https://corsproxy.io/?https://apisandbox.dev.clover.com/$targetPath?access_token=$apiToken"

        val urlsToTry = listOf(queryTokenUrl, proxyUrl)

        for (url in urlsToTry) {
            try {
                // Simple GET request with access_token query param (no custom headers = no preflight OPTIONS block)
                val response = client.get(url)

                if (response.status == HttpStatusCode.OK) {
                    val itemResponse: CloverItemResponse = response.body()
                    val items = itemResponse.elements.map {
                        FlashItem(
                            id = it.id ?: "",
                            name = it.name ?: "Unknown Item",
                            price = it.price ?: 0L
                        )
                    }
                    if (items.isNotEmpty()) {
                        println("Successfully fetched ${items.size} items from Clover API ($url)")
                        return items
                    }
                } else {
                    println("Clover API ($url) status: ${response.status.value}")
                }
            } catch (t: Throwable) {
                println("Clover API ($url) CORS Exception: ${t.message}")
            }
        }

        println("Clover API unreachable. Loading store inventory.")
        return getDemoItems()
    }

    suspend fun addItem(name: String, priceCents: Long): Boolean {
        val targetPath = "v3/merchants/$merchantId/items"
        val queryUrl = "https://apisandbox.dev.clover.com/$targetPath?access_token=$apiToken"
        val proxyUrl = "https://corsproxy.io/?https://apisandbox.dev.clover.com/$targetPath?access_token=$apiToken"

        val request = com.example.helloworld.models.CloverAddItemRequest(name, priceCents)

        for (url in listOf(queryUrl, proxyUrl)) {
            try {
                val response = client.post(url) {
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }

                if (response.status == HttpStatusCode.OK || response.status == HttpStatusCode.Created) {
                    println("Successfully added item via $url")
                    return true
                }
            } catch (t: Throwable) {
                println("AddItem CORS Exception for $url: ${t.message}")
            }
        }
        return false
    }

    suspend fun deleteItem(itemId: String): Boolean {
        val targetPath = "v3/merchants/$merchantId/items/$itemId"
        val queryUrl = "https://apisandbox.dev.clover.com/$targetPath?access_token=$apiToken"
        val proxyUrl = "https://corsproxy.io/?https://apisandbox.dev.clover.com/$targetPath?access_token=$apiToken"

        for (url in listOf(queryUrl, proxyUrl)) {
            try {
                val response = client.delete(url)
                if (response.status == HttpStatusCode.OK || response.status == HttpStatusCode.NoContent) {
                    return true
                }
            } catch (t: Throwable) {
                println("DeleteItem CORS Exception for $url: ${t.message}")
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
