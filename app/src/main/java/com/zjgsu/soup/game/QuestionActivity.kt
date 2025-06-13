package com.zjgsu.soup.game

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.zjgsu.soup.R
import kotlin.random.Random

class QuestionActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var questions: List<GameData>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_question)

        questions = intent.getParcelableArrayListExtra("question_list") ?: emptyList()

        // 初始化RecyclerView
        recyclerView = findViewById(R.id.questionsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = QuestionAdapter(questions) { question ->
            // 点击跳转到问答界面（修正括号）
            startActivity(Intent(this, PrepareActivity::class.java).apply {
                putExtra("question", question)
            })
        }

        // 添加卡片间分割线（修正括号）
        recyclerView.addItemDecoration(
            DividerItemDecoration(this, DividerItemDecoration.VERTICAL).apply {
                setDrawable(ContextCompat.getDrawable(this@QuestionActivity, R.drawable.divider)!!)
            }
        )
    }
}

class QuestionAdapter(
    private val questions: List<GameData>,
    private val onItemClick: (GameData) -> Unit
) : RecyclerView.Adapter<QuestionAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.questionTitle)
        val preview: TextView = view.findViewById(R.id.questionPreview)
        val tags: TextView = view.findViewById(R.id.tagsText)
        val difficulty: TextView = view.findViewById(R.id.difficultyBadge)
        val rating: TextView = view.findViewById(R.id.ratingText)
        val playCount: TextView = view.findViewById(R.id.playCountText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_question_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val question = questions[position]

        holder.title.text = question.title
        holder.preview.text = question.description.take(100) + "..."
        holder.tags.text = question.tags?.joinToString(" ") ?: ""
        holder.difficulty.text = question.difficulty
        val randomRating = String.format("%.1f分", 3.0 + Random.nextFloat() * 2.0)
        holder.rating.text = randomRating // 例如 "4.7分"
        holder.playCount.text = "${Random.nextInt(100, 1000)}人玩过"

        // 设置难度颜色
        holder.difficulty.setTextColor(when(question.difficulty) {
            "简单" -> Color.GREEN
            "中等" -> Color.BLUE
            "困难" -> Color.RED
            else -> Color.GRAY
        })

        holder.itemView.setOnClickListener { onItemClick(question) }
    }

    override fun getItemCount() = questions.size
}