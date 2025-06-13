package com.zjgsu.soup.openai

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface AIService {
    @POST("v1/chat/completions")
    suspend fun chatCompletion(
       @Body request: ChatRequest
    ): Response<ChatResponse>
}

data class ChatRequest(
    val model: String = "deepseek-chat",
    val messages: List<AIMessage>,
    val temperature: Double = 0.7
)

data class AIMessage(
    val role: String, // "user" or "assistant"
    val content: String
)

data class ChatResponse(
    val choices: List<Choice>
) {
    data class Choice(
        val message: AIMessage
    )
}