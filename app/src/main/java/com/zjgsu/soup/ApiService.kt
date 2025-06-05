package com.zjgsu.soup

import retrofit2.Response
import retrofit2.http.GET

interface ApiService {
    @GET("todos")
    suspend fun getGameData(): Response<List<GameData>>
}