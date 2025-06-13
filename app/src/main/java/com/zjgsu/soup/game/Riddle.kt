package com.zjgsu.soup.game
// 存储谜题数据，仅AI可见
data class Riddle(
    val surface: String, // 玩家可见的初始描述（汤面）
    val answer: String   // 仅AI知道的真相（汤底）
)