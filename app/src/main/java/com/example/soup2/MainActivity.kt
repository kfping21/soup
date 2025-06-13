package com.example.soup2

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.soup2.adapter.GameHistoryAdapter
import com.example.soup2.util.JsonDataUtil
import com.zjgsu.soup.R

class MainActivity : AppCompatActivity() {
    
    // 侧边栏面板
    private lateinit var settingsPanel: LinearLayout
    private lateinit var musicPanel: LinearLayout
    private lateinit var gameHistoryPanel: LinearLayout
    private lateinit var developersPanel: LinearLayout
    
    // 历史记录列表
    private lateinit var gameHistoryRecyclerView: RecyclerView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // 初始化UI组件
        initUI()
        
        // 设置点击侧边栏菜单的监听器
        setupMenuListeners()
    }
    
    private fun initUI() {
        // 初始化菜单按钮
        val menuSettings = findViewById<LinearLayout>(R.id.menuSettings)
        val menuMusic = findViewById<LinearLayout>(R.id.menuMusic)
        val menuGameHistory = findViewById<LinearLayout>(R.id.menuGameHistory)
        val menuDevelopers = findViewById<LinearLayout>(R.id.menuDevelopers)
        
        // 初始化面板
        settingsPanel = findViewById(R.id.settingsPanel)
        musicPanel = findViewById(R.id.musicPanel)
        gameHistoryPanel = findViewById(R.id.gameHistoryPanel)
        developersPanel = findViewById(R.id.developersPanel)
        
        // 初始化游戏历史记录列表
        gameHistoryRecyclerView = findViewById(R.id.gameHistoryRecyclerView)
        gameHistoryRecyclerView.layoutManager = LinearLayoutManager(this)
        
        // 加载游戏历史记录数据
        loadGameHistory()
    }
    
    private fun setupMenuListeners() {
        // 设置点击监听
        findViewById<LinearLayout>(R.id.menuSettings).setOnClickListener {
            togglePanel(settingsPanel)
        }
        
        findViewById<LinearLayout>(R.id.menuMusic).setOnClickListener {
            togglePanel(musicPanel)
        }
        
        findViewById<LinearLayout>(R.id.menuGameHistory).setOnClickListener {
            togglePanel(gameHistoryPanel)
            // 每次点击时刷新游戏历史记录
            loadGameHistory()
        }
        
        findViewById<LinearLayout>(R.id.menuDevelopers).setOnClickListener {
            togglePanel(developersPanel)
        }
    }
    
    // 切换面板的显示和隐藏
    private fun togglePanel(panel: LinearLayout) {
        // 隐藏所有面板
        settingsPanel.visibility = View.GONE
        musicPanel.visibility = View.GONE
        gameHistoryPanel.visibility = View.GONE
        developersPanel.visibility = View.GONE
        
        // 显示选中的面板
        panel.visibility = View.VISIBLE
    }
    
    // 加载游戏历史记录
    private fun loadGameHistory() {
        val historyList = JsonDataUtil.getGameHistory(this)
        val adapter = GameHistoryAdapter(historyList)
        gameHistoryRecyclerView.adapter = adapter
    }
    
    // 当用户完成题目时调用此方法记录游戏历史
    fun recordGameCompletion(questionId: Int, questionTitle: String, difficulty: String) {
        val gameHistory = JsonDataUtil.addGameHistory(
            context = this,
            userId = "user123", // 这里可以替换为实际的用户ID
            questionId = questionId,
            questionTitle = questionTitle,
            difficulty = difficulty
        )
        
        // TODO: 将新记录保存到db.json文件中
        // 这里需要实现将新记录写入JSON文件的逻辑
        // 可以使用SharedPreferences暂时存储，或者使用文件IO操作写入外部存储
    }
}