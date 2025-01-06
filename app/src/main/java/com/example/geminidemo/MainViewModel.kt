

package com.example.geminidemo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class Chat(private val model: GenerativeModel) {

    // Function to send a message and stream the response in chunks
    suspend fun sendMessageWithStreaming(input: String): Flow<String> = flow {
        val chatHistory = listOf(
            content("user") { text("I'm ready to assist! Please define a word, and I'll respond to related commands.") }
        )

        val chat = model.startChat(chatHistory)
        val response = chat.sendMessage(input)

        response.text?.chunked(100)?.forEach { chunk ->
            emit(chunk)
            delay(500) // Simulate delay between chunks
        }
    }
}

class MainViewModel : ViewModel() {

    private val _displayedMessages = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val displayedMessages: StateFlow<List<Pair<String, String>>> = _displayedMessages

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating

    private var currentWord: String? = null // Track the current word being defined

    // Initialize the Gemini model with configuration
    private val model = GenerativeModel(
        "gemini-1.0-pro",
        apiKey = "AIzaSyBSxfsS5wZAtLFQR5InIDolvPge4Eo7r4o", // Replace with your actual API key
        generationConfig = generationConfig {
            temperature = 0.9f
            topP = 1f
            maxOutputTokens = 4096
            responseMimeType = "text/plain"
        }
    )

    private val chat = Chat(model)

    // Handles user input and processes commands
    fun handleUserInput(input: String) {
        viewModelScope.launch {
            _isGenerating.value = true
            val updatedMessages = _displayedMessages.value.toMutableList()

            when {
                input.startsWith("Define ") -> handleDefineCommand(input, updatedMessages)
                currentWord != null -> handleWordCommands(input, updatedMessages)
                else -> {
                    updatedMessages.add("user" to input)
                    updatedMessages.add("model" to "Please define a word first using 'Define [word]'.")
                }
            }

            _displayedMessages.value = updatedMessages
            _isGenerating.value = false
        }
    }

    // Handles the "Define [word]" command
    private fun handleDefineCommand(input: String, updatedMessages: MutableList<Pair<String, String>>) {
        val word = input.removePrefix("Define ").trim()
        currentWord = word // Set the current word
        updatedMessages.clear() // Clear chat history
        updatedMessages.add("user" to input)
        generateResponse("Define $word", updatedMessages)
    }

    // Handles commands related to the current word
    private fun handleWordCommands(input: String, updatedMessages: MutableList<Pair<String, String>>) {
        val command = input.trim()
        currentWord?.let { word ->
            updatedMessages.add("user" to input)
            when {
                command.equals("Pronounce it", ignoreCase = true) -> generateResponse("Pronounce $word", updatedMessages)
                command.equals("Give me a Synonym", ignoreCase = true) -> generateResponse("Synonym of $word", updatedMessages)
                command.equals("Give me an Example", ignoreCase = true) -> generateResponse("Use $word in a sentence", updatedMessages)
                command.equals("Give me the Origin", ignoreCase = true) -> generateResponse("Origin of $word", updatedMessages)
                else -> {
                    updatedMessages.add("model" to "Unknown command. Try 'Pronounce it', 'Give me a Synonym', 'Give me an Example', or 'Give me the Origin'.")
                }
            }
        }
    }

    // Generates a response using the Gemini model
    private fun generateResponse(input: String, updatedMessages: MutableList<Pair<String, String>>) {
        viewModelScope.launch {
            try {
                chat.sendMessageWithStreaming(input).collect { chunk ->
                    updatedMessages.add("model" to chunk)
                    _displayedMessages.value = updatedMessages
                }
            } catch (e: Exception) {
                updatedMessages.add("model" to "Error: ${e.message}")
                _displayedMessages.value = updatedMessages
            }
        }
    }
}

