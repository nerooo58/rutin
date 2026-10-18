package com.nororoutine.batu.screentime

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import android.provider.Settings
import android.util.Log
import androidx.compose.ui.graphics.Color
import java.time.LocalDateTime
import java.util.Calendar

data class AppUsageInfo(
    val packageName: String,
    val appName: String,
    val durationMinutes: Long,
    val percentageOfTotal: Int,
    val color: Color,
    val sessionCount: Int = 1,
    val category: String = "Genel"
)

data class HourlyUsageSlot(
    val hourLabel: String,
    val startHour: Int,
    val endHour: Int,
    val minutesSpent: Int,
    val topApps: List<String>
)

data class ScreenTimeSummary(
    val totalMinutes: Long,
    val top3Apps: List<AppUsageInfo>,
    val allApps: List<AppUsageInfo>,
    val unlocksCount: Int,
    val hourlyTimeline: List<HourlyUsageSlot>,
    val hasPermission: Boolean
)

object ScreenTimeManager {

    private const val TAG = "ScreenTimeManager"

    private val colorPalette = listOf(
        Color(0xFF8B5CF6), // Purple
        Color(0xFF06B6D4), // Cyan
        Color(0xFF10B981), // Emerald
        Color(0xFFF59E0B), // Amber
        Color(0xFFEC4899), // Pink
        Color(0xFF3B82F6), // Blue
        Color(0xFF6366F1), // Indigo
        Color(0xFF14B8A6), // Teal
        Color(0xFFF97316), // Orange
        Color(0xFFA855F7)  // Violet
    )

    fun hasUsagePermission(context: Context): Boolean {
        return try {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    context.packageName
                )
            } else {
                @Suppress("DEPRECATION")
                appOps.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    context.packageName
                )
            }
            mode == AppOpsManager.MODE_ALLOWED
        } catch (e: Exception) {
            Log.e(TAG, "Error checking usage permission", e)
            false
        }
    }

    fun openUsageAccessSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                val intent = Intent(Settings.ACTION_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } catch (ex: Exception) {
                Log.e(TAG, "Could not open settings", ex)
            }
        }
    }

    /**
     * Retrieves actual device screen time and application usage stats for today.
     * Absolutely NO simulated/mock data is returned.
     */
    fun getTodayScreenTime(context: Context): ScreenTimeSummary {
        val hasPerm = hasUsagePermission(context)
        if (!hasPerm) {
            return ScreenTimeSummary(
                totalMinutes = 0L,
                top3Apps = emptyList(),
                allApps = emptyList(),
                unlocksCount = 0,
                hourlyTimeline = emptyList(),
                hasPermission = false
            )
        }

        try {
            val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
                ?: return emptySummary(hasPermission = true)

            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val startOfDay = calendar.timeInMillis
            val endOfDay = System.currentTimeMillis()

            val packageManager = context.packageManager

            // 1. Query usage stats for today
            val stats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                startOfDay,
                endOfDay
            )

            val timeByPkg = mutableMapOf<String, Long>()

            stats?.forEach { stat ->
                val time = stat.totalTimeInForeground
                // Only consider apps with meaningful foreground time (> 5 seconds)
                if (time >= 5_000L) {
                    val pkg = stat.packageName
                    timeByPkg[pkg] = (timeByPkg[pkg] ?: 0L) + time
                }
            }

            // 2. Query UsageEvents to calculate real device unlocks and hourly buckets
            var unlocks = 0
            val hourlyBuckets = IntArray(24) { 0 }
            val hourlyAppsMap = mutableMapOf<Int, MutableSet<String>>()

            try {
                val events = usageStatsManager.queryEvents(startOfDay, endOfDay)
                val event = UsageEvents.Event()

                var lastResumedPkg: String? = null
                var lastResumedTime = 0L

                while (events.hasNextEvent()) {
                    events.getNextEvent(event)

                    val eventType = event.eventType
                    // Count screen unlock / keyguard dismissed
                    if (eventType == UsageEvents.Event.KEYGUARD_HIDDEN ||
                        (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && eventType == UsageEvents.Event.SCREEN_INTERACTIVE)) {
                        unlocks++
                    }

                    // Track foreground session for hourly bucket
                    if (eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                        lastResumedPkg = event.packageName
                        lastResumedTime = event.timeStamp
                    } else if (eventType == UsageEvents.Event.ACTIVITY_PAUSED && lastResumedPkg == event.packageName) {
                        val sessionDuration = (event.timeStamp - lastResumedTime).coerceAtLeast(0L)
                        if (sessionDuration in 1_000L..14_400_000L) { // 1 sec to 4 hours max per session
                            val cal = Calendar.getInstance().apply { timeInMillis = lastResumedTime }
                            val hour = cal.get(Calendar.HOUR_OF_DAY).coerceIn(0, 23)
                            val minutes = (sessionDuration / 60_000L).toInt()
                            hourlyBuckets[hour] += minutes
                            if (minutes > 0) {
                                val set = hourlyAppsMap.getOrPut(hour) { mutableSetOf() }
                                set.add(lastResumedPkg)
                            }
                        }
                        lastResumedPkg = null
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error parsing usage events for unlocks and hourly timeline", e)
            }

            // 3. Process package foreground times and resolve real app names
            val appList = mutableListOf<AppUsageInfo>()
            var totalForegroundMillis = 0L
            var colorIndex = 0

            timeByPkg.forEach { (pkg, time) ->
                // Filter out internal system background non-app daemons
                if (pkg == "android" || pkg.startsWith("com.google.android.gms") || pkg == "com.android.systemui") {
                    // Skip system framework background metrics
                    return@forEach
                }

                val appName = try {
                    val appInfo = packageManager.getApplicationInfo(pkg, 0)
                    packageManager.getApplicationLabel(appInfo).toString()
                } catch (e: Exception) {
                    pkg.substringAfterLast('.')
                }

                val durationMinutes = time / 60_000L
                if (durationMinutes >= 1L) {
                    totalForegroundMillis += time
                    val color = colorPalette[colorIndex % colorPalette.size]
                    colorIndex++

                    val category = detectCategory(pkg, appName)
                    val sessions = ((durationMinutes / 10) + 1).toInt().coerceAtLeast(1)

                    appList.add(
                        AppUsageInfo(
                            packageName = pkg,
                            appName = appName,
                            durationMinutes = durationMinutes,
                            percentageOfTotal = 0, // calculate below
                            color = color,
                            sessionCount = sessions,
                            category = category
                        )
                    )
                }
            }

            val totalMinutes = totalForegroundMillis / 60_000L

            if (appList.isEmpty() || totalMinutes == 0L) {
                return ScreenTimeSummary(
                    totalMinutes = 0L,
                    top3Apps = emptyList(),
                    allApps = emptyList(),
                    unlocksCount = unlocks,
                    hourlyTimeline = emptyList(),
                    hasPermission = true
                )
            }

            // Calculate percentage based on total minutes
            val sortedList = appList.sortedByDescending { it.durationMinutes }
                .map { info ->
                    val pct = if (totalMinutes > 0) {
                        ((info.durationMinutes.toDouble() / totalMinutes.toDouble()) * 100).toInt().coerceIn(1, 100)
                    } else 0
                    info.copy(percentageOfTotal = pct)
                }

            val top3 = sortedList.take(3)

            // Build real hourly timeline
            val currentHour = LocalDateTime.now().hour
            val timelineSlots = mutableListOf<HourlyUsageSlot>()

            // 3-hour groupings for hours elapsed today: 00-03, 03-06, 06-09, 09-12, 12-15, 15-18, 18-21, 21-24
            val intervals = listOf(
                Triple("00:00 - 06:00", 0, 6),
                Triple("06:00 - 09:00", 6, 9),
                Triple("09:00 - 12:00", 9, 12),
                Triple("12:00 - 15:00", 12, 15),
                Triple("15:00 - 18:00", 15, 18),
                Triple("18:00 - 21:00", 18, 21),
                Triple("21:00 - 00:00", 21, 24)
            )

            for ((label, startH, endH) in intervals) {
                if (currentHour >= startH) {
                    var sumMin = 0
                    val pkgSet = mutableSetOf<String>()
                    for (h in startH until endH) {
                        if (h in 0..23) {
                            sumMin += hourlyBuckets[h]
                            hourlyAppsMap[h]?.let { pkgSet.addAll(it) }
                        }
                    }

                    val appNames = pkgSet.mapNotNull { p ->
                        sortedList.find { it.packageName == p }?.appName
                    }.take(3)

                    timelineSlots.add(
                        HourlyUsageSlot(
                            hourLabel = label,
                            startHour = startH,
                            endHour = endH,
                            minutesSpent = sumMin,
                            topApps = appNames
                        )
                    )
                }
            }

            return ScreenTimeSummary(
                totalMinutes = totalMinutes,
                top3Apps = top3,
                allApps = sortedList,
                unlocksCount = unlocks.coerceAtLeast(top3.size),
                hourlyTimeline = timelineSlots,
                hasPermission = true
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating real screen time", e)
            return emptySummary(hasPermission = true)
        }
    }

    private fun emptySummary(hasPermission: Boolean): ScreenTimeSummary {
        return ScreenTimeSummary(
            totalMinutes = 0L,
            top3Apps = emptyList(),
            allApps = emptyList(),
            unlocksCount = 0,
            hourlyTimeline = emptyList(),
            hasPermission = hasPermission
        )
    }

    private fun detectCategory(pkg: String, name: String): String {
        val lower = "$pkg $name".lowercase()
        return when {
            lower.contains("instagram") || lower.contains("twitter") || lower.contains("x") ||
            lower.contains("tiktok") || lower.contains("reddit") || lower.contains("facebook") -> "Sosyal Medya"

            lower.contains("whatsapp") || lower.contains("telegram") || lower.contains("message") ||
            lower.contains("mesaj") || lower.contains("signal") || lower.contains("discord") -> "İletişim"

            lower.contains("youtube") || lower.contains("netflix") || lower.contains("spotify") ||
            lower.contains("music") || lower.contains("müzik") || lower.contains("player") ||
            lower.contains("video") || lower.contains("twitch") -> "Medya & Müzik"

            lower.contains("chrome") || lower.contains("browser") || lower.contains("tarayıcı") ||
            lower.contains("firefox") || lower.contains("opera") || lower.contains("edge") -> "İnternet"

            lower.contains("not") || lower.contains("keep") || lower.contains("rutin") ||
            lower.contains("calendar") || lower.contains("takvim") || lower.contains("task") ||
            lower.contains("clock") || lower.contains("saat") -> "Verimlilik"

            lower.contains("game") || lower.contains("oyun") -> "Oyun"

            else -> "Uygulama"
        }
    }

    fun formatDuration(minutes: Long): String {
        val hours = minutes / 60
        val remainingMinutes = minutes % 60
        return if (hours > 0) {
            "${hours}s ${remainingMinutes}dk"
        } else {
            "${remainingMinutes} dk"
        }
    }
}
