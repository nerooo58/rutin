package com.nororoutine.batu.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nororoutine.batu.model.Routine
import com.nororoutine.batu.ui.illustrations.ILLUSTRATION_PRESETS
import com.nororoutine.batu.ui.illustrations.RoutineIllustration
import com.nororoutine.batu.ui.theme.AccentPurple
import com.nororoutine.batu.ui.theme.AccentTeal
import com.nororoutine.batu.ui.theme.ChipSelectedBg
import com.nororoutine.batu.ui.theme.ChipUnselectedBg
import com.nororoutine.batu.ui.theme.GlassCardBorder
import com.nororoutine.batu.ui.theme.GlassCardBorderSubtle
import com.nororoutine.batu.ui.theme.MutedWhite
import com.nororoutine.batu.ui.theme.PureWhite
import com.nororoutine.batu.ui.theme.SubtitleWhite

data class TemplateSuggestion(
    val name: String,
    val defaultTime: String,
    val isRange: Boolean,
    val defaultEndTime: String?,
    val category: String,
    val illustrationId: String
)

val TEMPLATE_SUGGESTIONS = listOf(
    TemplateSuggestion("Sabah Yürüyüşü", "07:00", true, "07:45", "Spor", "spor_running"),
    TemplateSuggestion("Meditasyon", "07:30", false, null, "Zihin", "meditasyon_calm"),
    TemplateSuggestion("Kitap Okuma", "21:30", true, "22:15", "Zihin", "calisma_books"),
    TemplateSuggestion("Soğuk Duş", "08:00", false, null, "Beslenme", "beslenme_water"),
    TemplateSuggestion("Günlük Yazımı", "22:00", false, null, "Zihin", "calisma_focus")
)

val DAY_LABELS = listOf(
    1 to "Pzt",
    2 to "Sal",
    3 to "Çar",
    4 to "Per",
    5 to "Cum",
    6 to "Cmt",
    7 to "Paz"
)

private val WEEKDAYS = setOf(1, 2, 3, 4, 5)
private val WEEKEND = setOf(6, 7)
private val ALL_DAYS = setOf(1, 2, 3, 4, 5, 6, 7)

@Composable
fun RoutineFormBottomSheet(
    routineToEdit: Routine? = null,
    availableCategories: List<String> = listOf("Spor", "Zihin", "Beslenme"),
    onAddNewCategory: (String) -> Unit = {},
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        time: String,
        isRange: Boolean,
        startTime: String,
        endTime: String?,
        days: Set<Int>,
        category: String,
        illustrationId: String
    ) -> Unit
) {
    var name by remember(routineToEdit) { mutableStateOf(routineToEdit?.name.orEmpty()) }
    var isRange by remember(routineToEdit) { mutableStateOf(routineToEdit?.isRange ?: false) }
    var startTime by remember(routineToEdit) { mutableStateOf(routineToEdit?.startTime ?: "08:00") }
    var endTime by remember(routineToEdit) { mutableStateOf(routineToEdit?.endTime ?: "09:00") }
    var displayTime by remember(routineToEdit) { mutableStateOf(routineToEdit?.time ?: "08:00") }

    var selectedDays by remember(routineToEdit) {
        mutableStateOf<Set<Int>>(routineToEdit?.daysIntSet?.ifEmpty { ALL_DAYS } ?: ALL_DAYS)
    }
    var selectedCategory by remember(routineToEdit, availableCategories) {
        mutableStateOf(routineToEdit?.category ?: availableCategories.firstOrNull() ?: "Zihin")
    }
    var selectedIllustrationId by remember(routineToEdit) {
        mutableStateOf(routineToEdit?.illustrationId ?: "meditasyon_calm")
    }

    var isAddingCustomCategory by remember { mutableStateOf(false) }
    var newCategoryInput by remember { mutableStateOf("") }
    var selectedIllustrationCategory by remember { mutableStateOf("Spor") }

    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .background(Color(0xFF13112A))
            .border(1.dp, GlassCardBorder, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
        ) {
            // Drag handle
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color(0x33FFFFFF))
                    .align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (routineToEdit != null) "Rutini Düzenle" else "Yeni Rutin Ekle",
                    color = PureWhite,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0x22FFFFFF))
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Kapat",
                        tint = PureWhite,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Template Suggestions Row
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.AutoFixHigh,
                    contentDescription = null,
                    tint = AccentTeal,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Şablon Önerileri",
                    color = SubtitleWhite,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TEMPLATE_SUGGESTIONS.forEach { template ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color(0x1F7CF4DE))
                            .border(1.dp, Color(0x337CF4DE), RoundedCornerShape(999.dp))
                            .clickable {
                                name = template.name
                                isRange = template.isRange
                                startTime = template.defaultTime
                                endTime = template.defaultEndTime ?: "09:00"
                                displayTime = if (template.isRange && template.defaultEndTime != null) {
                                    "${template.defaultTime} – ${template.defaultEndTime}"
                                } else {
                                    template.defaultTime
                                }
                                selectedCategory = template.category
                                selectedIllustrationId = template.illustrationId
                            }
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = template.name,
                            color = AccentTeal,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Field 1: Routine Name
            Text(
                text = "Rutin Adı",
                color = PureWhite,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = {
                    Text("Örn: Sabah Egzersizi", color = MutedWhite, fontSize = 14.sp)
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentPurple,
                    unfocusedBorderColor = GlassCardBorderSubtle,
                    focusedTextColor = PureWhite,
                    unfocusedTextColor = PureWhite,
                    focusedContainerColor = Color(0x33101C30),
                    unfocusedContainerColor = Color(0x22101C30),
                    cursorColor = AccentPurple
                )
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Field 2: Preset Illustration Selection (SVG Vector Illustrations)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Image,
                    contentDescription = null,
                    tint = AccentTeal,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Rutin İllüstrasyonu (SVG)",
                    color = PureWhite,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Illustration Category Filter Chips
            val illustrationCats = listOf("Spor", "Doğa", "Meditasyon", "Ders / Çalışma", "Beslenme", "Müzik", "Uyku / Dinlenme")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                illustrationCats.forEach { cat ->
                    val isCatSelected = selectedIllustrationCategory == cat
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(if (isCatSelected) Color(0x33A78BFA) else ChipUnselectedBg)
                            .border(1.dp, if (isCatSelected) AccentPurple else GlassCardBorderSubtle, RoundedCornerShape(999.dp))
                            .clickable { selectedIllustrationCategory = cat }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = cat,
                            color = if (isCatSelected) AccentPurple else SubtitleWhite,
                            fontSize = 11.sp,
                            fontWeight = if (isCatSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Illustration Preset Cards for Selected Category
            val matchingIllustrations = ILLUSTRATION_PRESETS.filter { it.category == selectedIllustrationCategory }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                matchingIllustrations.forEach { preset ->
                    val isSelected = selectedIllustrationId == preset.id
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) Color(0x227CF4DE) else Color(0x11FFFFFF))
                            .border(
                                2.dp,
                                if (isSelected) AccentTeal else Color(0x1FFFFFFF),
                                RoundedCornerShape(16.dp)
                            )
                            .clickable { selectedIllustrationId = preset.id }
                            .padding(8.dp)
                    ) {
                        RoutineIllustration(
                            illustrationId = preset.id,
                            size = 80.dp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = preset.title,
                            color = if (isSelected) AccentTeal else SubtitleWhite,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Field 3: Time Range Wheel Picker (Tek Saat / Aralık)
            Text(
                text = "Zamanlama Modu",
                color = PureWhite,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))

            TimeRangeWheelPicker(
                initialIsRange = isRange,
                initialStartTime = startTime,
                initialEndTime = endTime,
                onTimeConfigChange = { range, start, end, display ->
                    isRange = range
                    startTime = start
                    endTime = end ?: "09:00"
                    displayTime = display
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Field 4: Day Selection with Quick-Select Buttons
            Text(
                text = "Günler",
                color = PureWhite,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            val isWeekdaysSelected = selectedDays.containsAll(WEEKDAYS)
            val isWeekendSelected = selectedDays.containsAll(WEEKEND)
            val isAllDaysSelected = selectedDays.size == 7

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Hafta içi
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isWeekdaysSelected) Color(0x33A78BFA) else ChipUnselectedBg)
                        .border(
                            1.dp,
                            if (isWeekdaysSelected) AccentPurple else GlassCardBorderSubtle,
                            RoundedCornerShape(10.dp)
                        )
                        .clickable {
                            selectedDays = if (selectedDays.containsAll(WEEKDAYS)) {
                                val remaining = selectedDays - WEEKDAYS
                                if (remaining.isEmpty()) setOf(1) else remaining
                            } else {
                                selectedDays + WEEKDAYS
                            }
                        }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Hafta içi",
                        color = if (isWeekdaysSelected) AccentPurple else SubtitleWhite,
                        fontSize = 12.sp,
                        fontWeight = if (isWeekdaysSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }

                // Hafta sonu
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isWeekendSelected) Color(0x33A78BFA) else ChipUnselectedBg)
                        .border(
                            1.dp,
                            if (isWeekendSelected) AccentPurple else GlassCardBorderSubtle,
                            RoundedCornerShape(10.dp)
                        )
                        .clickable {
                            selectedDays = if (selectedDays.containsAll(WEEKEND)) {
                                val remaining = selectedDays - WEEKEND
                                if (remaining.isEmpty()) setOf(6) else remaining
                            } else {
                                selectedDays + WEEKEND
                            }
                        }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Hafta sonu",
                        color = if (isWeekendSelected) AccentPurple else SubtitleWhite,
                        fontSize = 12.sp,
                        fontWeight = if (isWeekendSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }

                // Her gün
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isAllDaysSelected) Color(0x337CF4DE) else ChipUnselectedBg)
                        .border(
                            1.dp,
                            if (isAllDaysSelected) AccentTeal else GlassCardBorderSubtle,
                            RoundedCornerShape(10.dp)
                        )
                        .clickable {
                            selectedDays = if (selectedDays.size == 7) setOf(1) else ALL_DAYS
                        }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Her gün",
                        color = if (isAllDaysSelected) AccentTeal else SubtitleWhite,
                        fontSize = 12.sp,
                        fontWeight = if (isAllDaysSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Individual Day Pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                DAY_LABELS.forEach { (dayIndex, label) ->
                    val isSelected = selectedDays.contains(dayIndex)
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) AccentPurple else ChipUnselectedBg)
                            .border(
                                1.dp,
                                if (isSelected) AccentPurple else GlassCardBorderSubtle,
                                CircleShape
                            )
                            .clickable {
                                selectedDays = if (isSelected) {
                                    if (selectedDays.size > 1) selectedDays - dayIndex else selectedDays
                                } else {
                                    selectedDays + dayIndex
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) Color(0xFF0D0B1E) else SubtitleWhite,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Field 5: Dynamic Categories
            Text(
                text = "Kategori",
                color = PureWhite,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                availableCategories.forEach { category ->
                    val isSelected = selectedCategory == category
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) ChipSelectedBg else ChipUnselectedBg)
                            .border(
                                1.dp,
                                if (isSelected) AccentTeal else GlassCardBorderSubtle,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { selectedCategory = category }
                            .padding(horizontal = 14.dp, vertical = 9.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = category,
                            color = if (isSelected) AccentTeal else SubtitleWhite,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }

                if (!isAddingCustomCategory) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x22A78BFA))
                            .border(1.dp, Color(0x44A78BFA), RoundedCornerShape(12.dp))
                            .clickable {
                                isAddingCustomCategory = true
                                newCategoryInput = ""
                            }
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = "Kategori Ekle",
                                tint = AccentPurple,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Yeni",
                                color = AccentPurple,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x33101C30))
                            .border(1.dp, AccentTeal, RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        OutlinedTextField(
                            value = newCategoryInput,
                            onValueChange = { newCategoryInput = it },
                            placeholder = {
                                Text("Kategori adı...", color = MutedWhite, fontSize = 11.sp)
                            },
                            singleLine = true,
                            modifier = Modifier.width(110.dp),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    val trimmed = newCategoryInput.trim()
                                    if (trimmed.isNotBlank()) {
                                        onAddNewCategory(trimmed)
                                        selectedCategory = trimmed
                                        isAddingCustomCategory = false
                                        newCategoryInput = ""
                                    }
                                }
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                focusedTextColor = PureWhite,
                                unfocusedTextColor = PureWhite,
                                cursorColor = AccentTeal
                            )
                        )

                        IconButton(
                            onClick = {
                                val trimmed = newCategoryInput.trim()
                                if (trimmed.isNotBlank()) {
                                    onAddNewCategory(trimmed)
                                    selectedCategory = trimmed
                                    isAddingCustomCategory = false
                                    newCategoryInput = ""
                                }
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = "Onayla",
                                tint = AccentTeal,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                isAddingCustomCategory = false
                                newCategoryInput = ""
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "İptal",
                                tint = MutedWhite,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(26.dp))

            // Save Routine Button
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(
                            name,
                            displayTime,
                            isRange,
                            startTime,
                            if (isRange) endTime else null,
                            selectedDays,
                            selectedCategory,
                            selectedIllustrationId
                        )
                        onDismiss()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .springPress {
                        if (name.isNotBlank()) {
                            onSave(
                                name,
                                displayTime,
                                isRange,
                                startTime,
                                if (isRange) endTime else null,
                                selectedDays,
                                selectedCategory,
                                selectedIllustrationId
                            )
                            onDismiss()
                        }
                    },
                enabled = name.isNotBlank(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentPurple,
                    contentColor = PureWhite,
                    disabledContainerColor = Color(0x33A78BFA),
                    disabledContentColor = Color(0x66FFFFFF)
                )
            ) {
                Text(
                    text = if (routineToEdit != null) "Değişiklikleri Kaydet" else "Rutini Oluştur",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    }
}
