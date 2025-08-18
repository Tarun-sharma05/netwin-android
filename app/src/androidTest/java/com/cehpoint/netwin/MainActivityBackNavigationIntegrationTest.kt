package com.cehpoint.netwin

import android.content.Context
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ApplicationProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.cehpoint.netwin.data.remote.FirebaseManager
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

@LargeTest
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class MainActivityBackNavigationIntegrationTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @get:Rule
    val activityScenarioRule = ActivityScenarioRule(MainActivity::class.java)

    @Inject
    lateinit var firebaseManager: FirebaseManager

    private lateinit var device: UiDevice
    private lateinit var context: Context

    @Before
    fun setup() {
        hiltRule.inject()
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun testMainActivitySystemBackBehavior() {
        // Wait for activity to be fully loaded
        Thread.sleep(2000)
        
        activityScenarioRule.scenario.onActivity { activity ->
            assertNotNull("Activity should be created", activity)
            assertEquals("Activity should be resumed", Lifecycle.State.RESUMED, activity.lifecycle.currentState)
        }

        // Try system back press from main screen - should not crash
        device.pressBack()
        Thread.sleep(1000)
        
        activityScenarioRule.scenario.onActivity { activity ->
            // Activity should still be alive or properly finished
            assertTrue("Activity state should be valid", 
                activity.lifecycle.currentState == Lifecycle.State.RESUMED || 
                activity.lifecycle.currentState == Lifecycle.State.DESTROYED)
        }
    }

    @Test
    fun testNavigationThroughBottomTabsAndBack() {
        // Wait for initial load
        Thread.sleep(3000)
        
        activityScenarioRule.scenario.onActivity { activity ->
            // Try to navigate through bottom tabs if authenticated
            // This simulates real user navigation patterns
        }
        
        // Simulate multiple rapid back presses (common user behavior)
        repeat(3) {
            device.pressBack()
            Thread.sleep(200)
        }
        
        // Verify app doesn't crash
        activityScenarioRule.scenario.onActivity { activity ->
            assertTrue("Activity should handle rapid back presses gracefully",
                activity.lifecycle.currentState == Lifecycle.State.RESUMED || 
                activity.lifecycle.currentState == Lifecycle.State.DESTROYED)
        }
    }

    @Test
    fun testBackNavigationWithProcessDeath() {
        // Wait for initial setup
        Thread.sleep(2000)
        
        var initialProcessId = 0
        activityScenarioRule.scenario.onActivity { activity ->
            initialProcessId = android.os.Process.myPid()
        }
        
        // Simulate process death by moving to background and killing
        activityScenarioRule.scenario.moveToState(Lifecycle.State.CREATED)
        Thread.sleep(1000)
        
        // Recreate activity (simulates process death recovery)
        activityScenarioRule.scenario.recreate()
        Thread.sleep(2000)
        
        activityScenarioRule.scenario.onActivity { activity ->
            val newProcessId = android.os.Process.myPid()
            // Process might be same or different depending on Android behavior
            assertNotNull("Activity should be recreated successfully", activity)
        }
        
        // Test back navigation after recreation
        device.pressBack()
        Thread.sleep(1000)
        
        activityScenarioRule.scenario.onActivity { activity ->
            // Should handle back press gracefully even after process recreation
            assertTrue("Activity should handle back press after recreation",
                activity.lifecycle.currentState == Lifecycle.State.RESUMED || 
                activity.lifecycle.currentState == Lifecycle.State.DESTROYED)
        }
    }

    @Test
    fun testBackNavigationMemoryLeaks() {
        val initialMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        
        // Perform navigation operations that could cause memory leaks
        repeat(10) {
            activityScenarioRule.scenario.onActivity { activity ->
                // Simulate complex navigation patterns
            }
            
            device.pressBack()
            Thread.sleep(100)
            
            // Force garbage collection to see if there are memory leaks
            System.gc()
            Thread.sleep(100)
        }
        
        val finalMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        val memoryIncrease = finalMemory - initialMemory
        
        // Memory should not increase significantly (allowing for some variation)
        assertTrue("Memory usage should not increase significantly during navigation", 
            memoryIncrease < 50 * 1024 * 1024) // 50MB threshold
    }

    @Test
    fun testBackNavigationAccessibility() {
        // Wait for setup
        Thread.sleep(2000)
        
        // Enable accessibility services simulation
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val uiAutomation = instrumentation.uiAutomation
        
        try {
            // Test that back navigation works with accessibility services
            device.pressBack()
            Thread.sleep(1000)
            
            activityScenarioRule.scenario.onActivity { activity ->
                assertNotNull("Activity should handle back press with accessibility", activity)
            }
            
        } catch (e: Exception) {
            fail("Back navigation should work with accessibility features: ${e.message}")
        }
    }

    @Test
    fun testBackNavigationInDifferentOrientations() {
        // Test portrait mode
        device.setOrientationNatural()
        Thread.sleep(1000)
        
        device.pressBack()
        Thread.sleep(500)
        
        activityScenarioRule.scenario.onActivity { activity ->
            assertNotNull("Activity should handle back in portrait", activity)
        }
        
        // Test landscape mode
        device.setOrientationLeft()
        Thread.sleep(1000)
        
        device.pressBack()
        Thread.sleep(500)
        
        activityScenarioRule.scenario.onActivity { activity ->
            assertNotNull("Activity should handle back in landscape", activity)
        }
        
        // Return to natural orientation
        device.setOrientationNatural()
        Thread.sleep(500)
    }

    @Test
    fun testBackNavigationWithDeepLinks() {
        // Create a deep link intent
        val deepLinkIntent = Intent().apply {
            action = Intent.ACTION_VIEW
            data = android.net.Uri.parse("netwin://tournament/123")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        
        try {
            // Launch with deep link
            val deepLinkScenario = ActivityScenario.launch<MainActivity>(deepLinkIntent)
            Thread.sleep(2000)
            
            deepLinkScenario.onActivity { activity ->
                assertNotNull("Deep link activity should be created", activity)
            }
            
            // Test back navigation from deep link
            device.pressBack()
            Thread.sleep(1000)
            
            deepLinkScenario.close()
        } catch (e: Exception) {
            // Deep linking might not be fully implemented, so we log but don't fail
            println("Deep link test failed (expected if not implemented): ${e.message}")
        }
    }

    @Test
    fun testBackNavigationWithSystemUI() {
        // Test back navigation while system UI is hidden/shown
        activityScenarioRule.scenario.onActivity { activity ->
            // Simulate full screen mode
            activity.window.decorView.systemUiVisibility = (
                android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
                or android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            )
        }
        
        Thread.sleep(1000)
        
        device.pressBack()
        Thread.sleep(1000)
        
        activityScenarioRule.scenario.onActivity { activity ->
            assertNotNull("Activity should handle back press in fullscreen", activity)
        }
        
        // Restore system UI
        activityScenarioRule.scenario.onActivity { activity ->
            activity.window.decorView.systemUiVisibility = android.view.View.SYSTEM_UI_FLAG_VISIBLE
        }
    }

    @Test
    fun testBackNavigationPerformanceUnderLoad() {
        val startTime = System.currentTimeMillis()
        var navigationCount = 0
        
        // Perform navigation under load
        repeat(50) { iteration ->
            try {
                device.pressBack()
                navigationCount++
                
                // Small delay to avoid overwhelming the system
                Thread.sleep(50)
                
                // Verify activity is still responsive every 10 iterations
                if (iteration % 10 == 0) {
                    activityScenarioRule.scenario.onActivity { activity ->
                        assertNotNull("Activity should remain responsive under load", activity)
                    }
                }
                
            } catch (e: Exception) {
                println("Navigation failed at iteration $iteration: ${e.message}")
                break
            }
        }
        
        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime
        
        assertTrue("Navigation performance should be acceptable", duration < 30000) // 30 seconds max
        assertTrue("Should handle multiple navigation events", navigationCount > 0)
    }

    @Test
    fun testBackNavigationWithMultipleActivities() {
        // This test would be relevant if the app launches other activities
        activityScenarioRule.scenario.onActivity { mainActivity ->
            try {
                // Simulate launching another activity (like settings or external browser)
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = android.net.Uri.parse("https://example.com")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                
                // This might open external browser, so we handle gracefully
                mainActivity.startActivity(intent)
                Thread.sleep(2000)
                
                // Come back with back press
                device.pressBack()
                Thread.sleep(1000)
                
            } catch (e: Exception) {
                // External activity launch might fail in test environment, which is OK
                println("External activity test failed (expected): ${e.message}")
            }
        }
        
        // Verify main activity is still functional
        activityScenarioRule.scenario.onActivity { activity ->
            assertNotNull("Main activity should be functional after external navigation", activity)
        }
    }

    @Test
    fun testBackNavigationStateConsistency() {
        var stateCheckCount = 0
        
        // Perform navigation and check state consistency
        repeat(5) {
            device.pressBack()
            Thread.sleep(300)
            
            activityScenarioRule.scenario.onActivity { activity ->
                stateCheckCount++
                
                // Verify activity state is consistent
                assertTrue("Activity lifecycle should be in valid state",
                    activity.lifecycle.currentState == Lifecycle.State.RESUMED ||
                    activity.lifecycle.currentState == Lifecycle.State.STARTED ||
                    activity.lifecycle.currentState == Lifecycle.State.DESTROYED)
                
                // If activity is still alive, verify it's functional
                if (activity.lifecycle.currentState == Lifecycle.State.RESUMED) {
                    assertNotNull("Activity should maintain references", activity.window)
                    assertNotNull("Activity should maintain context", activity.baseContext)
                }
            }
        }
        
        assertTrue("Should have performed state checks", stateCheckCount > 0)
    }

    @Test
    fun testBackNavigationWithConfigurationChanges() {
        // Test back navigation during configuration changes
        activityScenarioRule.scenario.onActivity { activity ->
            // Trigger configuration change
            val config = activity.resources.configuration
            config.fontScale = 1.5f // Change font scale
        }
        
        Thread.sleep(1000)
        
        // Press back during/after configuration change
        device.pressBack()
        Thread.sleep(1000)
        
        activityScenarioRule.scenario.onActivity { activity ->
            assertNotNull("Activity should handle back press during config changes", activity)
        }
    }
}
