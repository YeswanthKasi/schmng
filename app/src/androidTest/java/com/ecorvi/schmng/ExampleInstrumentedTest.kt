package com.ecorvi.schmng

import android.content.pm.PackageManager
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*

@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {

    @Test
    fun useAppContext() {
        // Verifies the app's package name
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.ecorvi.schmng", appContext.packageName)
    }

    @Test
    fun appHasInternetPermission() {
        // Verifies that the app has the INTERNET permission
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val hasPermission = appContext.checkCallingOrSelfPermission("android.permission.INTERNET") == PackageManager.PERMISSION_GRANTED
        assertTrue("App should have INTERNET permission", hasPermission)
    }

    @Test
    fun launchWebAppActivity() {
        // Verifies that MainActivity launches successfully
        val scenario = ActivityScenario.launch(WebAppActivity::class.java)
        assertNotNull("WebAppActivity launched successfully", scenario)
    }
}
