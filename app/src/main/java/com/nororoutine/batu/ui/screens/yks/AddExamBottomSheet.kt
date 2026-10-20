package com.nororoutine.batu.ui.screens.yks

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nororoutine.batu.model.ExamSubjectResult
import com.nororoutine.batu.model.YksExamType
import com.nororoutine.batu.ui.theme.AccentPurple
import com.nororoutine.batu.ui.theme.AccentTeal
import com.nororoutine.batu.ui.theme.GlassCardBorderSubtle
import com.nororoutine.batu.ui.theme.PureWhite
import com.nororoutine.batu.ui.theme.SubtitleWhite
import java.time.LocalDate

data class SubjectInputState(
    var subjectName: String,
    var totalQuestions: String,
    var correct: String,
    var incorrect: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExamBottomSheet(
    onDismiss: () -> Unit,
    onSave: (String, String, YksExamType, List<ExamSubjectResult>, String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var examTitle by remember { mutableStateOf("Yeni Deneme Sınavı") }
    var examDate by remember { mutableStateOf(LocalDate.now().toString()) }
    var examType by remember { mutableStateOf(YksExamType.TYT) }
    var notes by remember { mutableStateOf("") }

    val subjects = remember {
        mutableStateListOf(
            SubjectInputState("Türkçe", "40", "30", "5"),
            SubjectInputState("Temel Matematik", "40", "26", "4"),
            SubjectInputState("Fen Bilimleri", "20", "14", "3"),
            SubjectInputState("Sosyal Bilimler", "20", "15", "2")
        )
    }

    val scrollState = rememberScrollState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF121028)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .verticalScroll(scrollState)
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Yeni Deneme Sınavı Ekle",
                        color = PureWhite,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Ders bazında soru, doğru ve yanlışlarını gir",
                        color = SubtitleWhite,
                        fontSize = 11.sp
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Kapat",
                        tint = SubtitleWhite
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Deneme Adı
            OutlinedTextField(
                value = examTitle,
                onValueChange = { examTitle = it },
                label = { Text("Deneme Adı / Yayın (örn: 3D TYT-1, Özdebir)", color = SubtitleWhite) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentTeal,
                    unfocusedBorderColor = GlassCardBorderSubtle,
                    focusedTextColor = PureWhite,
                    unfocusedTextColor = PureWhite
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Tarih ve Sınav Türü
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = examDate,
                    onValueChange = { examDate = it },
                    label = { Text("Tarih (YYYY-AA-GG)", color = SubtitleWhite) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentTeal,
                        unfocusedBorderColor = GlassCardBorderSubtle,
                        focusedTextColor = PureWhite,
                        unfocusedTextColor = PureWhite
                    )
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x33A78BFA))
                        .border(1.dp, AccentPurple.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .clickable {
                            val types = YksExamType.values()
                            val nextIndex = (examType.ordinal + 1) % types.size
                            examType = types[nextIndex]
                        }
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Sınav Türü",
                            color = SubtitleWhite,
                            fontSize = 10.sp
                        )
                        Text(
                            text = examType.name,
                            color = PureWhite,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // DERSLER LİSTESİ
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Dersler & Net Dökümü",
                    color = AccentTeal,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(AccentTeal.copy(alpha = 0.2f))
                        .border(1.dp, AccentTeal.copy(alpha = 0.5f), RoundedCornerShape(999.dp))
                        .clickable {
                            subjects.add(SubjectInputState("Yeni Ders", "20", "10", "2"))
                        }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = null,
                            tint = AccentTeal,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Ders Ekle",
                            color = AccentTeal,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            subjects.forEachIndexed { index, sub ->
                val qCount = sub.totalQuestions.toIntOrNull() ?: 0
                val corr = sub.correct.toIntOrNull() ?: 0
                val incorr = sub.incorrect.toIntOrNull() ?: 0
                val empty = (qCount - corr - incorr).coerceAtLeast(0)
                val net = (corr.toFloat() - (incorr.toFloat() * 0.25f)).coerceAtLeast(0f)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1B1834))
                        .border(1.dp, GlassCardBorderSubtle, RoundedCornerShape(12.dp))
                        .padding(10.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = sub.subjectName,
                                onValueChange = { sub.subjectName = it },
                                label = { Text("Ders", color = SubtitleWhite, fontSize = 10.sp) },
                                modifier = Modifier.weight(1.3f),
                                shape = RoundedCornerShape(8.dp),
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, color = PureWhite),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = AccentTeal,
                                    unfocusedBorderColor = GlassCardBorderSubtle,
                                    focusedTextColor = PureWhite,
                                    unfocusedTextColor = PureWhite
                                ),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.width(6.dp))

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0x3306B6D4))
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "Net: %.2f".format(net),
                                    color = AccentTeal,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }

                            if (subjects.size > 1) {
                                IconButton(
                                    onClick = { subjects.removeAt(index) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.DeleteOutline,
                                        contentDescription = "Sil",
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedTextField(
                                value = sub.totalQuestions,
                                onValueChange = { sub.totalQuestions = it },
                                label = { Text("Soru", color = SubtitleWhite, fontSize = 9.sp) },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = RoundedCornerShape(8.dp),
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, color = PureWhite),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = AccentTeal,
                                    unfocusedBorderColor = GlassCardBorderSubtle,
                                    focusedTextColor = PureWhite,
                                    unfocusedTextColor = PureWhite
                                )
                            )

                            OutlinedTextField(
                                value = sub.correct,
                                onValueChange = { sub.correct = it },
                                label = { Text("Doğru", color = Color(0xFF10B981), fontSize = 9.sp) },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = RoundedCornerShape(8.dp),
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, color = PureWhite),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF10B981),
                                    unfocusedBorderColor = GlassCardBorderSubtle,
                                    focusedTextColor = PureWhite,
                                    unfocusedTextColor = PureWhite
                                )
                            )

                            OutlinedTextField(
                                value = sub.incorrect,
                                onValueChange = { sub.incorrect = it },
                                label = { Text("Yanlış", color = Color(0xFFEF4444), fontSize = 9.sp) },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = RoundedCornerShape(8.dp),
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, color = PureWhite),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFFEF4444),
                                    unfocusedBorderColor = GlassCardBorderSubtle,
                                    focusedTextColor = PureWhite,
                                    unfocusedTextColor = PureWhite
                                )
                            )

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0x1AFFFFFF))
                                    .padding(vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Boş", color = SubtitleWhite, fontSize = 9.sp)
                                    Text("$empty", color = PureWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Notlar
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Kişisel Notların (Süre, zorlanılan konular vb.)", color = SubtitleWhite) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentTeal,
                    unfocusedBorderColor = GlassCardBorderSubtle,
                    focusedTextColor = PureWhite,
                    unfocusedTextColor = PureWhite
                ),
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Kaydet Butonu
            Button(
                onClick = {
                    val results = subjects.map { sub ->
                        val qCount = sub.totalQuestions.toIntOrNull() ?: 0
                        val corr = sub.correct.toIntOrNull() ?: 0
                        val incorr = sub.incorrect.toIntOrNull() ?: 0
                        val empty = (qCount - corr - incorr).coerceAtLeast(0)
                        val net = (corr.toFloat() - (incorr.toFloat() * 0.25f)).coerceAtLeast(0f)
                        ExamSubjectResult(
                            subjectName = sub.subjectName.ifBlank { "Ders" },
                            correct = corr,
                            incorrect = incorr,
                            empty = empty,
                            totalQuestions = qCount,
                            net = net
                        )
                    }
                    onSave(examTitle, examDate, examType, results, notes)
                },
                colors = ButtonDefaults.buttonColors(containerColor = AccentTeal),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Kaydet & Analizi Başlat",
                    color = Color.Black,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
