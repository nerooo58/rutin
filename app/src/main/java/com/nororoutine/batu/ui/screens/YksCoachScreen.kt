package com.nororoutine.batu.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.nororoutine.batu.ui.screens.ders.DersScreen
import com.nororoutine.batu.viewmodel.RutinViewModel

@Composable
fun YksCoachScreen(
    rutinViewModel: RutinViewModel? = null,
    modifier: Modifier = Modifier
) {
    // New completely rewritten Ders section based on the user's document-grounded 5-type question flow
    DersScreen(modifier = modifier)
}
