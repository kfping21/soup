package com.zjgsu.soup.game

import android.annotation.SuppressLint
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.RotateAnimation
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import android.widget.VideoView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.zjgsu.soup.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GameMainActivity : AppCompatActivity() {

    private lateinit var sideMenu: View
    private lateinit var settingsPanel: View
    private lateinit var musicPanel: View
    private lateinit var gameHistoryPanel: View
    private lateinit var developersPanel: View
    private lateinit var brightnessSeekBar: SeekBar
    private lateinit var gameHistoryRecyclerView: RecyclerView
    private var isMenuShowing = false
    private lateinit var startGameButton: TextView
    private lateinit var loadingStatusText: TextView
    private lateinit var settingsButton: ImageButton
    private lateinit var backgroundVideo: VideoView
    private var gameDataList: List<GameData> = emptyList()

    // 音乐相关变量
    private lateinit var mediaPlayer: MediaPlayer
    private var currentMusicId = R.raw.music1 // 默认第五人格
    private var isMusicPlaying = true
    private var currentVolume = 0.5f // 默认音量

    private lateinit var musicControlButton: ImageView
    private lateinit var rotateAnimation: RotateAnimation
    private var isMusicIconRotating = true
    private lateinit var menuOverlay: View
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game_main)

        startGameButton = findViewById(R.id.startGameButton)
        settingsButton = findViewById(R.id.settingsButton)
        backgroundVideo = findViewById(R.id.backgroundVideo)
        loadingStatusText = findViewById(R.id.loadingStatusText)
        sideMenu = findViewById(R.id.sideMenu)
        settingsPanel = sideMenu.findViewById(R.id.settingsPanel)
        musicPanel = sideMenu.findViewById(R.id.musicPanel)
        gameHistoryPanel = sideMenu.findViewById(R.id.gameHistoryPanel)
        developersPanel = sideMenu.findViewById(R.id.developersPanel)
        brightnessSeekBar = sideMenu.findViewById(R.id.brightnessSeekBar)
        val volumeSeekBar = sideMenu.findViewById<SeekBar>(R.id.volumeSeekBar)
        val btnChangeMusic = sideMenu.findViewById<Button>(R.id.btnChangeMusic)
        val btnToggleMusic = sideMenu.findViewById<Button>(R.id.btnToggleMusic)

        // 初始化游戏历史记录列表
        gameHistoryRecyclerView = sideMenu.findViewById(R.id.gameHistoryRecyclerView)
        gameHistoryRecyclerView.layoutManager = LinearLayoutManager(this)

        menuOverlay = findViewById(R.id.menuOverlay)

        // 设置遮罩层点击事件
        menuOverlay.setOnClickListener {
            hideMenu()
        }
        // 初始化音乐播放器
        initMediaPlayer()

        initVideoBackground()
        //初始化视频播放器

        setupBlinkingAnimation()

        startGameButton.setOnClickListener {
            showDifficultyDialog() // 新增方法
        }
        // 初始化侧边菜单位置
        sideMenu.post {
            sideMenu.translationX = sideMenu.width.toFloat()
            sideMenu.visibility = View.VISIBLE
        }

        // 设置按钮点击事件 - 切换菜单显示
        settingsButton.setOnClickListener {
            if (isMenuShowing) {
                hideMenu()
            } else {
                showMenu()
            }
        }

        sideMenu.findViewById<LinearLayout>(R.id.menuSettings).setOnClickListener {
            togglePanel(settingsPanel)
            it.isClickable = true  // 确保可点击
            true  // 阻止事件冒泡到遮罩层
        }

        sideMenu.findViewById<LinearLayout>(R.id.menuMusic).setOnClickListener {
            togglePanel(musicPanel)
            true
        }

        sideMenu.findViewById<LinearLayout>(R.id.menuDevelopers).setOnClickListener {
            togglePanel(developersPanel)
            true
        }

        sideMenu.findViewById<LinearLayout>(R.id.menuGameHistory).setOnClickListener {
            togglePanel(gameHistoryPanel)
            // 每次点击时加载游戏历史记录
            loadGameHistory()
            true
        }

        // 亮度调节
        brightnessSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val layoutParams = window.attributes
                layoutParams.screenBrightness = progress / 100f
                window.attributes = layoutParams
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // 音量调节
        volumeSeekBar.progress = (currentVolume * 100).toInt()
        volumeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                currentVolume = progress / 100f
                mediaPlayer.setVolume(currentVolume, currentVolume)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // 音乐切换按钮
        btnChangeMusic.setOnClickListener {
            val musicList = listOf(R.raw.music1, R.raw.music2, R.raw.music3)
            val currentIndex = musicList.indexOf(currentMusicId)
            val nextIndex = (currentIndex + 1) % musicList.size
            changeMusic(musicList[nextIndex], "")
        }

        // 音乐暂停/播放按钮
        btnToggleMusic.setOnClickListener {
            isMusicPlaying = !isMusicPlaying
            if (isMusicPlaying) {
                mediaPlayer.start()
                btnToggleMusic.text = "暂停音乐"
            } else {
                mediaPlayer.pause()
                btnToggleMusic.text = "播放音乐"
            }
        }

        loadGameDataFromServer()
    }

    // 加载游戏历史记录
    private fun loadGameHistory() {
        // 获取当前登录用户ID
        val sessionManager = com.zjgsu.soup.game.util.SessionManager.getInstance(this)
        val userId = sessionManager.getUserId() ?: "user123" // 如果没有用户ID，使用默认ID "user123"

        // 获取特定用户的游戏历史记录
        val historyList = JsonDataUtil.getGameHistoryByUserId(this, userId)
        
        // 如果没有历史记录，则显示示例记录
        val displayList = if (historyList.isEmpty()) {
            JsonDataUtil.getSampleGameHistory()
        } else {
            historyList
        }
        
        val adapter = GameHistoryAdapter(displayList)
        gameHistoryRecyclerView.adapter = adapter
    }

    // 记录游戏完成信息
    fun recordGameCompletion(questionId: Int, questionTitle: String, difficulty: String) {
        // 获取当前登录用户ID
        val sessionManager = com.zjgsu.soup.game.util.SessionManager.getInstance(this)
        val userId = sessionManager.getUserId() ?: return

        JsonDataUtil.addGameHistory(
            context = this,
            userId = userId, // 使用当前登录用户ID
            questionId = questionId,
            questionTitle = questionTitle,
            difficulty = difficulty
        )
    }

    private fun setupBlinkingAnimation() {
        val blinkAnimation = AlphaAnimation(1f, 0.3f) // 从完全可见到半透明
        blinkAnimation.duration = 1000 // 动画持续时间1秒
        blinkAnimation.repeatCount = AlphaAnimation.INFINITE // 无限循环
        blinkAnimation.repeatMode = AlphaAnimation.REVERSE // 反向播放，形成闪烁效果

        startGameButton.startAnimation(blinkAnimation)
    }

    private fun initMediaPlayer() {
        mediaPlayer = MediaPlayer.create(this, currentMusicId).apply {
            isLooping = true
            setVolume(0.5f, 0.5f)
            start()
        }
    }

    private fun initVideoBackground() {
        // 从raw资源加载视频（假设视频文件名为bg_video.mp4）
        val videoUri = Uri.parse("android.resource://$packageName/${R.raw.bg_video}")
        backgroundVideo.setVideoURI(videoUri)

        backgroundVideo.setOnPreparedListener { mp ->
            mp.isLooping = true
            mp.setVolume(0f, 0f) // 静音视频，只播放背景音乐
            mp.start()
        }
    }

    private fun showMenu() {
        sideMenu.bringToFront()
        menuOverlay.bringToFront()
        menuOverlay.visibility = View.VISIBLE
        sideMenu.visibility = View.VISIBLE

        // 确保点击菜单项不会触发遮罩层点击
        sideMenu.isClickable = true

        sideMenu.animate()
            .translationX(0f)
            .setDuration(250)
            .withStartAction { hideAllPanels() }
            .start()
        isMenuShowing = true
    }

    private fun hideMenu() {
        sideMenu.animate()
            .translationX(-sideMenu.width.toFloat())
            .setDuration(250)
            .withEndAction {
                sideMenu.visibility = View.INVISIBLE
                menuOverlay.visibility = View.GONE
            }
            .start()
        isMenuShowing = false
    }


    private fun togglePanel(panel: View) {
        if (panel.visibility == View.VISIBLE) {
            panel.visibility = View.GONE
        } else {
            hideAllPanels()
            panel.visibility = View.VISIBLE
            // 添加这行确保面板显示
            panel.postDelayed({ panel.bringToFront() }, 50)
        }
    }


    private fun hideAllPanels() {
        settingsPanel.visibility = View.GONE
        musicPanel.visibility = View.GONE
        developersPanel.visibility = View.GONE
        gameHistoryPanel.visibility = View.GONE
    }

    private fun changeMusic(musicResId: Int, musicName: String) {
        if (currentMusicId == musicResId) return

        mediaPlayer.stop()
        mediaPlayer.release()
        currentMusicId = musicResId
        mediaPlayer = MediaPlayer.create(this, currentMusicId)
        mediaPlayer.isLooping = true
        mediaPlayer.setVolume(currentVolume, currentVolume)
        if (isMusicPlaying) {
            mediaPlayer.start()
            // 确保动画在运行
            if (!isMusicIconRotating) {
                musicControlButton.startAnimation(rotateAnimation)
                isMusicIconRotating = true
            }
        }
        Toast.makeText(this, "已切换", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer.release()
        startGameButton.clearAnimation()
    }

    private fun loadGameDataFromServer() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 修改为获取所有题目（或指定默认难度）
                val response = RetrofitClient.instance.getQuestionsByDifficulty("all")
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        gameDataList = response.body() ?: emptyList()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showError("网络错误: ${e.message}")
                }
            }
        }
    }

    private fun showError(message: String) {
        loadingStatusText.text = message
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun loadQuestions(difficulty: String) {
        // 获取当前登录用户ID
        val sessionManager = com.zjgsu.soup.game.util.SessionManager.getInstance(this)
        val userId = sessionManager.getUserId() ?: "user123" // 如果没有用户ID，使用默认ID
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.instance.getQuestionsByDifficulty(difficulty)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val questions = response.body() ?: emptyList()
                        if (questions.isNotEmpty()) {
                            // 在这里添加游戏记录，无论游戏是否完成
                            val selectedQuestion = questions.first() // 获取第一个问题作为记录
                            
                            // 记录进入游戏的数据
                            JsonDataUtil.addGameHistory(
                                context = this@GameMainActivity,
                                userId = userId,
                                questionId = selectedQuestion.id,
                                questionTitle = selectedQuestion.title,
                                difficulty = difficulty
                            )
                            
                            navigateToQuestionActivity(questions)
                        } else {
                            Toast.makeText(
                                this@GameMainActivity,
                                "该难度暂无题目",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@GameMainActivity,
                        "加载失败: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun showDifficultyDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_difficulty, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        dialogView.findViewById<Button>(R.id.btnEasy).setOnClickListener {
            loadQuestions("简单")
            dialog.dismiss()
        }

        dialogView.findViewById<Button>(R.id.btnNormal).setOnClickListener {
            loadQuestions("中等")
            dialog.dismiss()
        }

        dialogView.findViewById<Button>(R.id.btnHard).setOnClickListener {
            loadQuestions("困难")
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun navigateToQuestionActivity(questions: List<GameData>) {
        val intent = Intent(this, QuestionActivity::class.java).apply {
            putParcelableArrayListExtra("question_list", ArrayList(questions))
        }
        startActivity(intent)
    }
}