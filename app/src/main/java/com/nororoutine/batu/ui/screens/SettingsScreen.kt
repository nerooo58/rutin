package com.nororoutine.batu.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nororoutine.batu.ui.components.PulsingDot
import com.nororoutine.batu.ui.components.springPress
import com.nororoutine.batu.ui.theme.AccentPurple
import com.nororoutine.batu.ui.theme.AccentTeal
import com.nororoutine.batu.ui.theme.ChipSelectedBg
import com.nororoutine.batu.ui.theme.ChipUnselectedBg
import com.nororoutine.batu.ui.theme.DoneGreen
import com.nororoutine.batu.ui.theme.GlassCardBackground
import com.nororoutine.batu.ui.theme.GlassCardBorderSubtle
import com.nororoutine.batu.ui.theme.MissedCardBg
import com.nororoutine.batu.ui.theme.MissedCardBorder
import com.nororoutine.batu.ui.theme.MissedRed
import com.nororoutine.batu.ui.theme.MutedWhite
import com.nororoutine.batu.ui.theme.PureWhite
import com.nororoutine.batu.ui.theme.SubtitleWhite
import com.nororoutine.batu.viewmodel.RutinUiState
import com.nororoutine.batu.viewmodel.RutinViewModel

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

@Composable
fun SettingsScreen(
    viewModel: RutinViewModel,
    uiState: RutinUiState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showResetDialog by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                viewModel.setNotificationsEnabled(true)
            }
        }
    )

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp)
        ) {
            item {
                Text(
                    text = "Ayarlar",
                    color = PureWhite,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Uygulama tercihleri ve asistan bağlantısı",
                    color = MutedWhite,
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(24.dp))
            }

            // Section 1: Bildirimler
            item {
                SectionHeader(title = "Bildirimler", icon = Icons.Rounded.Notifications)
                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(GlassCardBackground)
                        .border(1.dp, GlassCardBorderSubtle, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Rutin Hatırlatıcıları",
                                    color = PureWhite,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Zamanı geldiğinde bildirim gönder",
                                    color = SubtitleWhite,
                                    fontSize = 12.sp
                                )
                            }

                            Switch(
                                checked = uiState.notificationsEnabled,
                                onCheckedChange = { isChecked ->
                                    if (isChecked) {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                                            != PackageManager.PERMISSION_GRANTED
                                        ) {
                                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        } else {
                                            viewModel.setNotificationsEnabled(true)
                                        }
                                    } else {
                                        viewModel.setNotificationsEnabled(false)
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = PureWhite,
                                    checkedTrackColor = AccentPurple,
                                    uncheckedThumbColor = SubtitleWhite,
                                    uncheckedTrackColor = Color(0x331E3250)
                                )
                            )
                        }

                        if (uiState.notificationsEnabled) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Hatırlatma Vakti",
                                color = SubtitleWhite,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            val reminderOptions = listOf(
                                0 to "Tam Vaktinde",
                                5 to "5 dk önce",
                                15 to "15 dk önce",
                                30 to "30 dk önce"
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                reminderOptions.forEach { (mins, label) ->
                                    val isSelected = uiState.reminderMinutes == mins
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isSelected) ChipSelectedBg else ChipUnselectedBg)
                                            .border(
                                                1.dp,
                                                if (isSelected) AccentTeal else GlassCardBorderSubtle,
                                                RoundedCornerShape(10.dp)
                                            )
                                            .clickable { viewModel.setReminderMinutes(mins) }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            color = if (isSelected) AccentTeal else SubtitleWhite,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }

            // Section 2: Noro Bağlantısı
            item {
                SectionHeader(title = "Noro Bağlantısı", icon = Icons.Rounded.AutoAwesome)
                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(GlassCardBackground)
                        .border(1.dp, GlassCardBorderSubtle, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Column {
                        val isConnected = uiState.isFirebaseConnected
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                PulsingDot(
                                    color = AccentTeal,
                                    dotSize = 7.dp,
                                    pulseMaxScale = 2.0f
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Noro AI Asistanı",
                                    color = PureWhite,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Connection Badge: dynamically based on uiState.isFirebaseConnected
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(if (isConnected) Color(0x2210B981) else Color(0x22F87171))
                                    .border(
                                        1.dp,
                                        if (isConnected) Color(0x4410B981) else Color(0x44F87171),
                                        RoundedCornerShape(999.dp)
                                    )
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (isConnected) "Firebase Bağlı" else "Firebase Bağlı Değil",
                                    color = if (isConnected) DoneGreen else MissedRed,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = if (isConnected) {
                                "Rutinleriniz ve kayıtlarınız gerçek zamanlı olarak Firestore bulutuyla eşitleniyor. Noro asistanı ile paylaşıma hazır."
                            } else {
                                "Firebase bağlantısı bekleniyor. Rutinleriniz yerel önbellekte tutuluyor."
                            },
                            color = SubtitleWhite,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Sync,
                                contentDescription = null,
                                tint = MutedWhite,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Son eşitleme: ${uiState.lastSyncText}",
                                color = MutedWhite,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }

            // Section 4: Veri Yönetimi
            item {
                SectionHeader(title = "Veri", icon = Icons.Rounded.DeleteForever)
                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MissedCardBg)
                        .border(1.dp, MissedCardBorder, RoundedCornerShape(16.dp))
                        .clickable { showResetDialog = true }
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Tüm veriyi sıfırla",
                                color = MissedRed,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Oluşturulan tüm rutinleri ve günlük kayıtlarını siler",
                                color = SubtitleWhite,
                                fontSize = 12.sp
                            )
                        }

                        Icon(
                            imageVector = Icons.Rounded.DeleteForever,
                            contentDescription = "Sıfırla",
                            tint = MissedRed,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }

        // Reset Data Dialog
        if (showResetDialog) {
            AlertDialog(
                onDismissRequest = { showResetDialog = false },
                containerColor = Color(0xFF16132C),
                titleContentColor = PureWhite,
                textContentColor = SubtitleWhite,
                title = {
                    Text(text = "Tüm Verileri Sıfırla", fontWeight = FontWeight.Bold)
                },
                text = {
                    Text(text = "Tüm rutinler ve geçmiş kayıtlarınız temizlenecektir. Devam etmek istiyor musunuz?")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.resetAllData()
                            showResetDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MissedRed)
                    ) {
                        Text(text = "Sıfırla", color = PureWhite, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showResetDialog = false }) {
                        Text(text = "İptal", color = PureWhite)
                    }
                }
            )
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = AccentPurple,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = title,
            color = PureWhite,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
