package com.example.soup2.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.soup2.model.GameHistory
import com.zjgsu.soup.R

class GameHistoryAdapter(private val historyList: List<GameHistory>) : 
    RecyclerView.Adapter<GameHistoryAdapter.HistoryViewHolder>() {
    
    class HistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvQuestionTitle: TextView = itemView.findViewById(R.id.tvQuestionTitle)
        val tvDifficulty: TextView = itemView.findViewById(R.id.tvDifficulty)
        val tvCompletionTime: TextView = itemView.findViewById(R.id.tvCompletionTime)
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_game_history, parent, false)
        return HistoryViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val history = historyList[position]
        holder.tvQuestionTitle.text = history.questionTitle
        holder.tvDifficulty.text = "难度：${history.difficulty}"
        holder.tvCompletionTime.text = history.completionTime
    }
    
    override fun getItemCount(): Int = historyList.size
}