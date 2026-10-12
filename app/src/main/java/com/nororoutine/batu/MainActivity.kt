package com.nororoutine.batu

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.nororoutine.batu.ui.MainScreen
import com.nororoutine.batu.ui.theme.RutinTheme
import com.nororoutine.batu.viewmodel.RutinViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: RutinViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleNotificationIntent(intent)
        setContent {
            RutinTheme {
                MainScreen(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationIntent(intent)
    }

    private fun handleNotificationIntent(intent: Intent?) {
        if (intent == null) return
        val targetTab = intent.getIntExtra("EXTRA_NAVIGATE_TAB", -1)
        if (targetTab >= 0) {
            viewModel.selectTab(targetTab)
        }
    }
}
