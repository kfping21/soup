package com.zjgsu.soup.game

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.OkHttpClient
import retrofit2.http.Body
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

interface ApiService {
    @GET("todos")
    suspend fun getQuestionsByDifficulty(
        @Query("difficulty") difficulty: String
    ): Response<List<GameData>>
    
    // 获取所有历史记录
    @GET("history")
    suspend fun getAllHistoryRecords(): Response<List<GameHistory>>
    
    // 获取特定用户的历史记录
    @GET("history")
    suspend fun getHistoryRecordsByUserId(
        @Query("userId") userId: String
    ): Response<List<GameHistory>>
    
    // 添加一条新的历史记录
    @POST("history")
    suspend fun addHistoryRecord(
        @Body gameHistory: GameHistory
    ): Response<GameHistory>

    companion object {
        private const val BASE_URL = "http://10.0.2.2:3000/"
        
        fun create(): ApiService {
            val client = OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS)
                .writeTimeout(5, TimeUnit.SECONDS)
                .build()
                
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ApiService::class.java)
        }
    }
}