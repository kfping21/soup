package com.zjgsu.soup.game

data class GameHistory(
    val id: Int,
    val userId: String,
    val questionId: Int,
    val questionTitle: String,
    val difficulty: String,
    val completionTime: String
)