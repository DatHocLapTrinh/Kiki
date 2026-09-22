package com.example.model

data class QAItem(
    val id: Int,
    val question: String,
    val imageUri: String? = null,
    val answer: String,
    val timestamp: Long = System.currentTimeMillis()
)
