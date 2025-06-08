package com.google.mediapipe.examples.llminference

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.QuerySnapshot

object FirebaseListener {

    private fun getFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()
    private var listenerRegistration: ListenerRegistration? = null

    /**
     * Listen for changes in the "queries" collection.
     * For every new or updated document, [onQueryReceived] is invoked with the query data.
     */
    fun startListeningForQueryChanges(onSnapshot: (QuerySnapshot) -> Unit) {
        if (listenerRegistration != null) return   // already listening

        listenerRegistration = getFirestore().collection("queries")
        .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    Log.e("FirebaseListener", "listen error", error)
                    return@addSnapshotListener
                }
                onSnapshot(snapshot)     // 👈 give the whole snapshot back
            }
    }




    fun stopListening() {
        listenerRegistration?.remove()
        listenerRegistration = null
    }
}
