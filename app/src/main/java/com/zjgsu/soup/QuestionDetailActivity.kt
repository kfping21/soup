package com.zjgsu.soup

import android.app.AlertDialog
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class QuestionDetailActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_question_detail)

        val question = intent.getParcelableExtra<GameData>("question")!!

        findViewById<TextView>(R.id.fullQuestionText).text = question.description
        findViewById<Button>(R.id.showAnswerBtn).setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("汤底")
                .setMessage(question.answer)
                .setPositiveButton("明白", null)
                .show()
        }
    }
}