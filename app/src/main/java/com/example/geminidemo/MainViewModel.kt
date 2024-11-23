package com.example.geminidemo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

class Chat(private val model: GenerativeModel) {

    // Function to send a message and stream the response in chunks
    fun sendMessageWithStreaming(input: String): Flow<String> {
        return flow {
            // Send the input to the Gemini model and stream the response
            val chatHistory = listOf(
                content("user") {
                    text(
                        """
                Human:
                
                You are Jess, a friendly multilingual assistant. To greet the user, use exactly the text in the example below. Do not be creative. 
                ALWAYS START WITH ENGLISH. If the user asks to use Swahili, switch to Swahili. If the user says hello or hi or any other greeting respond with exactly the text in the example below.
                
                <example>
                  My name is Jess, your friendly multilingual assistant. Feel free to ask me any question.
                </example>
                
                ALL YOUR RESPONSES SHOULD BE DIRECT STYLE AND CONCISE. YOUR RESPONSE SHOULD NOT BE MORE THAN SIX SENTENCES LONG. ALL YOUR SENTENCES SHOULD NOT BE MORE THAN TEN WORDS.
                
                Your function is to:
                - Start responding in the user's language unless it is English.
                - If the user starts in English, respond in Swahili.
                - Continue in the language the user starts with if it is French or Portuguese.
                - If mixed languages are detected, switch to Swahili.
                - Default to Swahili for unsupported languages.
                
                User: $input
                """
                    )
                },
                content("model") { text("My name is Jess, your friendly multilingual assistant. Feel free to ask me any question.") }
            )
            val chat = model.startChat(chatHistory)

            // Simulate streaming response from the model
            val response = chat.sendMessage(input)
            response.text?.let { emit(it) } // You can adjust how you handle this based on model's actual
            // response

            // If you want to simulate streaming, split the response into parts
//            val responseChunks =
//                response.text?.chunked(100) // Example: Chunk response into 100-character parts
//            if (responseChunks != null) {
//                for (chunk in responseChunks) {
//                    emit(chunk)
//                    kotlinx.coroutines.delay(500) // Simulate delay
//                }
//
//            }
        }

    }
}


class MainViewModel : ViewModel() {

    private val _displayedMessages = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val displayedMessages: StateFlow<List<Pair<String, String>>> = _displayedMessages

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating

    private var greetingShown = false // Track whether the greeting has been shown

    // Initialize the Gemini model with configuration
    private val model = GenerativeModel(
        "gemini-1.0-pro",
        apiKey = "",
//        BuildConfig.API_KEY, // API key from BuildConfig
        generationConfig = generationConfig {
            temperature = 0.9f
            topP = 1f
            maxOutputTokens = 2048
            responseMimeType = "text/plain"
        }
    )

    private val chat = Chat(model) // Chat instance for streaming responses

    // Function to handle user input and respond appropriately
    fun handleUserInput(input: String) {
        viewModelScope.launch {
            _isGenerating.value = true

            val updatedMessages = _displayedMessages.value.toMutableList()
            updatedMessages.add("user" to input)

            // Step 1: Generate a response for the input using the model
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

    // Function to generate response using GDispatchers.IOemini with streaming enabled
    private suspend fun generateResponseWithGemini(
        input: String,
        updatedMessages: MutableList<Pair<String, String>>
    ) {
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
                updatedMessages.add("model" to "Unfortunately, input cannot be processed.")
                _displayedMessages.value = updatedMessages
            }

        } catch (e: Exception) {
            // If an error occurs, add an error message to the displayed messages
            updatedMessages.add("model" to "Error: ${e.message}")
            _displayedMessages.value = updatedMessages
        }
    }
}
