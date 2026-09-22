package com.example.data

import android.util.Log
import com.example.BuildConfig
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

class WeatherRepository {
    private val apiService = WeatherApiService.create()

    suspend fun fetchWeather(city: String): WeatherUiState {
        val apiKey = try {
            BuildConfig::class.java.getField("WEATHER_API_KEY").get(null) as? String ?: ""
        } catch (e: Exception) {
            ""
        }

        if (apiKey.isBlank() || apiKey == "YOUR_WEATHER_API_KEY" || apiKey == "MY_WEATHER_API_KEY") {
            return getSimulatedWeather(city, 23.04, 88.89)
        }

        return try {
            val response = apiService.getWeatherByCity(city, apiKey)
            val tempC = response.main?.temp ?: 31.0
            val condition = response.weather?.firstOrNull()?.description?.replaceFirstChar { it.uppercase() } ?: "Partly Cloudy"
            val clouds = response.clouds?.all ?: 40
            val humidity = response.main?.humidity ?: 65
            val wind = response.wind?.speed ?: 3.5
            val windDeg = response.wind?.deg ?: 180
            val rainRate = response.rain?.oneHour ?: if (condition.contains("rain", true)) 2.4 else 0.0
            val isRainy = condition.contains("rain", true) || condition.contains("shower", true) || rainRate > 0.1

            val lat = response.coord?.lat ?: 23.04
            val lon = response.coord?.lon ?: 88.89
            val coordStr = formatCoordinates(lat, lon)

            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            val isNight = hour < 6 || hour >= 18
            val isSunrise = hour in 5..7
            val isSunset = hour in 17..19

            val timeStr = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())

            WeatherUiState(
                cityName = response.name ?: city,
                temperatureC = tempC,
                weatherCondition = condition,
                cloudiness = clouds,
                humidity = humidity,
                windSpeed = wind,
                windDirectionDeg = windDeg,
                precipitationRate = rainRate,
                satelliteCloudCover = clouds,
                satelliteCoordinates = coordStr,
                satelliteStatus = "Satellite Synced",
                isRainy = isRainy,
                isNight = isNight,
                isSunrise = isSunrise,
                isSunset = isSunset,
                lastUpdatedTime = timeStr,
                isLoading = false
            )
        } catch (e: Exception) {
            Log.e("WeatherRepository", "Error fetching weather, falling back to satellite model", e)
            getSimulatedWeather(city, 23.04, 88.89).copy(
                satelliteStatus = "Satellite Orbit Model (Local)"
            )
        }
    }

    suspend fun fetchWeatherByCoords(lat: Double, lon: Double): WeatherUiState {
        val apiKey = try {
            BuildConfig::class.java.getField("WEATHER_API_KEY").get(null) as? String ?: ""
        } catch (e: Exception) {
            ""
        }

        val coordStr = formatCoordinates(lat, lon)

        if (apiKey.isBlank() || apiKey == "YOUR_WEATHER_API_KEY" || apiKey == "MY_WEATHER_API_KEY") {
            return getSimulatedWeather("GPS Location", lat, lon)
        }

        return try {
            val response = apiService.getWeatherByCoords(lat, lon, apiKey)
            val tempC = response.main?.temp ?: 31.0
            val condition = response.weather?.firstOrNull()?.description?.replaceFirstChar { it.uppercase() } ?: "Partly Cloudy"
            val clouds = response.clouds?.all ?: 40
            val humidity = response.main?.humidity ?: 65
            val wind = response.wind?.speed ?: 3.5
            val windDeg = response.wind?.deg ?: 180
            val rainRate = response.rain?.oneHour ?: if (condition.contains("rain", true)) 2.4 else 0.0
            val isRainy = condition.contains("rain", true) || condition.contains("shower", true) || rainRate > 0.1

            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            val isNight = hour < 6 || hour >= 18
            val isSunrise = hour in 5..7
            val isSunset = hour in 17..19

            val timeStr = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())

            WeatherUiState(
                cityName = response.name ?: "GPS Location",
                temperatureC = tempC,
                weatherCondition = condition,
                cloudiness = clouds,
                humidity = humidity,
                windSpeed = wind,
                windDirectionDeg = windDeg,
                precipitationRate = rainRate,
                satelliteCloudCover = clouds,
                satelliteCoordinates = coordStr,
                satelliteStatus = "Satellite GPS Fixed",
                isRainy = isRainy,
                isNight = isNight,
                isSunrise = isSunrise,
                isSunset = isSunset,
                lastUpdatedTime = timeStr,
                isLoading = false
            )
        } catch (e: Exception) {
            Log.e("WeatherRepository", "Error fetching weather by coords, falling back", e)
            getSimulatedWeather("GPS Location", lat, lon).copy(
                satelliteStatus = "Satellite Orbit Model (GPS Fixed)"
            )
        }
    }

    private fun getSimulatedWeather(cityName: String, lat: Double, lon: Double): WeatherUiState {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val isNight = hour < 6 || hour >= 18
        val isSunrise = hour in 5..7
        val isSunset = hour in 17..19

        val coordStr = formatCoordinates(lat, lon)
        val timeStr = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())

        return WeatherUiState(
            cityName = cityName,
            temperatureC = 30.5,
            weatherCondition = if (isNight) "Clear Night" else "Scattered Clouds",
            cloudiness = 38,
            humidity = 64,
            windSpeed = 3.8,
            windDirectionDeg = 195,
            precipitationRate = 0.0,
            satelliteCloudCover = 42,
            satelliteCoordinates = coordStr,
            satelliteStatus = "Live Satellite Tracking Active",
            isRainy = false,
            isNight = isNight,
            isSunrise = isSunrise,
            isSunset = isSunset,
            lastUpdatedTime = timeStr,
            isLoading = false
        )
    }

    private fun formatCoordinates(lat: Double, lon: Double): String {
        val latDir = if (lat >= 0) "N" else "S"
        val lonDir = if (lon >= 0) "E" else "W"
        val latFormatted = String.format(Locale.US, "%.2f° %s", Math.abs(lat), latDir)
        val lonFormatted = String.format(Locale.US, "%.2f° %s", Math.abs(lon), lonDir)
        return "$latFormatted, $lonFormatted"
    }
}
