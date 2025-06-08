package com.google.mediapipe.examples.llminference

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore

fun uploadAnswerWithAutoID(answer: String) {
    val db = FirebaseFirestore.getInstance()
    val counterRef = db.collection("trial").document("counter")

    var uploadedDocId = ""  // Track the docId outside

    db.runTransaction { transaction ->
        val snapshot = transaction.get(counterRef)
        val currentCount = snapshot.getLong("responseCount") ?: 0L
        val newCount = currentCount + 1

        transaction.update(counterRef, "responseCount", newCount)

        val docId = "phone_$newCount"
        uploadedDocId = docId  // Save for logging
        val data = mapOf(
            "answer" to answer
        )
        val responseRef = db.collection("responses").document(docId)
        transaction.set(responseRef, data)

        null
    }.addOnSuccessListener {
        Log.d("FirebaseUpload", " Answer uploaded safely with ID $uploadedDocId")
    }.addOnFailureListener { e ->
        Log.e("FirebaseUpload", " Upload failed: $e")
    }
}


