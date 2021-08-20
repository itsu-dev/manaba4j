package dev.itsu.manaba.model

data class Assignment(
    val title: String,
    val url: String,
    val type: String,
    val typeURL: String,
    val course: String,
    val courseURL: String,
    val beginAt: Long,
    val expiredAt: Long
)