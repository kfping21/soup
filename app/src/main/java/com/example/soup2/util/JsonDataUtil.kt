package com.example.soup2.util

import android.content.Context
import com.example.soup2.model.GameHistory
import com.google.gson.Gson
import com.google.gson.JsonObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.*

object JsonDataUtil {

    // 从assets目录下读取JSON文件内容
    private fun loadJSONFromAsset(context: Context, fileName: String): String? {
        return try {
            val inputStream = context.assets.open(fileName)
            val bufferedReader = BufferedReader(InputStreamReader(inputStream))
            val stringBuilder = StringBuilder()
            var line: String?
            while (bufferedReader.readLine().also { line = it } != null) {
                stringBuilder.append(line)
            }
            inputStream.close()
            stringBuilder.toString()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // 获取游戏历史记录
    fun getGameHistory(context: Context): List<GameHistory> {
        val jsonString = loadJSONFromAsset(context, "db.json") ?: return emptyList()
        val gson = Gson()
        val jsonObject = gson.fromJson(jsonString, JsonObject::class.java)
        val historyArray = jsonObject.getAsJsonArray("history")
        return gson.fromJson(historyArray, Array<GameHistory>::class.java).toList()
    }
    
    // 添加新的游戏记录
    fun addGameHistory(context: Context, userId: String, questionId: Int, 
                      questionTitle: String, difficulty: String): GameHistory {
        val historyList = getGameHistory(context)
        val newId = if (historyList.isEmpty()) 1 else historyList.maxOf { it.id } + 1
        val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
        val currentTime = dateFormat.format(Date())
        
        return GameHistory(
            id = newId,
            userId = userId,
            questionId = questionId,
            questionTitle = questionTitle,
            difficulty = difficulty,
            completionTime = currentTime
        )
    }
}