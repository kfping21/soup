package com.zjgsu.soup.openai//package com.zjgsu.soup.openai
//
//import android.os.Message
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.widget.TextView
//import androidx.recyclerview.widget.RecyclerView
//
//class MessageAdapter(private val messages: List<Message>) : RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {
//
//    class MessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
//        val messageText: TextView = view.findViewById(android.R.id.text1)
//    }
//
//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
//        val view = LayoutInflater.from(parent.context)
//            .inflate(android.R.layout.simple_list_item_1, parent, false)
//        return MessageViewHolder(view)
//    }
//
//    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
//        val message = messages[position]
//        holder.messageText.text = "${message.role}: ${message.content}"
//    }
//
//    override fun getItemCount() = messages.size
//}