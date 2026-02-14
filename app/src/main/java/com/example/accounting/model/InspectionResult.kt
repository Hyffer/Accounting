package com.example.accounting.model

data class InspectionResult(
    val timestamp: Long,
    val packageName: String,
    val activityName: String?,
    val rootNode: ViewNodeInfo,
    val totalViewCount: Int
)
