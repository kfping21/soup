package com.zjgsu.soup.game

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.zjgsu.soup.R
import com.zjgsu.soup.game.util.SessionManager

class WarningActivity : AppCompatActivity() {
    private lateinit var confirmButton: TextView
    private lateinit var switchAccountText: TextView  // 新增

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_warning)

        confirmButton = findViewById(R.id.confirmButton)
        switchAccountText = findViewById(R.id.switchAccountText)  // 新增

        // 设置闪烁动画
        val animator = ObjectAnimator.ofFloat(confirmButton, "alpha", 0.2f, 1f).apply {
            duration = 1000
            repeatCount = ObjectAnimator.INFINITE
            repeatMode = ObjectAnimator.REVERSE
        }
        animator.start()

        // 点击整个布局跳转
        findViewById<View>(R.id.mainLayout).setOnClickListener {
            startActivity(Intent(this, LoadingActivity::class.java))
            finish()
        }

        // 新增：点击"切换"跳转回登录界面，并清除登录状态
        switchAccountText.setOnClickListener {
            // 清除登录状态
            val sessionManager = SessionManager.getInstance(applicationContext)
            sessionManager.logout()
            // 跳转到登录页面
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}