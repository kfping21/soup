package com.zjgsu.soup.game.util

import android.content.Context
import android.content.SharedPreferences

/**
 * 用户会话管理器
 * 用于在应用程序中存储和检索当前登录用户的信息
 */
class SessionManager(context: Context) {
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val editor: SharedPreferences.Editor = sharedPreferences.edit()

    companion object {
        private const val PREF_NAME = "UserSessionPref"
        private const val KEY_IS_LOGGED_IN = "isLoggedIn"
        private const val KEY_USER_ID = "userId"
        private const val KEY_USERNAME = "username"

        // 单例实例
        @Volatile
        private var instance: SessionManager? = null

        fun getInstance(context: Context): SessionManager {
            return instance ?: synchronized(this) {
                instance ?: SessionManager(context.applicationContext).also { instance = it }
            }
        }
    }

    /**
     * 创建用户登录会话
     */
    fun createLoginSession(userId: String, username: String) {
        editor.putBoolean(KEY_IS_LOGGED_IN, true)
        editor.putString(KEY_USER_ID, userId)
        editor.putString(KEY_USERNAME, username)
        editor.apply()
    }

    /**
     * 获取用户会话详情
     */
    fun getUserDetails(): HashMap<String, String?> {
        val user = HashMap<String, String?>()
        user[KEY_USER_ID] = sharedPreferences.getString(KEY_USER_ID, null)
        user[KEY_USERNAME] = sharedPreferences.getString(KEY_USERNAME, null)
        return user
    }

    /**
     * 获取当前用户ID
     */
    fun getUserId(): String? {
        return sharedPreferences.getString(KEY_USER_ID, null)
    }

    /**
     * 获取当前用户名
     */
    fun getUsername(): String? {
        return sharedPreferences.getString(KEY_USERNAME, null)
    }

    /**
     * 检查用户是否已登录
     */
    fun isLoggedIn(): Boolean {
        return sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    /**
     * 清除会话详情，退出登录
     */
    fun logout() {
        editor.clear()
        editor.apply()
    }
}