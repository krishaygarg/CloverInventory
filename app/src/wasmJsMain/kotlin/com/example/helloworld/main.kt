package com.example.helloworld

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.example.helloworld.services.AIService
import kotlinx.browser.document

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val container = document.getElementById("app-container") ?: document.body!!
    val aiService = AIService()

    ComposeViewport(container) {
        App(aiService)
    }
}
