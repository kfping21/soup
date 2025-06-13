package com.zjgsu.soup.game

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.zjgsu.soup.R

class LoadingActivity : AppCompatActivity() {
    private lateinit var progressBar: ProgressBar
    private lateinit var loadingText: TextView
    private var progressStatus = 0
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_loading)

        progressBar = findViewById(R.id.loadingProgressBar)
        loadingText = findViewById(R.id.loadingTextView)

        // 模拟5秒加载过程
        Thread(Runnable {
            while (progressStatus < 100) {
                progressStatus += 1
                // 更新进度条
                handler.post {
                    progressBar.progress = progressStatus
                    if (progressStatus == 100) {
                        loadingText.text = "加载成功"
                        // 加载完成后跳转到主界面
                        Handler(Looper.getMainLooper()).postDelayed({
                            startActivity(Intent(this@LoadingActivity, GameMainActivity::class.java))
                            finish()
                        }, 1000)
                    }
                }
                try {
                    // 5秒完成加载 (100步*50毫秒=5000毫秒)
                    Thread.sleep(50)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            }
        }).start()
    }
}