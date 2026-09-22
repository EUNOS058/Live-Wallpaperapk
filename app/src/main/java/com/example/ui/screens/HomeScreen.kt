package com.example.ui.screens

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.wallpaper.WeatherWallpaperService
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    weatherState: WeatherUiState,
    settings: WallpaperSettings,
    onNavigateToSettings: () -> Unit,
    onRefreshWeather: () -> Unit,
    onRequestLocationPermission: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            onRequestLocationPermission()
        }
    }

    val themeName = when {
        settings.autoThemeBasedOnWeather -> "Auto Theme (Adaptive)"
        settings.theme == THEME_SATELLITE -> "Live Earth / Satellite View"
        settings.theme == THEME_FOREST -> "Nature & Forest"
        settings.theme == THEME_MINIMALIST -> "Minimalist Sky"
        settings.theme == THEME_CITY -> "Neon City Lights"
        settings.theme == THEME_CUSTOM -> "Custom Photo Background"
        else -> "Park Bench Night"
    }

    val heroGradient = when {
        settings.theme == THEME_SATELLITE -> listOf(Color(0xFF000206), Color(0xFF06152F), Color(0xFF0A2B52))
        settings.theme == THEME_FOREST -> listOf(Color(0xFF0D1F1C), Color(0xFF1E3F3B), Color(0xFF2C5E55))
        settings.theme == THEME_MINIMALIST -> listOf(Color(0xFF09122C), Color(0xFF1B2A4A), Color(0xFF2C3E6B))
        settings.theme == THEME_CITY -> listOf(Color(0xFF0F0A1C), Color(0xFF21133B), Color(0xFF381A59))
        settings.theme == THEME_CUSTOM -> listOf(Color(0xFF1A1A24), Color(0xFF252636), Color(0xFF33354A))
        else -> listOf(Color(0xFF0B132B), Color(0xFF1C2541), Color(0xFF3A506B))
    }

    val isGlass = settings.headerStyle == HEADER_STYLE_GLASS

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Live Weather Wallpaper", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onRefreshWeather, modifier = Modifier.testTag("refresh_button")) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh Weather")
                    }
                    IconButton(onClick = onNavigateToSettings, modifier = Modifier.testTag("settings_button")) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    )
                )
                .verticalScroll(scrollState)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Hero Atmospheric Preview Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(360.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(Brush.linearGradient(heroGradient)),
                contentAlignment = Alignment.Center
            ) {
                // Live Animated Weather & Flight Canvas
                WeatherView(
                    settings = settings,
                    weatherState = weatherState,
                    modifier = Modifier.fillMaxSize()
                )

                // Frosted Glassmorphism or Minimalist Header Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    shape = RoundedCornerShape(22.dp),
                    border = if (isGlass) BorderStroke(1.5.dp, Color.White.copy(alpha = 0.35f)) else null,
                    colors = CardDefaults.cardColors(
                        containerColor = if (isGlass)
                            Color(0xFF0F172A).copy(alpha = 0.65f)
                        else
                            Color.Transparent
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 18.dp, horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Location Pill
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.15f),
                            modifier = Modifier.padding(bottom = 10.dp)
                        ) {
                            Text(
                                text = if (settings.theme == THEME_SATELLITE) "🛰️ ${weatherState.cityName.uppercase()} • SATELLITE" else "📍 ${weatherState.cityName.uppercase()}",
                                color = Color(0xFFE2E8F0),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }

                        // Digital Clock (Bold, Elegant)
                        val timeFormat = if (settings.is12HourFormat) "h:mm" else "HH:mm"
                        val timeDigits = SimpleDateFormat(timeFormat, Locale.getDefault()).format(Date())
                        val amPm = if (settings.is12HourFormat) SimpleDateFormat("a", Locale.getDefault()).format(Date()).uppercase() else ""

                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = timeDigits,
                                color = Color.White,
                                fontSize = 48.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            if (amPm.isNotEmpty()) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = amPm,
                                    color = if (!isGlass) Color(0xFF74B9FF) else Color.White.copy(alpha = 0.85f),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Integrated Weather & Temperature
                        val tempVal = if (settings.isCelsius) {
                            "${weatherState.temperatureC.toInt()}°C"
                        } else {
                            "${(weatherState.temperatureC * 9 / 5 + 32).toInt()}°F"
                        }

                        val weatherSummary = buildString {
                            if (settings.showTemperature) append("🌡️ $tempVal")
                            if (settings.showTemperature && settings.showWeather) append("   •   ")
                            if (settings.showWeather) append("☁️ ${weatherState.weatherCondition}")
                        }

                        if (weatherSummary.isNotEmpty()) {
                            Text(
                                text = weatherSummary,
                                color = Color(0xFFF1F5F9),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }

                        // Date Display
                        if (settings.showDate) {
                            val dateStr = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date())
                            Text(
                                text = "📅 $dateStr",
                                color = Color(0xFFCBD5E1),
                                fontSize = 13.sp
                            )
                        }

                        // Bengali Night Greeting
                        if (weatherState.isNight && settings.showNightGreeting) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "শুভ রাত্রি 🌙",
                                color = Color(0xFFFFD166),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Theme Tag at bottom-right
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.Black.copy(alpha = 0.45f)
                    ) {
                        Text(
                            text = "Theme: $themeName • Style: ${if (isGlass) "Glass" else "Clean"}",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Set as Live Wallpaper Button
            Button(
                onClick = {
                    try {
                        val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                            putExtra(
                                WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                                ComponentName(context, WeatherWallpaperService::class.java)
                            )
                        }
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        try {
                            val fallbackIntent = Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER)
                            context.startActivity(fallbackIntent)
                        } catch (ex: Exception) {
                            // ignore
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("set_wallpaper_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.Default.Palette, contentDescription = null, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Set as Live Wallpaper",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Quick Theme / Customize Button
            OutlinedButton(
                onClick = onNavigateToSettings,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Customize Theme, Clock & Satellite", fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(20.dp))

            // SATELLITE WEATHER TELEMETRY CARD
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, Color(0xFF00A8FF).copy(alpha = 0.25f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Public,
                                contentDescription = null,
                                tint = Color(0xFF00A8FF),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Satellite Weather Telemetry",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF00A8FF).copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "LIVE SYNC",
                                color = Color(0xFF00A8FF),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        WeatherDetailItem(label = "Satellite Clouds", value = "${weatherState.satelliteCloudCover}%")
                        WeatherDetailItem(label = "Precipitation", value = "${weatherState.precipitationRate} mm/h")
                        WeatherDetailItem(label = "Wind Velocity", value = "${weatherState.windSpeed} m/s")
                    }

                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Satellite GPS Coordinates",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = weatherState.satelliteCoordinates,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        IconButton(
                            onClick = {
                                locationPermissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
                            }
                        ) {
                            Icon(Icons.Default.MyLocation, contentDescription = "Acquire Satellite GPS Fix", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Standard Atmospheric Details Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Atmospheric Details",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        WeatherDetailItem(label = "Humidity", value = "${weatherState.humidity}%")
                        WeatherDetailItem(label = "Wind Bearing", value = "${weatherState.windDirectionDeg}°")
                        WeatherDetailItem(label = "Cloudiness", value = "${weatherState.cloudiness}%")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (settings.useCurrentLocation) {
                OutlinedButton(
                    onClick = {
                        locationPermissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.GpsFixed, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Acquire Satellite GPS Coordinates")
                }
            }
        }
    }
}

@Composable
fun WeatherDetailItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
    }
}
