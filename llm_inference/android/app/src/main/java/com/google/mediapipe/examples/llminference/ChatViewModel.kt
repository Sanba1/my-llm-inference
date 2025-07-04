package com.google.mediapipe.examples.llminference

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectIndexed
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect



class ChatViewModel(
    private val inferenceModel: InferenceModel
) : ViewModel() {

    private val _uiState: MutableStateFlow<UiState> = MutableStateFlow(inferenceModel.uiState)
    val uiState: StateFlow<UiState> =
        _uiState.asStateFlow()

    private val _textInputEnabled: MutableStateFlow<Boolean> =
        MutableStateFlow(true)
    val isTextInputEnabled: StateFlow<Boolean> =
        _textInputEnabled.asStateFlow()

    private var inferenceJob: Job? = null

    fun sendMessage(userMessage: String, onResponseDone: ((String?) -> Unit)? = null) {
        // Cancel any previous inference
        inferenceJob?.cancel()

        inferenceJob = viewModelScope.launch(Dispatchers.IO) {
            _uiState.value.addMessage(userMessage, USER_PREFIX)
            var currentMessageId: String? = _uiState.value.createLoadingMessage()
            setInputEnabled(false)

            try {
                inferenceModel.generateResponseAsync(userMessage)
                var fullResponse = ""

                withTimeout(30000L) {
                    inferenceModel.partialResults
                        .collect { (partialResult, done) ->
                            currentMessageId?.let {
                                currentMessageId = _uiState.value.appendMessage(it, partialResult, done)
                                fullResponse += partialResult
                            }

                            if (done) {
                                currentMessageId = null
                                setInputEnabled(true)
                                onResponseDone?.invoke(fullResponse)

                                InferenceModel.logMemoryUsage("After inference")
                                //  Mark the model as no longer busy
                                inferenceModel.clearSessionQuery()

                                cancel()// safely exit collection
                            }
                        }
                }

            } catch (e: TimeoutCancellationException) {
                _uiState.value.addMessage(" Model timed out after 30 seconds.", MODEL_PREFIX)
                setInputEnabled(true)
                onResponseDone?.invoke("No answer — model timed out.")
            } catch (e: Exception) {
                _uiState.value.addMessage(e.localizedMessage ?: "Unknown error occurred", MODEL_PREFIX)
                setInputEnabled(true)
            }
        }
    }




    private fun setInputEnabled(isEnabled: Boolean) {
        _textInputEnabled.value = isEnabled
    }

    fun getLatestModelMessage(): String? {
        return _uiState.value.messages.lastOrNull { it.author == MODEL_PREFIX }?.rawMessage
    }



    companion object {
        fun getFactory(context: Context) = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val inferenceModel = InferenceModel.getInstance(context)
                return ChatViewModel(inferenceModel) as T
            }
        }
    }
}
