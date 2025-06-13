package com.zjgsu.soup.game

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.zjgsu.soup.R

class PrepareActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_prepare)

        val question = intent.getParcelableExtra<GameData>("question")!!

        // 设置标题和内容（文字已在布局中设为白色）
        findViewById<TextView>(R.id.titleTextView).text = question.title
        findViewById<TextView>(R.id.contentTextView).text = question.description

        // 返回按钮（蓝色）
        findViewById<Button>(R.id.backButton).setOnClickListener {
            finish() // 返回题库界面
        }

        // 准备按钮（红色）
        findViewById<Button>(R.id.startButton).setOnClickListener {
            // 记录游玩历史 - 在点击进入故事时就记录，而不是等到完成
            recordGameEntered(question)

            // 跳转至故事详情页面
            startActivity(Intent(this, QuestionDetailActivity::class.java).apply {
                putExtra("question", question)
            })
            finish() // 关闭准备界面
        }
    }

    // 添加记录游玩历史的方法
    private fun recordGameEntered(question: GameData) {
        // 记录游玩历史
        JsonDataUtil.addGameHistory(
            context = this,
            userId = "user123", // 这里可以替换为实际的用户ID
            questionId = question.id,
            questionTitle = question.title,
            difficulty = question.difficulty
        )
    }
}