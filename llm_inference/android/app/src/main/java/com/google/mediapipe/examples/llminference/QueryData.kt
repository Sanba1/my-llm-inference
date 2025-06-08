package com.google.mediapipe.examples.llminference

data class QueryData(
    val id: String? = null,
    val question: String = "",
    val embedding: List<Float> = emptyList()
)

