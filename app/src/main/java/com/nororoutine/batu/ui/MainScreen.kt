package com.nororoutine.batu.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nororoutine.batu.ui.components.AmbientBackground
import com.nororoutine.batu.ui.components.RutinBottomBar
import com.nororoutine.batu.ui.screens.EditScreen
import com.nororoutine.batu.ui.screens.HistoryScreen
import com.nororoutine.batu.ui.screens.SettingsScreen
import com.nororoutine.batu.ui.screens.TodayScreen
import com.nororoutine.batu.viewmodel.RutinViewModel

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.nororoutine.batu.notification.NotificationScheduler

@Composable
fun MainScreen(
    viewModel: RutinViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                NotificationScheduler.rescheduleAll(
                    context = context,
                    routines = uiState.routines,
                    isGlobalEnabled = uiState.notificationsEnabled
                )
            }
        }
    )

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    var showSplash by remember { androidx.compose.runtime.mutableStateOf(true) }

    // App launch entrance animation: fade + scale up entrance with spring physics
    val launchScale = remember { Animatable(0.94f) }
    val launchAlpha = remember { Animatable(0f) }

    LaunchedEffect(showSplash) {
        if (!showSplash) {
            launchAlpha.animateTo(
                targetValue = 1f,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
            )
        }
    }
    LaunchedEffect(showSplash) {
        if (!showSplash) {
            launchScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
    }

    if (showSplash) {
        com.nororoutine.batu.ui.screens.SplashScreen(
            onSplashFinished = {
                showSplash = false
            }
        )
    } else {
        AmbientBackground {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = launchScale.value
                        scaleY = launchScale.value
                        alpha = launchAlpha.value
                    }
            ) {
            Scaffold(
                containerColor = Color.Transparent,
                bottomBar = {
                    RutinBottomBar(
                        currentTab = uiState.currentTab,
                        onTabSelected = { tabIndex ->
                            viewModel.selectTab(tabIndex)
                        }
                    )
                }
            ) { innerPadding ->
                androidx.activity.compose.BackHandler(enabled = uiState.currentTab != 0) {
                    viewModel.selectTab(0)
                }

                AnimatedContent(
                    targetState = uiState.currentTab,
                    transitionSpec = {
                        val isMovingForward = targetState > initialState
                        val slideOffset = { width: Int -> if (isMovingForward) width else -width }
                        val exitOffset = { width: Int -> if (isMovingForward) -width else width }

                        (slideInHorizontally(
                            initialOffsetX = slideOffset,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        ) + fadeIn(
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                        )).togetherWith(
                            slideOutHorizontally(
                                targetOffsetX = exitOffset,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                    stiffness = Spring.StiffnessMediumLow
                                )
                            ) + fadeOut(
                                animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                            )
                        )
                    },
                    modifier = Modifier.padding(innerPadding),
                    label = "tabTransition"
                ) { tabIndex ->
                    when (tabIndex) {
                        0 -> TodayScreen(
                            viewModel = viewModel,
                            uiState = uiState,
                            onNavigateToEdit = { viewModel.selectTab(1) },
                            onNavigateToYksCoach = { viewModel.selectTab(2) }
                        )
                        1 -> EditScreen(
                            viewModel = viewModel,
                            uiState = uiState
                        )
                        2 -> com.nororoutine.batu.ui.screens.YksCoachScreen()
                        3 -> HistoryScreen(
                            viewModel = viewModel,
                            uiState = uiState
                        )
                        4 -> SettingsScreen(
                            viewModel = viewModel,
                            uiState = uiState
                        )
                    }
                }
            }
        }
    }
}
}
