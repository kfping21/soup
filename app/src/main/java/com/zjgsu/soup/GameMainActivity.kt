package com.zjgsu.soup

import android.annotation.SuppressLint
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.view.animation.RotateAnimation
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.PopupWindow
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GameMainActivity : AppCompatActivity() {

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
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game_main)

        startGameButton = findViewById(R.id.startGameButton)
        loadingStatusText = findViewById(R.id.loadingStatusText)
        settingsButton = findViewById(R.id.settingsButton)
        musicControlButton = findViewById(R.id.musicControlButton) // 新增
        backgroundVideo = findViewById(R.id.backgroundVideo)


        // 初始化音乐播放器
        initMediaPlayer()

        initVideoBackground()
        //初始化视频播放器

        // 初始化旋转动画
        initRotationAnimation()

        // 设置音乐控制按钮点击事件
        musicControlButton.setOnClickListener {
            toggleMusicPlayback()
        }

        startGameButton.setOnClickListener {
            if (gameDataList.isNotEmpty()) {
                navigateToQuestionActivity()
            }
        }

        // 设置按钮点击事件
        settingsButton.setOnClickListener {
            showMusicSettingsPopup(it)
        }

        loadGameDataFromServer()
    }

    private fun initMediaPlayer() {
        mediaPlayer = MediaPlayer.create(this, currentMusicId).apply {
            isLooping = true
            setVolume(1.0f, 1.0f) // 原始值为0.5f，现提升到0.8f
            start()
        }
    }

    private fun initRotationAnimation() {
        rotateAnimation = RotateAnimation(
            0f, 360f,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        ).apply {
            duration = 2000 // 2秒完成一圈
            repeatCount = Animation.INFINITE // 无限循环
            interpolator = LinearInterpolator() // 匀速旋转
        }

        // 开始动画
        musicControlButton.startAnimation(rotateAnimation)
    }
    private fun toggleMusicPlayback() {
        isMusicPlaying = !isMusicPlaying

        if (isMusicPlaying) {
            mediaPlayer.start()
            // 恢复旋转动画
            musicControlButton.startAnimation(rotateAnimation)
        } else {
            mediaPlayer.pause()
            // 停止旋转动画
            musicControlButton.clearAnimation()
        }

        // 更新设置弹窗中的按钮状态（如果弹窗开着）
        updateSettingsPopupIfShowing()
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

    private fun updateSettingsPopupIfShowing() {
        // 如果有设置弹窗显示，更新其中的播放/暂停按钮状态
        try {
            val popup = settingsButton.tag as? PopupWindow
            val toggleButton = popup?.contentView?.findViewById<Button>(R.id.toggleMusicButton)
            toggleButton?.text = if (isMusicPlaying) "暂停音乐" else "播放音乐"
        } catch (e: Exception) {
            // 忽略异常
        }
    }


    private fun showMusicSettingsPopup(anchorView: View) {
        val popupView = layoutInflater.inflate(R.layout.popup_music_settings, null)
        val popupWindow = PopupWindow(
            popupView,
            resources.getDimensionPixelSize(R.dimen.popup_width),
            resources.getDimensionPixelSize(R.dimen.popup_height),
            true
        )

        // 设置背景和动画
        popupWindow.setBackgroundDrawable(ContextCompat.getDrawable(this, R.drawable.popup_background))
        popupWindow.animationStyle = R.style.PopupAnimation

        // 获取Popup中的控件
        val music1Button = popupView.findViewById<Button>(R.id.music1Button)
        val music2Button = popupView.findViewById<Button>(R.id.music2Button)
        val music3Button = popupView.findViewById<Button>(R.id.music3Button)
        val volumeSeekBar = popupView.findViewById<SeekBar>(R.id.volumeSeekBar)
        val toggleMusicButton = popupView.findViewById<Button>(R.id.toggleMusicButton)

        // 设置当前状态
        volumeSeekBar.progress = (currentVolume * 100).toInt()
        toggleMusicButton.text = if (isMusicPlaying) "暂停音乐" else "播放音乐"

        // 设置按钮点击事件
        music1Button.setOnClickListener { changeMusic(R.raw.music1, "第五人格") }
        music2Button.setOnClickListener { changeMusic(R.raw.music2, "庄园") }
        music3Button.setOnClickListener { changeMusic(R.raw.music3, "诡异") }

        // 音量控制
        volumeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                currentVolume = progress / 100f
                mediaPlayer.setVolume(currentVolume, currentVolume)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // 播放/暂停按钮
        toggleMusicButton.setOnClickListener {
            isMusicPlaying = !isMusicPlaying
            if (isMusicPlaying) {
                mediaPlayer.start()
                toggleMusicButton.text = "暂停音乐"
            } else {
                mediaPlayer.pause()
                toggleMusicButton.text = "播放音乐"
            }
        }

        // 显示PopupWindow
        popupWindow.showAsDropDown(anchorView, 0, 0, Gravity.END)
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
        Toast.makeText(this, "已切换至: $musicName", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer.release()
    }

    private fun loadGameDataFromServer() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.instance.getGameData()
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        gameDataList = response.body() ?: emptyList()
                        updateUIAfterLoading()
                    } else {
                        showError("加载失败: ${response.message()}")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showError("网络错误: ${e.message}")
                }
            }
        }
    }

    private fun updateUIAfterLoading() {
        loadingStatusText.text = "成功加载${gameDataList.size}个题目"
        startGameButton.isEnabled = true
        startGameButton.alpha = 1.0f

        Toast.makeText(
            this,
            "已加载${gameDataList.size}个题目，点击开始游戏",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun showError(message: String) {
        loadingStatusText.text = message
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun navigateToQuestionActivity() {
        // 创建Intent并传递数据
        val intent = Intent(this, QuestionActivity::class.java).apply {
            putParcelableArrayListExtra("question_list", ArrayList(gameDataList))
            putExtra("current_index", 0)
        }
        startActivity(intent)
    }
}