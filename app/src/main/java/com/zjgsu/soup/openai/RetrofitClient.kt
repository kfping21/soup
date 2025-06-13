package com.zjgsu.soup.openai

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private const val BASE_URL = "https://api.bianxieai.com/"
    private const val API_KEY = "sk-zDGaUFHiqKbXjtx9aqCVWWnMnoOlV6O0UhDsP8radjgO0MJJ" // 替换为你的API密钥

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("Authorization", getAuthHeader())
                .addHeader("Content-Type", "application/json")
                .build()
            chain.proceed(request)
        }
        .addInterceptor(loggingInterceptor)
        .connectTimeout(60, TimeUnit.SECONDS)  // 增加连接超时设置
        .readTimeout(60, TimeUnit.SECONDS)     // 增加读取超时设置
        .writeTimeout(60, TimeUnit.SECONDS)    // 增加写入超时设置
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val aiService: AIService = retrofit.create(AIService::class.java)

    fun getAuthHeader(): String = "Bearer $API_KEY"
}