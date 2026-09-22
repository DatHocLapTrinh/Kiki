package com.example.model

data class QuestItem(
    val question: String,
    val options: List<String>,
    val correctIndex: Int,
    var selectedIndex: Int = -1
)
