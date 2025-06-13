package com.zjgsu.soup.game

import android.app.AlertDialog
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.WindowCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.zjgsu.soup.R
import com.zjgsu.soup.openai.AIChatManager
import com.zjgsu.soup.game.ChatMessage as ChatMessage

class QuestionDetailActivity : AppCompatActivity() {

    private lateinit var chatRecyclerView: RecyclerView
    private lateinit var chatAdapter: ChatAdapter
    private val chatMessages = mutableListOf<ChatMessage>()
    private lateinit var aiChatManager: AIChatManager
    private lateinit var currentRiddle: Riddle
    private val clueHistory = mutableListOf<Pair<String, String>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_question_detail)

        val question = intent.getParcelableExtra<GameData>("question") ?: return
        currentRiddle = Riddle(
            surface = question.description,
            answer = question.answer
        )

        WindowCompat.setDecorFitsSystemWindows(window, false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isStatusBarContrastEnforced = false
        }

        aiChatManager = AIChatManager()

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        toolbar.title = question.title

        val questionText = findViewById<TextView>(R.id.fullQuestionText)
        val completeStoryBtn = findViewById<Button>(R.id.completeStoryBtn)
        val sendBtn = findViewById<Button>(R.id.sendBtn)
        val clueBtn = findViewById<Button>(R.id.clueBtn)
        val inputField = findViewById<EditText>(R.id.inputField)

        questionText.text = question.description

        chatRecyclerView = findViewById(R.id.chatRecyclerView)
        chatRecyclerView.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        chatAdapter = ChatAdapter(chatMessages)
        chatRecyclerView.adapter = chatAdapter

        aiChatManager.messages.observe(this) { messages ->
            chatMessages.clear()
            chatMessages.addAll(messages)
            chatAdapter.notifyDataSetChanged()
            chatRecyclerView.smoothScrollToPosition(chatMessages.size - 1)

            clueHistory.clear()
            messages.forEachIndexed { index, message ->
                if (!message.isUser && index > 0) {
                    val question = messages[index - 1].text
                    clueHistory.add(Pair(question, message.text))
                }
            }
        }

        aiChatManager.isLoading.observe(this) { isLoading ->
            sendBtn.isEnabled = !isLoading
            completeStoryBtn.isEnabled = !isLoading
            clueBtn.isEnabled = !isLoading
            sendBtn.text = if (isLoading) "发送中..." else "发送"
        }

        aiChatManager.error.observe(this) { error ->
            error?.let { Toast.makeText(this, it, Toast.LENGTH_SHORT).show() }
        }

        completeStoryBtn.setOnClickListener {
            showCompleteStoryDialog(question.answer)
        }

        sendBtn.setOnClickListener {
            val message = inputField.text.toString()
            if (message.isNotEmpty()) {
                aiChatManager.sendMessage(message, "当前故事背景:\n${questionText.text}")
                inputField.text.clear()
            }
        }

        clueBtn.setOnClickListener {
            showClueHistory()
        }

        val systemMessage = """
        你是一个海龟汤游戏的主持人，必须严格遵守以下规则：
        1. 已知汤面：「${currentRiddle.surface}」
        2. 已知汤底（必须保密）：「${currentRiddle.answer}」
        
        回答规则：
        - 只能回答以下四种之一：
          ○ "是"（完全符合汤底事实）
          ○ "不是"（完全不符合汤底事实）
          ○ "是也不是"（部分符合，需满足一下条件之一）
            *问题包含多个子问题且答案矛盾
            *问题涉及模糊的时间/角色状态变化
          ○ "不重要"（问题与汤底核心逻辑无关）
        - 禁止解释原因，禁止透露汤底内容。
        - 回答超出四种预设类型
        
        ### 死亡判定强制规则：
        1. 当问题包含以下关键词时，必须回答"是"：
          - 死亡/尸体相关：死了、死亡、自杀、杀死、尸体、腐烂、谋杀、去世、丧命
          - 行为描述：杀死了自己、结束生命、自尽、了断自己
          - 状态描述：已死、变成尸体、没有生命体征

        2. 特别注意以下情形必须判"是"：
          - "男友死了吗？" → 是（因明确描述自杀）
          - "妈妈还活着吗？" → 不是（因明确描述死亡）
          - "这是尸体吗？" → 是（当汤底涉及尸体时）

        ### 标准回答规则：
          - 只能回答："是"、"不是"、"是也不是"、"不重要"
          - 禁止解释原因或透露汤底细节

        ### 死亡关键词示例：
          用户：有人死了吗？ → 你：是
          用户：这是谋杀吗？ → 你：是
          用户：他杀死了自己？ → 你：是
        """.trimIndent()

        aiChatManager.sendSystemMessage(systemMessage)
    }

    private fun showCompleteStoryDialog(correctAnswer: String) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_complete_story, null)
        val answerInput = dialogView.findViewById<EditText>(R.id.answerInput)

        AlertDialog.Builder(this)
            .setView(dialogView)
            .setTitle("补全故事")
            .setPositiveButton("提交答案") { _, _ ->
                answerInput.text.toString().takeIf { it.isNotEmpty() }?.let { userAnswer ->
                    aiChatManager.sendSilentMessage("""
                        用户提交的答案：$userAnswer
                        标准汤底：$correctAnswer
                        
                        请做以下事情：
                        1. 评估用户答案与标准汤底的匹配度（1-100分）
                        2. 提供简短的反馈，说明为什么匹配或不匹配
                        3. 最后揭示标准汤底
                    """.trimIndent())
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showClueHistory() {
        val htmlContent = """
        <html>
        <head>
            <style>
                table {
                    width: 100%;
                    border-collapse: collapse;
                    margin: 10px 0;
                }
                th {
                    background-color: #333;
                    color: white;
                    padding: 12px;
                    text-align: left;
                }
                td {
                    padding: 12px;
                    border-bottom: 1px solid #ddd;
                }
                .question {
                    color: #FFD700;
                    font-weight: bold;
                }
                .answer {
                    color: #00FF00;
                }
                .star {
                    color: #FF0000;
                    font-weight: bold;
                }
                .no-clues {
                    text-align: center;
                    padding: 20px;
                    color: #888;
                }
            </style>
        </head>
        <body>
            <table>
                <tr>
                    <th>序号</th>
                    <th>问题</th>
                    <th>回答</th>
                </tr>
    """.trimIndent()

        val contentBuilder = StringBuilder(htmlContent)

        if (clueHistory.isEmpty()) {
            contentBuilder.append("""
            <tr>
                <td colspan="3" class="no-clues">暂无历史线索</td>
            </tr>
        """.trimIndent())
        } else {
            clueHistory.forEachIndexed { index, (question, answer) ->
                contentBuilder.append("""
                <tr>
                    <td>${index + 1}.</td>
                    <td class="question">${Html.escapeHtml(question)}</td>
                    <td class="answer">${if (answer.contains("★")) "<span class='star'>★</span>" else ""}${Html.escapeHtml(answer.replace("★", ""))}</td>
                </tr>
            """.trimIndent())
            }
        }

        contentBuilder.append("""
            </table>
        </body>
        </html>
    """.trimIndent())

        // 使用 WebView 来显示带样式的 HTML
        val webView = WebView(this).apply {
            loadDataWithBaseURL(
                null,
                contentBuilder.toString(),
                "text/html",
                "UTF-8",
                null
            )
            settings.javaScriptEnabled = false
        }

        AlertDialog.Builder(this)
            .setTitle("历史线索")
            .setView(webView)
            .setPositiveButton("确定", null)
            .show()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}

class ChatAdapter(private val messages: List<ChatMessage>) :
    RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val layout = if (viewType == 0) R.layout.item_user_message else R.layout.item_ai_message
        val view = LayoutInflater.from(parent.context).inflate(layout, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        holder.bind(messages[position])
    }

    override fun getItemCount() = messages.size

    override fun getItemViewType(position: Int) = if (messages[position].isUser) 0 else 1

    class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageText = itemView.findViewById<TextView>(R.id.messageText)
        fun bind(message: ChatMessage) {
            messageText.text = message.text
        }
    }
}