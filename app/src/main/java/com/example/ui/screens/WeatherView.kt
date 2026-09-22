package com.example.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import com.example.data.*
import kotlinx.coroutines.isActive
import java.io.File
import java.util.Calendar
import kotlin.math.*
import kotlin.random.Random

data class ComposeAirplaneFlight(
    var x: Float = -150f,
    var y: Float = 180f,
    var speedX: Float = 2.4f,
    var headingDeg: Float = 6f,
    var size: Float = 36f,
    var callsign: String = "BG 088",
    var altitudeFt: Int = 34000,
    var isActive: Boolean = false,
    var cooldownTicks: Int = 0,
    var beaconTick: Int = 0,
    val contrailPoints: MutableList<ContrailPointData> = mutableListOf()
)

data class ContrailPointData(
    var leftX: Float,
    var leftY: Float,
    var rightX: Float,
    var rightY: Float,
    var alpha: Float,
    var width: Float
)

data class ComposeCloud(var x: Float, var y: Float, var speed: Float, var scale: Float)
data class ComposeRainDrop(var x: Float, var y: Float, var speed: Float, var length: Float)
data class ComposeStar(val xRatio: Float, val yRatio: Float, val alpha: Float, val radius: Float)

@Composable
fun WeatherView(
    settings: WallpaperSettings,
    weatherState: WeatherUiState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var frameTick by remember { mutableLongStateOf(0L) }

    // Custom image bitmap state
    var customImageBitmap by remember(settings.customImagePath) {
        mutableStateOf<ImageBitmap?>(null)
    }

    LaunchedEffect(settings.customImagePath) {
        val path = settings.customImagePath
        if (path != null && File(path).exists()) {
            try {
                val bmp = BitmapFactory.decodeFile(path)
                if (bmp != null) {
                    customImageBitmap = bmp.asImageBitmap()
                }
            } catch (e: Exception) {
                customImageBitmap = null
            }
        } else {
            customImageBitmap = null
        }
    }

    // Animation elements
    val clouds = remember {
        mutableStateListOf(
            ComposeCloud(40f, 70f, 0.45f, 0.9f),
            ComposeCloud(220f, 130f, 0.35f, 1.1f),
            ComposeCloud(450f, 90f, 0.5f, 0.8f),
            ComposeCloud(680f, 160f, 0.3f, 1.2f)
        )
    }

    val rainDrops = remember {
        val list = mutableListOf<ComposeRainDrop>()
        for (i in 0..45) {
            list.add(
                ComposeRainDrop(
                    Random.nextFloat() * 1000f,
                    Random.nextFloat() * 800f,
                    Random.nextFloat() * 12f + 16f,
                    Random.nextFloat() * 16f + 10f
                )
            )
        }
        list
    }

    val stars = remember {
        val list = mutableListOf<ComposeStar>()
        for (i in 0..40) {
            list.add(
                ComposeStar(
                    Random.nextFloat(),
                    Random.nextFloat() * 0.6f,
                    Random.nextFloat() * 0.7f + 0.3f,
                    Random.nextFloat() * 2f + 1f
                )
            )
        }
        list
    }

    val airplane = remember {
        ComposeAirplaneFlight(
            x = -120f,
            y = 120f,
            speedX = 2.2f,
            headingDeg = 5f,
            callsign = "BG 088",
            altitudeFt = 34000,
            isActive = true
        )
    }

    // Continuous smooth animation loop
    LaunchedEffect(Unit) {
        while (isActive) {
            withInfiniteAnimationFrameMillis {
                frameTick++
            }
        }
    }

    Box(modifier = modifier.testTag("weather_view_canvas")) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            val calendar = Calendar.getInstance()
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val isNight = hour < 6 || hour >= 18
            val isSunrise = hour in 5..7
            val isSunset = hour in 17..19

            val isRainy = weatherState.weatherCondition.contains("Rain", ignoreCase = true) ||
                    weatherState.weatherCondition.contains("Drizzle", ignoreCase = true) ||
                    weatherState.weatherCondition.contains("Thunder", ignoreCase = true)

            val activeTheme = if (settings.autoThemeBasedOnWeather) {
                when {
                    isRainy -> THEME_CITY
                    isSunrise -> THEME_FOREST
                    isSunset -> THEME_MINIMALIST
                    isNight -> THEME_SATELLITE
                    else -> THEME_FOREST
                }
            } else {
                settings.theme
            }

            // 1. Draw atmospheric sky gradient
            drawAtmosphericBackground(
                theme = activeTheme,
                isNight = isNight,
                isSunrise = isSunrise,
                isSunset = isSunset,
                customBitmap = customImageBitmap,
                width = width,
                height = height
            )

            // 2. Stars & Moon (Night)
            if (isNight && activeTheme != THEME_SATELLITE) {
                for (star in stars) {
                    val sx = star.xRatio * width
                    val sy = star.yRatio * height
                    drawCircle(
                        color = Color.White.copy(alpha = star.alpha),
                        radius = star.radius,
                        center = Offset(sx, sy)
                    )
                }

                // Crescent / Full Moon
                drawCircle(
                    color = Color(0xFFFFFDD0),
                    radius = 28f,
                    center = Offset(width * 0.82f, height * 0.22f)
                )
                val maskColor = when (activeTheme) {
                    THEME_FOREST -> Color(0xFF040D12)
                    THEME_CITY -> Color(0xFF070B19)
                    else -> Color(0xFF0B132B)
                }
                drawCircle(
                    color = maskColor,
                    radius = 24f,
                    center = Offset(width * 0.80f, height * 0.20f)
                )
            } else if ((isSunrise || isSunset) && activeTheme != THEME_SATELLITE) {
                // Sun
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFFFD166), Color(0x66FF8A5B), Color.Transparent),
                        center = Offset(width * 0.5f, height * 0.35f),
                        radius = 80f
                    ),
                    radius = 80f,
                    center = Offset(width * 0.5f, height * 0.35f)
                )
            }

            // 3. ANIMATED AIRPLANE / FLIGHT TRACKING EFFECT
            if (settings.airplaneFlightAnimationEnabled) {
                drawAnimatedAirplane(
                    airplane = airplane,
                    width = width,
                    height = height,
                    isNight = isNight,
                    isRainy = isRainy,
                    cloudCover = weatherState.satelliteCloudCover
                )
            }

            // 4. Moving Atmospheric Clouds (drawn slightly over plane for realistic depth)
            if (settings.cloudAnimationEnabled && activeTheme != THEME_SATELLITE) {
                val cloudColor = if (isNight) Color(0x33475569) else Color(0x88FFFFFF)
                for (cloud in clouds) {
                    cloud.x += cloud.speed
                    if (cloud.x > width + 150f) {
                        cloud.x = -150f
                    }
                    val cx = cloud.x
                    val cy = cloud.y
                    val s = cloud.scale
                    drawCircle(cloudColor, radius = 34f * s, center = Offset(cx, cy))
                    drawCircle(cloudColor, radius = 28f * s, center = Offset(cx + 28f * s, cy - 14f * s))
                    drawCircle(cloudColor, radius = 32f * s, center = Offset(cx + 56f * s, cy))
                }
            }

            // 5. Rain Particle System
            if (settings.rainAnimationEnabled && (isRainy || settings.theme == THEME_CITY)) {
                val rainColor = Color(0x99B0E0E6)
                for (drop in rainDrops) {
                    drop.y += drop.speed
                    if (drop.y > height) {
                        drop.y = -10f
                        drop.x = Random.nextFloat() * width
                    }
                    drawLine(
                        color = rainColor,
                        start = Offset(drop.x, drop.y),
                        end = Offset(drop.x - 2f, drop.y + drop.length),
                        strokeWidth = 2f
                    )
                }
            }
        }
    }
}

/**
 * Draws the high-detail commercial aircraft with swept wings, twin engines,
 * jet contrails, blinking navigation lights, and storm-reactive turbulence/visibility.
 */
private fun DrawScope.drawAnimatedAirplane(
    airplane: ComposeAirplaneFlight,
    width: Float,
    height: Float,
    isNight: Boolean,
    isRainy: Boolean,
    cloudCover: Int
) {
    // 1. Flight Trajectory & State Machine
    if (!airplane.isActive) {
        airplane.cooldownTicks++
        // Respawn after short cooldown (approx 4-8 seconds)
        if (airplane.cooldownTicks > 120) {
            airplane.isActive = true
            airplane.cooldownTicks = 0
            airplane.x = -140f
            // Random altitude in upper 15% - 40% of canvas
            airplane.y = Random.nextFloat() * (height * 0.28f) + (height * 0.12f)
            airplane.speedX = Random.nextFloat() * 1.5f + 2.0f
            airplane.headingDeg = Random.nextFloat() * 8f + 3f
            airplane.contrailPoints.clear()
            val callsigns = listOf("BG 088", "SQ 318", "EK 582", "LH 402", "BA 249", "QR 638")
            airplane.callsign = callsigns[Random.nextInt(callsigns.size)]
            airplane.altitudeFt = Random.nextInt(28000, 39000)
        }
        return
    }

    // Weather Interaction: Turbulence Wobble in rain / storms
    var currentY = airplane.y
    if (isRainy) {
        val turbulence = sin((airplane.x * 0.04f).toDouble()).toFloat() * 2.8f
        currentY += turbulence
    }

    airplane.x += airplane.speedX
    airplane.y += (airplane.speedX * tan(Math.toRadians(airplane.headingDeg.toDouble()))).toFloat() * 0.2f
    airplane.beaconTick++

    // Out of screen detection
    if (airplane.x > width + 160f) {
        airplane.isActive = false
        airplane.cooldownTicks = 0
        return
    }

    // Weather Interaction: Visibility Occlusion
    // During heavy clouds or rain, airplane is diffused into mist, strobes cut through
    val airplaneAlpha = when {
        isRainy -> 0.45f
        cloudCover > 70 -> 0.65f
        else -> 0.95f
    }

    val planeHeadingRad = Math.toRadians(airplane.headingDeg.toDouble())
    val cosHeading = cos(planeHeadingRad).toFloat()
    val sinHeading = sin(planeHeadingRad).toFloat()

    // Engine positions for contrails
    val engineDistanceBehind = 14f
    val engineOffsetSide = 12f

    val leftEngineX = airplane.x - engineDistanceBehind * cosHeading - engineOffsetSide * sinHeading
    val leftEngineY = currentY - engineDistanceBehind * sinHeading + engineOffsetSide * cosHeading

    val rightEngineX = airplane.x - engineDistanceBehind * cosHeading + engineOffsetSide * sinHeading
    val rightEngineY = currentY - engineDistanceBehind * sinHeading - engineOffsetSide * cosHeading

    // Add contrail point every 2 ticks
    if (airplane.beaconTick % 2 == 0) {
        val initWidth = if (isRainy) 3.5f else 2.5f
        val initAlpha = if (isNight) 0.35f else 0.65f
        airplane.contrailPoints.add(
            ContrailPointData(
                leftX = leftEngineX,
                leftY = leftEngineY,
                rightX = rightEngineX,
                rightY = rightEngineY,
                alpha = initAlpha,
                width = initWidth
            )
        )
    }

    // 2. Draw Jet Contrails (Condensation Trails)
    val iterator = airplane.contrailPoints.iterator()
    while (iterator.hasNext()) {
        val pt = iterator.next()
        pt.alpha *= 0.965f // smooth fade out
        pt.width += 0.16f  // gradual atmospheric dispersion

        if (pt.alpha < 0.04f) {
            iterator.remove()
        } else {
            val contrailColor = if (isNight) Color(0xFFCBD5E1) else Color(0xFFF8FAFC)
            drawCircle(
                color = contrailColor.copy(alpha = pt.alpha),
                radius = pt.width,
                center = Offset(pt.leftX, pt.leftY)
            )
            drawCircle(
                color = contrailColor.copy(alpha = pt.alpha),
                radius = pt.width,
                center = Offset(pt.rightX, pt.rightY)
            )
        }
    }

    // 3. Render Airplane Body & Wings
    rotate(degrees = airplane.headingDeg, pivot = Offset(airplane.x, currentY)) {
        val planeCenter = Offset(airplane.x, currentY)
        val planeColor = if (isNight) Color(0xFFE2E8F0).copy(alpha = airplaneAlpha) else Color(0xFFFFFFFF).copy(alpha = airplaneAlpha)
        val shadowColor = Color(0xFF1E293B).copy(alpha = 0.5f * airplaneAlpha)
        val wingtipRed = Color(0xFFFF3B30)
        val wingtipGreen = Color(0xFF34C759)

        // Main Swept Wings
        val wingPath = Path().apply {
            moveTo(planeCenter.x + 6f, planeCenter.y)
            lineTo(planeCenter.x - 16f, planeCenter.y - 28f) // left wingtip
            lineTo(planeCenter.x - 22f, planeCenter.y - 28f)
            lineTo(planeCenter.x - 8f, planeCenter.y)
            lineTo(planeCenter.x - 22f, planeCenter.y + 28f) // right wingtip
            lineTo(planeCenter.x - 16f, planeCenter.y + 28f)
            close()
        }
        drawPath(wingPath, planeColor)

        // Jet Engine Nacelles
        drawRoundRect(
            color = Color(0xFF94A3B8).copy(alpha = airplaneAlpha),
            topLeft = Offset(planeCenter.x - 14f, planeCenter.y - 14f),
            size = Size(14f, 4f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f, 2f)
        )
        drawRoundRect(
            color = Color(0xFF94A3B8).copy(alpha = airplaneAlpha),
            topLeft = Offset(planeCenter.x - 14f, planeCenter.y + 10f),
            size = Size(14f, 4f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f, 2f)
        )

        // Horizontal Stabilizers (Tail Wings)
        val tailWingPath = Path().apply {
            moveTo(planeCenter.x - 24f, planeCenter.y)
            lineTo(planeCenter.x - 34f, planeCenter.y - 12f)
            lineTo(planeCenter.x - 37f, planeCenter.y - 12f)
            lineTo(planeCenter.x - 30f, planeCenter.y)
            lineTo(planeCenter.x - 37f, planeCenter.y + 12f)
            lineTo(planeCenter.x - 34f, planeCenter.y + 12f)
            close()
        }
        drawPath(tailWingPath, planeColor)

        // Streamlined Cylindrical Fuselage
        val fuselagePath = Path().apply {
            moveTo(planeCenter.x + 28f, planeCenter.y) // nose cone tip
            cubicTo(
                planeCenter.x + 18f, planeCenter.y - 5.5f,
                planeCenter.x - 18f, planeCenter.y - 5.5f,
                planeCenter.x - 32f, planeCenter.y - 2.5f // tail cone
            )
            lineTo(planeCenter.x - 35f, planeCenter.y)
            lineTo(planeCenter.x - 32f, planeCenter.y + 2.5f)
            cubicTo(
                planeCenter.x - 18f, planeCenter.y + 5.5f,
                planeCenter.x + 18f, planeCenter.y + 5.5f,
                planeCenter.x + 28f, planeCenter.y
            )
            close()
        }
        drawPath(fuselagePath, planeColor)

        // Cockpit Windshield (tinted dark glass)
        drawCircle(
            color = shadowColor,
            radius = 2.5f,
            center = Offset(planeCenter.x + 18f, planeCenter.y)
        )

        // Vertical Stabilizer Fin Accent (Airline deep blue/crimson tail fin)
        drawRect(
            color = Color(0xFF00A8FF).copy(alpha = airplaneAlpha),
            topLeft = Offset(planeCenter.x - 30f, planeCenter.y - 2f),
            size = Size(8f, 4f)
        )

        // 4. SIGNAL NAVIGATION LIGHTS
        // Left Wingtip: Port (Red Light)
        val portLightPos = Offset(planeCenter.x - 19f, planeCenter.y - 28f)
        drawCircle(wingtipRed, radius = 3f, center = portLightPos)

        // Right Wingtip: Starboard (Green Light)
        val stbdLightPos = Offset(planeCenter.x - 19f, planeCenter.y + 28f)
        drawCircle(wingtipGreen, radius = 3f, center = stbdLightPos)

        // Double-flash Anti-Collision Strobe Beacon (Top & Tail)
        val strobeCycle = airplane.beaconTick % 36
        val strobeActive = (strobeCycle in 0..3) || (strobeCycle in 8..11)

        if (strobeActive) {
            val strobeColor = Color(0xFFFFFFFF)
            val strobePos = Offset(planeCenter.x - 2f, planeCenter.y)
            drawCircle(strobeColor, radius = 4f, center = strobePos)
            // Luminous halo around strobe cutting through night or cloud haze
            drawCircle(strobeColor.copy(alpha = 0.5f), radius = 10f, center = strobePos)

            // Wingtip strobe flash
            drawCircle(Color.White.copy(alpha = 0.7f), radius = 4f, center = portLightPos)
            drawCircle(Color.White.copy(alpha = 0.7f), radius = 4f, center = stbdLightPos)
        }
    }
}

/**
 * Draws dynamic backgrounds according to the active theme.
 */
private fun DrawScope.drawAtmosphericBackground(
    theme: String,
    isNight: Boolean,
    isSunrise: Boolean,
    isSunset: Boolean,
    customBitmap: ImageBitmap?,
    width: Float,
    height: Float
) {
    if (theme == THEME_CUSTOM && customBitmap != null) {
        drawImage(
            image = customBitmap,
            dstSize = androidx.compose.ui.unit.IntSize(width.toInt(), height.toInt())
        )
        // Readability overlay
        drawRect(color = Color(0x55000000), size = Size(width, height))
        return
    }

    val gradientColors = when (theme) {
        THEME_SATELLITE -> listOf(Color(0xFF000206), Color(0xFF06152F), Color(0xFF0A2B52))
        THEME_FOREST -> if (isNight) listOf(Color(0xFF040D12), Color(0xFF183D3D), Color(0xFF0E2929))
        else listOf(Color(0xFF1C3B57), Color(0xFF2E6171), Color(0xFF1F3E3A))
        THEME_CITY -> listOf(Color(0xFF070B19), Color(0xFF151128), Color(0xFF28163B))
        THEME_MINIMALIST -> if (isNight) listOf(Color(0xFF09122C), Color(0xFF1E2A47), Color(0xFF2E3A59))
        else listOf(Color(0xFF142850), Color(0xFF27496D), Color(0xFF0C7B93))
        else -> when {
            isNight -> listOf(Color(0xFF050811), Color(0xFF0B132B), Color(0xFF1C2541))
            isSunrise -> listOf(Color(0xFFFF7B54), Color(0xFFFFB26B), Color(0xFF4A90E2))
            isSunset -> listOf(Color(0xFF2D1B4E), Color(0xFFE45858), Color(0xFFF0A500))
            else -> listOf(Color(0xFF1CB5E0), Color(0xFF000851))
        }
    }

    drawRect(
        brush = Brush.verticalGradient(gradientColors),
        size = Size(width, height)
    )
}
