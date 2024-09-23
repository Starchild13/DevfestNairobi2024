package com.example.geminidemo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.asTextOrNull
import com.google.ai.client.generativeai.type.generationConfig
import com.google.mlkit.nl.languageid.LanguageIdentification
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class MainViewModel : ViewModel() {

    private val _displayedMessages = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val displayedMessages: StateFlow<List<Pair<String, String>>> = _displayedMessages

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating

    private var greetingShown = false // Track whether the greeting has been shown

    // Initialize the Gemini model with configuration
    private val model = GenerativeModel(
        "gemini-1.0-pro",
        BuildConfig.API_KEY, // API key from BuildConfig
        generationConfig = generationConfig {
            temperature = 0.9f
            topP = 1f
            maxOutputTokens = 2048
            responseMimeType = "text/plain"
        }
    )

    // Initialize ML Kit Language Identification
    private val languageIdentifier = LanguageIdentification.getClient()

    // Internal predefined prompts and responses
    private val internalChatPrompts = mapOf(
        "hello" to "My name is Jess, your friendly multilingual assistant. Feel free to converse with me.",
        "hi" to "My name is Jess, your friendly multilingual assistant. Feel free to converse with me.",
        "habari yako" to "Good, thank you! How can I assist you?",
        "nataka kuenda Kisumu" to "Where would you like to go in Kisumu?",
        "niongeleshe na Kiswahili" to "No problem, we will converse in Swahili."
    )

    // Function to handle user input and respond appropriately
    fun handleUserInput(input: String) {
        viewModelScope.launch {
            _isGenerating.value = true

            val updatedMessages = _displayedMessages.value.toMutableList()
            updatedMessages.add("user" to input)
            _displayedMessages.value = updatedMessages

            // Step 1: Check for predefined responses first (immediate response if found)
            val predefinedResponse = getPredefinedResponse(input)
            if (predefinedResponse != null) {
                // Handle greeting logic to prevent repeating it
                if (predefinedResponse.contains("My name is Jess") && greetingShown) {
                    _isGenerating.value = false
                    return@launch
                } else if (predefinedResponse.contains("My name is Jess")) {
                    greetingShown = true
                }

                // Add predefined response instantly without delay
                updatedMessages.add("model" to predefinedResponse)
                _displayedMessages.value = updatedMessages
                _isGenerating.value = false
                return@launch
            }

            // Step 2: If no predefined response is found, proceed with language detection and translation (background processing)
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    // Detect the language of the input
                    val detectedLanguage = detectLanguage(input)

                    // Generate response based on detected language
                    val modelResponse = generateResponseWithLanguageRules(input, detectedLanguage)
                    viewModelScope.launch(Dispatchers.Main) {
                        updatedMessages.add("model" to modelResponse)
                        _displayedMessages.value = updatedMessages
                    }
                } catch (e: Exception) {
                    viewModelScope.launch(Dispatchers.Main) {
                        updatedMessages.add("model" to "Error: ${e.message}")
                        _displayedMessages.value = updatedMessages
                    }
                } finally {
                    _isGenerating.value = false
                }
            }
        }
    }

    // Function to check predefined responses based on input
    private fun getPredefinedResponse(input: String): String? {
        return internalChatPrompts[input.lowercase()]
    }

    // Function to generate response with language rules
    private suspend fun generateResponseWithLanguageRules(input: String, detectedLanguage: String): String {
        return when (detectedLanguage) {
            "fr", "sw", "pt" -> translateInputToEnglish(input) // Translate non-English languages to English
            else -> input // If it's already English, return the input as is
        }
    }

    // Function to detect the language using ML Kit
    private suspend fun detectLanguage(input: String): String {
        return suspendCancellableCoroutine { continuation ->
            languageIdentifier.identifyLanguage(input)
                .addOnSuccessListener { languageCode ->
                    continuation.resume(languageCode ?: "unknown")
                }
                .addOnFailureListener { e ->
                    continuation.resumeWithException(e)
                }
        }
    }

    // Function to translate input to English using the Gemini model
    private suspend fun translateInputToEnglish(input: String): String {
        return try {
            val response = model.startChat().sendMessage("Translate to English: $input")

            // Get the translated text from the model
            val translatedText = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.asTextOrNull()

            translatedText ?: "Error translating text"
        } catch (e: Exception) {
            "Error translating text: ${e.message}"
        }
    }
}
