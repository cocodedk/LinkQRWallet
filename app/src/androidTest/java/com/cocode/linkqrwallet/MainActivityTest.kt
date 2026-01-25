package com.cocode.linkqrwallet

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

class MainActivityTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun appLaunches_andShowsLibraryUi() {
        composeRule.onNodeWithText("Link QR Wallet").assertIsDisplayed()
        composeRule.onNodeWithText("Search").assertIsDisplayed()
    }
}
