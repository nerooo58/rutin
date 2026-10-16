package com.nororoutine.batu.ui.components

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import android.widget.Toast
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.FileProvider
import com.nororoutine.batu.model.Routine
import com.nororoutine.batu.model.RoutineLog
import com.nororoutine.batu.network.GeminiStoryAnalysis
import com.nororoutine.batu.viewmodel.RutinUiState
import java.io.File
import java.io.FileOutputStream
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

enum class StoryPeriod(val label: String) {
    DAILY("Günlük"),
    WEEKLY("Haftalık"),
    MONTHLY("Aylık"),
    YEARLY("Yıllık")
}

data class CategoryFocusStat(
    val category: String,
    val totalSeconds: Int,
    val completedCount: Int,
    val color: Color
) {
    val hours: Int get() = totalSeconds / 3600
    val minutes: Int get() = (totalSeconds % 3600) / 60
    val formattedDuration: String
        get() = when {
            hours > 0 && minutes > 0 -> "${hours}sa ${minutes}dk"
            hours > 0 -> "${hours} saat"
            minutes > 0 -> "${minutes} dk"
            totalSeconds > 0 -> "${totalSeconds} sn"
            else -> "0 dk"
        }
}

data class StoryCardData(
    val period: StoryPeriod,
    val periodTitle: String,
    val dateSubtitle: String,
    val targetRoutineName: String,
    val totalFocusSeconds: Int,
    val completedCount: Int,
    val totalCount: Int,
    val completionPercent: Int,
    val currentStreak: Int,
    val top3Accomplishments: List<String>,
    val categoryStats: List<CategoryFocusStat>,
    val achievementBadge: String,
    val motivationalQuote: String,
    val isAiGenerated: Boolean = false
) {
    val totalHours: Int get() = totalFocusSeconds / 3600
    val totalMinutes: Int get() = (totalFocusSeconds % 3600) / 60
    val formattedTotalFocus: String
        get() = when {
            totalHours > 0 && totalMinutes > 0 -> "${totalHours} saat ${totalMinutes} dk"
            totalHours > 0 -> "${totalHours} saat"
            totalMinutes > 0 -> "${totalMinutes} dakika"
            totalFocusSeconds > 0 -> "${totalFocusSeconds} saniye"
            else -> "0 dakika"
        }

    fun buildShareText(): String {
        val sb = StringBuilder()
        sb.append("🌟 Rutin Başarı Hikayem 🚀\n")
        sb.append("📅 Dönem: $dateSubtitle ($periodTitle)\n")
        sb.append("🎯 Kapsam: $targetRoutineName\n")
        sb.append("⏱️ Toplam Tutulan Süre: $formattedTotalFocus\n")
        if (currentStreak > 0) {
            sb.append("🔥 Seri: $currentStreak Gün!\n")
        }

        if (top3Accomplishments.isNotEmpty()) {
            sb.append("\n🏆 Bu Sürede Neler Yapıldı (Top 3):\n")
            top3Accomplishments.take(3).forEach { item ->
                sb.append("• ${item.removePrefix("•").trim()}\n")
            }
        }

        sb.append("\n\"$motivationalQuote\"\n")
        if (isAiGenerated) {
            sb.append("🤖 Yapay Zeka (Gemini) Destekli Rapor\n")
        }
        sb.append("✨ Rutin App ile takip edildi.")
        return sb.toString()
    }
}

object StoryCardHelper {

    fun computeStoryData(
        period: StoryPeriod,
        uiState: RutinUiState,
        anchorDate: LocalDate = LocalDate.now(),
        selectedRoutineId: String? = null,
        aiAnalysis: GeminiStoryAnalysis? = null
    ): StoryCardData {
        val locale = Locale("tr", "TR")
        val routines = uiState.routines
        val logs = uiState.logs
        val streak = uiState.stats.currentStreak

        val targetRoutine = routines.find { it.id == selectedRoutineId }
        val targetRoutineName = targetRoutine?.name ?: "Tüm Rutinler"

        // 1. Filter Logs by Period Date Range
        val filteredLogs = when (period) {
            StoryPeriod.DAILY -> {
                val dateStr = anchorDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
                logs.values.filter { it.date == dateStr }
            }
            StoryPeriod.WEEKLY -> {
                val monday = anchorDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                val sunday = anchorDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
                val mondayStr = monday.format(DateTimeFormatter.ISO_LOCAL_DATE)
                val sundayStr = sunday.format(DateTimeFormatter.ISO_LOCAL_DATE)
                logs.values.filter { it.date in mondayStr..sundayStr }
            }
            StoryPeriod.MONTHLY -> {
                val yearMonthPrefix = anchorDate.format(DateTimeFormatter.ofPattern("yyyy-MM"))
                logs.values.filter { it.date.startsWith(yearMonthPrefix) }
            }
            StoryPeriod.YEARLY -> {
                val yearPrefix = anchorDate.year.toString()
                logs.values.filter { it.date.startsWith(yearPrefix) }
            }
        }

        // 2. Filter by Specific Routine if selected
        val periodLogs = if (selectedRoutineId != null) {
            filteredLogs.filter { it.routineId == selectedRoutineId }
        } else {
            filteredLogs
        }

        // 3. Compute Focus Time Strictly From Recorded Times
        val totalSeconds = periodLogs.sumOf { log ->
            if (log.focusDurationSeconds > 0) {
                log.focusDurationSeconds
            } else if (log.isDone) {
                val r = routines.find { it.id == log.routineId }
                if (r != null && r.durationMinutes > 0) r.durationMinutes * 60 else 0
            } else {
                0
            }
        }

        // 4. Compute Counts & Percentage
        val completedCount = periodLogs.count { it.isDone }
        val totalExpected = if (selectedRoutineId != null) {
            when (period) {
                StoryPeriod.DAILY -> 1
                StoryPeriod.WEEKLY -> 7
                StoryPeriod.MONTHLY -> 30
                StoryPeriod.YEARLY -> 365
            }
        } else {
            when (period) {
                StoryPeriod.DAILY -> routines.size.coerceAtLeast(1)
                StoryPeriod.WEEKLY -> (routines.size * 5).coerceAtLeast(1)
                StoryPeriod.MONTHLY -> (routines.size * 22).coerceAtLeast(1)
                StoryPeriod.YEARLY -> (routines.size * 260).coerceAtLeast(1)
            }
        }
        val percent = if (totalExpected > 0) (completedCount * 100 / totalExpected).coerceIn(0, 100) else 0

        // 5. Extract User Session Notes & Descriptions
        val rawSessionNotes = mutableListOf<String>()
        for (log in periodLogs) {
            val rName = routines.find { it.id == log.routineId }?.name ?: ""
            if (log.focusSessionNotes.isNotEmpty()) {
                for (noteEntry in log.focusSessionNotes) {
                    val parts = noteEntry.split("|")
                    val note = if (parts.size >= 3) parts[2] else noteEntry
                    val secs = parts.getOrNull(1)?.toIntOrNull() ?: 0
                    val durStr = if (secs > 0) "${secs / 60} dk" else ""
                    val item = if (selectedRoutineId == null) {
                        "$rName: $note ($durStr)".trim()
                    } else {
                        "$note ($durStr)".trim()
                    }
                    rawSessionNotes.add(item)
                }
            } else if (log.note.isNotBlank()) {
                val item = if (selectedRoutineId == null) "$rName: ${log.note}" else log.note
                rawSessionNotes.add(item)
            }
        }

        // 6. Category stats
        val categoryStats = calculateCategoryStats(periodLogs.filter { it.isDone || it.focusDurationSeconds > 0 }, routines)

        // 7. Extract Top 3 Accomplishments (From AI if available, or extracted from user notes)
        val top3: List<String>
        val quote: String
        val isAi: Boolean

        if (aiAnalysis != null && aiAnalysis.topHighlights.isNotEmpty()) {
            top3 = aiAnalysis.topHighlights.take(3)
            quote = aiAnalysis.motivationalQuote
            isAi = aiAnalysis.isAiGenerated
        } else {
            isAi = false
            val localExtracted = mutableListOf<String>()
            for (item in rawSessionNotes) {
                val clean = item.trim()
                if (clean.isNotBlank() && !localExtracted.contains(clean)) {
                    localExtracted.add("• $clean")
                }
            }

            if (localExtracted.isNotEmpty()) {
                top3 = localExtracted.take(3)
            } else if (categoryStats.isNotEmpty()) {
                top3 = categoryStats.take(3).map { "• ${it.category}: ${it.formattedDuration} odaklanıldı" }
            } else {
                top3 = listOf(
                    "• $targetRoutineName: Disiplinli seans tamamlandı",
                    "• Odak süresi hedefine ulaşıldı",
                    "• Rutin takibi aksatılmadı"
                )
            }

            val (badge, defaultQuote) = getMotivationalQuote(percent, streak)
            quote = defaultQuote
        }

        val (badge, _) = getMotivationalQuote(percent, streak)

        val (periodTitle, dateSubtitle) = when (period) {
            StoryPeriod.DAILY -> Pair(
                "Günün Başarısı",
                anchorDate.format(DateTimeFormatter.ofPattern("d MMMM yyyy, EEEE", locale))
            )
            StoryPeriod.WEEKLY -> {
                val mon = anchorDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                val sun = anchorDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
                Pair(
                    "Haftalık Rapor",
                    "${mon.dayOfMonth} – ${sun.dayOfMonth} ${sun.format(DateTimeFormatter.ofPattern("MMMM yyyy", locale))}"
                )
            }
            StoryPeriod.MONTHLY -> Pair(
                "Aylık Rapor",
                anchorDate.format(DateTimeFormatter.ofPattern("MMMM yyyy", locale)).replaceFirstChar { it.uppercase() }
            )
            StoryPeriod.YEARLY -> Pair(
                "Yıllık Başarı",
                "${anchorDate.year} Yılı Özeti"
            )
        }

        return StoryCardData(
            period = period,
            periodTitle = periodTitle,
            dateSubtitle = dateSubtitle,
            targetRoutineName = targetRoutineName,
            totalFocusSeconds = totalSeconds,
            completedCount = completedCount,
            totalCount = totalExpected,
            completionPercent = percent,
            currentStreak = streak,
            top3Accomplishments = top3,
            categoryStats = categoryStats,
            achievementBadge = if (isAi) "🤖 AI Destekli Rapor" else badge,
            motivationalQuote = quote,
            isAiGenerated = isAi
        )
    }

    private fun calculateCategoryStats(
        logs: List<RoutineLog>,
        routines: List<Routine>
    ): List<CategoryFocusStat> {
        val routineMap = routines.associateBy { it.id }
        val categorySeconds = mutableMapOf<String, Int>()
        val categoryCounts = mutableMapOf<String, Int>()

        for (log in logs) {
            val routine = routineMap[log.routineId] ?: continue
            val cat = routine.category.ifBlank { "Genel" }
            val seconds = if (log.focusDurationSeconds > 0) {
                log.focusDurationSeconds
            } else if (routine.durationMinutes > 0) {
                routine.durationMinutes * 60
            } else {
                0
            }
            if (seconds > 0) {
                categorySeconds[cat] = (categorySeconds[cat] ?: 0) + seconds
                categoryCounts[cat] = (categoryCounts[cat] ?: 0) + 1
            }
        }

        val colors = listOf(
            Color(0xFF7CF4DE), // Teal
            Color(0xFFA78BFA), // Purple
            Color(0xFF4ADE80), // Green
            Color(0xFFFBBF24), // Amber
            Color(0xFFF472B6), // Pink
            Color(0xFF60A5FA)  // Blue
        )

        return categorySeconds.entries
            .sortedByDescending { it.value }
            .mapIndexed { index, entry ->
                CategoryFocusStat(
                    category = entry.key,
                    totalSeconds = entry.value,
                    completedCount = categoryCounts[entry.key] ?: 0,
                    color = colors[index % colors.size]
                )
            }
    }

    private fun getMotivationalQuote(percent: Int, streak: Int): Pair<String, String> {
        return when {
            percent >= 90 && streak >= 7 -> Pair("🔥 ZİRVE ODAKLANMA", "Her gün küçük adımlar, devasa başarılar yaratır.")
            percent >= 80 -> Pair("⭐ YÜKSEK PERFORMANS", "Zamanını kontrol eden, geleceğini inşa eder.")
            percent >= 60 -> Pair("🚀 GÜÇLÜ İLERLEME", "Süreklilik yetenekten daha güçlüdür.")
            percent >= 30 -> Pair("🌱 İVME KAZANIYOR", "Başlamak yolun yarısıdır, devam et!")
            else -> Pair("✨ YENİ BİR ADIM", "Her yeni an, yeni bir odaklanma fırsatıdır.")
        }
    }

    /**
     * Draws the pristine 9:16 Instagram Stories & WhatsApp Status Bitmap (1080x1920)
     */
    fun createStoryBitmap(context: Context, data: StoryCardData): Bitmap {
        val width = 1080
        val height = 1920
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 1. Deep Midnight Gradient Background
        val bgPaint = Paint().apply {
            shader = LinearGradient(
                0f, 0f, 0f, height.toFloat(),
                intArrayOf(
                    android.graphics.Color.parseColor("#080614"),
                    android.graphics.Color.parseColor("#100E26"),
                    android.graphics.Color.parseColor("#181A38")
                ),
                null,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // 2. Ambient Glow Mesh
        val glowPaintTeal = Paint().apply {
            shader = RadialGradient(
                width * 0.85f, height * 0.20f, 500f,
                intArrayOf(android.graphics.Color.parseColor("#337CF4DE"), android.graphics.Color.TRANSPARENT),
                null,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawCircle(width * 0.85f, height * 0.20f, 500f, glowPaintTeal)

        val glowPaintPurple = Paint().apply {
            shader = RadialGradient(
                width * 0.15f, height * 0.65f, 540f,
                intArrayOf(android.graphics.Color.parseColor("#44A78BFA"), android.graphics.Color.TRANSPARENT),
                null,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawCircle(width * 0.15f, height * 0.65f, 540f, glowPaintPurple)

        // 3. Main Glass Frame Border
        val frameRect = RectF(50f, 80f, width - 50f, height - 90f)
        val framePaint = Paint().apply {
            color = android.graphics.Color.parseColor("#18FFFFFF")
            style = Paint.Style.STROKE
            strokeWidth = 3f
            isAntiAlias = true
        }
        canvas.drawRoundRect(frameRect, 48f, 48f, framePaint)

        val textPaint = Paint().apply { isAntiAlias = true }

        // 4. Header: App Branding & Scope
        textPaint.apply {
            textSize = 34f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.15f
            textAlign = Paint.Align.CENTER
            color = android.graphics.Color.parseColor("#FFA78BFA")
        }
        canvas.drawText("✦ RUTİN • ODAK BAŞARI RAPORU ✦", width / 2f, 170f, textPaint)

        // Scope / Routine Title
        textPaint.apply {
            textSize = 62f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = -0.02f
            color = android.graphics.Color.WHITE
        }
        val titleText = if (data.targetRoutineName == "Tüm Rutinler") data.periodTitle else data.targetRoutineName
        canvas.drawText(titleText.take(24), width / 2f, 260f, textPaint)

        textPaint.apply {
            textSize = 32f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            color = android.graphics.Color.parseColor("#B3FFFFFF")
            letterSpacing = 0.02f
        }
        canvas.drawText("${data.dateSubtitle} • ${data.periodTitle}", width / 2f, 315f, textPaint)

        // 5. Hero Card: Toplam Tutulan Süre
        val heroRect = RectF(90f, 370f, width - 90f, 680f)
        val heroBg = Paint().apply {
            shader = LinearGradient(
                heroRect.left, heroRect.top, heroRect.right, heroRect.bottom,
                intArrayOf(android.graphics.Color.parseColor("#2E33266B"), android.graphics.Color.parseColor("#241D3B60")),
                null,
                Shader.TileMode.CLAMP
            )
            isAntiAlias = true
        }
        canvas.drawRoundRect(heroRect, 36f, 36f, heroBg)

        val heroBorder = Paint().apply {
            color = android.graphics.Color.parseColor("#66A78BFA")
            style = Paint.Style.STROKE
            strokeWidth = 3f
            isAntiAlias = true
        }
        canvas.drawRoundRect(heroRect, 36f, 36f, heroBorder)

        textPaint.apply {
            textSize = 28f
            letterSpacing = 0.12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = android.graphics.Color.parseColor("#CC7CF4DE")
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("TOPLAM TUTULAN ODAK SÜRESİ", width / 2f, 440f, textPaint)

        // Duration Big Text
        textPaint.apply {
            textSize = 88f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = android.graphics.Color.WHITE
            letterSpacing = -0.02f
        }
        canvas.drawText(data.formattedTotalFocus, width / 2f, 545f, textPaint)

        // Tag: AI or Routine
        val tagY = 600f
        val tagRect = RectF(width / 2f - 240f, tagY, width / 2f + 240f, tagY + 54f)
        val tagBg = Paint().apply {
            color = if (data.isAiGenerated) android.graphics.Color.parseColor("#338B5CF6") else android.graphics.Color.parseColor("#3310B981")
            isAntiAlias = true
        }
        canvas.drawRoundRect(tagRect, 27f, 27f, tagBg)

        textPaint.apply {
            textSize = 26f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = if (data.isAiGenerated) android.graphics.Color.parseColor("#FFA78BFA") else android.graphics.Color.parseColor("#FF4ADE80")
            textAlign = Paint.Align.CENTER
        }
        val badgeLabel = if (data.isAiGenerated) "🤖 Yapay Zeka (Gemini) Analizi" else "⏱️ Kaydedilen Odak Seansları"
        canvas.drawText(badgeLabel, width / 2f, tagY + 37f, textPaint)

        // 6. Section: Bu Sürede Neler Yapıldı? (En Çok Yapılan 3 Şey)
        var curY = 740f
        textPaint.apply {
            textSize = 34f
            letterSpacing = 0.08f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = android.graphics.Color.parseColor("#FF7CF4DE")
            textAlign = Paint.Align.LEFT
        }
        canvas.drawText("🏆 BU SÜREDE NELER YAPILDI? (EN ÇOK YAPILAN 3 ŞEY)", 100f, curY, textPaint)

        curY += 40f
        val itemsToDraw = data.top3Accomplishments.take(3)
        if (itemsToDraw.isNotEmpty()) {
            val cardBg = Paint().apply {
                color = android.graphics.Color.parseColor("#1F1C2B47")
                isAntiAlias = true
            }
            val cardBorder = Paint().apply {
                color = android.graphics.Color.parseColor("#26FFFFFF")
                style = Paint.Style.STROKE
                strokeWidth = 2f
                isAntiAlias = true
            }

            for ((idx, item) in itemsToDraw.withIndex()) {
                val cleanText = item.removePrefix("•").trim()
                val itemRect = RectF(90f, curY, width - 90f, curY + 120f)
                canvas.drawRoundRect(itemRect, 24f, 24f, cardBg)
                canvas.drawRoundRect(itemRect, 24f, 24f, cardBorder)

                // Index badge
                val badgeCirclePaint = Paint().apply {
                    color = when (idx) {
                        0 -> android.graphics.Color.parseColor("#FF7CF4DE")
                        1 -> android.graphics.Color.parseColor("#FFA78BFA")
                        else -> android.graphics.Color.parseColor("#FFFBBF24")
                    }
                    isAntiAlias = true
                }
                canvas.drawCircle(150f, curY + 60f, 24f, badgeCirclePaint)

                textPaint.apply {
                    textSize = 28f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    color = android.graphics.Color.parseColor("#0F172A")
                    textAlign = Paint.Align.CENTER
                }
                canvas.drawText("${idx + 1}", 150f, curY + 70f, textPaint)

                // Item description text
                textPaint.apply {
                    textSize = 32f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    color = android.graphics.Color.WHITE
                    textAlign = Paint.Align.LEFT
                }
                canvas.drawText(cleanText.take(40), 200f, curY + 70f, textPaint)

                curY += 140f
            }
        } else {
            textPaint.apply {
                textSize = 30f
                color = android.graphics.Color.parseColor("#80FFFFFF")
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("Bu dönemde kaydedilmiş seans detayı bulunamadı.", width / 2f, curY + 60f, textPaint)
            curY += 140f
        }

        // 7. Category Distribution Bar if Available
        if (data.categoryStats.isNotEmpty()) {
            curY += 20f
            textPaint.apply {
                textSize = 28f
                letterSpacing = 0.08f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                color = android.graphics.Color.parseColor("#B3FFFFFF")
                textAlign = Paint.Align.LEFT
            }
            canvas.drawText("KATEGORİ DAĞILIMI", 100f, curY, textPaint)
            curY += 40f

            val cat = data.categoryStats.firstOrNull()
            if (cat != null) {
                textPaint.apply {
                    textSize = 30f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    color = android.graphics.Color.WHITE
                }
                canvas.drawText("• En Çok: ${cat.category} (${cat.formattedDuration})", 100f, curY, textPaint)
                curY += 50f
            }
        }

        // 8. Motivational Quote Card
        curY += 30f
        val quoteRect = RectF(90f, curY, width - 90f, curY + 200f)
        val quoteBg = Paint().apply {
            color = android.graphics.Color.parseColor("#22201944")
            isAntiAlias = true
        }
        canvas.drawRoundRect(quoteRect, 28f, 28f, quoteBg)

        val quoteBorder = Paint().apply {
            color = android.graphics.Color.parseColor("#44A78BFA")
            style = Paint.Style.STROKE
            strokeWidth = 2f
            isAntiAlias = true
        }
        canvas.drawRoundRect(quoteRect, 28f, 28f, quoteBorder)

        textPaint.apply {
            textSize = 30f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = android.graphics.Color.parseColor("#FF7CF4DE")
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(data.achievementBadge, width / 2f, curY + 70f, textPaint)

        textPaint.apply {
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            color = android.graphics.Color.parseColor("#F2FFFFFF")
        }
        canvas.drawText("\"${data.motivationalQuote.take(55)}\"", width / 2f, curY + 140f, textPaint)

        // 9. Footer Watermark
        val footerY = height - 120f
        textPaint.apply {
            textSize = 26f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = android.graphics.Color.parseColor("#66FFFFFF")
            letterSpacing = 0.08f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("RUTİN APP • DİSİPLİN VE ODAK 🚀", width / 2f, footerY, textPaint)

        return bitmap
    }

    fun shareStoryImage(context: Context, bitmap: Bitmap, shareText: String) {
        try {
            val imagesFolder = File(context.cacheDir, "images").apply { mkdirs() }
            val file = File(imagesFolder, "rutin_story_${System.currentTimeMillis()}.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, shareText)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Başarı Hikayeni Paylaş"))
        } catch (e: Exception) {
            Toast.makeText(context, "Paylaşım hatası: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    fun shareStoryText(context: Context, shareText: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        context.startActivity(Intent.createChooser(intent, "Metin Olarak Paylaş"))
    }
}
