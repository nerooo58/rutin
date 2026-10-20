package com.nororoutine.batu.ui.screens.yks

import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Quiz
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nororoutine.batu.model.QuizQuestion
import com.nororoutine.batu.model.StudySession
import com.nororoutine.batu.ui.theme.AccentPurple
import com.nororoutine.batu.ui.theme.AccentTeal
import com.nororoutine.batu.ui.theme.GlassCardBorderSubtle
import com.nororoutine.batu.ui.theme.PureWhite
import com.nororoutine.batu.ui.theme.SubtitleWhite
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

@Composable
fun StudySectionView(
    pastStudies: List<StudySession>,
    activeSession: StudySession?,
    currentQuestionIndex: Int,
    selectedAnswers: Map<String, Int>,
    revealedHintIds: Set<String>,
    isGenerating: Boolean,
    isAnalyzing: Boolean,
    onStartQuiz: (String, Int, String?, String?, String?) -> Unit,
    onSelectAnswer: (String, Int) -> Unit,
    onRevealHint: (String) -> Unit,
    onNextQuestion: () -> Unit,
    onPreviousQuestion: () -> Unit,
    onSetQuestionIndex: (Int) -> Unit,
    onFinishQuiz: () -> Unit,
    onViewPastStudy: (StudySession) -> Unit,
    onDeletePastStudy: (String) -> Unit,
    onCloseActiveStudy: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (activeSession == null) {
        // --- VIEW A: CREATE QUIZ FROM RESOURCE & PAST STUDIES LIST ---
        StudyHomeView(
            pastStudies = pastStudies,
            isGenerating = isGenerating,
            onStartQuiz = onStartQuiz,
            onViewPastStudy = onViewPastStudy,
            onDeletePastStudy = onDeletePastStudy,
            modifier = modifier
        )
    } else if (!activeSession.isCompleted) {
        // --- VIEW B: ACTIVE QUIZ (SINGLE QUESTION AT A TIME) ---
        ActiveQuizView(
            session = activeSession,
            currentIndex = currentQuestionIndex,
            selectedAnswers = selectedAnswers,
            revealedHintIds = revealedHintIds,
            isAnalyzing = isAnalyzing,
            onSelectAnswer = onSelectAnswer,
            onRevealHint = onRevealHint,
            onNextQuestion = onNextQuestion,
            onPreviousQuestion = onPreviousQuestion,
            onSetQuestionIndex = onSetQuestionIndex,
            onFinishQuiz = onFinishQuiz,
            onCancelQuiz = onCloseActiveStudy,
            modifier = modifier
        )
    } else {
        // --- VIEW C: COMPLETED QUIZ RESULTS & AI TOPIC ANALYSIS ---
        StudyResultsView(
            session = activeSession,
            onNewStudy = onCloseActiveStudy,
            modifier = modifier
        )
    }
}

// -------------------------------------------------------------
// VIEW A: CREATE QUIZ (IMAGE/DOC UPLOAD + DESCRIPTION + HISTORY)
// -------------------------------------------------------------
@Composable
fun StudyHomeView(
    pastStudies: List<StudySession>,
    isGenerating: Boolean,
    onStartQuiz: (String, Int, String?, String?, String?) -> Unit,
    onViewPastStudy: (StudySession) -> Unit,
    onDeletePastStudy: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var imageBase64 by remember { mutableStateOf<String?>(null) }
    var descriptionText by remember { mutableStateOf("Bunun gibi başka örnek soru sor") }
    var docNotesText by remember { mutableStateOf("") }
    var questionCount by remember { mutableIntStateOf(5) }
    var showDocInput by remember { mutableStateOf(false) }

    // Android Photo Picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        selectedImageUri = uri
        if (uri != null) {
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    if (bitmap != null) {
                        val outputStream = ByteArrayOutputStream()
                        // Compress to JPEG for lightweight API transport
                        bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, outputStream)
                        val bytes = outputStream.toByteArray()
                        val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                        withContext(Dispatchers.Main) {
                            imageBase64 = b64
                        }
                    }
                } catch (e: Exception) {
                    // Ignore decode failure
                }
            }
        } else {
            imageBase64 = null
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Title banner
        item {
            Column(modifier = Modifier.padding(top = 4.dp)) {
                Text(
                    text = "Kaynak Yükle & 5 Şıklı Quiz Hazırla",
                    color = PureWhite,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Soru fotoğrafı, not veya açıklama gir; yapay zeka sana özel 5 şıklı test hazırlasın.",
                    color = SubtitleWhite,
                    fontSize = 11.sp
                )
            }
        }

        // Image / Resource Upload Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF191630))
                    .border(1.dp, GlassCardBorderSubtle, RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "1. Kaynak veya Soru Fotoğrafı",
                            color = PureWhite,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )

                        if (selectedImageUri != null) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(Color(0xFFEF4444).copy(alpha = 0.2f))
                                    .clickable {
                                        selectedImageUri = null
                                        imageBase64 = null
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "Fotoğrafı Kaldır",
                                    color = Color(0xFFEF4444),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (selectedImageUri == null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(110.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0x33A78BFA))
                                .border(1.dp, AccentPurple.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .clickable {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Rounded.AddPhotoAlternate,
                                    contentDescription = "Fotoğraf Seç",
                                    tint = AccentPurple,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Soru veya Kitap Fotoğrafı Seç",
                                    color = PureWhite,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Galeriden veya kameradan yükle",
                                    color = SubtitleWhite,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    } else {
                        // Image selected preview
                        val bitmap = remember(selectedImageUri) {
                            try {
                                val stream = context.contentResolver.openInputStream(selectedImageUri!!)
                                BitmapFactory.decodeStream(stream)
                            } catch (e: Exception) {
                                null
                            }
                        }

                        if (bitmap != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(160.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(1.dp, AccentTeal, RoundedCornerShape(12.dp))
                            ) {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = "Yüklenen Kaynak",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )

                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(8.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xCC000000))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "✓ Görsel Yüklendi",
                                        color = AccentTeal,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Document text toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDocInput = !showDocInput },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Description,
                            contentDescription = null,
                            tint = AccentTeal,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (showDocInput) "Metin / Not Girişini Kapat" else "+ Ek Metin / Belge / Not Yapıştır",
                            color = AccentTeal,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (showDocInput) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = docNotesText,
                            onValueChange = { docNotesText = it },
                            placeholder = { Text("Buraya soru metni, formül veya ders notlarını yapıştırabilirsin...", color = SubtitleWhite.copy(alpha = 0.6f), fontSize = 11.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            maxLines = 4,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AccentTeal,
                                unfocusedBorderColor = GlassCardBorderSubtle,
                                focusedTextColor = PureWhite,
                                unfocusedTextColor = PureWhite
                            )
                        )
                    }
                }
            }
        }

        // Description Input Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF191630))
                    .border(1.dp, GlassCardBorderSubtle, RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Column {
                    Text(
                        text = "2. Soru Üretim Talimatı / Açıklama",
                        color = PureWhite,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Örn: 'bunun gibi başka örnek soru sor' veya 'bu bilgilerden sorular sor'",
                        color = SubtitleWhite,
                        fontSize = 10.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = descriptionText,
                        onValueChange = { descriptionText = it },
                        placeholder = { Text("Nasıl sorular üretilsin?", color = SubtitleWhite) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentTeal,
                            unfocusedBorderColor = GlassCardBorderSubtle,
                            focusedTextColor = PureWhite,
                            unfocusedTextColor = PureWhite
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Quick suggestion chips
                    val chips = listOf(
                        "Bunun gibi başka örnek soru sor",
                        "Bu bilgilerden sorular sor",
                        "Zor ve eleyici 5 şıklı sorular hazırla",
                        "Grafik yorumlama soruları üret"
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(chips) { chip ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(Color(0x33A78BFA))
                                    .border(1.dp, AccentPurple.copy(alpha = 0.4f), RoundedCornerShape(999.dp))
                                    .clickable { descriptionText = chip }
                                    .padding(horizontal = 9.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = chip,
                                    color = AccentPurple,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Question Count Selector
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF191630))
                    .border(1.dp, GlassCardBorderSubtle, RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "3. İstenen Soru Sayısı",
                            color = PureWhite,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$questionCount Soru",
                            color = AccentTeal,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val counts = listOf(1, 3, 5, 8, 10)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        counts.forEach { c ->
                            val isSelected = questionCount == c
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) AccentTeal else Color(0x22FFFFFF))
                                    .border(
                                        1.dp,
                                        if (isSelected) AccentTeal else GlassCardBorderSubtle,
                                        CircleShape
                                    )
                                    .clickable { questionCount = c },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$c",
                                    color = if (isSelected) Color.Black else PureWhite,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Start Quiz Button
        item {
            Button(
                onClick = {
                    onStartQuiz(
                        descriptionText,
                        questionCount,
                        imageBase64,
                        selectedImageUri?.toString(),
                        docNotesText.takeIf { it.isNotBlank() }
                    )
                },
                enabled = !isGenerating && (descriptionText.isNotBlank() || selectedImageUri != null || docNotesText.isNotBlank()),
                colors = ButtonDefaults.buttonColors(containerColor = AccentTeal),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.Black,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "5 Şıklı Quiz Hazırlanıyor...",
                        color = Color.Black,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "$questionCount Soruluk 5 Şıklı Quiz Başlat",
                        color = Color.Black,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // --- PAST STUDIES SECTION (TARİH TARİH KAYITLAR) ---
        item {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.History,
                        contentDescription = null,
                        tint = AccentPurple,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Önceki Çalışmalarım & Sorular",
                        color = PureWhite,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "${pastStudies.size} Kayıt",
                    color = SubtitleWhite,
                    fontSize = 11.sp
                )
            }
        }

        if (pastStudies.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x221B1834))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Henüz kaydedilmiş çalışma testi yok. Yukarıdan ilk testini oluştur!",
                        color = SubtitleWhite,
                        fontSize = 11.sp
                    )
                }
            }
        } else {
            items(pastStudies, key = { it.id }) { study ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF1B1834))
                        .border(1.dp, GlassCardBorderSubtle, RoundedCornerShape(14.dp))
                        .clickable { onViewPastStudy(study) }
                        .padding(12.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.DateRange,
                                    contentDescription = null,
                                    tint = AccentTeal,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = study.formattedDate,
                                    color = AccentTeal,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            IconButton(
                                onClick = { onDeletePastStudy(study.id) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.DeleteOutline,
                                    contentDescription = "Sil",
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = study.description,
                            color = PureWhite,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${study.questions.size} Soru • D: ${study.correctCount} / Y: ${study.incorrectCount} / B: ${study.emptyCount}",
                                color = SubtitleWhite,
                                fontSize = 11.sp
                            )

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0x3306B6D4))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "%.2f Net".format(study.netScore),
                                    color = AccentTeal,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// -------------------------------------------------------------
// VIEW B: ACTIVE QUIZ (SINGLE QUESTION AT A TIME, PREV/NEXT/FINISH)
// -------------------------------------------------------------
@Composable
fun ActiveQuizView(
    session: StudySession,
    currentIndex: Int,
    selectedAnswers: Map<String, Int>,
    revealedHintIds: Set<String>,
    isAnalyzing: Boolean,
    onSelectAnswer: (String, Int) -> Unit,
    onRevealHint: (String) -> Unit,
    onNextQuestion: () -> Unit,
    onPreviousQuestion: () -> Unit,
    onSetQuestionIndex: (Int) -> Unit,
    onFinishQuiz: () -> Unit,
    onCancelQuiz: () -> Unit,
    modifier: Modifier = Modifier
) {
    val questions = session.questions
    val question = questions.getOrNull(currentIndex) ?: return
    val totalCount = questions.size

    val selectedOptionIndex = selectedAnswers[question.id]
    val isHintRevealed = revealedHintIds.contains(question.id)
    val optionLetters = listOf("A", "B", "C", "D", "E")

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .navigationBarsPadding()
    ) {
        // Quiz Top Bar: Progress and Exit
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Soru ${currentIndex + 1} / $totalCount",
                    color = AccentTeal,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "${question.subject} • ${question.topic}",
                    color = SubtitleWhite,
                    fontSize = 11.sp
                )
            }

            IconButton(onClick = onCancelQuiz) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Testten Çık",
                    tint = SubtitleWhite
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Progress bar
        LinearProgressIndicator(
            progress = { (currentIndex + 1).toFloat() / totalCount.toFloat() },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = AccentTeal,
            trackColor = Color(0x33FFFFFF)
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Question Scrollable Body
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Question Card
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF1E1B38))
                        .border(1.dp, GlassCardBorderSubtle, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(AccentPurple.copy(alpha = 0.25f))
                                    .padding(horizontal = 7.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = question.skillType,
                                    color = AccentPurple,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = question.questionText,
                            color = PureWhite,
                            fontSize = 14.sp,
                            lineHeight = 21.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Hint Button ("ipucu secenegide olsun basınca ipucu çıksın")
            item {
                val hintText = question.hints.firstOrNull() ?: question.hint ?: "Kavramları ve verilen verileri adım adım karşılaştır."

                if (!isHintRevealed) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x22FBBF24))
                            .border(1.dp, Color(0xFFFBBF24).copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .clickable { onRevealHint(question.id) }
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Lightbulb,
                                contentDescription = "İpucu",
                                tint = Color(0xFFFBBF24),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "💡 Bu Soru İçin İpucu Al",
                                color = Color(0xFFFBBF24),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x22F59E0B))
                            .border(1.dp, Color(0xFFF59E0B), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.Lightbulb,
                                    contentDescription = null,
                                    tint = Color(0xFFFBBF24),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "İpucu:",
                                    color = Color(0xFFFBBF24),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = hintText,
                                color = PureWhite,
                                fontSize = 12.sp,
                                lineHeight = 17.sp
                            )
                        }
                    }
                }
            }

            // 5 Options (A, B, C, D, E)
            // Note: Correctness is NOT revealed until test is finished!
            items(question.options.indices.toList()) { optIndex ->
                val optText = question.options[optIndex]
                val isSelected = selectedOptionIndex == optIndex
                val letter = optionLetters.getOrElse(optIndex) { "${optIndex + 1}" }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (isSelected) AccentTeal.copy(alpha = 0.25f)
                            else Color(0xFF16132C)
                        )
                        .border(
                            1.dp,
                            if (isSelected) AccentTeal else GlassCardBorderSubtle,
                            RoundedCornerShape(14.dp)
                        )
                        .clickable { onSelectAnswer(question.id, optIndex) }
                        .padding(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Letter badge
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) AccentTeal else Color(0x22FFFFFF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = letter,
                                color = if (isSelected) Color.Black else PureWhite,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Text(
                            text = optText.removePrefix("$letter) ").removePrefix("$letter-) "),
                            color = PureWhite,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(10.dp))
            }
        }

        // --- BOTTOM NAVIGATION BUTTONS (Önceki, Sonraki, Testi Bitir) ---
        // As requested: "sorular tek tek gosterilsin altta sonraki önceki ve bitir butonları olsun."
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Önceki Butonu
            OutlinedButton(
                onClick = onPreviousQuestion,
                enabled = currentIndex > 0,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Önceki", fontSize = 12.sp)
            }

            // Sonraki Butonu
            Button(
                onClick = onNextQuestion,
                enabled = currentIndex < totalCount - 1,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0x33A78BFA)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
            ) {
                Text("Sonraki", color = PureWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                    contentDescription = null,
                    tint = PureWhite,
                    modifier = Modifier.size(16.dp)
                )
            }

            // Testi Bitir Butonu
            Button(
                onClick = onFinishQuiz,
                enabled = !isAnalyzing,
                colors = ButtonDefaults.buttonColors(containerColor = AccentTeal),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1.2f)
                    .height(46.dp)
            ) {
                if (isAnalyzing) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black)
                } else {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Testi Bitir", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// -------------------------------------------------------------
// VIEW C: TEST FINISHED & AI TOPIC ANALYSIS ("Şu konulara önem ver")
// -------------------------------------------------------------
@Composable
fun StudyResultsView(
    session: StudySession,
    onNewStudy: () -> Unit,
    modifier: Modifier = Modifier
) {
    val optionLetters = listOf("A", "B", "C", "D", "E")

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Summary Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF191630))
                    .border(1.dp, GlassCardBorderSubtle, RoundedCornerShape(18.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Test Tamamlandı! 🎯",
                            color = PureWhite,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = session.formattedDate,
                            color = SubtitleWhite,
                            fontSize = 10.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Big Score Pill
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x3306B6D4))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "NET SKORUN",
                                color = AccentTeal,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "%.2f Net".format(session.netScore),
                                color = PureWhite,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "${session.correctCount} Doğru • ${session.incorrectCount} Yanlış",
                                color = PureWhite,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${session.emptyCount} Boş / Toplam ${session.questions.size} Soru",
                                color = SubtitleWhite,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }

        // --- AI TOPIC ANALYSIS CARD ("şu konulara daha önem ver ve çalış") ---
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF201B3C))
                    .border(1.dp, AccentPurple.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = AccentPurple,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Yapay Zeka Konu & Gelişim Analizi",
                            color = PureWhite,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (session.weakTopicsToFocus.isNotEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.WarningAmber,
                                contentDescription = null,
                                tint = Color(0xFFFBBF24),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Şu Konulara Daha Çok Önem Ver ve Çalış:",
                                color = Color(0xFFFBBF24),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        session.weakTopicsToFocus.forEach { topic ->
                            Text(
                                text = "• $topic",
                                color = PureWhite,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(start = 12.dp, top = 2.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Text(
                        text = session.analysisFeedback,
                        color = SubtitleWhite,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )
                }
            }
        }

        // Questions Review Header
        item {
            Text(
                text = "Tüm Soruları İncele",
                color = PureWhite,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        // Each Question Card in Review
        items(session.questions.indices.toList()) { qIdx ->
            val q = session.questions[qIdx]
            val chosen = session.userAnswers[q.id]
            val isCorrect = chosen == q.correctOptionIndex
            val isEmpty = chosen == null

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF1B1834))
                    .border(
                        1.dp,
                        when {
                            isCorrect -> Color(0xFF10B981).copy(alpha = 0.5f)
                            isEmpty -> Color(0xFF94A3B8).copy(alpha = 0.3f)
                            else -> Color(0xFFEF4444).copy(alpha = 0.5f)
                        },
                        RoundedCornerShape(14.dp)
                    )
                    .padding(12.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${qIdx + 1}. Soru (${q.subject} - ${q.topic})",
                            color = PureWhite,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    when {
                                        isCorrect -> Color(0x3310B981)
                                        isEmpty -> Color(0x3394A3B8)
                                        else -> Color(0x33EF4444)
                                    }
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = when {
                                    isCorrect -> "Doğru"
                                    isEmpty -> "Boş Bırakıldı"
                                    else -> "Yanlış"
                                },
                                color = when {
                                    isCorrect -> Color(0xFF10B981)
                                    isEmpty -> Color(0xFF94A3B8)
                                    else -> Color(0xFFEF4444)
                                },
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = q.questionText,
                        color = PureWhite,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Options Review
                    q.options.forEachIndexed { oIdx, optText ->
                        val isChosen = chosen == oIdx
                        val isAnswer = q.correctOptionIndex == oIdx
                        val letter = optionLetters.getOrElse(oIdx) { "${oIdx + 1}" }

                        val bg = when {
                            isAnswer -> Color(0x3310B981)
                            isChosen && !isCorrect -> Color(0x33EF4444)
                            else -> Color.Transparent
                        }

                        val textColor = when {
                            isAnswer -> Color(0xFF10B981)
                            isChosen && !isCorrect -> Color(0xFFEF4444)
                            else -> SubtitleWhite
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(bg)
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "$letter)",
                                color = textColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = optText.removePrefix("$letter) ").removePrefix("$letter-) "),
                                color = textColor,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Solution explanation
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0x22FFFFFF))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "Çözüm: ${q.explanation}",
                            color = SubtitleWhite,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }

        // New Study Button
        item {
            Button(
                onClick = onNewStudy,
                colors = ButtonDefaults.buttonColors(containerColor = AccentTeal),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text("Yeni Bir Çalışma Başlat", color = Color.Black, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
