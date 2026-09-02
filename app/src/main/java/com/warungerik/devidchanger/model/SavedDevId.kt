package com.warungerik.devidchanger.model

import java.util.UUID

data class SavedDevId(
    val id: String = UUID.randomUUID().toString(),
    val label: String,
    val devId: String,
    val timestamp: Long = System.currentTimeMillis()
)
