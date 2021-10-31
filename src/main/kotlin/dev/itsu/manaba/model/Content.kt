package dev.itsu.manaba.model

data class Content(
    val title: String,
    val contentUrl: String,
    val contentTitle: String,
    val textHtml: String,
    val updatedAt: Long,
    val releasedAt: Long,
    val unReleasedAt: Long,
    val pages: Map<String, String>
)