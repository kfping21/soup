package com.zjgsu.soup

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class GameData(
    val id: Int,
    val title: String,
    val description: String,
    val answer: String,
    val difficulty: String,
    val imageUrl: String
) : Parcelable