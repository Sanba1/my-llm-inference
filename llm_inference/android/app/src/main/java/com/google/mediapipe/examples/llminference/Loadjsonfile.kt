package com.google.mediapipe.examples.llminference
import android.content.Context
import android.util.Log
import androidx.lifecycle.viewModelScope
import com.google.mediapipe.examples.llminference.ChatViewModel
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.io.File
import android.util.JsonReader
import java.io.InputStreamReader
import com.google.mediapipe.examples.llminference.TextChunk
import com.google.mediapipe.examples.llminference.InferenceModel



// Data classes to hold text and its vector
data class TextChunk(val text: String, val vector: List<Float>)
data class QuestionEmbedding(val question: String, val embedding: List<Float>)

// Function to load vectorized text chunks from JSON


fun loadPdfChunksFromJson(context: Context, jsonFolder: String): List<TextChunk> {
    val textChunks = mutableListOf<TextChunk>()
    val files = context.assets.list(jsonFolder) ?: return textChunks

    for (filename in files) {
        if (filename.startsWith("pdf")) {
            val inputStream = context.assets.open("$jsonFolder/$filename")
            val reader = JsonReader(InputStreamReader(inputStream, "UTF-8"))

            reader.beginArray()
            while (reader.hasNext()) {
                var text: String? = null
                var vector: List<Float> = emptyList()

                reader.beginObject()
                while (reader.hasNext()) {
                    when (reader.nextName()) {
                        "text" -> text = reader.nextString()
                        "vector" -> {
                            val vectorList = mutableListOf<Float>()
                            reader.beginArray()
                            while (reader.hasNext()) {
                                vectorList.add(reader.nextDouble().toFloat())
                            }
                            reader.endArray()
                            vector = vectorList
                        }
                        else -> reader.skipValue()
                    }
                }
                reader.endObject()

                if (text != null) {
                    textChunks.add(TextChunk(text, vector))
                }
            }
            reader.endArray()
            reader.close()
        }
    }

    return textChunks
}


// Function to load questions with embeddings from JSON
fun loadQuestionFromJson(context: Context, jsonFolder: String): QuestionEmbedding? {
    val files = context.assets.list(jsonFolder) ?: return null

    for (filename in files) {
        if (filename.startsWith("question")) {  // Assuming filenames start with 'question' for questions
            val inputStream: InputStream = context.assets.open("$jsonFolder/$filename")
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            val jsonObj = JSONObject(jsonString)
            val question = jsonObj.getString("question")
            val embedding = jsonObj.getJSONArray("embedding").let { array ->
                List(array.length()) { array.getDouble(it).toFloat() }
            }
            println(" Loaded question with vector size: ${embedding.size}")
            return QuestionEmbedding(question, embedding)
        }
    }
    return null
}
fun loadQuestionFromStorage(context: Context, filePath: String): QuestionEmbedding? {
    return try {
        val file = File(filePath)
        val jsonString = file.bufferedReader().use { it.readText() }
        val jsonObj = JSONObject(jsonString)
        val question = jsonObj.getString("question")
        val embedding = jsonObj.getJSONArray("embedding").let { array ->
            List(array.length()) { array.getDouble(it).toFloat() }
        }
        println("Loaded question from storage with vector size: ${embedding.size}")
        QuestionEmbedding(question, embedding)
    } catch (e: Exception) {
        println(" Error reading question from storage: ${e.message}")
        null
    }
}
// Function to calculate cosine similarity between two vectors
fun cosineSimilarity(vec1: List<Float>, vec2: List<Float>): Float {
    var dotProduct = 0.0f
    var norm1 = 0.0f
    var norm2 = 0.0f

    for (i in vec1.indices) {
        dotProduct += vec1[i] * vec2[i]
        norm1 += vec1[i] * vec1[i]
        norm2 += vec2[i] * vec2[i]
    }

    norm1 = kotlin.math.sqrt(norm1)
    norm2 = kotlin.math.sqrt(norm2)
    return dotProduct / (norm1 * norm2)
}

// Function to retrieve relevant passages based on a question vector
fun retrieveRelevantPassages(
    questionVector: List<Float>,
    storedChunks: List<TextChunk>,
    topK: Int = 3
): List<String> {
    val scoredChunks = storedChunks.map { chunk ->
        val score = cosineSimilarity(questionVector, chunk.vector)
        println(" Chunk preview: \"${chunk.text.take(40)}...\" → Similarity score: $score")
        chunk.text to score
    }

    val topChunks = scoredChunks
        .filter { it.second > 0.7f } //  Only keep chunks highly relevant to the question
        .sortedByDescending { it.second }
        .take(topK)


    println(" Top $topK chunks selected:")
    topChunks.forEachIndexed { index, (text, score) ->
        println("  ${index + 1}: \"${text.take(50)}...\" → $score")
    }

    return topChunks.map { it.first }
}

// Function to format the prompt for displaying or further processing
fun formatPrompt(retrievedPassages: List<String>, question: String): String {
    val context = retrievedPassages.joinToString("\n") { "- $it" }

    return """
        ||### Question
        |$question
        
        |### Task
        |Using **only** the information in the *Context* section, write a concise answer to the *Question*.
        |
        |### Rules
        |1. **Do not** add facts or details that are not explicitly present in the context.  
        |2. If, after carefully reading the context, you still cannot answer, reply with  
        |   “I’m sorry, the provided context does not include that information.”  
        |3. Write your answer as one or more coherent **paragraphs**.  
        |4. **Do not** use bullet points, numbered lists, or tables.  
        |
            
        |
        |### Context
        |$context
        |
        |### Answer
        |""".trimMargin()
}



suspend fun retrieveAndFormatResponseFromRoom(
    context: Context,
    questionEmbedding: QuestionEmbedding?,
    jsonFolder: String
): Pair<String, Long> {
    val startSearch = System.currentTimeMillis()

    // Log memory before retrieval
    InferenceModel.logMemoryUsage("Before retrieval")

    val pdfChunks = loadPdfChunksFromJson(context, jsonFolder)

    val formattedPrompt: String
    if (questionEmbedding != null) {
        val relevantPassages = retrieveRelevantPassages(questionEmbedding.embedding, pdfChunks, 5)
        formattedPrompt = formatPrompt(relevantPassages, questionEmbedding.question)
    } else {
        formattedPrompt = "No question found or no data available."
    }

    val endSearch = System.currentTimeMillis()
    // Optionally log memory after retrieval
    InferenceModel.logMemoryUsage("After retrieval")
    val searchDuration = endSearch - startSearch

    return Pair(formattedPrompt, searchDuration)
}




fun runRAGWithTopKFromRoom(
    context: Context,
    viewModel: ChatViewModel,
    jsonFolder: String,
    uploadAnswer: (String, String) -> Unit
) {
    viewModel.viewModelScope.launch {
        val chunks = ChunkRepository.getAllChunks()
        val latestQuestionChunk = chunks.lastOrNull()
        if (latestQuestionChunk != null) {
            val (formattedPrompt, searchDuration) = retrieveAndFormatResponseFromRoom(
                context,
                QuestionEmbedding(latestQuestionChunk.text, latestQuestionChunk.embedding.toList()),
                jsonFolder
            )

            val startLLM = System.currentTimeMillis()

            viewModel.sendMessage(formattedPrompt) { finalResponse ->
                val endLLM = System.currentTimeMillis()
                val llmDuration = endLLM - startLLM

                Log.d("Timing", " Vector search duration: ${searchDuration}ms")
                Log.d("Timing", " LLM response duration: ${llmDuration}ms")

                if (!finalResponse.isNullOrBlank()) {
                    uploadAnswer(latestQuestionChunk.chunkId, finalResponse)
                }
            }
        } else {
            viewModel.sendMessage(" No questions found in database.")
        }
    }
}


