package com.example.geminidemo

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import com.example.geminidemo.ui.theme.GeminiChatTheme
import kotlinx.coroutines.launch
import androidx.compose.runtime.collectAsState

class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val mainViewModel by viewModels<MainViewModel>()

        setContent {
            GeminiChatTheme {

                var promptText by remember { mutableStateOf("") }
                val conversations by mainViewModel.displayedMessages.collectAsState(initial = emptyList()) // Using collectAsState with initial value
                val isGenerating by mainViewModel.isGenerating.collectAsState()

                val keyboardController = LocalSoftwareKeyboardController.current
                val context = LocalContext.current

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Scaffold(
                        topBar = {
                            CenterAlignedTopAppBar(
                                title = { Text(text = "Gemini Chat") },
                            )
                        },
                        bottomBar = {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .wrapContentHeight()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                                        .wrapContentHeight(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = promptText,
                                        onValueChange = { promptText = it },
                                        label = { Text(text = "Message") },
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))

                                    FloatingActionButton(
                                        elevation = FloatingActionButtonDefaults.elevation(
                                            defaultElevation = if (isGenerating) 0.dp else 6.dp,
                                            pressedElevation = 0.dp
                                        ),
                                        onClick = {
                                            if (promptText.isNotBlank() && !isGenerating) {
                                                mainViewModel.handleUserInput(promptText) // Updated to handle input with ViewModel logic
                                                promptText = ""
                                                keyboardController?.hide()
                                            } else if (promptText.isBlank()) {
                                                Toast.makeText(
                                                    context,
                                                    "Please enter a message",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                    ) {
                                        AnimatedContent(
                                            targetState = isGenerating,
                                            label = ""
                                        ) { isGenerating ->
                                            if (isGenerating) {
                                                CircularProgressIndicator()
                                            } else {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                                    contentDescription = null
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    ) { contentPadding ->
                        ConversationScreen(
                            conversations = conversations,
                            modifier = Modifier.padding(contentPadding)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ConversationScreen(
    conversations: List<Pair<String, String>>, // Use List of Pair for user/model and text
    modifier: Modifier = Modifier
) {
    val lazyListState = rememberLazyListState()

    LazyColumn(
        state = lazyListState,
        modifier = modifier,
        contentPadding = PaddingValues(vertical = 24.dp, horizontal = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(conversations) { conversation ->
            MessageItem(
                isInComing = conversation.first == "model",  // Model message is "incoming"
                content = conversation.second
            )
        }
    }
}

@Composable
fun MessageItem(
    isInComing: Boolean,
    content: String
) {
    val cardShape by remember {
        derivedStateOf {
            if (isInComing) {
                RoundedCornerShape(16.dp, 16.dp, 16.dp, 0.dp) // Rounded top-left for model messages
            } else {
                RoundedCornerShape(16.dp, 16.dp, 0.dp, 16.dp) // Rounded top-right for user messages
            }
        }
    }

    val cardPadding by remember {
        derivedStateOf {
            if (isInComing) {
                PaddingValues(end = 24.dp) // Padding on the right for model messages (left side)
            } else {
                PaddingValues(start = 24.dp) // Padding on the left for user messages (right side)
            }
        }
    }

    Card(
        modifier = Modifier
            .wrapContentHeight()
            .fillMaxWidth()
            .padding(cardPadding),
        shape = cardShape,
        colors = CardDefaults.cardColors(
            containerColor = if (isInComing) {
                Color(0xFF4CAF50) // Green for model's messages
            } else {
                MaterialTheme.colorScheme.primary // Default primary color for user messages
            }
        )
    ) {
        Row(modifier = Modifier.padding(16.dp)) {
            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.animateContentSize(
                    animationSpec = spring()
                )
            )
        }
    }
}
