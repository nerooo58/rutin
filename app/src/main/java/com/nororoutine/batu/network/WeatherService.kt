package com.nororoutine.batu.network

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.resume

enum class WeatherCondition(val label: String) {
    CLEAR("Açık"),
    CLOUDY("Bulutlu"),
    RAINY("Yağmurlu"),
    STORMY("Fırtınalı"),
    SNOWY("Karlı")
}

data class WeatherInfo(
    val temperature: Double = 22.0,
    val weatherCode: Int = 0,
    val condition: WeatherCondition = WeatherCondition.CLEAR,
    val windSpeed: Double = 12.0,
    val isDay: Boolean = true,
    val locationName: String = "Mevcut Konum",
    val district: String = "Konak",
    val city: String = "İzmir"
)

object WeatherService {

    // Default fallback coordinates: İzmir (Konak)
    const val IZMIR_LAT = 38.4237
    const val IZMIR_LON = 27.1428
    const val IZMIR_CITY = "İzmir"
    const val IZMIR_DISTRICT = "Konak"

    fun mapWeatherCode(code: Int): WeatherCondition {
        return when (code) {
            0, 1 -> WeatherCondition.CLEAR
            2, 3, 45, 48 -> WeatherCondition.CLOUDY
            51, 53, 55, 61, 63, 65, 80, 81, 82 -> WeatherCondition.RAINY
            71, 73, 75, 77, 85, 86 -> WeatherCondition.SNOWY
            95, 96, 99 -> WeatherCondition.STORMY
            else -> WeatherCondition.CLEAR
        }
    }

    suspend fun fetchWeather(lat: Double, lon: Double): WeatherInfo = withContext(Dispatchers.IO) {
        try {
            val urlString = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current_weather=true"
            val url = URL(urlString)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 8000
                requestMethod = "GET"
                setRequestProperty("User-Agent", "RutinAndroidApp/1.0")
            }

            if (connection.responseCode == 200) {
                val jsonString = connection.inputStream.bufferedReader().use { it.readText() }
                val root = JSONObject(jsonString)
                val current = root.optJSONObject("current_weather")
                if (current != null) {
                    val temp = current.optDouble("temperature", 22.0)
                    val code = current.optInt("weathercode", 0)
                    val wind = current.optDouble("windspeed", 10.0)
                    val isDayInt = current.optInt("is_day", 1)
                    return@withContext WeatherInfo(
                        temperature = temp,
                        weatherCode = code,
                        condition = mapWeatherCode(code),
                        windSpeed = wind,
                        isDay = isDayInt == 1
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        // Fallback default (İzmir)
        WeatherInfo(
            temperature = 22.0,
            weatherCode = 1,
            condition = WeatherCondition.CLEAR,
            windSpeed = 10.0,
            isDay = true,
            district = IZMIR_DISTRICT,
            city = IZMIR_CITY
        )
    }

    suspend fun reverseGeocodeLocation(context: Context, lat: Double, lon: Double): Pair<String, String> = withContext(Dispatchers.IO) {
        // 1. Try Android built-in Geocoder
        try {
            if (android.location.Geocoder.isPresent()) {
                val geocoder = android.location.Geocoder(context, java.util.Locale("tr", "TR"))
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(lat, lon, 1)
                if (!addresses.isNullOrEmpty()) {
                    val addr = addresses[0]
                    val district = addr.subAdminArea
                        ?: addr.subLocality
                        ?: addr.locality.orEmpty()
                    val city = addr.adminArea
                        ?: addr.locality.orEmpty()
                    if (city.isNotBlank() || district.isNotBlank()) {
                        return@withContext Pair(district.trim(), city.trim())
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. HTTP Reverse Geocoding fallback via free BigDataCloud API
        try {
            val urlString = "https://api.bigdatacloud.net/data/reverse-geocode-client?latitude=$lat&longitude=$lon&localityLanguage=tr"
            val url = URL(urlString)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 4000
                readTimeout = 4000
                requestMethod = "GET"
                setRequestProperty("User-Agent", "RutinAndroidApp/1.0")
            }
            if (connection.responseCode == 200) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)
                val city = json.optString("principalSubdivision", json.optString("city", ""))
                val district = json.optString("locality", json.optString("subLocality", ""))
                if (city.isNotBlank() || district.isNotBlank()) {
                    return@withContext Pair(district.trim(), city.trim())
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        Pair("", "")
    }

    suspend fun getLastKnownLocation(context: Context): Pair<Double, Double>? = withContext(Dispatchers.IO) {
        // 1. Verify runtime permissions
        val fineGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!fineGranted && !coarseGranted) {
            return@withContext null
        }

        // 2. Check if Location service is actually enabled on the device
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        val isGpsEnabled = lm?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true
        val isNetEnabled = lm?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true

        if (!isGpsEnabled && !isNetEnabled) {
            // Location is turned off on device -> returns null so caller falls back to İzmir!
            return@withContext null
        }

        try {
            val fusedClient = LocationServices.getFusedLocationProviderClient(context)

            // 3. Attempt fresh GPS / Network fix via getCurrentLocation with a 4-second timeout
            val freshLocation: Location? = withTimeoutOrNull(4000L) {
                suspendCancellableCoroutine { cont ->
                    val cts = CancellationTokenSource()
                    cont.invokeOnCancellation { cts.cancel() }
                    try {
                        fusedClient.getCurrentLocation(
                            Priority.PRIORITY_HIGH_ACCURACY,
                            cts.token
                        ).addOnSuccessListener { loc: Location? ->
                            if (cont.isActive) cont.resume(loc)
                        }.addOnFailureListener {
                            if (cont.isActive) cont.resume(null)
                        }
                    } catch (e: SecurityException) {
                        if (cont.isActive) cont.resume(null)
                    } catch (e: Exception) {
                        if (cont.isActive) cont.resume(null)
                    }
                }
            }

            if (freshLocation != null) {
                return@withContext Pair(freshLocation.latitude, freshLocation.longitude)
            }

            // 4. Fallback to cached fusedClient.lastLocation
            val lastLoc: Location? = suspendCancellableCoroutine { cont ->
                try {
                    fusedClient.lastLocation
                        .addOnSuccessListener { loc: Location? ->
                            if (cont.isActive) cont.resume(loc)
                        }
                        .addOnFailureListener {
                            if (cont.isActive) cont.resume(null)
                        }
                } catch (e: SecurityException) {
                    if (cont.isActive) cont.resume(null)
                } catch (e: Exception) {
                    if (cont.isActive) cont.resume(null)
                }
            }

            if (lastLoc != null) {
                return@withContext Pair(lastLoc.latitude, lastLoc.longitude)
            }

            // 5. Fallback to Android native LocationManager providers
            if (lm != null) {
                val gpsLoc = try { lm.getLastKnownLocation(LocationManager.GPS_PROVIDER) } catch (e: SecurityException) { null }
                val netLoc = try { lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER) } catch (e: SecurityException) { null }
                val passLoc = try { lm.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER) } catch (e: SecurityException) { null }
                val chosen = gpsLoc ?: netLoc ?: passLoc
                if (chosen != null) {
                    return@withContext Pair(chosen.latitude, chosen.longitude)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        null
    }
}
