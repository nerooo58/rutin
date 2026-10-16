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
import com.nororoutine.batu.model.DAY_STRING_TO_INT
import com.nororoutine.batu.model.Routine
import com.nororoutine.batu.model.RoutineLog
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
    val totalFocusSeconds: Int,
    val completedCount: Int,
    val totalCount: Int,
    val completionPercent: Int,
    val currentStreak: Int,
    val categoryStats: List<CategoryFocusStat>,
    val topRoutineName: String?,
    val topRoutineDuration: String?,
    val achievementBadge: String,
    val motivationalQuote: String
) {
    val totalHours: Int get() = totalFocusSeconds / 3600
    val totalMinutes: Int get() = (totalFocusSeconds % 3600) / 60
    val formattedTotalFocus: String
        get() = when {
            totalHours > 0 && totalMinutes > 0 -> "${totalHours}sa ${totalMinutes}dk"
            totalHours > 0 -> "${totalHours} saat"
            totalMinutes > 0 -> "${totalMinutes} dakika"
            totalFocusSeconds > 0 -> "${totalFocusSeconds} sn"
            else -> "0 dakika"
        }

    fun buildShareText(): String {
        val sb = StringBuilder()
        sb.append("🌟 Rutin Başarı Hikayem 🚀\n")
        sb.append("📅 Dönem: $dateSubtitle ($periodTitle)\n")
        sb.append("⏱️ Toplam Odak: $formattedTotalFocus\n")
        sb.append("✅ Tamamlama: %$completionPercent ($completedCount/$totalCount Rutin)\n")
        if (currentStreak > 0) {
            sb.append("🔥 Seri: $currentStreak Gün!\n")
        }
        if (categoryStats.isNotEmpty()) {
            sb.append("\n🎯 Odak Dağılımı:\n")
            categoryStats.take(4).forEach { cat ->
                sb.append("• ${cat.category}: ${cat.formattedDuration}\n")
            }
        }
        if (!topRoutineName.isNullOrBlank() && !topRoutineDuration.isNullOrBlank()) {
            sb.append("⭐ En Çok Odaklanılan: $topRoutineName ($topRoutineDuration)\n")
        }
        sb.append("\n\"$motivationalQuote\"\n")
        sb.append("✨ Rutin App ile takip edildi.")
        return sb.toString()
    }
}

object StoryCardHelper {

    fun computeStoryData(
        period: StoryPeriod,
        uiState: RutinUiState,
        anchorDate: LocalDate = LocalDate.now()
    ): StoryCardData {
        val locale = Locale("tr", "TR")
        val routines = uiState.routines
        val logs = uiState.logs
        val streak = uiState.stats.currentStreak

        return when (period) {
            StoryPeriod.DAILY -> {
                val dateStr = anchorDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
                val dayOfWeek = anchorDate.dayOfWeek.value
                val dayRoutines = routines.filter { it.daysIntSet.contains(dayOfWeek) }
                val totalCount = dayRoutines.size
                val completedLogs = logs.values.filter { it.date == dateStr && it.isDone }
                val completedCount = completedLogs.size
                val percent = if (totalCount > 0) (completedCount * 100 / totalCount).coerceIn(0, 100) else 0

                val categoryStats = calculateCategoryStats(completedLogs, routines)
                val totalSeconds = categoryStats.sumOf { it.totalSeconds }
                val (topName, topDur) = findTopRoutine(completedLogs, routines)

                val (badge, quote) = getMotivationalQuote(percent, streak)

                StoryCardData(
                    period = period,
                    periodTitle = "Günün Başarısı",
                    dateSubtitle = anchorDate.format(DateTimeFormatter.ofPattern("d MMMM yyyy, EEEE", locale)),
                    totalFocusSeconds = totalSeconds,
                    completedCount = completedCount,
                    totalCount = totalCount,
                    completionPercent = percent,
                    currentStreak = streak,
                    categoryStats = categoryStats,
                    topRoutineName = topName,
                    topRoutineDuration = topDur,
                    achievementBadge = badge,
                    motivationalQuote = quote
                )
            }

            StoryPeriod.WEEKLY -> {
                val monday = anchorDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                val sunday = anchorDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
                val mondayStr = monday.format(DateTimeFormatter.ISO_LOCAL_DATE)
                val sundayStr = sunday.format(DateTimeFormatter.ISO_LOCAL_DATE)

                val weekLogs = logs.values.filter { it.date in mondayStr..sundayStr && it.isDone }
                val completedCount = weekLogs.size
                val totalWeekly = uiState.stats.weeklyTotal.takeIf { it > 0 } ?: (routines.size * 5)
                val percent = if (totalWeekly > 0) (completedCount * 100 / totalWeekly).coerceIn(0, 100) else uiState.stats.weeklyPercent

                val categoryStats = calculateCategoryStats(weekLogs, routines)
                val totalSeconds = categoryStats.sumOf { it.totalSeconds }
                val (topName, topDur) = findTopRoutine(weekLogs, routines)

                val (badge, quote) = getMotivationalQuote(percent, streak)

                val rangeLabel = "${monday.dayOfMonth} – ${sunday.dayOfMonth} ${sunday.format(DateTimeFormatter.ofPattern("MMMM yyyy", locale))}"
                StoryCardData(
                    period = period,
                    periodTitle = "Haftalık Rapor",
                    dateSubtitle = rangeLabel,
                    totalFocusSeconds = totalSeconds,
                    completedCount = completedCount,
                    totalCount = totalWeekly,
                    completionPercent = percent,
                    currentStreak = streak,
                    categoryStats = categoryStats,
                    topRoutineName = topName,
                    topRoutineDuration = topDur,
                    achievementBadge = badge,
                    motivationalQuote = quote
                )
            }

            StoryPeriod.MONTHLY -> {
                val yearMonthPrefix = anchorDate.format(DateTimeFormatter.ofPattern("yyyy-MM"))
                val monthLogs = logs.values.filter { it.date.startsWith(yearMonthPrefix) && it.isDone }
                val completedCount = monthLogs.size
                val totalMonthly = uiState.stats.monthlyTotal.takeIf { it > 0 } ?: (routines.size * 22)
                val percent = if (totalMonthly > 0) (completedCount * 100 / totalMonthly).coerceIn(0, 100) else uiState.stats.monthlyPercent

                val categoryStats = calculateCategoryStats(monthLogs, routines)
                val totalSeconds = categoryStats.sumOf { it.totalSeconds }
                val (topName, topDur) = findTopRoutine(monthLogs, routines)

                val (badge, quote) = getMotivationalQuote(percent, streak)

                StoryCardData(
                    period = period,
                    periodTitle = "Aylık Rapor",
                    dateSubtitle = anchorDate.format(DateTimeFormatter.ofPattern("MMMM yyyy", locale)).replaceFirstChar { it.uppercase() },
                    totalFocusSeconds = totalSeconds,
                    completedCount = completedCount,
                    totalCount = totalMonthly,
                    completionPercent = percent,
                    currentStreak = streak,
                    categoryStats = categoryStats,
                    topRoutineName = topName,
                    topRoutineDuration = topDur,
                    achievementBadge = badge,
                    motivationalQuote = quote
                )
            }

            StoryPeriod.YEARLY -> {
                val yearPrefix = anchorDate.year.toString()
                val yearLogs = logs.values.filter { it.date.startsWith(yearPrefix) && it.isDone }
                val completedCount = yearLogs.size
                val totalYearly = uiState.stats.yearlyTotal.takeIf { it > 0 } ?: (routines.size * 260)
                val percent = if (totalYearly > 0) (completedCount * 100 / totalYearly).coerceIn(0, 100) else uiState.stats.yearlyPercent

                val categoryStats = calculateCategoryStats(yearLogs, routines)
                val totalSeconds = categoryStats.sumOf { it.totalSeconds }
                val (topName, topDur) = findTopRoutine(yearLogs, routines)

                val (badge, quote) = getMotivationalQuote(percent, streak)

                StoryCardData(
                    period = period,
                    periodTitle = "Yıllık Başarı",
                    dateSubtitle = "${anchorDate.year} Yılı Özeti",
                    totalFocusSeconds = totalSeconds,
                    completedCount = completedCount,
                    totalCount = totalYearly,
                    completionPercent = percent,
                    currentStreak = streak,
                    categoryStats = categoryStats,
                    topRoutineName = topName,
                    topRoutineDuration = topDur,
                    achievementBadge = badge,
                    motivationalQuote = quote
                )
            }
        }
    }

    private fun calculateCategoryStats(
        completedLogs: List<RoutineLog>,
        routines: List<Routine>
    ): List<CategoryFocusStat> {
        val routineMap = routines.associateBy { it.id }
        val categorySeconds = mutableMapOf<String, Int>()
        val categoryCounts = mutableMapOf<String, Int>()

        for (log in completedLogs) {
            val routine = routineMap[log.routineId] ?: continue
            val cat = routine.category.ifBlank { "Genel" }
            val seconds = if (log.focusDurationSeconds > 0) {
                log.focusDurationSeconds
            } else if (routine.durationMinutes > 0) {
                routine.durationMinutes * 60
            } else {
                15 * 60 // 15 min estimated completion time
            }
            categorySeconds[cat] = (categorySeconds[cat] ?: 0) + seconds
            categoryCounts[cat] = (categoryCounts[cat] ?: 0) + 1
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

    private fun findTopRoutine(
        completedLogs: List<RoutineLog>,
        routines: List<Routine>
    ): Pair<String?, String?> {
        val routineMap = routines.associateBy { it.id }
        val routineSeconds = mutableMapOf<String, Int>()

        for (log in completedLogs) {
            val routine = routineMap[log.routineId] ?: continue
            val seconds = if (log.focusDurationSeconds > 0) {
                log.focusDurationSeconds
            } else if (routine.durationMinutes > 0) {
                routine.durationMinutes * 60
            } else {
                15 * 60
            }
            routineSeconds[routine.name] = (routineSeconds[routine.name] ?: 0) + seconds
        }

        val top = routineSeconds.maxByOrNull { it.value } ?: return Pair(null, null)
        val h = top.value / 3600
        val m = (top.value % 3600) / 60
        val durStr = when {
            h > 0 && m > 0 -> "${h}sa ${m}dk"
            h > 0 -> "${h} saat"
            m > 0 -> "${m} dakika"
            else -> "${top.value} sn"
        }
        return Pair(top.key, durStr)
    }

    private fun getMotivationalQuote(percent: Int, streak: Int): Pair<String, String> {
        return when {
            percent >= 90 && streak >= 5 -> Pair("🏆 Kusursuz Ritim", "Muhteşem bir istikrar ve disiplin sergiliyorsun!")
            percent >= 80 -> Pair("🌟 Harika Odaklanma", "Günün ve dönemin hedeflerine güçlü adımlarla ulaştın.")
            percent >= 60 -> Pair("🔥 Yüksek Verim", "Ritmini koruyorsun, her çaba geleceğe büyük bir yatırım.")
            percent >= 40 -> Pair("✨ Kararlı Gelişim", "Süreklilik mükemmellikten daha değerlidir, harika gidiyorsun!")
            else -> Pair("🌱 Yeni Başlangıç", "Küçük rutinler büyük başarıların tohumudur. Devam et!")
        }
    }

    /**
     * Renders a pristine 1080x1920 9:16 high-resolution Instagram / WhatsApp story bitmap
     */
    fun createStoryBitmap(context: Context, data: StoryCardData): Bitmap {
        val width = 1080
        val height = 1920
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 1. Deep Midnight Background Gradient
        val bgPaint = Paint().apply {
            shader = LinearGradient(
                0f, 0f, 0f, height.toFloat(),
                intArrayOf(
                    android.graphics.Color.parseColor("#090714"),
                    android.graphics.Color.parseColor("#121026"),
                    android.graphics.Color.parseColor("#181B36")
                ),
                null,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // 2. Ambient Glow Mesh Circles
        val glowPaintTeal = Paint().apply {
            shader = RadialGradient(
                width * 0.85f, height * 0.22f, 480f,
                intArrayOf(android.graphics.Color.parseColor("#337CF4DE"), android.graphics.Color.TRANSPARENT),
                null,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawCircle(width * 0.85f, height * 0.22f, 480f, glowPaintTeal)

        val glowPaintPurple = Paint().apply {
            shader = RadialGradient(
                width * 0.15f, height * 0.70f, 520f,
                intArrayOf(android.graphics.Color.parseColor("#44A78BFA"), android.graphics.Color.TRANSPARENT),
                null,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawCircle(width * 0.15f, height * 0.70f, 520f, glowPaintPurple)

        // 3. Main Glass Frame (Border)
        val frameRect = RectF(50f, 80f, width - 50f, height - 90f)
        val framePaint = Paint().apply {
            color = android.graphics.Color.parseColor("#14FFFFFF")
            style = Paint.Style.STROKE
            strokeWidth = 3f
            isAntiAlias = true
        }
        canvas.drawRoundRect(frameRect, 48f, 48f, framePaint)

        // Text Paints
        val textPaint = Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.WHITE
        }

        // Header: App Logo & Badge
        textPaint.apply {
            textSize = 34f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.15f
            textAlign = Paint.Align.CENTER
            color = android.graphics.Color.parseColor("#FFA78BFA")
        }
        canvas.drawText("✦ RUTİN • BAŞARI HİKAYESİ ✦", width / 2f, 170f, textPaint)

        // Period Title & Date
        textPaint.apply {
            textSize = 58f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = -0.02f
            color = android.graphics.Color.WHITE
        }
        canvas.drawText(data.periodTitle, width / 2f, 255f, textPaint)

        textPaint.apply {
            textSize = 32f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            color = android.graphics.Color.parseColor("#B3FFFFFF")
            letterSpacing = 0.02f
        }
        canvas.drawText(data.dateSubtitle, width / 2f, 310f, textPaint)

        // 4. Hero Card: Toplam Odak Süresi
        val heroRect = RectF(90f, 370f, width - 90f, 740f)
        val heroBg = Paint().apply {
            shader = LinearGradient(
                heroRect.left, heroRect.top, heroRect.right, heroRect.bottom,
                intArrayOf(android.graphics.Color.parseColor("#26382B6B"), android.graphics.Color.parseColor("#1F1E3860")),
                null,
                Shader.TileMode.CLAMP
            )
            isAntiAlias = true
        }
        canvas.drawRoundRect(heroRect, 36f, 36f, heroBg)

        val heroBorder = Paint().apply {
            color = android.graphics.Color.parseColor("#4DA78BFA")
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
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
        canvas.drawText("TOPLAM ODAKLANMA SÜRESİ", width / 2f, 440f, textPaint)

        // Duration Big Text
        textPaint.apply {
            textSize = 96f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = android.graphics.Color.WHITE
            letterSpacing = -0.03f
        }
        canvas.drawText(data.formattedTotalFocus, width / 2f, 555f, textPaint)

        // Two Pills: % Tamamlama & Seri
        val pillY = 620f
        val pillHeight = 70f
        val leftPillRect = RectF(130f, pillY, 510f, pillY + pillHeight)
        val rightPillRect = RectF(570f, pillY, width - 130f, pillY + pillHeight)

        val greenPillPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#264ADE80")
            isAntiAlias = true
        }
        canvas.drawRoundRect(leftPillRect, 35f, 35f, greenPillPaint)
        val greenPillBorder = Paint().apply {
            color = android.graphics.Color.parseColor("#804ADE80")
            style = Paint.Style.STROKE
            strokeWidth = 2f
            isAntiAlias = true
        }
        canvas.drawRoundRect(leftPillRect, 35f, 35f, greenPillBorder)

        textPaint.apply {
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = android.graphics.Color.parseColor("#FF4ADE80")
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("✓ %${data.completionPercent} (${data.completedCount}/${data.totalCount})", (leftPillRect.left + leftPillRect.right) / 2f, pillY + 45f, textPaint)

        val amberPillPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#33FBBF24")
            isAntiAlias = true
        }
        canvas.drawRoundRect(rightPillRect, 35f, 35f, amberPillPaint)
        val amberPillBorder = Paint().apply {
            color = android.graphics.Color.parseColor("#80FBBF24")
            style = Paint.Style.STROKE
            strokeWidth = 2f
            isAntiAlias = true
        }
        canvas.drawRoundRect(rightPillRect, 35f, 35f, amberPillBorder)

        textPaint.color = android.graphics.Color.parseColor("#FFFBBF24")
        val streakText = if (data.currentStreak > 0) "🔥 ${data.currentStreak} Gün Seri" else "✨ Aktif Ritim"
        canvas.drawText(streakText, (rightPillRect.left + rightPillRect.right) / 2f, pillY + 45f, textPaint)

        // 5. Section: Odak Dağılımı (Ders, Spor, Zihin vs.)
        textPaint.apply {
            textSize = 32f
            letterSpacing = 0.10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = android.graphics.Color.parseColor("#B3FFFFFF")
            textAlign = Paint.Align.LEFT
        }
        canvas.drawText("KATEGORİ BAZLI ODAK SÜRELERİ", 100f, 820f, textPaint)

        var curY = 870f
        val maxSeconds = data.categoryStats.maxOfOrNull { it.totalSeconds }?.coerceAtLeast(1) ?: 1

        val barBgPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#1AFFFFFF")
            isAntiAlias = true
        }

        val catsToDraw = data.categoryStats.take(4)
        if (catsToDraw.isNotEmpty()) {
            for (cat in catsToDraw) {
                // Category Label
                textPaint.apply {
                    textSize = 30f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    color = android.graphics.Color.WHITE
                    textAlign = Paint.Align.LEFT
                }
                canvas.drawText(cat.category, 100f, curY + 30f, textPaint)

                // Duration text on the right
                textPaint.apply {
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    color = cat.color.toArgb()
                    textAlign = Paint.Align.RIGHT
                }
                canvas.drawText(cat.formattedDuration, width - 100f, curY + 30f, textPaint)

                // Progress Bar
                val barRect = RectF(100f, curY + 48f, width - 100f, curY + 66f)
                canvas.drawRoundRect(barRect, 9f, 9f, barBgPaint)

                val ratio = (cat.totalSeconds.toFloat() / maxSeconds.toFloat()).coerceIn(0.08f, 1f)
                val fillRect = RectF(100f, curY + 48f, 100f + (width - 200f) * ratio, curY + 66f)
                val fillPaint = Paint().apply {
                    color = cat.color.toArgb()
                    isAntiAlias = true
                }
                canvas.drawRoundRect(fillRect, 9f, 9f, fillPaint)

                curY += 105f
            }
        } else {
            textPaint.apply {
                textSize = 28f
                color = android.graphics.Color.parseColor("#80FFFFFF")
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("Henüz kaydedilmiş odak süresi bulunmuyor.", width / 2f, curY + 50f, textPaint)
            curY += 100f
        }

        // 6. Top Routine Spotlight (if available)
        if (!data.topRoutineName.isNullOrBlank() && !data.topRoutineDuration.isNullOrBlank()) {
            val spotRect = RectF(90f, curY + 30f, width - 90f, curY + 160f)
            val spotBg = Paint().apply {
                color = android.graphics.Color.parseColor("#22201944")
                isAntiAlias = true
            }
            canvas.drawRoundRect(spotRect, 28f, 28f, spotBg)
            val spotBorder = Paint().apply {
                color = android.graphics.Color.parseColor("#33A78BFA")
                style = Paint.Style.STROKE
                strokeWidth = 2f
                isAntiAlias = true
            }
            canvas.drawRoundRect(spotRect, 28f, 28f, spotBorder)

            textPaint.apply {
                textSize = 26f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                color = android.graphics.Color.parseColor("#FFA78BFA")
                textAlign = Paint.Align.LEFT
            }
            canvas.drawText("⭐ EN ÇOK ODAKLANILAN RUTİN", 130f, curY + 75f, textPaint)

            textPaint.apply {
                textSize = 34f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                color = android.graphics.Color.WHITE
            }
            canvas.drawText(data.topRoutineName, 130f, curY + 125f, textPaint)

            textPaint.apply {
                textSize = 32f
                color = android.graphics.Color.parseColor("#FF7CF4DE")
                textAlign = Paint.Align.RIGHT
            }
            canvas.drawText(data.topRoutineDuration, width - 130f, curY + 125f, textPaint)

            curY += 180f
        } else {
            curY += 30f
        }

        // 7. Motivational Quote & Badge Card
        val quoteRect = RectF(90f, curY + 20f, width - 90f, curY + 200f)
        val quoteBg = Paint().apply {
            color = android.graphics.Color.parseColor("#1AFFFFFF")
            isAntiAlias = true
        }
        canvas.drawRoundRect(quoteRect, 28f, 28f, quoteBg)

        textPaint.apply {
            textSize = 32f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = android.graphics.Color.parseColor("#FF7CF4DE")
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(data.achievementBadge, width / 2f, curY + 80f, textPaint)

        textPaint.apply {
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            color = android.graphics.Color.parseColor("#E6FFFFFF")
        }
        canvas.drawText("\"${data.motivationalQuote}\"", width / 2f, curY + 145f, textPaint)

        // 8. Footer Watermark
        val footerY = height - 120f
        textPaint.apply {
            textSize = 26f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = android.graphics.Color.parseColor("#66FFFFFF")
            letterSpacing = 0.08f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("RUTİN APP • HER GÜN DAHA İYİ BİR SEN ✨", width / 2f, footerY, textPaint)

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
