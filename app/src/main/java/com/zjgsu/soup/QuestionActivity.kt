package com.zjgsu.soup

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide

class QuestionActivity : AppCompatActivity() {

    private lateinit var currentQuestion: GameData
    private var currentIndex = 0
    private lateinit var questions: List<GameData>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_question)

        // 获取传递的数据
        questions = intent.getParcelableArrayListExtra<GameData>("question_list") ?: emptyList()
        currentIndex = intent.getIntExtra("current_index", 0)

        if (questions.isNotEmpty()) {
            showQuestion(currentIndex)
            setupNavigationButtons()
        } else {
            Toast.makeText(this, "没有可用的题目", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun showQuestion(index: Int) {
        if (index in 0 until questions.size) {
            currentQuestion = questions[index]
            // 更新UI显示题目
            findViewById<TextView>(R.id.questionTitle).text = currentQuestion.title
            findViewById<TextView>(R.id.questionDescription).text = currentQuestion.description
            // 使用Glide加载图片
            Glide.with(this)
                .load(currentQuestion.imageUrl)
                .into(findViewById(R.id.questionImage))
        }
    }

    private fun setupNavigationButtons() {
        findViewById<Button>(R.id.nextButton).setOnClickListener {
            if (currentIndex < questions.size - 1) {
                showQuestion(++currentIndex)
            } else {
                Toast.makeText(this, "已经是最后一题", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<Button>(R.id.prevButton).setOnClickListener {
            if (currentIndex > 0) {
                showQuestion(--currentIndex)
            }
        }

        findViewById<Button>(R.id.showAnswerButton).setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("答案")
                .setMessage(currentQuestion.answer)
                .setPositiveButton("确定", null)
                .show()
        }
    }
}