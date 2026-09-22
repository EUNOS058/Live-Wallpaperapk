package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: WallpaperSettings,
    onBack: () -> Unit,
    onUpdateUseCurrentLocation: (Boolean) -> Unit,
    onUpdateManualCity: (String) -> Unit,
    onUpdateIsCelsius: (Boolean) -> Unit,
    onUpdateIs12Hour: (Boolean) -> Unit,
    onUpdateShowTemperature: (Boolean) -> Unit,
    onUpdateShowDate: (Boolean) -> Unit,
    onUpdateShowWeather: (Boolean) -> Unit,
    onUpdateShowNightGreeting: (Boolean) -> Unit,
    onUpdateRainAnimation: (Boolean) -> Unit,
    onUpdateCloudAnimation: (Boolean) -> Unit,
    onUpdateSatelliteCloudOverlay: (Boolean) -> Unit,
    onUpdateAirplaneAnimation: (Boolean) -> Unit,
    onUpdateAnimationQuality: (String) -> Unit,
    onUpdateTheme: (String) -> Unit,
    onUpdateAutoTheme: (Boolean) -> Unit,
    onUpdateHeaderStyle: (String) -> Unit,
    onSelectCustomPhoto: (Uri) -> Unit,
    onClearCustomPhoto: () -> Unit
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    var cityInput by remember(settings.manualCity) { mutableStateOf(settings.manualCity) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            onSelectCustomPhoto(uri)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Wallpaper Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                .verticalScroll(scrollState)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // CLOCK & HEADER STYLE SECTION
            Text(
                "Clock & Weather Header Style",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                HeaderStyleCard(
                    title = "Glassmorphic Card",
                    subtitle = "Frosted glass card with luminous border & shadow",
                    icon = Icons.Default.BlurOn,
                    isSelected = settings.headerStyle == HEADER_STYLE_GLASS,
                    onClick = { onUpdateHeaderStyle(HEADER_STYLE_GLASS) },
                    modifier = Modifier.weight(1f)
                )

                HeaderStyleCard(
                    title = "Minimalist Clean",
                    subtitle = "Floating digital clock with subtle neon glow",
                    icon = Icons.Default.TextFields,
                    isSelected = settings.headerStyle == HEADER_STYLE_MINIMALIST,
                    onClick = { onUpdateHeaderStyle(HEADER_STYLE_MINIMALIST) },
                    modifier = Modifier.weight(1f)
                )
            }

            HorizontalDivider()

            // THEME SELECTION SECTION
            Text(
                "Choose Wallpaper Theme",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            // Auto-Change Theme Switch
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (settings.autoThemeBasedOnWeather)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Dynamic Auto-Change Theme",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            "Auto-switches theme based on day, night, sunrise, sunset, and satellite weather",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Switch(
                        checked = settings.autoThemeBasedOnWeather,
                        onCheckedChange = onUpdateAutoTheme,
                        modifier = Modifier.testTag("auto_theme_switch")
                    )
                }
            }

            // Theme Cards Grid/List
            ThemeSelectionCard(
                title = "Live Earth / Satellite View",
                subtitle = "Orbital Earth view, dynamic satellite clouds, city night lights & space station",
                icon = Icons.Default.Public,
                accentColor = Color(0xFF00A8FF),
                isSelected = !settings.autoThemeBasedOnWeather && settings.theme == THEME_SATELLITE,
                onClick = { onUpdateTheme(THEME_SATELLITE) }
            )

            ThemeSelectionCard(
                title = "Park Bench Night",
                subtitle = "Cozy glowing street lamp, trees & starry quiet night",
                icon = Icons.Default.Park,
                accentColor = Color(0xFFFFD166),
                isSelected = !settings.autoThemeBasedOnWeather && settings.theme == THEME_PARK_BENCH,
                onClick = { onUpdateTheme(THEME_PARK_BENCH) }
            )

            ThemeSelectionCard(
                title = "Nature & Forest",
                subtitle = "Serene pine trees, mountain ridges & glowing fireflies",
                icon = Icons.Default.Forest,
                accentColor = Color(0xFF2ECC71),
                isSelected = !settings.autoThemeBasedOnWeather && settings.theme == THEME_FOREST,
                onClick = { onUpdateTheme(THEME_FOREST) }
            )

            ThemeSelectionCard(
                title = "Minimalist Sky",
                subtitle = "Clean geometric gradient, stylish celestial moon & smooth clouds",
                icon = Icons.Default.FilterDrama,
                accentColor = Color(0xFF00CEC9),
                isSelected = !settings.autoThemeBasedOnWeather && settings.theme == THEME_MINIMALIST,
                onClick = { onUpdateTheme(THEME_MINIMALIST) }
            )

            ThemeSelectionCard(
                title = "Neon City Lights",
                subtitle = "Dark cityscape, illuminated windows & wet asphalt reflections",
                icon = Icons.Default.LocationCity,
                accentColor = Color(0xFFE84393),
                isSelected = !settings.autoThemeBasedOnWeather && settings.theme == THEME_CITY,
                onClick = { onUpdateTheme(THEME_CITY) }
            )

            // Custom Photo Theme Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onUpdateTheme(THEME_CUSTOM) },
                shape = RoundedCornerShape(16.dp),
                border = if (!settings.autoThemeBasedOnWeather && settings.theme == THEME_CUSTOM)
                    BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                else
                    BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                colors = CardDefaults.cardColors(
                    containerColor = if (!settings.autoThemeBasedOnWeather && settings.theme == THEME_CUSTOM)
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                    else
                        MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF6C5CE7).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.AddPhotoAlternate,
                                contentDescription = null,
                                tint = Color(0xFF6C5CE7)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Custom Photo Background", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(
                                "Your own gallery photo with live weather overlays",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        RadioButton(
                            selected = !settings.autoThemeBasedOnWeather && settings.theme == THEME_CUSTOM,
                            onClick = { onUpdateTheme(THEME_CUSTOM) }
                        )
                    }

                    if (!settings.autoThemeBasedOnWeather && settings.theme == THEME_CUSTOM) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(if (settings.customImagePath != null) "Change Photo" else "Pick from Gallery")
                            }

                            if (settings.customImagePath != null) {
                                OutlinedButton(
                                    onClick = onClearCustomPhoto,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Reset")
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider()

            // Location Settings Section
            Text("Satellite GPS Location & Weather", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Auto-Acquire Satellite GPS Coordinates")
                    Text(
                        "Coords: ${String.format("%.2f° N, %.2f° E", settings.latitude, settings.longitude)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = settings.useCurrentLocation,
                    onCheckedChange = onUpdateUseCurrentLocation,
                    modifier = Modifier.testTag("gps_switch")
                )
            }

            if (!settings.useCurrentLocation) {
                OutlinedTextField(
                    value = cityInput,
                    onValueChange = { cityInput = it },
                    label = { Text("City Name (e.g. Benapole, Dhaka)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("city_input"),
                    singleLine = true,
                    trailingIcon = {
                        TextButton(onClick = { onUpdateManualCity(cityInput) }) {
                            Text("Save")
                        }
                    }
                )
            }

            HorizontalDivider()

            // Units & Format Section
            Text("Units & Format", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Temperature Unit (°C / °F)")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("°F", modifier = Modifier.padding(end = 4.dp))
                    Switch(
                        checked = settings.isCelsius,
                        onCheckedChange = onUpdateIsCelsius,
                        modifier = Modifier.testTag("unit_switch")
                    )
                    Text("°C", modifier = Modifier.padding(start = 4.dp))
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Time Format (12-Hour AM/PM)")
                Switch(
                    checked = settings.is12HourFormat,
                    onCheckedChange = onUpdateIs12Hour,
                    modifier = Modifier.testTag("time_format_switch")
                )
            }

            HorizontalDivider()

            // Display Elements Section
            Text("Wallpaper Information Display", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)

            SettingSwitchRow("Show Temperature", settings.showTemperature, onUpdateShowTemperature)
            SettingSwitchRow("Show Date", settings.showDate, onUpdateShowDate)
            SettingSwitchRow("Show Weather Condition", settings.showWeather, onUpdateShowWeather)
            SettingSwitchRow("Show Bengali Night Greeting (শুভ রাত্রি 🌙)", settings.showNightGreeting, onUpdateShowNightGreeting)

            HorizontalDivider()

            // Animation & Satellite Cloud Section
            Text("Animations & Satellite Overlays", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)

            SettingSwitchRow("Show Animated Flights / Airplanes", settings.airplaneFlightAnimationEnabled, onUpdateAirplaneAnimation, "airplane_switch")
            SettingSwitchRow("Live Satellite Cloud Movement Overlay", settings.satelliteCloudOverlay, onUpdateSatelliteCloudOverlay)
            SettingSwitchRow("Rain Particle Animation", settings.rainAnimationEnabled, onUpdateRainAnimation)
            SettingSwitchRow("Atmospheric Cloud Animation", settings.cloudAnimationEnabled, onUpdateCloudAnimation)

            // Animation Quality Selector
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Animation Quality & Battery Mode", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Low", "Medium", "High").forEach { quality ->
                        FilterChip(
                            selected = settings.animationQuality == quality,
                            onClick = { onUpdateAnimationQuality(quality) },
                            label = { Text(if (quality == "Low") "Low (Battery Saver)" else quality) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
fun HeaderStyleCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        border = if (isSelected)
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        else
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(30.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                fontSize = 11.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            RadioButton(selected = isSelected, onClick = onClick)
        }
    }
}

@Composable
fun ThemeSelectionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        border = if (isSelected)
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        else
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = accentColor)
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            RadioButton(
                selected = isSelected,
                onClick = onClick
            )
        }
    }
}

@Composable
fun SettingSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = if (testTag != null) Modifier.testTag(testTag) else Modifier
        )
    }
}
