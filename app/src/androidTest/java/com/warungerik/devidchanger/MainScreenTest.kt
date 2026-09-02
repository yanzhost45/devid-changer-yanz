package com.warungerik.devidchanger

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.lifecycle.viewmodel.compose.viewModel
import com.warungerik.devidchanger.ui.screens.MainScreen
import com.warungerik.devidchanger.ui.theme.DevidChangerTheme
import org.junit.Rule
import org.junit.Test

class MainScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun mainScreen_rendersWithoutCrash() {
        composeTestRule.setContent {
            DevidChangerTheme {
                val viewModel: MainViewModel = viewModel()
                MainScreen(viewModel = viewModel)
            }
        }
        composeTestRule.onNodeWithText("DEVID CHANGER").assertExists()
    }
}
