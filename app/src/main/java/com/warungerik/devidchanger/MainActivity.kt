package com.warungerik.devidchanger

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.warungerik.devidchanger.ui.screens.MainScreen
import com.warungerik.devidchanger.ui.theme.DevidChangerTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val uiState by viewModel.uiState.collectAsState()
            DevidChangerTheme(darkTheme = uiState.isDarkMode) {
                MainScreen(viewModel = viewModel)
            }
        }
    }
}
