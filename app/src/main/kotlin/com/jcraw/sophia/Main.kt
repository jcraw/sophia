package com.jcraw.sophia

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.jcraw.sophia.philosophers.ConversationState
import com.jcraw.sophia.service.PhilosopherService
import com.jcraw.sophia.ui.ConversationScreen
import com.jcraw.sophia.ui.ConversationSetupScreen
import kotlinx.coroutines.launch

@Composable
fun SophiaApp() {
    val philosopherService = remember {
        val apiKey = Config.openAiApiKey.ifBlank {
            System.getenv("OPENAI_API_KEY") ?: ""
        }
        if (apiKey.isBlank()) {
            println("⚠️ No API key found in config.properties or environment variable OPENAI_API_KEY")
        } else {
            println("✅ Found API key: ${apiKey.take(10)}...")
        }
        PhilosopherService(apiKey = apiKey)
    }

    val scope = rememberCoroutineScope()
    val conversationState by philosopherService.conversationState.collectAsState()
    var showSetup by remember { mutableStateOf(true) }

    DisposableEffect(Unit) {
        onDispose {
            philosopherService.close()
        }
    }

    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            if (showSetup && conversationState is ConversationState.NotStarted) {
                ConversationSetupScreen(
                    philosophers = philosopherService.getAllPhilosophers(),
                    onStartConversation = { config ->
                        scope.launch {
                            philosopherService.startConversation(config)
                            showSetup = false
                        }
                    }
                )
            } else {
                ConversationScreen(
                    state = conversationState,
                    onNewConversation = {
                        philosopherService.resetConversation()
                        showSetup = true
                    }
                )
            }
        }
    }
}

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Sophia - Philosophical Discussions",
        state = rememberWindowState(width = 1000.dp, height = 700.dp)
    ) {
        SophiaApp()
    }
}