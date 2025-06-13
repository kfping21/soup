package com.zjgsu.soup.openai

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.zjgsu.soup.game.ChatMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

class AIChatManager(private val gameRules: String = "") {
    private val _messages = MutableLiveData<List<ChatMessage>>()
    val messages: LiveData<List<ChatMessage>> = _messages

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val conversationHistory = mutableListOf<AIMessage>().apply {
        // 初始化系统消息（游戏规则）
        if (gameRules.isNotEmpty()) {
            add(AIMessage("system", gameRules))
        }
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()

    fun sendMessage(message: String, context: String = "", isInitialMessage: Boolean = false) {
        if (message.isBlank() || _isLoading.value == true) return

        // 添加用户消息
        val isEvaluation = message.startsWith("[评估答案]")
        val content = if (isEvaluation) message else "$context\n$message"
        val userMessage = ChatMessage(message, true)
        updateMessages(userMessage)
        conversationHistory.add(AIMessage("user", content))

        CoroutineScope(Dispatchers.IO).launch {
            try {
                _isLoading.postValue(true)
                _error.postValue(null)

                val response = RetrofitClient.aiService.chatCompletion(
                    request = ChatRequest(
                        model = "deepseek-chat",
                        messages = conversationHistory,
                        temperature = if (isEvaluation) 0.3 else 0.7 // 评估时降低随机性
                    )
                )

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        response.body()?.choices?.firstOrNull()?.message?.let { aiMessage ->
                            // 校验AI回答是否合法（可根据需要修改校验逻辑）
                            val isValidResponse = when {
                                isEvaluation -> true // 评估模式不校验
                                else -> aiMessage.content.isNotBlank() // 基础校验
                            }

                            if (isValidResponse) {
                                conversationHistory.add(aiMessage)
                                updateMessages(ChatMessage(aiMessage.content, false))
                            } else {
                                _error.postValue("AI回答不符合预期格式")
                            }
                        } ?: run {
                            _error.postValue("AI返回空响应")
                        }
                    } else {
                        val errorBody = response.errorBody()?.string() ?: "未知错误"
                        _error.postValue("请求失败: ${response.code()} - $errorBody")
                    }
                }
            } catch (e: Exception) {
                _error.postValue("网络错误: ${e.message}")
                e.printStackTrace()
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    fun sendSilentMessage(message: String) {
        if (message.isBlank() || _isLoading.value == true) return

        // 添加到对话历史但不显示在UI
        conversationHistory.add(AIMessage("user", message))

        CoroutineScope(Dispatchers.IO).launch {
            try {
                _isLoading.postValue(true)
                _error.postValue(null)

                val response = RetrofitClient.aiService.chatCompletion(
                    request = ChatRequest(
                        model = "gpt-3.5-turbo",
                        messages = conversationHistory,
                        temperature = 0.3 // 评估时使用低随机性
                    )
                )

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        response.body()?.choices?.firstOrNull()?.message?.let { aiMessage ->
                            conversationHistory.add(aiMessage)
                            // 只显示AI的回复，不显示用户的评估请求
                            updateMessages(ChatMessage(aiMessage.content, false))
                        }
                    }
                    // ... 错误处理保持不变 ...
                }
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    fun sendSystemMessage(message: String) {
        conversationHistory.add(AIMessage("system", message))
    }

    private fun updateMessages(newMessage: ChatMessage) {
        val current = _messages.value?.toMutableList() ?: mutableListOf()
        current.add(newMessage)
        _messages.postValue(current)
    }

    fun clearConversation() {
        conversationHistory.clear()
        _messages.postValue(emptyList())
    }
}