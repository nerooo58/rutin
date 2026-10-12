package com.nororoutine.batu.network

import android.content.Context
import android.location.Location
import android.location.LocationManager
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
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
    val district: String = "",
    val city: String = ""
)

object WeatherService {

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
                    val temp = current.optDouble("temperature", 20.0)
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
        // Fallback default
        WeatherInfo(
            temperature = 21.0,
            weatherCode = 1,
            condition = WeatherCondition.CLEAR,
            windSpeed = 10.0,
            isDay = true
        )
    }

    suspend fun reverseGeocodeLocation(context: Context, lat: Double, lon: Double): Pair<String, String> = withContext(Dispatchers.IO) {
        try {
            val geocoder = android.location.Geocoder(context, java.util.Locale("tr", "TR"))
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(lat, lon, 1)
            if (!addresses.isNullOrEmpty()) {
                val addr = addresses[0]
                val district = addr.subAdminArea ?: addr.subLocality ?: addr.locality.orEmpty()
                val city = addr.adminArea ?: addr.locality.orEmpty()
                return@withContext Pair(district, city)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        Pair("", "")
    }

    suspend fun getLastKnownLocation(context: Context): Pair<Double, Double>? = withContext(Dispatchers.IO) {
        try {
            val fusedClient = LocationServices.getFusedLocationProviderClient(context)
            suspendCancellableCoroutine { cont ->
                fusedClient.lastLocation
                    .addOnSuccessListener { loc: Location? ->
                        if (loc != null) {
                            cont.resume(Pair(loc.latitude, loc.longitude))
                        } else {
                            // Fallback to LocationManager
                            val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                            val gpsLoc = try { lm?.getLastKnownLocation(LocationManager.GPS_PROVIDER) } catch (e: SecurityException) { null }
                            val netLoc = try { lm?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER) } catch (e: SecurityException) { null }
                            val chosen = gpsLoc ?: netLoc
                            if (chosen != null) {
                                cont.resume(Pair(chosen.latitude, chosen.longitude))
                            } else {
                                cont.resume(null)
                            }
                        }
                    }
                    .addOnFailureListener {
                        cont.resume(null)
                    }
            }
        } catch (e: SecurityException) {
            null
        } catch (e: Exception) {
            null
        }
    }
}

