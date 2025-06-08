package com.google.mediapipe.examples.llminference

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession.LlmInferenceSessionOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.io.File
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

class ModelLoadFailException :
    Exception("Failed to load model, please try again")

class InferenceModel private constructor(context: Context) {
    private var llmInference: LlmInference
    private var llmInferenceSession: LlmInferenceSession
    private val TAG = InferenceModel::class.qualifiedName

    private val _partialResults = MutableSharedFlow<Pair<String, Boolean>>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val partialResults: SharedFlow<Pair<String, Boolean>> = _partialResults.asSharedFlow()
    val uiState: UiState
    private val sessionOptions =  LlmInferenceSessionOptions.builder()
        .setTemperature(model.temperature)
        .setTopK(model.topK)
        .setTopP(model.topP)
        .build()
    init {
        if (!modelExists(context)) {
            throw IllegalArgumentException("Model not found at path: ${modelPath(context)}")
        }

        val inferenceOptions = LlmInference.LlmInferenceOptions.builder()
            .setModelPath(modelPath(context))
            .setMaxTokens(1024)
            .setResultListener { partialResult, done ->
                _partialResults.tryEmit(partialResult to done)
            }
            .build()



        uiState = model.uiState
        try {
            llmInference = LlmInference.createFromOptions(context, inferenceOptions)
            llmInferenceSession =
                LlmInferenceSession.createFromOptions(llmInference, sessionOptions)
        } catch (e: Exception) {
            Log.e(TAG, "Load model error: ${e.message}", e)
            throw ModelLoadFailException()
        }
    }

    fun generateResponseAsync(prompt: String) {
        val formattedPrompt = model.uiState.formatPrompt(prompt)
        isModelBusy = true  // mark busy
        llmInferenceSession.addQueryChunk(formattedPrompt)
        llmInferenceSession.generateResponseAsync()
    }


    fun close() {
        llmInferenceSession.close()
        llmInference.close()
    }
    fun resetSession() {
        llmInferenceSession.close()
        llmInferenceSession = LlmInferenceSession.createFromOptions(llmInference, sessionOptions)
    }
    fun clearSessionQuery() {
        try {
            // No clearQuery() method available, so simulate by resetting session
            resetSession()
        } catch (e: Exception) {
            Log.e("InferenceModel", "Session clear (via reset) failed: ${e.message}")
        }
    }

    companion object {
        var model: Model = Model.GEMMA_CPU
        private var instance: InferenceModel? = null
        private var isModelBusy = false


        fun getInstance(context: Context): InferenceModel {
            return if (instance != null) {
                instance!!
            } else {
                InferenceModel(context).also { instance = it }
            }
        }

        fun resetInstance(context: Context): InferenceModel {
            return InferenceModel(context).also { instance = it }
        }

        fun modelPathFromUrl(context: Context): String {
            if (model.url.isNotEmpty()) {
                val urlFileName = Uri.parse(model.url).lastPathSegment
                if (!urlFileName.isNullOrEmpty()) {
                    return File(context.filesDir, urlFileName).absolutePath
                }
            }

            return ""
        }

        fun modelPath(context: Context): String {
            return model.getLocalPath(context)
        }
        /*
        private var questionCount = 0

        fun maybeResetModel(context: Context, every: Int = 7) {
            questionCount++
            Log.d("InferenceModel", "maybeResetModel called: questionCount = $questionCount")

            // Delay before reset to let MediaPipe finish
            CoroutineScope(Dispatchers.IO).launch {
                delay(2000L)  // 2s pause before any reset
                if (questionCount % every == 0) {
                    Log.d("InferenceModel", "Resetting full instance after $questionCount questions")
                    resetInstance(context)
                } else {
                    try {
                        getInstance(context).resetSession()
                    } catch (e: Exception) {
                        Log.e("InferenceModel", "Session reset failed, falling back to full reset", e)
                        resetInstance(context)
                    }
                }
            }
        }*/




        fun modelExists(context: Context): Boolean {
            return File(modelPath(context)).exists()
        }
    }
}