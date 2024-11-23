package com.example.geminidemo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

class Chat(private val model: GenerativeModel) {

    // Function to send a message and stream the response in chunks
    suspend fun sendMessageWithStreaming(input: String): Flow<String> {
        return flow {
            // Simulating response chunks for streaming
            val responseChunks = listOf(
                "Interpret and translate if necessary: $input",
                "This is a stream response part 1.",
                "Stream response part 2.",
                "Final part of the response."
            )
            for (chunk in responseChunks) {
                emit(chunk) // Emit each chunk of the response
                kotlinx.coroutines.delay(500) // Simulate some delay for each chunk
            }
        }
    }
}

class MainViewModel : ViewModel() {

    private val _displayedMessages = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val displayedMessages: StateFlow<List<Pair<String, String>>> = _displayedMessages

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating

    private var greetingShown = false // Track whether the greeting has been shown
     val api_key = ""
    // Initialize the Gemini model with configuration
    private val model = GenerativeModel(
        "gemini-1.0-pro",
        api_key,
//        BuildConfig.API_KEY, // API key from BuildConfig
        generationConfig = generationConfig {
            temperature = 0.9f
            topP = 1f
            maxOutputTokens = 2048
            responseMimeType = "text/plain"
        }
    )

    private val chat = Chat(model) // Chat instance for streaming responses

    // Internal predefined prompts and responses with English translations
    private val internalChatPrompts = mapOf(
        "hello" to "My name is Jess, your friendly multilingual assistant. Feel free to converse with me.",
        "hi" to "My name is Jess, your friendly multilingual assistant. Feel free to converse with me.",
        "habari yako" to "Good, thank you! How can I assist you?",  // Swahili to English
        "nataka kuenda Kisumu" to "Where would you like to go in Kisumu?", // Swahili to English
        "niongeleshe na Kiswahili" to "No problem, we will converse in Swahili.", // Swahili to English
        "bonjour" to "Hello! My name is Jess, your multilingual assistant. How can I assist you?",  // French to English
        "comment ça va" to "I'm doing well, thank you! How can I assist you?", // French to English
        "olá" to "Hello! My name is Jess, your multilingual assistant. How can I assist you?",  // Portuguese to English
        "como você está" to "I’m doing well, thank you! How can I help you?", // Portuguese to English
        "role" to "Okay, I understand. From now on, if you type in a language other than English, I will do my best to translate it into English for you.\n",
        
    )

    // Function to handle user input and respond appropriately
    fun handleUserInput(input: String) {
        viewModelScope.launch {
            _isGenerating.value = true

            val updatedMessages = _displayedMessages.value.toMutableList()
            updatedMessages.add("user" to input)

            // Step 1: Check for predefined responses first (immediate response if found)
            val predefinedResponse = getPredefinedResponse(input)
            if (predefinedResponse != null) {
                // Handle greeting logic to prevent repeating it
//                if (predefinedResponse.contains("My name is Jess") && greetingShown) {
//                    _isGenerating.value = false
//                    return@launch
//                } else if (predefinedResponse.contains("My name is Jess")) {
//                    greetingShown = true
//                }

                // Add predefined response instantly without delay
                updatedMessages.add("model" to predefinedResponse)
                _displayedMessages.value = updatedMessages
                _isGenerating.value = false
                return@launch
            }

            // Step 2: Generate a general response for unrecognized input
            try {
                generateResponseWithGemini(input, updatedMessages)
            } catch (e: Exception) {
                updatedMessages.add("model" to "Error: ${e.message}")
                _displayedMessages.value = updatedMessages
            } finally {
                _isGenerating.value = false
            }
        }
    }

    // Function to check predefined responses based on input
    private fun getPredefinedResponse(input: String): String {
        return internalChatPrompts[input.lowercase()] ?: input
    }

    // Function to generate response using Gemini with streaming enabled
    private suspend fun generateResponseWithGemini(input: String, updatedMessages: MutableList<Pair<String, String>>) {
        try {
            // Use the Chat class to send the message and stream the response
            chat.sendMessageWithStreaming(input).collect { chunk ->
                // Each chunk is emitted one at a time
                // Append each chunk to the updated messages
                updatedMessages.add("model" to chunk)
                _displayedMessages.value = updatedMessages
            }

            // After the stream ends, check if there are any responses
            if (updatedMessages.isEmpty()) {
                updatedMessages.add("model" to "Unfortunately, input cannot be translated.")
                _displayedMessages.value = updatedMessages
            }

        } catch (e: Exception) {
            // If an error occurs, add an error message to the displayed messages
            updatedMessages.add("model" to "Error: ${e.message}")
            _displayedMessages.value = updatedMessages
        }
    }
}
