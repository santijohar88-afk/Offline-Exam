package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
    assertEquals("EKIII Exam Hub", appName)
  }

  @Test
  fun `verify bundled html assets exist`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val assets = context.assets.list("") ?: emptyArray()
    assertTrue("index.html must exist in assets", assets.contains("index.html"))
    assertTrue("seal.jpg must exist in assets", assets.contains("seal.jpg"))
    assertTrue("jsQR.min.js must exist in assets", assets.contains("jsQR.min.js"))
  }

  @Test
  fun `verify camera permission declared in manifest`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val packageInfo = context.packageManager.getPackageInfo(
        context.packageName,
        android.content.pm.PackageManager.GET_PERMISSIONS
    )
    val permissions = packageInfo.requestedPermissions ?: emptyArray()
    assertTrue("android.permission.CAMERA must be declared", permissions.contains(android.Manifest.permission.CAMERA))
  }
}
