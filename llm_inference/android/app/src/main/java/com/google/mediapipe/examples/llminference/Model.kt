package com.google.mediapipe.examples.llminference

import android.content.Context
import java.io.File

// NB: Make sure the filename is *unique* per model you use!
// Weight caching is currently based on filename alone.
enum class Model(
    val assetFilename: String,
    val url: String,
    val licenseUrl: String,
    val needsAuth: Boolean,
    val uiState: UiState,
    val temperature: Float,
    val topK: Int,
    val topP: Float,
) {
    GEMMA_CPU(
        assetFilename = "llm/gemma-1.1-2b-it-cpu-int4.bin", // must match assets folder path
        url = "https://huggingface.co/...",
        licenseUrl = "...",
        needsAuth = true,
        uiState = GemmaUiState(),
        temperature = 0.8f,
        topK = 40,
        topP = 1.0f
    );

    //  Function inside the enum class
    fun getLocalPath(context: Context): String {
        val outFile = File(context.filesDir, assetFilename.substringAfterLast("/"))

        if (!outFile.exists()) {
            context.assets.open(assetFilename).use { input ->
                outFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }

        return outFile.absolutePath
    }
}
