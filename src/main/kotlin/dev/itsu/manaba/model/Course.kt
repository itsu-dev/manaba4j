package dev.itsu.manaba.model

data class Course(
    val title: String,
    val url: String,
    val isNewsAvailable: Boolean,
    val isTestOrQuestionAvailable: Boolean,
    val isReportAvailable: Boolean,
    val isThreadAvailable: Boolean,
    val isIndividualAvailable: Boolean,
    val year: Int,
    val information: String,
    val teachers: List<String>
)