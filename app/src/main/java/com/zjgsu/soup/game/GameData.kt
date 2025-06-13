package com.zjgsu.soup.game

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class GameData(
    val id: Int,
    val title: String,
    val description: String,
    val answer: String,
    val difficulty: String,
    val tags: List<String>? = null // 新增标签字段
) : Parcelable