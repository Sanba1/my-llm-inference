package com.google.mediapipe.examples.llminference

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember

@Composable
internal fun ChatRoute(
    onClose: () -> Unit
) {
    val context = LocalContext.current.applicationContext
    val chatViewModel: ChatViewModel = viewModel(factory = ChatViewModel.getFactory(context))

    val uiState by chatViewModel.uiState.collectAsStateWithLifecycle()
    val textInputEnabled by chatViewModel.isTextInputEnabled.collectAsStateWithLifecycle()

    // Automatic test trigger
    ///LaunchedEffect(Unit) {
    ///    val testQuestion = "What are the effects of climate change?"
    ///    val testContext = listOf(
    ///        "Climate change affects weather patterns and leads to more extreme events.",
    ///        "It is primarily caused by greenhouse gas emissions.",
    //        "The Arctic is warming twice as fast as the rest of the world."
    //    )
    //   chatViewModel.runAutomaticTest(testQuestion, testContext)
   // }

    ChatScreen(
        context,
        uiState,
        textInputEnabled,
        onSendMessage = { message ->
            chatViewModel.sendMessage(message)
        },
        onClose = onClose
    )
}

@Composable
fun ChatScreen(
    context: Context,
    uiState: UiState,
    textInputEnabled: Boolean,
    onSendMessage: (String) -> Unit,
    onClose: () -> Unit
) {
    var userMessage by rememberSaveable { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.Bottom
    ) {
        // Top bar with close button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = InferenceModel.model.toString(),
                style = MaterialTheme.typography.titleSmall
            )
            IconButton(onClick = {
                InferenceModel.getInstance(context).close()
                uiState.clearMessages()
                onClose()
            }) {
                Icon(Icons.Default.Close, contentDescription = "Close Chat")
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            reverseLayout = true
        ) {
            items(uiState.messages) { chat ->
                ChatItem(chat)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Column { }

            Spacer(modifier = Modifier.width(8.dp))

            TextField(
                value = userMessage,
                onValueChange = { userMessage = it },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                ),
                label = {
                    Text(stringResource(R.string.chat_label))
                },
                modifier = Modifier
                    .weight(0.85f),
                enabled = textInputEnabled
            )

            IconButton(
                onClick = {
                    if (userMessage.isNotBlank()) {
                        onSendMessage(userMessage)
                        userMessage = ""
                    }
                },
                modifier = Modifier
                    .padding(start = 16.dp)
                    .align(Alignment.CenterVertically)
                    .fillMaxWidth()
                    .weight(0.15f),
                enabled = textInputEnabled
            ) {
                Icon(
                    Icons.AutoMirrored.Default.Send,
                    contentDescription = stringResource(R.string.action_send),
                    modifier = Modifier
                )
            }
        }
    }
}

@Composable
fun ChatItem(
    chatMessage: ChatMessage
) {
    val backgroundColor = if (chatMessage.isFromUser) {
        MaterialTheme.colorScheme.tertiaryContainer
    } else if (chatMessage.isThinking) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.secondaryContainer
    }

    val bubbleShape = if (chatMessage.isFromUser) {
        RoundedCornerShape(20.dp, 4.dp, 20.dp, 20.dp)
    } else {
        RoundedCornerShape(4.dp, 20.dp, 20.dp, 20.dp)
    }

    val horizontalAlignment = if (chatMessage.isFromUser) {
        Alignment.End
    } else {
        Alignment.Start
    }

    Column(
        horizontalAlignment = horizontalAlignment,
        modifier = Modifier
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .fillMaxWidth()
    ) {
        val author = if (chatMessage.isFromUser) {
            stringResource(R.string.user_label)
        } else if (chatMessage.isThinking) {
            stringResource(R.string.thinking_label)
        } else {
            stringResource(R.string.model_label)
        }
        Text(
            text = author,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Row {
            BoxWithConstraints {
                Card(
                    colors = CardDefaults.cardColors(containerColor = backgroundColor),
                    shape = bubbleShape,
                    modifier = Modifier.widthIn(0.dp, maxWidth * 0.9f)
                ) {
                    if (chatMessage.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(16.dp)
                        )
                    } else {
                        Text(
                            text = chatMessage.message,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        }
    }
}
