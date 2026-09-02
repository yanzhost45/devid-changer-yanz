package com.warungerik.devidchanger.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

enum class AppTab(
    val indonesianTitle: String,
    val englishTitle: String,
    val icon: ImageVector
) {
    HOME("Home", "Home", Icons.Default.Home),
    CHECK("Cek ID", "Check ID", Icons.Default.Search),
    CONFIG("Config", "Config", Icons.Default.Bookmark),
    INFO("Info", "Info", Icons.Default.Info),
    SETTINGS("Pengaturan", "Settings", Icons.Default.Settings);

    fun getTitle(language: AppLanguage): String {
        return if (language == AppLanguage.INDONESIAN) indonesianTitle else englishTitle
    }
}
