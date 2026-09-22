package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Live Weather Wallpaper", appName)
  }

  @Test
  fun `verify default airplane flight animation setting is enabled`() {
    val settings = com.example.data.WallpaperSettings()
    assertEquals(true, settings.airplaneFlightAnimationEnabled)
  }
}
