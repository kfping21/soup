package com.zjgsu.soup

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.RotateAnimation
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.PopupWindow
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import android.widget.VideoView
import androidx.appcompat.app.AlertDialog
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
        settingsButton = findViewById(R.id.settingsButton)
        backgroundVideo = findViewById(R.id.backgroundVideo)
        loadingStatusText = findViewById(R.id.loadingStatusText)


        // 初始化音乐播放器
        initMediaPlayer()

        initVideoBackground()
        //初始化视频播放器

        setupBlinkingAnimation()

        startGameButton.setOnClickListener {
            showDifficultyDialog() // 新增方法
        }

        // 设置按钮点击事件
        settingsButton.setOnClickListener {
            showMusicSettingsPopup(it)
        }

        loadGameDataFromServer()
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
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.instance.getQuestionsByDifficulty(difficulty)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val questions = response.body() ?: emptyList()
                        if (questions.isNotEmpty()) {
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