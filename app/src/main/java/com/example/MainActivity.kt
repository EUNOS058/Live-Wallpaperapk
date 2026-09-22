package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.WeatherViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: WeatherViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    WeatherWallpaperApp(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun WeatherWallpaperApp(viewModel: WeatherViewModel) {
    val navController = rememberNavController()
    val weatherState by viewModel.weatherState.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                weatherState = weatherState,
                settings = settings,
                onNavigateToSettings = { navController.navigate("settings") },
                onRefreshWeather = { viewModel.loadWeather() },
                onRequestLocationPermission = { viewModel.fetchGpsLocation(context) }
            )
        }
        composable("settings") {
            SettingsScreen(
                settings = settings,
                onBack = { navController.popBackStack() },
                onUpdateUseCurrentLocation = { viewModel.updateUseCurrentLocation(it) },
                onUpdateManualCity = { viewModel.updateManualCity(it) },
                onUpdateIsCelsius = { viewModel.updateIsCelsius(it) },
                onUpdateIs12Hour = { viewModel.updateIs12Hour(it) },
                onUpdateShowTemperature = { viewModel.updateShowTemperature(it) },
                onUpdateShowDate = { viewModel.updateShowDate(it) },
                onUpdateShowWeather = { viewModel.updateShowWeather(it) },
                onUpdateShowNightGreeting = { viewModel.updateShowNightGreeting(it) },
                onUpdateRainAnimation = { viewModel.updateRainAnimation(it) },
                onUpdateCloudAnimation = { viewModel.updateCloudAnimation(it) },
                onUpdateSatelliteCloudOverlay = { viewModel.updateSatelliteCloudOverlay(it) },
                onUpdateAirplaneAnimation = { viewModel.updateAirplaneAnimation(it) },
                onUpdateAnimationQuality = { viewModel.updateAnimationQuality(it) },
                onUpdateTheme = { viewModel.updateTheme(it) },
                onUpdateAutoTheme = { viewModel.updateAutoTheme(it) },
                onUpdateHeaderStyle = { viewModel.updateHeaderStyle(it) },
                onSelectCustomPhoto = { uri -> viewModel.setCustomBackgroundImage(uri, context) },
                onClearCustomPhoto = { viewModel.clearCustomBackgroundImage() }
            )
        }
    }
}
