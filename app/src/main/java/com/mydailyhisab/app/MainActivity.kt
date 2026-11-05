package com.mydailyhisab.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.mydailyhisab.app.data.local.AppDatabase
import com.mydailyhisab.app.data.preferences.ThemePreferences
import com.mydailyhisab.app.data.repository.HisabRepository
import com.mydailyhisab.app.ui.navigation.MainAppNavHost
import com.mydailyhisab.app.ui.theme.MyDailyHisabTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        
        // Immersive Full-Screen Mode
        // Completely hides the status bar (time, battery, network, 5G+)
        // Uses BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE so swiping from the edge shows transient bars
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        insetsController.hide(WindowInsetsCompat.Type.statusBars())

        val database = AppDatabase.getInstance(this)
        val repository = HisabRepository(database.transactionDao(), database.categoryDao())
        val themePreferences = ThemePreferences(this)

        setContent {
            // Restore persistent theme from DataStore (Initial/First launch is ALWAYS "light")
            // Never follows system theme automatically.
            val themeMode by themePreferences.themeModeFlow.collectAsState(initial = ThemePreferences.THEME_LIGHT)
            val isDarkMode = themeMode == ThemePreferences.THEME_DARK

            MyDailyHisabTheme(isDarkMode = isDarkMode) {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Main Compose Navigation Container
                    MainAppNavHost(
                        repository = repository,
                        themePreferences = themePreferences,
                        isDarkMode = isDarkMode
                    )
                }
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            insetsController.hide(WindowInsetsCompat.Type.statusBars())
        }
    }
}

