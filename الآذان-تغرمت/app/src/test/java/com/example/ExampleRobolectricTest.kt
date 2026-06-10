package com.example

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.AdhanViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
    assertEquals("الآذان تغرمت", appName)
  }

  @Test
  fun `viewModel instantiation does not crash`() {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val viewModel = AdhanViewModel(app)
    assertNotNull(viewModel)
    assertNotNull(viewModel.prayerTimes.value)
  }
}
