package com.signaturelens

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.signaturelens.ui.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test for SignatureLens app startup.
 * Verifies that app launches without crashes.
 */
@RunWith(AndroidJUnit4::class)
class AppStartupTest {
    
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()
    
    @Test
    fun app_launches_successfully() {
        // Wait for UI to be displayed
        // Note: Actual permission dialog may appear, but app shouldn't crash
        composeRule.waitForIdle()
        
        // If permission is granted (in test environment), we should see the app title
        // If denied, we should see the permission rationale
        // Either way, the app should be running
    }
}
