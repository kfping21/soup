package com.example.soup2.model

data class GameHistory(
    val id: Int,
    val userId: String,
    val questionId: Int,
    val questionTitle: String,
    val difficulty: String,
    val completionTime: String
)