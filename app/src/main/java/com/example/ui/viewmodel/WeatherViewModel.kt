package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.net.Uri
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

class WeatherViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsRepository = SettingsRepository(application)
    private val weatherRepository = WeatherRepository()
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(application)

    val settings: StateFlow<WallpaperSettings> = settingsRepository.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), WallpaperSettings())

    private val _weatherState = MutableStateFlow(WeatherUiState())
    val weatherState: StateFlow<WeatherUiState> = _weatherState.asStateFlow()

    init {
        loadWeather()
    }

    fun loadWeather() {
        viewModelScope.launch {
            _weatherState.value = _weatherState.value.copy(isLoading = true)
            val currentSettings = settings.value
            val state = if (currentSettings.useCurrentLocation) {
                weatherRepository.fetchWeatherByCoords(currentSettings.latitude, currentSettings.longitude)
            } else {
                weatherRepository.fetchWeather(currentSettings.manualCity)
            }
            _weatherState.value = state
        }
    }

    fun fetchGpsLocation(context: Context) {
        viewModelScope.launch {
            if (ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                    .addOnSuccessListener { location ->
                        if (location != null) {
                            viewModelScope.launch {
                                settingsRepository.updateCoordinates(location.latitude, location.longitude)
                                val cityName = try {
                                    val geocoder = Geocoder(context, Locale.getDefault())
                                    val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                                    addresses?.firstOrNull()?.locality
                                        ?: addresses?.firstOrNull()?.subAdminArea
                                        ?: addresses?.firstOrNull()?.adminArea
                                } catch (e: Exception) {
                                    null
                                }

                                val state = weatherRepository.fetchWeatherByCoords(location.latitude, location.longitude)
                                val finalState = if (!cityName.isNullOrBlank()) {
                                    state.copy(cityName = cityName)
                                } else {
                                    state
                                }
                                _weatherState.value = finalState
                            }
                        } else {
                            loadWeather()
                        }
                    }
                    .addOnFailureListener {
                        loadWeather()
                    }
            } else {
                loadWeather()
            }
        }
    }

    fun updateUseCurrentLocation(value: Boolean) {
        viewModelScope.launch { settingsRepository.updateUseCurrentLocation(value) }
    }

    fun updateManualCity(city: String) {
        viewModelScope.launch {
            settingsRepository.updateManualCity(city)
            loadWeather()
        }
    }

    fun updateIsCelsius(value: Boolean) {
        viewModelScope.launch { settingsRepository.updateIsCelsius(value) }
    }

    fun updateIs12Hour(value: Boolean) {
        viewModelScope.launch { settingsRepository.updateIs12Hour(value) }
    }

    fun updateShowTemperature(value: Boolean) {
        viewModelScope.launch { settingsRepository.updateShowTemperature(value) }
    }

    fun updateShowDate(value: Boolean) {
        viewModelScope.launch { settingsRepository.updateShowDate(value) }
    }

    fun updateShowWeather(value: Boolean) {
        viewModelScope.launch { settingsRepository.updateShowWeather(value) }
    }

    fun updateShowNightGreeting(value: Boolean) {
        viewModelScope.launch { settingsRepository.updateShowNightGreeting(value) }
    }

    fun updateRainAnimation(value: Boolean) {
        viewModelScope.launch { settingsRepository.updateRainAnimation(value) }
    }

    fun updateCloudAnimation(value: Boolean) {
        viewModelScope.launch { settingsRepository.updateCloudAnimation(value) }
    }

    fun updateSatelliteCloudOverlay(value: Boolean) {
        viewModelScope.launch { settingsRepository.updateSatelliteCloudOverlay(value) }
    }

    fun updateAirplaneAnimation(value: Boolean) {
        viewModelScope.launch { settingsRepository.updateAirplaneAnimation(value) }
    }

    fun updateAnimationQuality(quality: String) {
        viewModelScope.launch { settingsRepository.updateAnimationQuality(quality) }
    }

    fun updateTheme(theme: String) {
        viewModelScope.launch { settingsRepository.updateTheme(theme) }
    }

    fun updateAutoTheme(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateAutoTheme(enabled) }
    }

    fun updateHeaderStyle(style: String) {
        viewModelScope.launch { settingsRepository.updateHeaderStyle(style) }
    }

    fun setCustomBackgroundImage(uri: Uri, context: Context) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    if (inputStream != null) {
                        val destinationFile = File(context.filesDir, "custom_wallpaper_bg.jpg")
                        FileOutputStream(destinationFile).use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }
                        inputStream.close()
                        settingsRepository.updateCustomImagePath(destinationFile.absolutePath)
                        settingsRepository.updateTheme(THEME_CUSTOM)
                    }
                } catch (e: Exception) {
                    Log.e("WeatherViewModel", "Failed to save custom wallpaper image", e)
                }
            }
        }
    }

    fun clearCustomBackgroundImage() {
        viewModelScope.launch {
            settingsRepository.updateCustomImagePath(null)
        }
    }
}
