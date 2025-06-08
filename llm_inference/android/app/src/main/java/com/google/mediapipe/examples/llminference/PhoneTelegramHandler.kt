/*package com.google.mediapipe.examples.llminference

import org.json.JSONObject
import android.util.Log

/**
 * We define an 'object' so we can reference
 * PhoneTelegramHandler.handleChunkAssignment(...) from MyPhoneBot.
 */
object PhoneTelegramHandler {

    fun handleChunkAssignment(jsonString: String) {
        try {
            val jsonObj = JSONObject(jsonString)
            val pdfId = jsonObj.optString("pdfId", "unknown_pdf")
            val chunksArray = jsonObj.optJSONArray("chunks") ?: return

            for (i in 0 until chunksArray.length()) {
                val chunkObj = chunksArray.getJSONObject(i)
                val chunkId = chunkObj.getString("chunk_id")
                val text = chunkObj.getString("text")
                val embeddingJson = chunkObj.getJSONArray("embedding")

                val embedding = FloatArray(embeddingJson.length()) { idx ->
                    embeddingJson.getDouble(idx).toFloat()
                }

                val chunkData = ChunkData(chunkId, text, embedding)
                ChunkRepository.addChunk(chunkData)
            }

            // Print how many total chunks we have now
            val totalChunks = ChunkRepository.getAllChunks().size
            Log.d("PhoneTelegramHandler", "handleChunkAssignment => now have $totalChunks total chunks stored.")

        } catch (e: Exception) {
            Log.e("PhoneTelegramHandler", "Error parsing chunk assignment: ${e.message}")
        }
    }

    fun handleQueryMessage(jsonString: String): String {
        // In a real scenario, parse the JSON, do local retrieval, run LLM, etc.
        // Here, we just return a dummy partial summary JSON for demonstration.
        return """{
          "queryId": "dummyQueryId",
          "phoneName": "phone_1",
          "partialSummary": "Simulated partial summary"
        }"""
    }
}*/