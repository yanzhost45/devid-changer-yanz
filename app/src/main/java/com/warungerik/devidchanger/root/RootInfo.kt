package com.warungerik.devidchanger.root

data class RootInfo(
    val isRooted: Boolean = false,
    val rootType: RootType = RootType.NONE,
    val rootVersion: String? = null,
    val seLinuxStatus: String = "Unknown",
    val suPath: String? = null
)
