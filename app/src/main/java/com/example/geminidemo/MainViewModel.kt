package com.example.geminidemo

import android.graphics.Bitmap
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.StampedPathEffectStyle.Companion.Translate
import androidx.lifecycle.ViewModel
import com.google.cloud.translate.Translate
import com.google.cloud.translate.TranslateOptions
import com.google.cloud.translate.Translate.TranslateOption
import com.google.cloud.translate.Translate.TranslateOption.*


class MainViewModel : ViewModel() {

    private val translates = TranslateOptions.newBuilder()
        .setApiKey(BuildConfig.TRANSLATE_API_KEY)
        .build()
        .service
    private val translate: Translate = TranslateOptions.getDefaultInstance().service

    var conversations = mutableStateListOf<Triple<String, String, List<Bitmap>?>>()
        private set

    var isGenerating = mutableStateOf(false)
        private set

    fun sendText(promptText: String, images: List<Bitmap>) {
        isGenerating.value = true

        // Translate the text to Portuguese
        val translatedText = translateTextToPortuguese(promptText)

        // Simulate sending and receiving messages
        conversations.add(Triple("sent", promptText, images))
        conversations.add(Triple("received", translatedText, images))

        isGenerating.value = false
    }

    private fun translateTextToPortuguese(text: String): String {
        val translation = translates.translate(
            text,
            targetLanguage("pt")
        )
        return translation.translatedText
    }
}



