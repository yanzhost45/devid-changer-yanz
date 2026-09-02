package com.warungerik.devidchanger.model

data class MlbbUser(
    val username: String,
    val country: String
)

data class MlbbCheckResult(
    val userId: String,
    val serverId: String,
    val username: String,
    val country: String
)

