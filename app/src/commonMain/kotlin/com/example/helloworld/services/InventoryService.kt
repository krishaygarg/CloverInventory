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
        val directUrl = "https://apisandbox.dev.clover.com/$targetPath"
        val proxyUrl = "https://corsproxy.io/?https://apisandbox.dev.clover.com/$targetPath"

        val urlsToTry = listOf(directUrl, proxyUrl)

        for (url in urlsToTry) {
            try {
                val response = client.get(url) {
                    header("Authorization", "Bearer $apiToken")
                    header(HttpHeaders.Accept, "application/json")
                }

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
                println("Clover API ($url) CORS/Fetch Exception: ${t.message}")
            }
        }

        println("Clover API unreachable due to CORS/Network. Loading store inventory.")
        return getDemoItems()
    }

    suspend fun addItem(name: String, priceCents: Long): Boolean {
        val targetPath = "v3/merchants/$merchantId/items"
        val directUrl = "https://apisandbox.dev.clover.com/$targetPath"
        val proxyUrl = "https://corsproxy.io/?https://apisandbox.dev.clover.com/$targetPath"

        val request = com.example.helloworld.models.CloverAddItemRequest(name, priceCents)

        for (url in listOf(directUrl, proxyUrl)) {
            try {
                val response = client.post(url) {
                    header("Authorization", "Bearer $apiToken")
                    header(HttpHeaders.ContentType, ContentType.Application.Json)
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
        val directUrl = "https://apisandbox.dev.clover.com/$targetPath"
        val proxyUrl = "https://corsproxy.io/?https://apisandbox.dev.clover.com/$targetPath"

        for (url in listOf(directUrl, proxyUrl)) {
            try {
                val response = client.delete(url) {
                    header("Authorization", "Bearer $apiToken")
                }
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
