package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "wallpaper_settings")

const val THEME_PARK_BENCH = "park_bench"
const val THEME_FOREST = "nature_forest"
const val THEME_MINIMALIST = "minimalist_sky"
const val THEME_CITY = "neon_city"
const val THEME_SATELLITE = "satellite_earth"
const val THEME_CUSTOM = "custom_photo"

const val HEADER_STYLE_GLASS = "glassmorphic"
const val HEADER_STYLE_MINIMALIST = "minimalist_text"

data class WallpaperSettings(
    val useCurrentLocation: Boolean = true,
    val manualCity: String = "Benapole",
    val latitude: Double = 23.04,
    val longitude: Double = 88.89,
    val isCelsius: Boolean = true,
    val is12HourFormat: Boolean = true,
    val showTemperature: Boolean = true,
    val showDate: Boolean = true,
    val showWeather: Boolean = true,
    val showNightGreeting: Boolean = true,
    val rainAnimationEnabled: Boolean = true,
    val cloudAnimationEnabled: Boolean = true,
    val satelliteCloudOverlay: Boolean = true,
    val airplaneFlightAnimationEnabled: Boolean = true,
    val animationQuality: String = "Medium", // Low, Medium, High
    val theme: String = THEME_PARK_BENCH,
    val autoThemeBasedOnWeather: Boolean = false,
    val customImagePath: String? = null,
    val headerStyle: String = HEADER_STYLE_GLASS
)

class SettingsRepository(private val context: Context) {

    companion object {
        private val USE_CURRENT_LOCATION = booleanPreferencesKey("use_current_location")
        private val MANUAL_CITY = stringPreferencesKey("manual_city")
        private val LATITUDE = doublePreferencesKey("latitude")
        private val LONGITUDE = doublePreferencesKey("longitude")
        private val IS_CELSIUS = booleanPreferencesKey("is_celsius")
        private val IS_12_HOUR = booleanPreferencesKey("is_12_hour")
        private val SHOW_TEMP = booleanPreferencesKey("show_temp")
        private val SHOW_DATE = booleanPreferencesKey("show_date")
        private val SHOW_WEATHER = booleanPreferencesKey("show_weather")
        private val SHOW_GREETING = booleanPreferencesKey("show_greeting")
        private val RAIN_ANIM = booleanPreferencesKey("rain_anim")
        private val CLOUD_ANIM = booleanPreferencesKey("cloud_anim")
        private val SATELLITE_CLOUD = booleanPreferencesKey("satellite_cloud")
        private val AIRPLANE_ANIM = booleanPreferencesKey("airplane_anim")
        private val ANIM_QUALITY = stringPreferencesKey("anim_quality")
        private val THEME_KEY = stringPreferencesKey("wallpaper_theme")
        private val AUTO_THEME_KEY = booleanPreferencesKey("auto_theme_weather")
        private val CUSTOM_IMAGE_PATH_KEY = stringPreferencesKey("custom_image_path")
        private val HEADER_STYLE_KEY = stringPreferencesKey("header_style")
    }

    val settingsFlow: Flow<WallpaperSettings> = context.dataStore.data
        .map { prefs ->
            WallpaperSettings(
                useCurrentLocation = prefs[USE_CURRENT_LOCATION] ?: true,
                manualCity = prefs[MANUAL_CITY] ?: "Benapole",
                latitude = prefs[LATITUDE] ?: 23.04,
                longitude = prefs[LONGITUDE] ?: 88.89,
                isCelsius = prefs[IS_CELSIUS] ?: true,
                is12HourFormat = prefs[IS_12_HOUR] ?: true,
                showTemperature = prefs[SHOW_TEMP] ?: true,
                showDate = prefs[SHOW_DATE] ?: true,
                showWeather = prefs[SHOW_WEATHER] ?: true,
                showNightGreeting = prefs[SHOW_GREETING] ?: true,
                rainAnimationEnabled = prefs[RAIN_ANIM] ?: true,
                cloudAnimationEnabled = prefs[CLOUD_ANIM] ?: true,
                satelliteCloudOverlay = prefs[SATELLITE_CLOUD] ?: true,
                airplaneFlightAnimationEnabled = prefs[AIRPLANE_ANIM] ?: true,
                animationQuality = prefs[ANIM_QUALITY] ?: "Medium",
                theme = prefs[THEME_KEY] ?: THEME_PARK_BENCH,
                autoThemeBasedOnWeather = prefs[AUTO_THEME_KEY] ?: false,
                customImagePath = prefs[CUSTOM_IMAGE_PATH_KEY],
                headerStyle = prefs[HEADER_STYLE_KEY] ?: HEADER_STYLE_GLASS
            )
        }

    suspend fun updateUseCurrentLocation(value: Boolean) {
        context.dataStore.edit { it[USE_CURRENT_LOCATION] = value }
    }

    suspend fun updateManualCity(city: String) {
        context.dataStore.edit { it[MANUAL_CITY] = city }
    }

    suspend fun updateCoordinates(lat: Double, lon: Double) {
        context.dataStore.edit {
            it[LATITUDE] = lat
            it[LONGITUDE] = lon
        }
    }

    suspend fun updateIsCelsius(value: Boolean) {
        context.dataStore.edit { it[IS_CELSIUS] = value }
    }

    suspend fun updateIs12Hour(value: Boolean) {
        context.dataStore.edit { it[IS_12_HOUR] = value }
    }

    suspend fun updateShowTemperature(value: Boolean) {
        context.dataStore.edit { it[SHOW_TEMP] = value }
    }

    suspend fun updateShowDate(value: Boolean) {
        context.dataStore.edit { it[SHOW_DATE] = value }
    }

    suspend fun updateShowWeather(value: Boolean) {
        context.dataStore.edit { it[SHOW_WEATHER] = value }
    }

    suspend fun updateShowNightGreeting(value: Boolean) {
        context.dataStore.edit { it[SHOW_GREETING] = value }
    }

    suspend fun updateRainAnimation(value: Boolean) {
        context.dataStore.edit { it[RAIN_ANIM] = value }
    }

    suspend fun updateCloudAnimation(value: Boolean) {
        context.dataStore.edit { it[CLOUD_ANIM] = value }
    }

    suspend fun updateSatelliteCloudOverlay(value: Boolean) {
        context.dataStore.edit { it[SATELLITE_CLOUD] = value }
    }

    suspend fun updateAirplaneAnimation(value: Boolean) {
        context.dataStore.edit { it[AIRPLANE_ANIM] = value }
    }

    suspend fun updateAnimationQuality(quality: String) {
        context.dataStore.edit { it[ANIM_QUALITY] = quality }
    }

    suspend fun updateTheme(theme: String) {
        context.dataStore.edit { it[THEME_KEY] = theme }
    }

    suspend fun updateAutoTheme(enabled: Boolean) {
        context.dataStore.edit { it[AUTO_THEME_KEY] = enabled }
    }

    suspend fun updateCustomImagePath(path: String?) {
        context.dataStore.edit {
            if (path != null) {
                it[CUSTOM_IMAGE_PATH_KEY] = path
            } else {
                it.remove(CUSTOM_IMAGE_PATH_KEY)
            }
        }
    }

    suspend fun updateHeaderStyle(style: String) {
        context.dataStore.edit { it[HEADER_STYLE_KEY] = style }
    }
}
