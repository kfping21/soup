package com.zjgsu.soup

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiService {
    @GET("todos")
    suspend fun getQuestionsByDifficulty(
        @Query("difficulty") difficulty: String
    ): Response<List<GameData>>
}