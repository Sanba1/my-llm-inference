package com.google.mediapipe.examples.llminference

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import java.util.UUID
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.mediapipe.examples.llminference.ChunkRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.Job

class QueryViewModel(
    application: Application,
    private val chatViewModel: ChatViewModel
) : AndroidViewModel(application) {

    init {
        // 🔄  NEW: listen for document‑level changes
        FirebaseListener.startListeningForQueryChanges { snapshot ->
            viewModelScope.launch {
                // iterate over ONLY the changes in this snapshot
                for (change in snapshot.getDocumentChanges()) {   // ← no KTX needed
                    if (change.getType() == DocumentChange.Type.ADDED) {
                        val q = change.getDocument().toObject(QueryData::class.java)
                        runQueryOnce(q.copy(id = change.getDocument().getId()))
                    }
                }

            }
        }
    }

    private suspend fun runQueryOnce(q: QueryData) {
        try {
            Log.d("QueryViewModel", "Processing new query: ${q.question.take(30)}...")

            val chunk = ChunkData(
                chunkId = q.id ?: UUID.randomUUID().toString(),
                text = q.question,
                embedding = q.embedding.toFloatArray()
            )
            ChunkRepository.addChunk(chunk)

            runRAGWithTopKFromRoom(
                getApplication(),
                chatViewModel,
                "json"
            ) { _, answer ->
                uploadAnswerWithAutoID(answer)

                viewModelScope.launch {
                    try {
                        ChunkRepository.clearAll()
                        delay(3000) // give MediaPipe time to clean up
                        //InferenceModel.resetInstance(getApplication())
                        Log.d("QueryViewModel", "Model reset complete")
                    } catch (e: Exception) {
                        Log.e("QueryViewModel", "Error during model reset: ${e.message}", e)
                    }
                }

                val docId = q.id
                if (docId != null) {
                    try {
                        FirebaseFirestore.getInstance()
                            .collection("queries")
                            .document(docId)
                            .delete()
                            .addOnSuccessListener {
                                Log.d("QueryViewModel", "Deleted Firestore doc: $docId")
                            }
                            .addOnFailureListener { e ->
                                Log.e("QueryViewModel", "Failed to delete Firestore doc: ${e.message}", e)
                            }
                    } catch (e: Exception) {
                        Log.e("QueryViewModel", "Exception while deleting Firestore doc: ${e.message}", e)
                    }
                } else {
                    Log.e("QueryViewModel", "QueryData.id is null — not deleting")
                }
            }

        } catch (e: Exception) {
            Log.e("QueryViewModel", "runQueryOnce failed: ${e.message}", e)
        }
    }


    private fun generateUniqueId(): String = UUID.randomUUID().toString()

    override fun onCleared() {
        super.onCleared()
        FirebaseListener.stopListening()
    }
}

