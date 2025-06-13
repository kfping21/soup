package com.zjgsu.soup.game

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import android.widget.Toast
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*

object JsonDataUtil {

    private const val TAG = "JsonDataUtil"
    private const val PREFS_NAME = "game_history_prefs"
    private const val HISTORY_KEY = "game_history_data"
    
    // 从SharedPreferences获取所有游戏历史记录
    fun getGameHistory(context: Context): List<GameHistory> {
        Log.d(TAG, "正在获取游戏历史记录...")
        // 首先尝试从db.json中获取记录
        val dbRecords = DbJsonUtil.getHistoryRecords(context)
        if (dbRecords.isNotEmpty()) {
            Log.d(TAG, "从db.json成功获取到${dbRecords.size}条历史记录")
            return dbRecords
        } else {
            Log.d(TAG, "db.json中没有找到历史记录，尝试从SharedPreferences获取")
        }
        
        // 如果db.json中没有记录，则从SharedPreferences获取
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val historyJson = prefs.getString(HISTORY_KEY, null)
        
        if (historyJson == null) {
            Log.d(TAG, "SharedPreferences中也没有找到历史记录，返回示例数据")
            return getSampleGameHistory()
        }
        
        val type = object : TypeToken<List<GameHistory>>() {}.type
        return try {
            val result = Gson().fromJson<List<GameHistory>>(historyJson, type)
            Log.d(TAG, "从SharedPreferences成功获取到${result.size}条历史记录")
            result
        } catch (e: Exception) {
            Log.e(TAG, "解析SharedPreferences中的历史记录失败: ${e.message}")
            e.printStackTrace()
            getSampleGameHistory()
        }
    }
    
    // 获取特定用户的游戏历史记录
    fun getGameHistoryByUserId(context: Context, userId: String): List<GameHistory> {
        Log.d(TAG, "正在获取用户 $userId 的历史记录...")
        // 尝试从db.json获取用户特定记录
        val dbRecords = DbJsonUtil.getHistoryRecordsByUserId(context, userId)
        if (dbRecords.isNotEmpty()) {
            Log.d(TAG, "从db.json成功获取到用户 $userId 的${dbRecords.size}条记录")
            return dbRecords
        } else {
            Log.d(TAG, "db.json中没有找到用户 $userId 的记录，从SharedPreferences中过滤")
        }
        
        // 如果db.json没有该用户的记录，从SharedPreferences中过滤
        val allHistory = getGameHistory(context)
        val userRecords = allHistory.filter { it.userId == userId }
        Log.d(TAG, "从所有历史记录中过滤出${userRecords.size}条用户 $userId 的记录")
        return userRecords
    }
    
    // 添加新的游戏记录
    fun addGameHistory(context: Context, userId: String, questionId: Int, 
                      questionTitle: String, difficulty: String): GameHistory {
        Log.d(TAG, "正在添加新的游戏记录: 用户=$userId, 问题=$questionTitle")
        // 先从本地获取历史记录列表
        val historyList = getGameHistory(context).toMutableList()
        val newId = if (historyList.isEmpty()) 1 else historyList.maxOf { it.id } + 1
        val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
        val currentTime = dateFormat.format(Date())
        
        val newHistory = GameHistory(
            id = newId,
            userId = userId,
            questionId = questionId,
            questionTitle = questionTitle,
            difficulty = difficulty,
            completionTime = currentTime
        )
        
        Log.d(TAG, "创建了新的历史记录: $newHistory")
        
        // 添加到SharedPreferences
        historyList.add(0, newHistory)
        val saveToPrefs = saveGameHistory(context, historyList)
        Log.d(TAG, "保存到SharedPreferences结果: $saveToPrefs")
        
        // 同时保存到db.json文件
        val success = DbJsonUtil.addHistoryRecord(context, newHistory)
        Log.d(TAG, "保存到db.json结果: $success")
        
        if (success) {
            Toast.makeText(context, "记录已保存", Toast.LENGTH_SHORT).show()
        } else {
            Log.w(TAG, "保存到db.json失败，但已保存到SharedPreferences")
            Toast.makeText(context, "记录已部分保存", Toast.LENGTH_SHORT).show()
        }
        
        return newHistory
    }
    
    // 保存游戏历史记录到SharedPreferences
    private fun saveGameHistory(context: Context, historyList: List<GameHistory>): Boolean {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val historyJson = Gson().toJson(historyList)
            val success = prefs.edit().putString(HISTORY_KEY, historyJson).commit() // 使用commit而非apply以确保立即写入
            Log.d(TAG, "保存历史记录到SharedPreferences, 结果: $success, 记录数: ${historyList.size}")
            return success
        } catch (e: Exception) {
            Log.e(TAG, "保存历史记录到SharedPreferences失败: ${e.message}")
            e.printStackTrace()
            return false
        }
    }
    
    // 返回示例数据，如果没有实际记录
    fun getSampleGameHistory(): List<GameHistory> {
        Log.d(TAG, "返回示例游戏历史数据")
        return listOf(
            GameHistory(
                id = 1,
                userId = "user123",
                questionId = 101,
                questionTitle = "第五人格-医生",
                difficulty = "简单",
                completionTime = "2025/06/08 15:30"
            ),
            GameHistory(
                id = 2,
                userId = "user123",
                questionId = 102,
                questionTitle = "原神-钟离",
                difficulty = "中等",
                completionTime = "2025/06/07 10:15"
            ),
            GameHistory(
                id = 3,
                userId = "user123",
                questionId = 103,
                questionTitle = "明日方舟-陈",
                difficulty = "困难",
                completionTime = "2025/06/05 18:45"
            )
        )
    }
    
    // 检查是否有真实的游戏历史记录（非示例数据）
    fun hasRealGameHistory(context: Context): Boolean {
        // 检查db.json
        val dbRecords = DbJsonUtil.getHistoryRecords(context)
        if (dbRecords.isNotEmpty()) {
            Log.d(TAG, "db.json中有真实记录: ${dbRecords.size}条")
            return true
        }
        
        // 检查SharedPreferences
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val hasPrefs = prefs.contains(HISTORY_KEY)
        Log.d(TAG, "SharedPreferences中有历史记录: $hasPrefs")
        return hasPrefs
    }
}