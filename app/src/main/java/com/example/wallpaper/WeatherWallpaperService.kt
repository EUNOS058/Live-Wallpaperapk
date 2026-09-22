package com.example.wallpaper

import android.graphics.*
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import com.example.data.*
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*
import kotlin.random.Random

data class RainDrop(var x: Float, var y: Float, var speed: Float, var length: Float)
data class Cloud(var x: Float, var y: Float, var speed: Float, var scale: Float)
data class Star(val x: Float, val y: Float, val alpha: Float, val radius: Float)
data class Bird(var x: Float, var y: Float, var speed: Float)
data class Firefly(var x: Float, var y: Float, var vx: Float, var vy: Float, var alpha: Float)
data class CityBuilding(
    val left: Float,
    val right: Float,
    val top: Float,
    val color: Int,
    val windowColor: Int,
    val hasAntenna: Boolean
)
data class WallpaperContrailPoint(
    var leftX: Float,
    var leftY: Float,
    var rightX: Float,
    var rightY: Float,
    var alpha: Float,
    var width: Float
)

class WeatherWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine {
        return WeatherEngine()
    }

    inner class WeatherEngine : WallpaperService.Engine() {
        private val handler = Handler(Looper.getMainLooper())
        private val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
        private var settingsRepository: SettingsRepository? = null
        private var settings = WallpaperSettings()

        private var isVisibleState = false

        // Particles & scene elements
        private val rainDrops = mutableListOf<RainDrop>()
        private val clouds = mutableListOf<Cloud>()
        private val stars = mutableListOf<Star>()
        private val birds = mutableListOf<Bird>()
        private val fireflies = mutableListOf<Firefly>()
        private val buildings = mutableListOf<CityBuilding>()

        // Animated Airplane flight tracking state
        private var airplaneX = -200f
        private var airplaneY = 320f
        private var airplaneSpeedX = 3.2f
        private var airplaneHeadingDeg = 7f
        private var airplaneIsActive = true
        private var airplaneCooldownTicks = 0
        private var airplaneBeaconTick = 0
        private var airplaneCallsign = "BG 088"
        private var airplaneAltitudeFt = 34000
        private val airplaneContrailPoints = mutableListOf<WallpaperContrailPoint>()

        // Satellite earth animations
        private var satelliteOrbitAngle = 0.4f
        private var earthCloudOffset = 0f

        // Custom bitmap cache
        private var cachedBitmap: Bitmap? = null
        private var cachedPath: String? = null

        private val paint = Paint().apply {
            isAntiAlias = true
        }

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            settingsRepository = SettingsRepository(applicationContext)
            coroutineScope.launch {
                settingsRepository?.settingsFlow?.collect { newSettings ->
                    settings = newSettings
                }
            }
            initParticles(1080, 1920)
        }

        private fun initParticles(width: Int, height: Int) {
            rainDrops.clear()
            val dropCount = when (settings.animationQuality) {
                "High" -> 90
                "Low" -> 35
                else -> 60
            }
            for (i in 0..dropCount) {
                rainDrops.add(
                    RainDrop(
                        Random.nextFloat() * width,
                        Random.nextFloat() * height,
                        Random.nextFloat() * 16f + 24f,
                        Random.nextFloat() * 22f + 12f
                    )
                )
            }

            clouds.clear()
            for (i in 0..5) {
                clouds.add(
                    Cloud(
                        Random.nextFloat() * width,
                        Random.nextFloat() * (height * 0.38f),
                        Random.nextFloat() * 0.6f + 0.25f,
                        Random.nextFloat() * 0.8f + 0.6f
                    )
                )
            }

            stars.clear()
            for (i in 0..55) {
                stars.add(
                    Star(
                        Random.nextFloat() * width,
                        Random.nextFloat() * (height * 0.65f),
                        Random.nextFloat() * 0.75f + 0.25f,
                        Random.nextFloat() * 2.5f + 1f
                    )
                )
            }

            birds.clear()
            for (i in 0..3) {
                birds.add(
                    Bird(
                        Random.nextFloat() * width,
                        Random.nextFloat() * (height * 0.25f) + 120f,
                        Random.nextFloat() * 1.6f + 1.1f
                    )
                )
            }

            fireflies.clear()
            for (i in 0..25) {
                fireflies.add(
                    Firefly(
                        Random.nextFloat() * width,
                        (height * 0.6f) + Random.nextFloat() * (height * 0.35f),
                        (Random.nextFloat() - 0.5f) * 1.2f,
                        (Random.nextFloat() - 0.5f) * 1.2f,
                        Random.nextFloat() * 0.8f + 0.2f
                    )
                )
            }

            // Generate cityscape buildings
            buildings.clear()
            var currentX = 0f
            val groundY = height * 0.82f
            val buildingColors = intArrayOf(
                Color.parseColor("#0A0E1A"),
                Color.parseColor("#0E1424"),
                Color.parseColor("#12192D"),
                Color.parseColor("#161E34")
            )
            val windowGlows = intArrayOf(
                Color.parseColor("#FFEAA7"),
                Color.parseColor("#74B9FF"),
                Color.parseColor("#FD79A8"),
                Color.parseColor("#55EFC4")
            )

            while (currentX < width) {
                val bWidth = Random.nextFloat() * 90f + 70f
                val bHeight = Random.nextFloat() * (height * 0.38f) + (height * 0.15f)
                val color = buildingColors[Random.nextInt(buildingColors.size)]
                val winColor = windowGlows[Random.nextInt(windowGlows.size)]
                val hasAntenna = Random.nextBoolean()

                buildings.add(
                    CityBuilding(
                        left = currentX,
                        right = currentX + bWidth,
                        top = groundY - bHeight,
                        color = color,
                        windowColor = winColor,
                        hasAntenna = hasAntenna
                    )
                )
                currentX += bWidth + 6f
            }
        }

        override fun onVisibilityChanged(visible: Boolean) {
            isVisibleState = visible
            if (visible) {
                drawFrame()
            }
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            initParticles(width, height)
            drawFrame()
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            super.onSurfaceDestroyed(holder)
            isVisibleState = false
            handler.removeCallbacksAndMessages(null)
            coroutineScope.cancel()
            cachedBitmap?.recycle()
            cachedBitmap = null
        }

        private fun getCustomImageBitmap(path: String, width: Int, height: Int): Bitmap? {
            if (path == cachedPath && cachedBitmap != null && !cachedBitmap!!.isRecycled) {
                return cachedBitmap
            }
            return try {
                cachedBitmap?.recycle()
                val boundsOnly = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(path, boundsOnly)

                var sample = 1
                while (boundsOnly.outWidth / (sample * 2) >= width && boundsOnly.outHeight / (sample * 2) >= height) {
                    sample *= 2
                }

                val options = BitmapFactory.Options().apply {
                    inSampleSize = sample
                    inPreferredConfig = Bitmap.Config.RGB_565
                }
                val loaded = BitmapFactory.decodeFile(path, options)
                cachedBitmap = loaded
                cachedPath = path
                loaded
            } catch (e: Exception) {
                null
            }
        }

        private fun drawFrame() {
            if (!isVisibleState) return

            val holder = surfaceHolder
            var canvas: Canvas? = null
            try {
                canvas = holder.lockCanvas()
                if (canvas != null) {
                    renderScene(canvas)
                }
            } catch (e: Exception) {
                // Ignore
            } finally {
                if (canvas != null) {
                    try {
                        holder.unlockCanvasAndPost(canvas)
                    } catch (e: Exception) {
                        // Ignore
                    }
                }
            }

            handler.removeCallbacksAndMessages(null)
            if (isVisibleState) {
                val delay = when (settings.animationQuality) {
                    "High" -> 25L   // ~40 FPS
                    "Low" -> 50L    // ~20 FPS (maximum battery save)
                    else -> 33L     // ~30 FPS
                }
                handler.postDelayed({ drawFrame() }, delay)
            }
        }

        private fun renderScene(canvas: Canvas) {
            val width = canvas.width.toFloat()
            val height = canvas.height.toFloat()

            val calendar = Calendar.getInstance()
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val isNight = hour < 6 || hour >= 18
            val isSunrise = hour in 5..7
            val isSunset = hour in 17..19

            // Determine active theme
            val activeTheme = if (settings.autoThemeBasedOnWeather) {
                when {
                    settings.rainAnimationEnabled && (hour in 12..17) -> THEME_CITY
                    isSunrise -> THEME_FOREST
                    isSunset -> THEME_MINIMALIST
                    isNight -> THEME_SATELLITE
                    else -> THEME_FOREST
                }
            } else {
                settings.theme
            }

            // 1. Draw Background
            when (activeTheme) {
                THEME_SATELLITE -> {
                    drawLiveSatelliteEarthScene(canvas, width, height, isNight, isSunrise, isSunset)
                }
                THEME_CUSTOM -> {
                    val customPath = settings.customImagePath
                    val bmp = if (customPath != null) getCustomImageBitmap(customPath, width.toInt(), height.toInt()) else null
                    if (bmp != null) {
                        val src = Rect(0, 0, bmp.width, bmp.height)
                        val dst = Rect(0, 0, width.toInt(), height.toInt())
                        canvas.drawBitmap(bmp, src, dst, paint)

                        // Dark atmospheric overlay for readability
                        paint.color = Color.parseColor("#55000000")
                        canvas.drawRect(0f, 0f, width, height, paint)
                    } else {
                        drawGradientBackground(canvas, width, height, isNight, isSunrise, isSunset)
                    }
                }
                THEME_CITY -> {
                    val cityBgShader = LinearGradient(
                        0f, 0f, 0f, height,
                        intArrayOf(Color.parseColor("#070B19"), Color.parseColor("#151128"), Color.parseColor("#28163B")),
                        null, Shader.TileMode.CLAMP
                    )
                    paint.shader = cityBgShader
                    canvas.drawRect(0f, 0f, width, height, paint)
                    paint.shader = null
                }
                THEME_MINIMALIST -> {
                    val miniShader = LinearGradient(
                        0f, 0f, 0f, height,
                        if (isNight) intArrayOf(Color.parseColor("#09122C"), Color.parseColor("#1E2A47"), Color.parseColor("#2E3A59"))
                        else intArrayOf(Color.parseColor("#142850"), Color.parseColor("#27496D"), Color.parseColor("#0C7B93")),
                        null, Shader.TileMode.CLAMP
                    )
                    paint.shader = miniShader
                    canvas.drawRect(0f, 0f, width, height, paint)
                    paint.shader = null
                }
                THEME_FOREST -> {
                    val forestBgShader = LinearGradient(
                        0f, 0f, 0f, height,
                        if (isNight) intArrayOf(Color.parseColor("#040D12"), Color.parseColor("#183D3D"), Color.parseColor("#0E2929"))
                        else if (isSunrise || isSunset) intArrayOf(Color.parseColor("#5C2E2E"), Color.parseColor("#8E4A49"), Color.parseColor("#2C423F"))
                        else intArrayOf(Color.parseColor("#1C3B57"), Color.parseColor("#2E6171"), Color.parseColor("#1F3E3A")),
                        null, Shader.TileMode.CLAMP
                    )
                    paint.shader = forestBgShader
                    canvas.drawRect(0f, 0f, width, height, paint)
                    paint.shader = null
                }
                else -> {
                    drawGradientBackground(canvas, width, height, isNight, isSunrise, isSunset)
                }
            }

            // 2. Stars & Celestial Moon (When not in satellite view, or in space)
            if (activeTheme != THEME_SATELLITE && isNight) {
                paint.color = Color.WHITE
                for (star in stars) {
                    paint.alpha = (star.alpha * 255).toInt()
                    canvas.drawCircle(star.x, star.y, star.radius, paint)
                }
                paint.alpha = 255

                // Moon rendering
                if (activeTheme == THEME_MINIMALIST) {
                    paint.color = Color.parseColor("#22FFFFFF")
                    canvas.drawCircle(width * 0.8f, height * 0.18f, 75f, paint)
                    paint.color = Color.parseColor("#FFFDF0")
                    canvas.drawCircle(width * 0.8f, height * 0.18f, 48f, paint)
                } else {
                    paint.color = Color.parseColor("#FFFDD0")
                    canvas.drawCircle(width * 0.8f, height * 0.18f, 60f, paint)
                    val maskColor = when (activeTheme) {
                        THEME_FOREST -> Color.parseColor("#183D3D")
                        THEME_CITY -> Color.parseColor("#151128")
                        else -> Color.parseColor("#0B132B")
                    }
                    paint.color = maskColor
                    canvas.drawCircle(width * 0.77f, height * 0.16f, 52f, paint)
                }
            }

            // 3. Sun (Sunrise / Sunset)
            if ((isSunrise || isSunset) && activeTheme != THEME_CUSTOM && activeTheme != THEME_SATELLITE) {
                paint.color = Color.parseColor("#FFD166")
                canvas.drawCircle(width * 0.5f, height * 0.35f, 90f, paint)
            }

            // 4. Animated Commercial Airplane / Flight Tracking
            if (settings.airplaneFlightAnimationEnabled) {
                drawAnimatedAirplaneFlight(canvas, width, height, isNight, settings.rainAnimationEnabled)
            }

            // 5. Moving Clouds
            if (settings.cloudAnimationEnabled && activeTheme != THEME_SATELLITE) {
                paint.color = if (isNight) Color.parseColor("#223344") else Color.parseColor("#DDEEFF")
                paint.alpha = 180
                for (cloud in clouds) {
                    cloud.x += cloud.speed
                    if (cloud.x > width + 200f) cloud.x = -200f

                    val cx = cloud.x
                    val cy = cloud.y
                    canvas.drawCircle(cx, cy, 50f * cloud.scale, paint)
                    canvas.drawCircle(cx + 40f * cloud.scale, cy - 20f * cloud.scale, 40f * cloud.scale, paint)
                    canvas.drawCircle(cx + 80f * cloud.scale, cy, 45f * cloud.scale, paint)
                }
                paint.alpha = 255
            }

            // 5. Birds (Daytime)
            if (!isNight && activeTheme != THEME_MINIMALIST && activeTheme != THEME_SATELLITE) {
                paint.color = Color.parseColor("#112233")
                paint.strokeWidth = 3f
                paint.style = Paint.Style.STROKE
                for (bird in birds) {
                    bird.x += bird.speed
                    if (bird.x > width + 50f) bird.x = -50f

                    val path = Path().apply {
                        moveTo(bird.x, bird.y)
                        quadTo(bird.x + 15f, bird.y - 15f, bird.x + 30f, bird.y)
                        quadTo(bird.x + 45f, bird.y - 15f, bird.x + 60f, bird.y)
                    }
                    canvas.drawPath(path, paint)
                }
                paint.style = Paint.Style.FILL
            }

            // 6. Theme Specific Scenery Silhouettes
            val groundY = height * 0.82f
            when (activeTheme) {
                THEME_SATELLITE -> {
                    // Handled inside drawLiveSatelliteEarthScene
                }
                THEME_FOREST -> {
                    val mountainPaint = Paint().apply {
                        isAntiAlias = true
                        color = if (isNight) Color.parseColor("#09181B") else Color.parseColor("#19373D")
                    }
                    val mPath = Path().apply {
                        moveTo(0f, groundY - 60f)
                        quadTo(width * 0.25f, groundY - 180f, width * 0.5f, groundY - 80f)
                        quadTo(width * 0.75f, groundY - 220f, width, groundY - 50f)
                        lineTo(width, height)
                        lineTo(0f, height)
                        close()
                    }
                    canvas.drawPath(mPath, mountainPaint)

                    paint.color = if (isNight) Color.parseColor("#030A0D") else Color.parseColor("#0D1F1C")
                    canvas.drawRect(0f, groundY, width, height, paint)

                    drawPineTree(canvas, width * 0.08f, groundY, 180f, paint)
                    drawPineTree(canvas, width * 0.22f, groundY, 240f, paint)
                    drawPineTree(canvas, width * 0.38f, groundY, 190f, paint)
                    drawPineTree(canvas, width * 0.65f, groundY, 260f, paint)
                    drawPineTree(canvas, width * 0.82f, groundY, 210f, paint)
                    drawPineTree(canvas, width * 0.94f, groundY, 170f, paint)

                    if (isNight) {
                        val fireflyPaint = Paint().apply { isAntiAlias = true }
                        for (ff in fireflies) {
                            ff.x += ff.vx
                            ff.y += ff.vy
                            if (ff.x < 0f) ff.x = width
                            if (ff.x > width) ff.x = 0f
                            if (ff.y < groundY - 250f) ff.y = groundY + 50f
                            if (ff.y > height) ff.y = groundY - 200f

                            fireflyPaint.color = Color.parseColor("#C8FF00")
                            fireflyPaint.alpha = (ff.alpha * 220).toInt()
                            canvas.drawCircle(ff.x, ff.y, 4f, fireflyPaint)
                        }
                    }
                }
                THEME_CITY -> {
                    val bPaint = Paint().apply { isAntiAlias = true }
                    val winPaint = Paint().apply { isAntiAlias = true }

                    for (b in buildings) {
                        bPaint.color = b.color
                        canvas.drawRect(b.left, b.top, b.right, groundY, bPaint)

                        if (b.hasAntenna) {
                            val aX = (b.left + b.right) / 2f
                            bPaint.strokeWidth = 3f
                            canvas.drawLine(aX, b.top, aX, b.top - 40f, bPaint)
                            winPaint.color = Color.RED
                            canvas.drawCircle(aX, b.top - 40f, 4f, winPaint)
                        }

                        winPaint.color = b.windowColor
                        winPaint.alpha = 180
                        var wy = b.top + 20f
                        while (wy < groundY - 30f) {
                            var wx = b.left + 15f
                            while (wx < b.right - 15f) {
                                canvas.drawRect(wx, wy, wx + 8f, wy + 12f, winPaint)
                                wx += 18f
                            }
                            wy += 25f
                        }
                    }

                    paint.color = Color.parseColor("#070A12")
                    canvas.drawRect(0f, groundY, width, height, paint)

                    val reflectShader = LinearGradient(
                        0f, groundY, 0f, groundY + 60f,
                        intArrayOf(Color.parseColor("#33FF007F"), Color.parseColor("#2200D2D3"), Color.TRANSPARENT),
                        null, Shader.TileMode.CLAMP
                    )
                    paint.shader = reflectShader
                    canvas.drawRect(0f, groundY, width, groundY + 60f, paint)
                    paint.shader = null
                }
                THEME_MINIMALIST -> {
                    paint.color = if (isNight) Color.parseColor("#060A14") else Color.parseColor("#0A192F")
                    val miniPath = Path().apply {
                        moveTo(0f, groundY + 10f)
                        quadTo(width * 0.5f, groundY - 40f, width, groundY + 15f)
                        lineTo(width, height)
                        lineTo(0f, height)
                        close()
                    }
                    canvas.drawPath(miniPath, paint)
                }
                THEME_CUSTOM -> {
                    paint.color = Color.parseColor("#33000000")
                    canvas.drawRect(0f, groundY, width, height, paint)
                }
                else -> {
                    paint.color = if (isNight) Color.parseColor("#060913") else Color.parseColor("#1B2A1B")
                    canvas.drawRect(0f, groundY, width, height, paint)

                    paint.color = if (isNight) Color.parseColor("#09101E") else Color.parseColor("#122215")
                    canvas.drawRect(width * 0.12f, groundY - 220f, width * 0.18f, groundY, paint)
                    canvas.drawCircle(width * 0.15f, groundY - 250f, 90f, paint)

                    canvas.drawRect(width * 0.82f, groundY - 280f, width * 0.89f, groundY, paint)
                    canvas.drawCircle(width * 0.855f, groundY - 310f, 110f, paint)

                    paint.color = Color.parseColor("#100804")
                    canvas.drawRect(width * 0.35f, groundY - 45f, width * 0.55f, groundY - 35f, paint)
                    canvas.drawRect(width * 0.37f, groundY - 35f, width * 0.39f, groundY, paint)
                    canvas.drawRect(width * 0.51f, groundY - 35f, width * 0.53f, groundY, paint)

                    val lampX = width * 0.28f
                    val lampY = groundY - 320f
                    paint.color = Color.parseColor("#1A1A1A")
                    canvas.drawRect(lampX - 6f, lampY, lampX + 6f, groundY, paint)
                    canvas.drawRect(lampX - 25f, lampY - 30f, lampX + 25f, lampY, paint)

                    if (isNight || isSunset) {
                        val glowPaint = Paint().apply {
                            isAntiAlias = true
                            shader = RadialGradient(
                                lampX, lampY + 15f, 350f,
                                intArrayOf(Color.parseColor("#66FFEE88"), Color.parseColor("#22FFCC44"), Color.TRANSPARENT),
                                floatArrayOf(0.0f, 0.4f, 1.0f),
                                Shader.TileMode.CLAMP
                            )
                        }
                        canvas.drawCircle(lampX, lampY + 15f, 350f, glowPaint)
                    }
                }
            }

            // 7. Rain Animation (All themes when enabled)
            if (settings.rainAnimationEnabled && activeTheme != THEME_SATELLITE) {
                paint.color = if (activeTheme == THEME_CITY) Color.parseColor("#A8D8EA") else Color.parseColor("#88AACCFF")
                paint.strokeWidth = 3f
                for (drop in rainDrops) {
                    drop.y += drop.speed
                    if (drop.y > height) {
                        drop.y = -20f
                        drop.x = Random.nextFloat() * width
                    }
                    canvas.drawLine(drop.x, drop.y, drop.x - 2f, drop.y + drop.length, paint)
                }
            }

            // 8. REDESIGNED ULTRA-PREMIUM HEADER UI (Clock, Weather, Location, Date)
            drawUltraPremiumHeader(canvas, width, height, isNight, activeTheme == THEME_SATELLITE)
        }

        /**
         * Renders high-fidelity Live Earth from Space with dynamic satellite clouds, day/night city lights, and orbiting satellite.
         */
        private fun drawLiveSatelliteEarthScene(
            canvas: Canvas,
            width: Float,
            height: Float,
            isNight: Boolean,
            isSunrise: Boolean,
            isSunset: Boolean
        ) {
            // Deep cosmic space background
            val spaceShader = LinearGradient(
                0f, 0f, 0f, height,
                intArrayOf(
                    Color.parseColor("#000206"),
                    Color.parseColor("#020713"),
                    Color.parseColor("#061126")
                ),
                null, Shader.TileMode.CLAMP
            )
            paint.shader = spaceShader
            canvas.drawRect(0f, 0f, width, height, paint)
            paint.shader = null

            // Distant starry cosmic dust
            paint.color = Color.WHITE
            for (star in stars) {
                paint.alpha = (star.alpha * 240).toInt()
                canvas.drawCircle(star.x, star.y, star.radius, paint)
            }
            paint.alpha = 255

            // Earth Globe Parameters
            val earthCenterX = width * 0.5f
            val earthCenterY = height * 0.74f
            val earthRadius = width * 0.62f

            // 1. Atmosphere Blue Outer Corona Glow
            val glowPaint = Paint().apply {
                isAntiAlias = true
                shader = RadialGradient(
                    earthCenterX, earthCenterY, earthRadius * 1.25f,
                    intArrayOf(
                        Color.argb(0, 0, 168, 255),
                        Color.argb(120, 0, 140, 255),
                        Color.argb(45, 0, 110, 220),
                        Color.TRANSPARENT
                    ),
                    floatArrayOf(0.0f, 0.78f, 0.88f, 1.0f),
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawCircle(earthCenterX, earthCenterY, earthRadius * 1.25f, glowPaint)

            // Clip drawing to Earth Sphere
            val earthPath = Path().apply {
                addCircle(earthCenterX, earthCenterY, earthRadius, Path.Direction.CW)
            }
            canvas.save()
            canvas.clipPath(earthPath)

            // 2. Earth Base (Deep Ocean Blues)
            val oceanShader = LinearGradient(
                earthCenterX - earthRadius, earthCenterY - earthRadius,
                earthCenterX + earthRadius, earthCenterY + earthRadius,
                if (isNight)
                    intArrayOf(Color.parseColor("#031326"), Color.parseColor("#020B16"))
                else
                    intArrayOf(Color.parseColor("#0B4F8A"), Color.parseColor("#09335E")),
                null, Shader.TileMode.CLAMP
            )
            val oceanPaint = Paint().apply {
                isAntiAlias = true
                shader = oceanShader
            }
            canvas.drawCircle(earthCenterX, earthCenterY, earthRadius, oceanPaint)

            // 3. Continents & Landmasses (Geometric stylized continents visible from orbit)
            val landPaint = Paint().apply {
                isAntiAlias = true
                color = if (isNight) Color.parseColor("#071F1E") else Color.parseColor("#25633C")
            }

            // India / South Asia subcontinent contour
            val continent1 = Path().apply {
                moveTo(earthCenterX - 60f, earthCenterY - 140f)
                lineTo(earthCenterX + 120f, earthCenterY - 150f)
                lineTo(earthCenterX + 170f, earthCenterY - 50f)
                lineTo(earthCenterX + 70f, earthCenterY + 40f)
                lineTo(earthCenterX - 20f, earthCenterY + 30f)
                lineTo(earthCenterX - 70f, earthCenterY - 60f)
                close()
            }
            canvas.drawPath(continent1, landPaint)

            // Adjacent landmasses / islands
            canvas.drawCircle(earthCenterX + 110f, earthCenterY + 70f, 24f, landPaint)
            canvas.drawCircle(earthCenterX - 140f, earthCenterY - 40f, 65f, landPaint)
            canvas.drawCircle(earthCenterX + 220f, earthCenterY - 110f, 50f, landPaint)

            // 4. Night-side Glowing City Lights (Metropolitan cluster luminescence)
            if (isNight) {
                val cityLightPaint = Paint().apply {
                    isAntiAlias = true
                    color = Color.parseColor("#FFEAA7")
                    setShadowLayer(8f, 0f, 0f, Color.parseColor("#FDCB6E"))
                }
                val cityDots = floatArrayOf(
                    earthCenterX + 20f, earthCenterY - 80f,
                    earthCenterX + 45f, earthCenterY - 65f,
                    earthCenterX - 10f, earthCenterY - 30f,
                    earthCenterX + 60f, earthCenterY - 15f,
                    earthCenterX + 90f, earthCenterY - 40f,
                    earthCenterX + 10f, earthCenterY + 10f,
                    earthCenterX - 110f, earthCenterY - 50f,
                    earthCenterX - 130f, earthCenterY - 20f,
                    earthCenterX + 190f, earthCenterY - 90f
                )
                for (i in cityDots.indices step 2) {
                    canvas.drawCircle(cityDots[i], cityDots[i + 1], 3.5f, cityLightPaint)
                    cityLightPaint.alpha = 110
                    canvas.drawCircle(cityDots[i], cityDots[i + 1], 7f, cityLightPaint)
                    cityLightPaint.alpha = 255
                }
            }

            // 5. Dynamic Satellite Cloud Bands (Moving procedural vortex clouds)
            if (settings.cloudAnimationEnabled || settings.satelliteCloudOverlay) {
                earthCloudOffset += 0.25f
                if (earthCloudOffset > width) earthCloudOffset = 0f

                val cloudPaint = Paint().apply {
                    isAntiAlias = true
                    color = Color.WHITE
                    alpha = 140
                }

                val cloudY1 = earthCenterY - 110f
                val cloudY2 = earthCenterY - 10f
                val cloudY3 = earthCenterY + 80f

                val cX1 = (earthCenterX - 180f + earthCloudOffset) % (width * 1.4f) - width * 0.2f
                val cX2 = (earthCenterX - 60f + earthCloudOffset * 0.7f) % (width * 1.4f) - width * 0.2f
                val cX3 = (earthCenterX + 80f + earthCloudOffset * 1.2f) % (width * 1.4f) - width * 0.2f

                // Dynamic cloud swirls
                canvas.drawOval(RectF(cX1, cloudY1 - 25f, cX1 + 180f, cloudY1 + 25f), cloudPaint)
                canvas.drawOval(RectF(cX2 - 60f, cloudY2 - 35f, cX2 + 140f, cloudY2 + 25f), cloudPaint)
                canvas.drawOval(RectF(cX3, cloudY3 - 20f, cX3 + 130f, cloudY3 + 20f), cloudPaint)
            }

            // 6. Day/Night Curved Shadow Terminator on Earth
            val terminatorPaint = Paint().apply {
                isAntiAlias = true
                shader = LinearGradient(
                    earthCenterX - earthRadius * 0.3f, earthCenterY - earthRadius,
                    earthCenterX + earthRadius * 0.9f, earthCenterY + earthRadius,
                    if (isNight)
                        intArrayOf(Color.argb(90, 0, 0, 0), Color.argb(220, 0, 4, 12))
                    else
                        intArrayOf(Color.argb(0, 0, 0, 0), Color.argb(160, 0, 6, 16)),
                    null, Shader.TileMode.CLAMP
                )
            }
            canvas.drawCircle(earthCenterX, earthCenterY, earthRadius, terminatorPaint)

            canvas.restore() // End of clipped Earth sphere

            // 7. Active Orbiting Weather Satellite (ISS / METEOSAT with Solar Panels)
            satelliteOrbitAngle += 0.007f
            if (satelliteOrbitAngle > (PI * 2).toFloat()) satelliteOrbitAngle = 0f

            val satOrbitRadiusX = width * 0.42f
            val satOrbitRadiusY = height * 0.14f
            val satCenterX = width * 0.5f + cos(satelliteOrbitAngle) * satOrbitRadiusX
            val satCenterY = height * 0.45f + sin(satelliteOrbitAngle) * satOrbitRadiusY

            // Draw Orbit Trajectory Line (Faint blue dotted arc)
            val orbitPathPaint = Paint().apply {
                isAntiAlias = true
                style = Paint.Style.STROKE
                strokeWidth = 1.5f
                color = Color.argb(55, 0, 180, 255)
                pathEffect = DashPathEffect(floatArrayOf(12f, 16f), 0f)
            }
            val orbitRect = RectF(
                width * 0.5f - satOrbitRadiusX,
                height * 0.45f - satOrbitRadiusY,
                width * 0.5f + satOrbitRadiusX,
                height * 0.45f + satOrbitRadiusY
            )
            canvas.drawOval(orbitRect, orbitPathPaint)

            // Draw Satellite Body
            val satPaint = Paint().apply { isAntiAlias = true }

            // Satellite Solar Arrays (Left & Right Wings)
            satPaint.color = Color.parseColor("#0984E3")
            // Left Solar Array
            canvas.drawRect(satCenterX - 36f, satCenterY - 8f, satCenterX - 12f, satCenterY + 8f, satPaint)
            // Right Solar Array
            canvas.drawRect(satCenterX + 12f, satCenterY - 8f, satCenterX + 36f, satCenterY + 8f, satPaint)

            // Array grid lines
            satPaint.color = Color.parseColor("#74B9FF")
            satPaint.strokeWidth = 1f
            satPaint.style = Paint.Style.STROKE
            canvas.drawRect(satCenterX - 36f, satCenterY - 8f, satCenterX - 12f, satCenterY + 8f, satPaint)
            canvas.drawRect(satCenterX + 12f, satCenterY - 8f, satCenterX + 36f, satCenterY + 8f, satPaint)
            satPaint.style = Paint.Style.FILL

            // Central Satellite Bus (Metallic Chassis)
            satPaint.color = Color.parseColor("#DFE6E9")
            canvas.drawRect(satCenterX - 10f, satCenterY - 12f, satCenterX + 10f, satCenterY + 12f, satPaint)

            // Sensor Lens / Dish
            satPaint.color = Color.parseColor("#636E72")
            canvas.drawCircle(satCenterX, satCenterY + 12f, 4f, satPaint)

            // Blinking Telemetry Beacon (Green/Red signal)
            val blink = (sin(System.currentTimeMillis() / 250.0) > 0)
            satPaint.color = if (blink) Color.parseColor("#00FF88") else Color.parseColor("#006633")
            canvas.drawCircle(satCenterX, satCenterY - 13f, 3f, satPaint)

            // Satellite Telemetry Badge (Floating in space)
            val badgePaint = Paint().apply {
                isAntiAlias = true
                color = Color.argb(160, 116, 185, 255)
                textSize = 24f
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
            }
            canvas.drawText("🛰️ SATELLITE ORBIT SYNC • ALT 35,786 KM", width * 0.08f, height * 0.42f, badgePaint)
        }

        private fun drawUltraPremiumHeader(
            canvas: Canvas,
            width: Float,
            height: Float,
            isNight: Boolean,
            isSatelliteView: Boolean
        ) {
            val centerX = width / 2f
            val isGlass = settings.headerStyle == HEADER_STYLE_GLASS

            val timeFormat = if (settings.is12HourFormat) "h:mm" else "HH:mm"
            val timeDigits = SimpleDateFormat(timeFormat, Locale.getDefault()).format(Date())
            val amPm = if (settings.is12HourFormat) SimpleDateFormat("a", Locale.getDefault()).format(Date()).uppercase() else ""
            val dateStr = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date())

            val tempStr = if (settings.isCelsius) "31°C" else "88°F"
            val showGreeting = isNight && settings.showNightGreeting

            val cardWidth = width * 0.90f
            val cardLeft = (width - cardWidth) / 2f
            val cardRight = cardLeft + cardWidth
            val cardTop = height * 0.085f
            val cardHeight = if (showGreeting) 470f else 405f
            val cardBottom = cardTop + cardHeight
            val cornerRadius = 46f

            if (isGlass) {
                // Sleek Glassmorphism frosted card background
                val glassPaint = Paint().apply {
                    isAntiAlias = true
                    style = Paint.Style.FILL
                    shader = LinearGradient(
                        cardLeft, cardTop, cardLeft, cardBottom,
                        intArrayOf(
                            Color.argb(135, 12, 19, 36),
                            Color.argb(185, 20, 32, 54)
                        ),
                        null,
                        Shader.TileMode.CLAMP
                    )
                    setShadowLayer(32f, 0f, 10f, Color.argb(160, 0, 0, 0))
                }
                val rectF = RectF(cardLeft, cardTop, cardRight, cardBottom)
                canvas.drawRoundRect(rectF, cornerRadius, cornerRadius, glassPaint)

                // Glassmorphic luminous border
                val borderPaint = Paint().apply {
                    isAntiAlias = true
                    style = Paint.Style.STROKE
                    strokeWidth = 2.5f
                    shader = LinearGradient(
                        cardLeft, cardTop, cardRight, cardBottom,
                        intArrayOf(
                            Color.argb(150, 255, 255, 255),
                            Color.argb(35, 255, 255, 255),
                            Color.argb(90, 255, 255, 255)
                        ),
                        floatArrayOf(0.0f, 0.5f, 1.0f),
                        Shader.TileMode.CLAMP
                    )
                }
                canvas.drawRoundRect(rectF, cornerRadius, cornerRadius, borderPaint)
            }

            var currentY = cardTop + 52f

            // A. Location & Satellite GPS Status Pill at top
            val locationText = if (isSatelliteView) {
                "🛰️ ${settings.manualCity.uppercase()} • GPS LINKED"
            } else {
                "📍 ${settings.manualCity.uppercase()}"
            }

            val locPaint = Paint().apply {
                isAntiAlias = true
                color = Color.parseColor("#E2E8F0")
                textSize = 32f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
                setShadowLayer(10f, 0f, 2f, Color.argb(160, 0, 0, 0))
            }

            val locWidth = locPaint.measureText(locationText) + 40f
            val pillPaint = Paint().apply {
                isAntiAlias = true
                style = Paint.Style.FILL
                color = Color.argb(75, 255, 255, 255)
            }
            val pillRect = RectF(centerX - locWidth / 2f, currentY - 28f, centerX + locWidth / 2f, currentY + 12f)
            canvas.drawRoundRect(pillRect, 20f, 20f, pillPaint)
            canvas.drawText(locationText, centerX, currentY, locPaint)

            currentY += 106f

            // B. Ultra-Modern Digital Clock Widget
            val clockPaint = Paint().apply {
                isAntiAlias = true
                color = Color.WHITE
                textSize = 120f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
                if (!isGlass) {
                    setShadowLayer(26f, 0f, 0f, Color.argb(200, 78, 170, 255))
                } else {
                    setShadowLayer(16f, 0f, 4f, Color.argb(180, 0, 0, 0))
                }
            }

            val fullClockText = if (amPm.isNotEmpty()) "$timeDigits $amPm" else timeDigits
            canvas.drawText(fullClockText, centerX, currentY, clockPaint)

            currentY += 58f

            // C. Integrated Weather & Temperature Modern Row
            val weatherRowPaint = Paint().apply {
                isAntiAlias = true
                color = Color.parseColor("#F1F5F9")
                textSize = 42f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
                setShadowLayer(12f, 0f, 3f, Color.argb(160, 0, 0, 0))
            }

            val weatherItems = mutableListOf<String>()
            if (settings.showTemperature) {
                weatherItems.add("🌡️ $tempStr")
            }
            if (settings.showWeather) {
                weatherItems.add("☁️ Partly Cloudy")
            }

            if (weatherItems.isNotEmpty()) {
                val weatherText = weatherItems.joinToString("   •   ")
                canvas.drawText(weatherText, centerX, currentY, weatherRowPaint)
                currentY += 54f
            }

            // D. Date Display
            if (settings.showDate) {
                val datePaint = Paint().apply {
                    isAntiAlias = true
                    color = Color.parseColor("#CBD5E1")
                    textSize = 34f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    textAlign = Paint.Align.CENTER
                    setShadowLayer(8f, 0f, 2f, Color.argb(160, 0, 0, 0))
                }
                canvas.drawText("📅 $dateStr", centerX, currentY, datePaint)
                currentY += 56f
            }

            // E. Bengali Night Greeting ("শুভ রাত্রি 🌙")
            if (showGreeting) {
                val greetPaint = Paint().apply {
                    isAntiAlias = true
                    color = Color.parseColor("#FFD166")
                    textSize = 46f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textAlign = Paint.Align.CENTER
                    setShadowLayer(20f, 0f, 2f, Color.argb(180, 255, 180, 0))
                }
                canvas.drawText("শুভ রাত্রি 🌙", centerX, currentY, greetPaint)
            }
        }

        private fun drawGradientBackground(
            canvas: Canvas,
            width: Float,
            height: Float,
            isNight: Boolean,
            isSunrise: Boolean,
            isSunset: Boolean
        ) {
            val bgShader = when {
                isNight -> LinearGradient(
                    0f, 0f, 0f, height,
                    intArrayOf(Color.parseColor("#050811"), Color.parseColor("#0B132B"), Color.parseColor("#1C2541")),
                    null, Shader.TileMode.CLAMP
                )
                isSunrise -> LinearGradient(
                    0f, 0f, 0f, height,
                    intArrayOf(Color.parseColor("#FF7B54"), Color.parseColor("#FFB26B"), Color.parseColor("#4A90E2")),
                    null, Shader.TileMode.CLAMP
                )
                isSunset -> LinearGradient(
                    0f, 0f, 0f, height,
                    intArrayOf(Color.parseColor("#2D1B4E"), Color.parseColor("#E45858"), Color.parseColor("#F0A500")),
                    null, Shader.TileMode.CLAMP
                )
                else -> LinearGradient(
                    0f, 0f, 0f, height,
                    intArrayOf(Color.parseColor("#1CB5E0"), Color.parseColor("#000851")),
                    null, Shader.TileMode.CLAMP
                )
            }
            paint.shader = bgShader
            canvas.drawRect(0f, 0f, width, height, paint)
            paint.shader = null
        }

        private fun drawPineTree(canvas: Canvas, x: Float, groundY: Float, treeHeight: Float, p: Paint) {
            val trunkWidth = treeHeight * 0.08f
            canvas.drawRect(x - trunkWidth / 2f, groundY - treeHeight * 0.25f, x + trunkWidth / 2f, groundY, p)

            val tiers = 3
            val tierHeight = treeHeight * 0.32f
            for (i in 0 until tiers) {
                val tierBaseY = groundY - (treeHeight * 0.2f) - (i * tierHeight * 0.75f)
                val tierWidth = (treeHeight * 0.45f) * (1f - (i * 0.22f))
                val tPath = Path().apply {
                    moveTo(x - tierWidth / 2f, tierBaseY)
                    lineTo(x + tierWidth / 2f, tierBaseY)
                    lineTo(x, tierBaseY - tierHeight)
                    close()
                }
                canvas.drawPath(tPath, p)
            }
        }

        private fun drawAnimatedAirplaneFlight(
            canvas: Canvas,
            width: Float,
            height: Float,
            isNight: Boolean,
            isRainy: Boolean
        ) {
            if (!airplaneIsActive) {
                airplaneCooldownTicks++
                if (airplaneCooldownTicks > 130) {
                    airplaneIsActive = true
                    airplaneCooldownTicks = 0
                    airplaneX = -180f
                    airplaneY = Random.nextFloat() * (height * 0.28f) + (height * 0.12f)
                    airplaneSpeedX = Random.nextFloat() * 1.8f + 2.5f
                    airplaneHeadingDeg = Random.nextFloat() * 8f + 3f
                    airplaneContrailPoints.clear()
                    val callsigns = listOf("BG 088", "SQ 318", "EK 582", "LH 402", "BA 249", "QR 638")
                    airplaneCallsign = callsigns[Random.nextInt(callsigns.size)]
                    airplaneAltitudeFt = Random.nextInt(28000, 39000)
                }
                return
            }

            // Weather Interaction: Turbulence during rain / storm
            var currentY = airplaneY
            if (isRainy) {
                val turbulence = (sin(airplaneX.toDouble() * 0.035).toFloat() * 3.5f)
                currentY += turbulence
            }

            airplaneX += airplaneSpeedX
            airplaneY += (airplaneSpeedX * tan(Math.toRadians(airplaneHeadingDeg.toDouble()))).toFloat() * 0.2f
            airplaneBeaconTick++

            if (airplaneX > width + 200f) {
                airplaneIsActive = false
                airplaneCooldownTicks = 0
                return
            }

            val planeHeadingRad = Math.toRadians(airplaneHeadingDeg.toDouble())
            val cosHeading = cos(planeHeadingRad).toFloat()
            val sinHeading = sin(planeHeadingRad).toFloat()

            // Engine positions for dual contrails
            val engineDistanceBehind = 16f
            val engineOffsetSide = 14f

            val leftEngineX = airplaneX - engineDistanceBehind * cosHeading - engineOffsetSide * sinHeading
            val leftEngineY = currentY - engineDistanceBehind * sinHeading + engineOffsetSide * cosHeading

            val rightEngineX = airplaneX - engineDistanceBehind * cosHeading + engineOffsetSide * sinHeading
            val rightEngineY = currentY - engineDistanceBehind * sinHeading - engineOffsetSide * cosHeading

            // Emit contrail points periodically
            if (airplaneBeaconTick % 2 == 0) {
                val initWidth = if (isRainy) 4f else 3f
                val initAlpha = if (isNight) 0.38f else 0.65f
                airplaneContrailPoints.add(
                    WallpaperContrailPoint(
                        leftX = leftEngineX,
                        leftY = leftEngineY,
                        rightX = rightEngineX,
                        rightY = rightEngineY,
                        alpha = initAlpha,
                        width = initWidth
                    )
                )
            }

            // 1. Draw Jet Contrails
            val contrailPaint = Paint().apply {
                isAntiAlias = true
                style = Paint.Style.FILL
            }

            val iter = airplaneContrailPoints.iterator()
            while (iter.hasNext()) {
                val pt = iter.next()
                pt.alpha *= 0.965f
                pt.width += 0.18f

                if (pt.alpha < 0.04f) {
                    iter.remove()
                } else {
                    contrailPaint.color = if (isNight) Color.parseColor("#CBD5E1") else Color.parseColor("#F8FAFC")
                    contrailPaint.alpha = (pt.alpha * 255).toInt()
                    canvas.drawCircle(pt.leftX, pt.leftY, pt.width, contrailPaint)
                    canvas.drawCircle(pt.rightX, pt.rightY, pt.width, contrailPaint)
                }
            }

            // Weather Interaction: Visibility Occlusion
            val alphaFactor = if (isRainy) 0.45f else 1.0f

            // 2. Draw Airplane Body
            canvas.save()
            canvas.translate(airplaneX, currentY)
            canvas.rotate(airplaneHeadingDeg)

            val planePaint = Paint().apply {
                isAntiAlias = true
                style = Paint.Style.FILL
                color = if (isNight) Color.parseColor("#E2E8F0") else Color.WHITE
                alpha = (255 * alphaFactor).toInt()
            }

            // Main Swept Wings
            val wingPath = Path().apply {
                moveTo(8f, 0f)
                lineTo(-20f, -36f) // left wingtip
                lineTo(-28f, -36f)
                lineTo(-10f, 0f)
                lineTo(-28f, 36f)  // right wingtip
                lineTo(-20f, 36f)
                close()
            }
            canvas.drawPath(wingPath, planePaint)

            // Jet Engines
            val enginePaint = Paint().apply {
                isAntiAlias = true
                color = Color.parseColor("#94A3B8")
                alpha = (240 * alphaFactor).toInt()
            }
            canvas.drawRoundRect(RectF(-18f, -18f, -2f, -13f), 3f, 3f, enginePaint)
            canvas.drawRoundRect(RectF(-18f, 13f, -2f, 18f), 3f, 3f, enginePaint)

            // Horizontal Tail Stabilizers
            val tailWings = Path().apply {
                moveTo(-30f, 0f)
                lineTo(-42f, -16f)
                lineTo(-46f, -16f)
                lineTo(-38f, 0f)
                lineTo(-46f, 16f)
                lineTo(-42f, 16f)
                close()
            }
            canvas.drawPath(tailWings, planePaint)

            // Sleek Fuselage
            val fuselagePath = Path().apply {
                moveTo(36f, 0f) // nose cone
                cubicTo(24f, -7f, -22f, -7f, -40f, -3f)
                lineTo(-44f, 0f)
                lineTo(-40f, 3f)
                cubicTo(-22f, 7f, 24f, 7f, 36f, 0f)
                close()
            }
            canvas.drawPath(fuselagePath, planePaint)

            // Cockpit glass
            val cockpitPaint = Paint().apply {
                isAntiAlias = true
                color = Color.parseColor("#1E293B")
                alpha = (220 * alphaFactor).toInt()
            }
            canvas.drawCircle(22f, 0f, 3f, cockpitPaint)

            // Airline Livery Tail Fin Accent
            val finPaint = Paint().apply {
                isAntiAlias = true
                color = Color.parseColor("#00A8FF")
                alpha = (255 * alphaFactor).toInt()
            }
            canvas.drawRect(-38f, -2.5f, -28f, 2.5f, finPaint)

            // 3. Navigation Signal Lights
            val wingtipRed = Paint().apply {
                isAntiAlias = true
                color = Color.parseColor("#FF3B30")
            }
            val wingtipGreen = Paint().apply {
                isAntiAlias = true
                color = Color.parseColor("#34C759")
            }

            // Port (Left wingtip)
            canvas.drawCircle(-24f, -36f, 4f, wingtipRed)
            // Starboard (Right wingtip)
            canvas.drawCircle(-24f, 36f, 4f, wingtipGreen)

            // Double-flash Anti-Collision Strobe Beacon (Top & Tail)
            val strobeCycle = airplaneBeaconTick % 36
            val strobeActive = (strobeCycle in 0..3) || (strobeCycle in 8..11)

            if (strobeActive) {
                val strobePaint = Paint().apply {
                    isAntiAlias = true
                    color = Color.WHITE
                }
                val glowPaint = Paint().apply {
                    isAntiAlias = true
                    color = Color.argb(140, 255, 255, 255)
                }

                // Center fuselage strobe
                canvas.drawCircle(-2f, 0f, 5f, strobePaint)
                canvas.drawCircle(-2f, 0f, 14f, glowPaint)

                // Wingtip flash
                canvas.drawCircle(-24f, -36f, 5f, strobePaint)
                canvas.drawCircle(-24f, 36f, 5f, strobePaint)
            }

            canvas.restore()
        }
    }
}
