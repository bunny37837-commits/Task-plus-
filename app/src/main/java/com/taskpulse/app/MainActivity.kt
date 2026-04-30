package com.taskpulse.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.taskpulse.app.data.datastore.AppDataStore
import com.taskpulse.app.presentation.navigation.TaskPulseNavGraph
import com.taskpulse.app.presentation.ui.theme.TaskPulseTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var appDataStore: AppDataStore

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val darkTheme = appDataStore.darkThemeFlow.collectAsStateWithLifecycle(initialValue = true).value
            TaskPulseTheme(darkTheme = darkTheme) {
                TaskPulseNavGraph()
            }
        }
    }
}
