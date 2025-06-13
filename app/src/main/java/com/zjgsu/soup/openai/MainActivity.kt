package com.zjgsu.soup.openai//package com.zjgsu.myapplication
//
//import androidx.appcompat.app.AppCompatActivity
//import android.os.Bundle
//import android.view.View
//import android.widget.EditText
//import android.widget.ProgressBar
//import android.widget.Toast
//import androidx.activity.viewModels
//import androidx.recyclerview.widget.LinearLayoutManager
//import androidx.recyclerview.widget.RecyclerView
//import com.zjgsu.soup.openai.ChatViewModel
//import com.zjgsu.soup.openai.MessageAdapter
//
//class MainActivity : AppCompatActivity() {
//    private val viewModel: ChatViewModel by viewModels()
//    private lateinit var adapter: MessageAdapter
//    private lateinit var recyclerView: RecyclerView
//    private lateinit var etMessage: EditText
//    private lateinit var progressBar: ProgressBar
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_main)
//
//        recyclerView = findViewById(R.id.recyclerView)
//        etMessage = findViewById(R.id.etMessage)
//        progressBar = findViewById(R.id.progressBar)
//
//        adapter = MessageAdapter(emptyList())
//        recyclerView.layoutManager = LinearLayoutManager(this)
//        recyclerView.adapter = adapter
//
//        findViewById<View>(R.id.btnSend).setOnClickListener {
//            val message = etMessage.text.toString()
//            if (message.isNotBlank()) {
//                viewModel.sendMessage(message)
//                etMessage.text.clear()
//            }
//        }
//
//        viewModel.messages.observe(this) { messages ->
//            adapter = MessageAdapter(messages)
//            recyclerView.adapter = adapter
//            recyclerView.scrollToPosition(messages.size - 1)
//        }
//
//        viewModel.isLoading.observe(this) { isLoading ->
//            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
//        }
//
//        viewModel.error.observe(this) { error ->
//            error?.let {
//                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
//            }
//        }
//    }
//}