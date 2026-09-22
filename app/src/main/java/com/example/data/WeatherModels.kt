package com.example.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class WeatherResponse(
    @Json(name = "name") val name: String?,
    @Json(name = "coord") val coord: Coord?,
    @Json(name = "main") val main: Main?,
    @Json(name = "weather") val weather: List<Weather>?,
    @Json(name = "wind") val wind: Wind?,
    @Json(name = "clouds") val clouds: Clouds?,
    @Json(name = "rain") val rain: Rain?,
    @Json(name = "sys") val sys: Sys?
)

@JsonClass(generateAdapter = true)
data class Coord(
    @Json(name = "lat") val lat: Double?,
    @Json(name = "lon") val lon: Double?
)

@JsonClass(generateAdapter = true)
data class Main(
    @Json(name = "temp") val temp: Double?,
    @Json(name = "humidity") val humidity: Int?,
    @Json(name = "pressure") val pressure: Int?
)

@JsonClass(generateAdapter = true)
data class Weather(
    @Json(name = "id") val id: Int?,
    @Json(name = "main") val main: String?,
    @Json(name = "description") val description: String?,
    @Json(name = "icon") val icon: String?
)

@JsonClass(generateAdapter = true)
data class Wind(
    @Json(name = "speed") val speed: Double?,
    @Json(name = "deg") val deg: Int?
)

@JsonClass(generateAdapter = true)
data class Clouds(
    @Json(name = "all") val all: Int?
)

@JsonClass(generateAdapter = true)
data class Rain(
    @Json(name = "1h") val oneHour: Double?
)

@JsonClass(generateAdapter = true)
data class Sys(
    @Json(name = "sunrise") val sunrise: Long?,
    @Json(name = "sunset") val sunset: Long?,
    @Json(name = "country") val country: String?
)

data class WeatherUiState(
    val cityName: String = "Benapole",
    val temperatureC: Double = 31.0,
    val weatherCondition: String = "Partly Cloudy",
    val cloudiness: Int = 40,
    val humidity: Int = 65,
    val windSpeed: Double = 3.5,
    val windDirectionDeg: Int = 180,
    val precipitationRate: Double = 0.0,
    val satelliteCloudCover: Int = 42,
    val satelliteCoordinates: String = "23.04° N, 88.89° E",
    val satelliteStatus: String = "Orbit Sync Active",
    val isRainy: Boolean = false,
    val isNight: Boolean = false,
    val isSunrise: Boolean = false,
    val isSunset: Boolean = false,
    val lastUpdatedTime: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
