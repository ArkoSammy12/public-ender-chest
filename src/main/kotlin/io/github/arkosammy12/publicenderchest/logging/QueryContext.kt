package io.github.arkosammy12.publicenderchest.logging

data class QueryContext @JvmOverloads constructor(
    val timeQueryType: TimeQueryType,
    val days: Int,
    val hours: Int = 0,
    val minutes: Int = 0,
    val seconds: Int = 0,
    val playerName: String? = null,
)
