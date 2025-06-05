package com.zjgsu.soup

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class WarningActivity : AppCompatActivity() {
    private lateinit var confirmButton: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_warning)

        confirmButton = findViewById(R.id.confirmButton)

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
    }
}