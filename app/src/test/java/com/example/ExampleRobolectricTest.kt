package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.core.ai.ApiKeyManager
import com.example.core.ai.ModelRouter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Aeris", appName)
  }

  @Test
  fun `verify model router default configuration`() {
    val router = ModelRouter()
    assertEquals("openai/gpt-oss-120b", router.getMainModel())
    assertEquals("qwen/qwen3.8-27b", router.getVisionModel())
    assertEquals("whisper-large-v3-turbo", router.getSttModel())
  }

  @Test
  fun `verify api key masking`() {
    val masked = ApiKeyManager.maskKey("gsk_1234567890abcdef")
    assertTrue(masked.startsWith("gsk_"))
    assertTrue(masked.contains("••••••••"))
    assertTrue(masked.endsWith("cdef"))
  }
}
