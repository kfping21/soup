package com.zjgsu.soup.game

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import com.zjgsu.soup.game.GameHistory
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

object DbJsonUtil {
    private const val TAG = "DbJsonUtil"
    private const val DB_FILE_NAME = "db.json"
    
    // JSON Server API 地址
    private const val JSON_SERVER_URL = "http://10.0.2.2:3000" // 修改为安卓模拟器可访问的地址
    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .writeTimeout(5, TimeUnit.SECONDS)
        .build()
    
    // 创建一个协程范围用于启动后台任务
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // 检查JSON Server是否可用的方法
    suspend fun isJsonServerAvailable(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "正在检查JSON Server是否可用: $JSON_SERVER_URL")
                val request = Request.Builder()
                    .url(JSON_SERVER_URL)
                    .get()
                    .build()
                
                client.newCall(request).awaitResponse().use { response ->
                    val available = response.isSuccessful
                    Log.d(TAG, "JSON Server ${if (available) "可用" else "不可用"}, 状态码: ${response.code}")
                    available
                }
            } catch (e: Exception) {
                Log.e(TAG, "检查JSON Server可用性时发生异常: ${e.message}")
                e.printStackTrace()
                false
            }
        }
    }
    
    // 为UI组件提供一个检查JSON Server可用性的便捷方法
    fun checkJsonServerAvailability(callback: (Boolean) -> Unit) {
        coroutineScope.launch {
            val isAvailable = isJsonServerAvailable()
            withContext(Dispatchers.Main) {
                callback(isAvailable)
            }
        }
    }
    
    // OkHttp的异步扩展，将回调转为挂起函数
    private suspend fun Call.awaitResponse(): Response = suspendCoroutine { continuation ->
        enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                continuation.resume(response)
            }
            
            override fun onFailure(call: Call, e: IOException) {
                continuation.resumeWithException(e)
            }
        })
    }
    
    // 从JSON Server获取db.json内容
    fun getDbJsonContent(context: Context): JsonObject? {
        try {
            Log.d(TAG, "开始尝试从 JSON Server 读取数据")
            val jsonObject = JsonObject()
            
            // 获取todos数据
            val todosJson = getDataFromJsonServer("$JSON_SERVER_URL/todos")
            if (todosJson != null) {
                val todosArray = JsonParser.parseString(todosJson).asJsonArray
                jsonObject.add("todos", todosArray)
                Log.d(TAG, "成功获取todos数据，共${todosArray.size()}条")
            } else {
                jsonObject.add("todos", JsonParser.parseString("[]").asJsonArray)
                Log.d(TAG, "无法获取todos数据，创建空数组")
            }
            
            // 获取history数据
            val historyJson = getDataFromJsonServer("$JSON_SERVER_URL/history")
            if (historyJson != null) {
                val historyArray = JsonParser.parseString(historyJson).asJsonArray
                jsonObject.add("history", historyArray)
                Log.d(TAG, "成功获取history数据，共${historyArray.size()}条")
            } else {
                jsonObject.add("history", JsonParser.parseString("[]").asJsonArray)
                Log.d(TAG, "无法获取history数据，创建空数组")
            }
            
            // 如果无法从 JSON Server 获取数据，尝试读取本地文件
            if (jsonObject.getAsJsonArray("todos").size() == 0 && jsonObject.getAsJsonArray("history").size() == 0) {
                Log.d(TAG, "从 JSON Server 获取数据为空，尝试从本地文件读取")
                return getDbJsonFromFile(context)
            }
            
            return jsonObject
        } catch (e: Exception) {
            Log.e(TAG, "从 JSON Server 读取数据失败: ${e.message}")
            e.printStackTrace()
            
            // 尝试从本地文件读取
            Log.d(TAG, "尝试从本地文件读取数据")
            return getDbJsonFromFile(context)
        }
    }
    
    // 通过 OkHttp 从 JSON Server 获取数据（挂起函数版本）
    private suspend fun getDataFromJsonServerAsync(url: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url(url)
                    .get()
                    .build()
                
                val response = client.newCall(request).awaitResponse()
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    Log.d(TAG, "从 $url 成功获取数据")
                    responseBody
                } else {
                    Log.e(TAG, "从 $url 获取数据失败，状态码: ${response.code}")
                    null
                }
            } catch (e: Exception) {
                Log.e(TAG, "请求 $url 异常: ${e.message}")
                e.printStackTrace()
                null
            }
        }
    }
    
    // 保持原来的同步方法用于内部调用，但添加警告表明它应该被替换
    @Deprecated("此方法在主线程调用会导致NetworkOnMainThreadException，请使用异步版本")
    private fun getDataFromJsonServer(url: String): String? {
        try {
            val request = Request.Builder()
                .url(url)
                .get()
                .build()
            
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                Log.d(TAG, "从 $url 成功获取数据")
                return responseBody
            } else {
                Log.e(TAG, "从 $url 获取数据失败，状态码: ${response.code}")
                return null
            }
        } catch (e: Exception) {
            Log.e(TAG, "请求 $url 异常: ${e.message}")
            e.printStackTrace()
            return null
        }
    }
    
    // 从本地文件读取 db.json
    private fun getDbJsonFromFile(context: Context): JsonObject? {
        try {
            Log.d(TAG, "开始尝试从本地读取db.json文件")
            // 仅从指定路径加载db.json文件
            val sourceFile = File(context.getExternalFilesDir(null)?.parentFile?.parentFile?.parentFile?.parentFile, "soup2/srcjson/db.json")
            Log.d(TAG, "尝试路径: ${sourceFile.absolutePath}, 是否存在: ${sourceFile.exists()}")
            
            if (!sourceFile.exists()) {
                Log.e(TAG, "找不到db.json文件，返回null")
                return null
            }
            
            val jsonString = FileReader(sourceFile).use { it.readText() }
            Log.d(TAG, "成功从 ${sourceFile.absolutePath} 加载db.json")
            return JsonParser.parseString(jsonString).asJsonObject
        } catch (e: Exception) {
            Log.e(TAG, "Error reading db.json: ${e.message}")
            e.printStackTrace()
            return null
        }
    }
    
    // 保存更新后的db.json内容通过 JSON Server
    fun saveDbJsonContent(context: Context, jsonObject: JsonObject): Boolean {
        try {
            Log.d(TAG, "尝试通过 JSON Server 保存数据")
            
            // 首先尝试通过 JSON Server API 保存数据
            val historySaved = if (jsonObject.has("history")) {
                val historyArray = jsonObject.getAsJsonArray("history")
                updateHistoryData(historyArray)
            } else {
                true // 如果没有 history 数据，认为保存成功
            }
            
            // 如果通过 API 保存失败，尝试保存到本地文件
            if (!historySaved) {
                Log.d(TAG, "通过 JSON Server 保存数据失败，尝试保存到本地文件")
                return saveDbJsonToFile(context, jsonObject)
            }
            
            return true
        } catch (e: Exception) {
            Log.e(TAG, "通过 JSON Server 保存数据异常: ${e.message}")
            e.printStackTrace()
            
            // 尝试保存到本地文件
            Log.d(TAG, "尝试保存到本地文件")
            return saveDbJsonToFile(context, jsonObject)
        }
    }
    
    // 通过 OkHttp 更新 history 数据
    private fun updateHistoryData(historyArray: com.google.gson.JsonArray): Boolean {
        try {
            // 获取当前服务器上的所有 history 记录
            val existingHistoryJson = getDataFromJsonServer("$JSON_SERVER_URL/history")
            val existingHistoryArray = if (existingHistoryJson != null) {
                JSONArray(existingHistoryJson)
            } else {
                JSONArray()
            }
            
            // 获取最后一条新记录，只更新最新的记录
            if (historyArray.size() > 0) {
                val newHistoryItem = historyArray.get(historyArray.size() - 1)
                val gson = Gson()
                val historyObj = gson.fromJson(newHistoryItem, GameHistory::class.java)
                
                // 准备 JSON 数据
                val jsonBody = JSONObject().apply {
                    put("id", historyObj.id)
                    put("userId", historyObj.userId)
                    put("questionId", historyObj.questionId)
                    put("questionTitle", historyObj.questionTitle)
                    put("difficulty", historyObj.difficulty)
                    put("completionTime", historyObj.completionTime)
                }
                
                // 创建请求
                val requestBody = RequestBody.create(
                    "application/json; charset=utf-8".toMediaType(),
                    jsonBody.toString()
                )
                
                val request = Request.Builder()
                    .url("$JSON_SERVER_URL/history")
                    .post(requestBody)
                    .build()
                
                // 执行请求
                val response = client.newCall(request).execute()
                val success = response.isSuccessful
                Log.d(TAG, "向 JSON Server 发送POST请求保存历史记录 ${if (success) "成功" else "失败"}, 状态码: ${response.code}")
                return success
            }
            
            return true // 没有新记录需要保存
        } catch (e: Exception) {
            Log.e(TAG, "更新 history 数据异常: ${e.message}")
            e.printStackTrace()
            return false
        }
    }
    
    // 保存到本地文件
    private fun saveDbJsonToFile(context: Context, jsonObject: JsonObject): Boolean {
        try {
            // 只使用外部存储路径
            val targetFile = File(context.getExternalFilesDir(null)?.parentFile?.parentFile?.parentFile?.parentFile, "soup2/srcjson/db.json")
            Log.d(TAG, "尝试写入路径: ${targetFile.absolutePath}, 是否存在: ${targetFile.exists()}")
            
            // 检查全路径并创建必要的目录
            val externalDir = File(context.getExternalFilesDir(null)?.parentFile?.parentFile?.parentFile?.parentFile, "soup2/srcjson")
            if (!externalDir.exists()) {
                val success = externalDir.mkdirs()
                Log.d(TAG, "创建目录: ${externalDir.absolutePath}, 成功: $success")
            }
            
            // 使用Pretty Printing格式化JSON
            val gson = GsonBuilder().setPrettyPrinting().create()
            val prettyJson = gson.toJson(jsonObject)
            
            FileWriter(targetFile).use { it.write(prettyJson) }
            Log.d(TAG, "成功写入db.json文件: ${targetFile.absolutePath}")
            return true
        } catch (e: IOException) {
            Log.e(TAG, "写入db.json文件失败: ${e.message}")
            e.printStackTrace()
            return false
        }
    }
    
    // 添加新记录到history数组
    fun addHistoryRecord(context: Context, gameHistory: GameHistory): Boolean {
        Log.d(TAG, "开始添加游戏记录: $gameHistory")
        
        // 首先尝试直接通过 API 添加记录
        try {
            val gson = Gson()
            val jsonString = gson.toJson(gameHistory)
            
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = RequestBody.create(mediaType, jsonString)
            
            val request = Request.Builder()
                .url("$JSON_SERVER_URL/history")
                .post(requestBody)
                .build()
            
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                Log.d(TAG, "成功通过 API 添加游戏记录")
                return true
            } else {
                Log.e(TAG, "通过 API 添加游戏记录失败，状态码: ${response.code}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "通过 API 添加游戏记录异常: ${e.message}")
            e.printStackTrace()
        }
        
        // 如果 API 添加失败，尝试获取整个 JSON 对象并更新
        val jsonObject = getDbJsonContent(context)
        
        if (jsonObject == null) {
            Log.e(TAG, "获取db.json内容失败，无法添加历史记录")
            return false
        }
        
        try {
            // 获取history数组
            var historyArray = jsonObject.getAsJsonArray("history")
            
            // 如果数组不存在，创建一个新的
            if (historyArray == null) {
                Log.d(TAG, "history数组不存在，创建一个新数组")
                historyArray = JsonParser.parseString("[]").asJsonArray
                jsonObject.add("history", historyArray)
            }
            
            // 将新记录转换为JsonElement并添加到数组
            val gson = Gson()
            val historyElement = gson.toJsonTree(gameHistory)
            historyArray.add(historyElement)
            
            Log.d(TAG, "成功添加记录到history数组，当前记录数: ${historyArray.size()}")
            
            // 保存回文件
            val saveResult = saveDbJsonContent(context, jsonObject)
            Log.d(TAG, "保存结果: $saveResult")
            return saveResult
        } catch (e: Exception) {
            Log.e(TAG, "添加历史记录失败: ${e.message}")
            e.printStackTrace()
            return false
        }
    }
    
    // 从db.json中获取所有历史记录
    fun getHistoryRecords(context: Context): List<GameHistory> {
        // 首先尝试通过 API 获取历史记录
        try {
            Log.d(TAG, "尝试通过 API 获取历史记录")
            val historyJson = getDataFromJsonServer("$JSON_SERVER_URL/history")
            
            if (historyJson != null) {
                val gson = Gson()
                val type = object : TypeToken<List<GameHistory>>() {}.type
                val historyList = gson.fromJson<List<GameHistory>>(historyJson, type)
                Log.d(TAG, "成功通过 API 获取${historyList.size}条历史记录")
                return historyList
            } else {
                Log.e(TAG, "通过 API 获取历史记录失败，尝试从本地JSON对象获取")
            }
        } catch (e: Exception) {
            Log.e(TAG, "通过 API 获取历史记录异常: ${e.message}")
            e.printStackTrace()
        }
        
        // 如果 API 获取失败，尝试从本地 JSON 对象获取
        val jsonObject = getDbJsonContent(context)
        
        if (jsonObject == null) {
            Log.e(TAG, "获取db.json内容失败，无法读取历史记录")
            return emptyList()
        }
        
        return try {
            if (!jsonObject.has("history")) {
                Log.d(TAG, "db.json中不存在history字段")
                return emptyList()
            }
            
            val historyArray = jsonObject.getAsJsonArray("history")
            val gson = Gson()
            val historyList = mutableListOf<GameHistory>()
            
            for (i in 0 until historyArray.size()) {
                val historyElement = historyArray.get(i)
                val history = gson.fromJson(historyElement, GameHistory::class.java)
                historyList.add(history)
            }
            
            Log.d(TAG, "成功从本地JSON对象读取${historyList.size}条历史记录")
            historyList
        } catch (e: Exception) {
            Log.e(TAG, "解析历史记录失败: ${e.message}")
            e.printStackTrace()
            emptyList()
        }
    }
    
    // 根据用户ID获取特定用户的历史记录
    fun getHistoryRecordsByUserId(context: Context, userId: String): List<GameHistory> {
        Log.d(TAG, "尝试获取用户 $userId 的历史记录")
        
        // 首先尝试通过 API 获取该用户的历史记录
        try {
            val url = "$JSON_SERVER_URL/history?userId=$userId"
            Log.d(TAG, "尝试从 $url 获取数据")
            
            val userRecordsJson = getDataFromJsonServer(url)
            if (userRecordsJson != null) {
                val gson = Gson()
                val type = object : TypeToken<List<GameHistory>>() {}.type
                val userRecords = gson.fromJson<List<GameHistory>>(userRecordsJson, type)
                Log.d(TAG, "成功通过 API 获取用户 $userId 的${userRecords.size}条记录")
                return userRecords
            } else {
                Log.e(TAG, "通过 API 获取用户记录失败，尝试从所有记录中过滤")
            }
        } catch (e: Exception) {
            Log.e(TAG, "通过 API 获取用户记录异常: ${e.message}")
            e.printStackTrace()
        }
        
        // 如果 API 获取失败，从所有记录中过滤
        val allHistoryRecords = getHistoryRecords(context)
        val userRecords = allHistoryRecords.filter { it.userId == userId }
        Log.d(TAG, "从所有记录中过滤出用户 $userId 的 ${userRecords.size} 条历史记录")
        return userRecords
    }
    
    // 异步获取db.json内容
    suspend fun getDbJsonContentAsync(context: Context): JsonObject? {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "开始尝试从 JSON Server 异步读取数据")
                val jsonObject = JsonObject()
                
                // 获取todos数据
                val todosJson = getDataFromJsonServerAsync("$JSON_SERVER_URL/todos")
                if (todosJson != null) {
                    val todosArray = JsonParser.parseString(todosJson).asJsonArray
                    jsonObject.add("todos", todosArray)
                    Log.d(TAG, "成功获取todos数据，共${todosArray.size()}条")
                } else {
                    jsonObject.add("todos", JsonParser.parseString("[]").asJsonArray)
                    Log.d(TAG, "无法获取todos数据，创建空数组")
                }
                
                // 获取history数据
                val historyJson = getDataFromJsonServerAsync("$JSON_SERVER_URL/history")
                if (historyJson != null) {
                    val historyArray = JsonParser.parseString(historyJson).asJsonArray
                    jsonObject.add("history", historyArray)
                    Log.d(TAG, "成功获取history数据，共${historyArray.size()}条")
                } else {
                    jsonObject.add("history", JsonParser.parseString("[]").asJsonArray)
                    Log.d(TAG, "无法获取history数据，创建空数组")
                }
                
                // 如果无法从 JSON Server 获取数据，尝试读取本地文件
                if (jsonObject.getAsJsonArray("todos").size() == 0 && jsonObject.getAsJsonArray("history").size() == 0) {
                    Log.d(TAG, "从 JSON Server 获取数据为空，尝试从本地文件读取")
                    return@withContext getDbJsonFromFile(context)
                }
                
                jsonObject
            } catch (e: Exception) {
                Log.e(TAG, "从 JSON Server 异步读取数据失败: ${e.message}")
                e.printStackTrace()
                
                // 尝试从本地文件读取
                Log.d(TAG, "尝试从本地文件读取数据")
                getDbJsonFromFile(context)
            }
        }
    }
    
    // 异步获取所有历史记录
    suspend fun getHistoryRecordsAsync(context: Context): List<GameHistory> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "尝试通过 API 异步获取历史记录")
                val historyJson = getDataFromJsonServerAsync("$JSON_SERVER_URL/history")
                
                if (historyJson != null) {
                    val gson = Gson()
                    val type = object : TypeToken<List<GameHistory>>() {}.type
                    val historyList = gson.fromJson<List<GameHistory>>(historyJson, type)
                    Log.d(TAG, "成功通过 API 异步获取${historyList.size}条历史记录")
                    return@withContext historyList
                } else {
                    Log.e(TAG, "通过 API 异步获取历史记录失败，尝试从本地JSON对象获取")
                }
            } catch (e: Exception) {
                Log.e(TAG, "通过 API 异步获取历史记录异常: ${e.message}")
                e.printStackTrace()
            }
            
            // 如果 API 获取失败，尝试从本地 JSON 对象获取
            val jsonObject = getDbJsonFromFile(context) // 这个方法已经在IO线程中了，所以不需要再包装
            
            if (jsonObject == null) {
                Log.e(TAG, "获取db.json内容失败，无法读取历史记录")
                return@withContext emptyList<GameHistory>()
            }
            
            try {
                if (!jsonObject.has("history")) {
                    Log.d(TAG, "db.json中不存在history字段")
                    return@withContext emptyList<GameHistory>()
                }
                
                val historyArray = jsonObject.getAsJsonArray("history")
                val gson = Gson()
                val historyList = mutableListOf<GameHistory>()
                
                for (i in 0 until historyArray.size()) {
                    val historyElement = historyArray.get(i)
                    val history = gson.fromJson(historyElement, GameHistory::class.java)
                    historyList.add(history)
                }
                
                Log.d(TAG, "成功从本地JSON对象读取${historyList.size}条历史记录")
                historyList
            } catch (e: Exception) {
                Log.e(TAG, "解析历史记录失败: ${e.message}")
                e.printStackTrace()
                emptyList()
            }
        }
    }
    
    // 异步获取特定用户的历史记录
    suspend fun getHistoryRecordsByUserIdAsync(context: Context, userId: String): List<GameHistory> {
        return withContext(Dispatchers.IO) {
            Log.d(TAG, "尝试异步获取用户 $userId 的历史记录")
            
            // 首先尝试通过 API 获取该用户的历史记录
            try {
                val url = "$JSON_SERVER_URL/history?userId=$userId"
                Log.d(TAG, "尝试从 $url 异步获取数据")
                
                val userRecordsJson = getDataFromJsonServerAsync(url)
                if (userRecordsJson != null) {
                    val gson = Gson()
                    val type = object : TypeToken<List<GameHistory>>() {}.type
                    val userRecords = gson.fromJson<List<GameHistory>>(userRecordsJson, type)
                    Log.d(TAG, "成功通过 API 异步获取用户 $userId 的${userRecords.size}条记录")
                    return@withContext userRecords
                } else {
                    Log.e(TAG, "通过 API 异步获取用户记录失败，尝试从所有记录中过滤")
                }
            } catch (e: Exception) {
                Log.e(TAG, "通过 API 异步获取用户记录异常: ${e.message}")
                e.printStackTrace()
            }
            
            // 如果 API 获取失败，从所有记录中过滤
            val allHistoryRecords = getHistoryRecordsAsync(context)
            val userRecords = allHistoryRecords.filter { it.userId == userId }
            Log.d(TAG, "从所有记录中过滤出用户 $userId 的 ${userRecords.size} 条历史记录")
            userRecords
        }
    }
    
    // 为UI组件提供便捷方法，在后台执行网络操作并返回结果
    fun getHistoryRecordsInBackground(context: Context, callback: (List<GameHistory>) -> Unit) {
        coroutineScope.launch {
            val result = getHistoryRecordsAsync(context)
            // 切换到主线程回调
            withContext(Dispatchers.Main) {
                callback(result)
            }
        }
    }
    
    // 为UI组件提供便捷方法，在后台获取特定用户的历史记录
    fun getHistoryRecordsByUserIdInBackground(context: Context, userId: String, callback: (List<GameHistory>) -> Unit) {
        coroutineScope.launch {
            val result = getHistoryRecordsByUserIdAsync(context, userId)
            // 切换到主线程回调
            withContext(Dispatchers.Main) {
                callback(result)
            }
        }
    }
    
    // 异步保存历史记录
    suspend fun addHistoryRecordAsync(context: Context, gameHistory: GameHistory): Boolean {
        return withContext(Dispatchers.IO) {
            addHistoryRecord(context, gameHistory)
        }
    }
    
    // 为UI组件提供便捷方法，在后台添加历史记录
    fun addHistoryRecordInBackground(context: Context, gameHistory: GameHistory, callback: (Boolean) -> Unit) {
        coroutineScope.launch {
            val result = addHistoryRecordAsync(context, gameHistory)
            withContext(Dispatchers.Main) {
                callback(result)
            }
        }
    }
}