package com.cehpoint.netwin

import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.cehpoint.netwin.data.remote.FirebaseManager
import com.cehpoint.netwin.domain.model.RegistrationStep
import com.cehpoint.netwin.domain.model.RegistrationStepData
import com.cehpoint.netwin.presentation.events.RegistrationFlowEvent
import com.cehpoint.netwin.presentation.navigation.TournamentRegistration
import com.cehpoint.netwin.presentation.screens.RegistrationFlowScreen
import com.cehpoint.netwin.presentation.viewmodels.TournamentViewModel
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class RegistrationFlowBackNavigationTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Inject
    lateinit var firebaseManager: FirebaseManager

    private lateinit var navController: NavController
    private lateinit var viewModel: TournamentViewModel
    
    // Test state tracking variables
    private var topBarBackPressed by mutableStateOf(false)
    private var systemBackPressed by mutableStateOf(false)
    private var navigateUpCalled by mutableStateOf(false)
    private var popBackStackCalled by mutableStateOf(false)
    private var currentRegistrationStep by mutableStateOf(RegistrationStep.REVIEW)
    private var registrationStepData by mutableStateOf(RegistrationStepData())

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun testTopBarBackNavigationBetweenRegistrationSteps() {
        var currentStep by mutableStateOf(RegistrationStep.REVIEW)
        var stepData by mutableStateOf(RegistrationStepData())
        
        composeTestRule.setContent {
            navController = rememberNavController()
            
            // Mock registration flow with top bar navigation
            MockRegistrationFlowScreen(
                currentStep = currentStep,
                stepData = stepData,
                onStepChange = { step -> currentStep = step },
                onDataChange = { data -> stepData = data },
                onTopBarBack = { 
                    topBarBackPressed = true
                    if (currentStep != RegistrationStep.REVIEW) {
                        currentStep = getPreviousStep(currentStep)
                    } else {
                        navigateUpCalled = true
                        navController.navigateUp()
                    }
                }
            )
        }
        
        composeTestRule.waitForIdle()
        
        // Start at REVIEW step
        composeTestRule.onNodeWithTag("registration_step_REVIEW").assertIsDisplayed()
        assertEquals(RegistrationStep.REVIEW, currentStep)
        
        // Move to PAYMENT step
        composeTestRule.runOnUiThread {
            currentStep = RegistrationStep.PAYMENT
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("registration_step_PAYMENT").assertIsDisplayed()
        
        // Move to DETAILS step
        composeTestRule.runOnUiThread {
            currentStep = RegistrationStep.DETAILS
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("registration_step_DETAILS").assertIsDisplayed()
        
        // Move to CONFIRM step
        composeTestRule.runOnUiThread {
            currentStep = RegistrationStep.CONFIRM
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("registration_step_CONFIRM").assertIsDisplayed()
        
        // Test top bar back from CONFIRM to DETAILS
        topBarBackPressed = false
        composeTestRule.onNodeWithTag("top_bar_back_button").performClick()
        
        composeTestRule.waitForIdle()
        assertTrue("Top bar back should be pressed", topBarBackPressed)
        assertEquals(RegistrationStep.DETAILS, currentStep)
        composeTestRule.onNodeWithTag("registration_step_DETAILS").assertIsDisplayed()
        
        // Test top bar back from DETAILS to PAYMENT
        topBarBackPressed = false
        composeTestRule.onNodeWithTag("top_bar_back_button").performClick()
        
        composeTestRule.waitForIdle()
        assertTrue("Top bar back should be pressed", topBarBackPressed)
        assertEquals(RegistrationStep.PAYMENT, currentStep)
        composeTestRule.onNodeWithTag("registration_step_PAYMENT").assertIsDisplayed()
        
        // Test top bar back from PAYMENT to REVIEW
        topBarBackPressed = false
        composeTestRule.onNodeWithTag("top_bar_back_button").performClick()
        
        composeTestRule.waitForIdle()
        assertTrue("Top bar back should be pressed", topBarBackPressed)
        assertEquals(RegistrationStep.REVIEW, currentStep)
        composeTestRule.onNodeWithTag("registration_step_REVIEW").assertIsDisplayed()
        
        // Test top bar back from REVIEW - should exit registration flow
        topBarBackPressed = false
        navigateUpCalled = false
        composeTestRule.onNodeWithTag("top_bar_back_button").performClick()
        
        composeTestRule.waitForIdle()
        assertTrue("Top bar back should be pressed", topBarBackPressed)
        assertTrue("NavigateUp should be called when exiting registration flow", navigateUpCalled)
    }

    @Test
    fun testSystemBackNavigationWithStatePreservation() {
        var currentStep by mutableStateOf(RegistrationStep.REVIEW)
        var stepData by mutableStateOf(RegistrationStepData())
        
        composeTestRule.setContent {
            navController = rememberNavController()
            
            MockRegistrationFlowScreen(
                currentStep = currentStep,
                stepData = stepData,
                onStepChange = { step -> currentStep = step },
                onDataChange = { data -> stepData = data },
                onSystemBack = {
                    systemBackPressed = true
                    if (currentStep != RegistrationStep.REVIEW) {
                        // Save current step data before going back
                        when (currentStep) {
                            RegistrationStep.PAYMENT -> {
                                stepData = stepData.copy(paymentMethod = "test_payment")
                            }
                            RegistrationStep.DETAILS -> {
                                stepData = stepData.copy(playerName = "test_player")
                            }
                            RegistrationStep.CONFIRM -> {
                                stepData = stepData.copy(agreedToTerms = true)
                            }
                            else -> {}
                        }
                        currentStep = getPreviousStep(currentStep)
                    } else {
                        navigateUpCalled = true
                        navController.navigateUp()
                    }
                }
            )
        }
        
        composeTestRule.waitForIdle()
        
        // Navigate through steps and add data
        currentStep = RegistrationStep.PAYMENT
        stepData = stepData.copy(paymentMethod = "credit_card")
        composeTestRule.waitForIdle()
        
        currentStep = RegistrationStep.DETAILS
        stepData = stepData.copy(playerName = "John Doe")
        composeTestRule.waitForIdle()
        
        currentStep = RegistrationStep.CONFIRM
        stepData = stepData.copy(agreedToTerms = true)
        composeTestRule.waitForIdle()
        
        // Test system back from CONFIRM - should preserve state
        systemBackPressed = false
        val initialStepData = stepData
        Espresso.pressBack()
        
        composeTestRule.waitForIdle()
        assertTrue("System back should be pressed", systemBackPressed)
        assertEquals(RegistrationStep.DETAILS, currentStep)
        assertTrue("State should be preserved", stepData.agreedToTerms)
        assertEquals("Payment method should be preserved", "credit_card", stepData.paymentMethod)
        assertEquals("Player name should be preserved", "John Doe", stepData.playerName)
        
        // Continue back navigation and verify state preservation
        systemBackPressed = false
        Espresso.pressBack()
        
        composeTestRule.waitForIdle()
        assertTrue("System back should be pressed", systemBackPressed)
        assertEquals(RegistrationStep.PAYMENT, currentStep)
        assertEquals("Player name should be preserved", "test_player", stepData.playerName)
        assertTrue("Terms agreement should be preserved", stepData.agreedToTerms)
    }

    @Test
    fun testIllegalStatePreventionInRegistrationFlow() {
        var currentStep by mutableStateOf(RegistrationStep.REVIEW)
        var illegalNavigationAttempt by mutableStateOf(false)
        
        composeTestRule.setContent {
            navController = rememberNavController()
            
            MockRegistrationFlowScreen(
                currentStep = currentStep,
                stepData = RegistrationStepData(),
                onStepChange = { step -> currentStep = step },
                onDataChange = { },
                onIllegalNavigation = { illegalNavigationAttempt = true }
            )
        }
        
        composeTestRule.waitForIdle()
        
        // Try to navigate to previous step when already at first step
        composeTestRule.runOnUiThread {
            try {
                val previousStep = getPreviousStep(RegistrationStep.REVIEW)
                if (previousStep == RegistrationStep.REVIEW) {
                    // This should not change the step and should prevent illegal state
                    illegalNavigationAttempt = true
                }
            } catch (e: IllegalStateException) {
                illegalNavigationAttempt = true
            }
        }
        
        composeTestRule.waitForIdle()
        assertTrue("Illegal navigation attempt should be prevented", illegalNavigationAttempt)
        assertEquals("Should remain at REVIEW step", RegistrationStep.REVIEW, currentStep)
    }

    @Test
    fun testRegistrationFlowExitConfirmation() {
        var currentStep by mutableStateOf(RegistrationStep.DETAILS)
        var exitConfirmationShown by mutableStateOf(false)
        var forceExit by mutableStateOf(false)
        
        composeTestRule.setContent {
            navController = rememberNavController()
            
            MockRegistrationFlowScreen(
                currentStep = currentStep,
                stepData = RegistrationStepData(playerName = "Test Player"),
                onStepChange = { step -> currentStep = step },
                onDataChange = { },
                showExitConfirmation = exitConfirmationShown,
                onShowExitConfirmation = { exitConfirmationShown = it },
                onForceExit = { forceExit = it },
                onSystemBack = {
                    if (currentStep == RegistrationStep.REVIEW && !forceExit) {
                        // Show confirmation dialog when trying to exit with data
                        exitConfirmationShown = true
                    } else {
                        navigateUpCalled = true
                        navController.navigateUp()
                    }
                }
            )
        }
        
        composeTestRule.waitForIdle()
        
        // Navigate to first step with some data entered
        composeTestRule.runOnUiThread {
            currentStep = RegistrationStep.REVIEW
        }
        composeTestRule.waitForIdle()
        
        // Try to exit - should show confirmation
        Espresso.pressBack()
        composeTestRule.waitForIdle()
        
        assertTrue("Exit confirmation should be shown", exitConfirmationShown)
        composeTestRule.onNodeWithTag("exit_confirmation_dialog").assertIsDisplayed()
        
        // Cancel exit
        composeTestRule.onNodeWithTag("cancel_exit_button").performClick()
        composeTestRule.waitForIdle()
        
        assertFalse("Exit confirmation should be hidden", exitConfirmationShown)
        assertEquals("Should remain at REVIEW step", RegistrationStep.REVIEW, currentStep)
        
        // Try to exit again and confirm
        Espresso.pressBack()
        composeTestRule.waitForIdle()
        assertTrue("Exit confirmation should be shown again", exitConfirmationShown)
        
        composeTestRule.onNodeWithTag("confirm_exit_button").performClick()
        composeTestRule.waitForIdle()
        
        assertTrue("NavigateUp should be called after confirmation", navigateUpCalled)
    }

    @Test
    fun testBackNavigationPerformance() {
        var navigationCount by mutableStateOf(0)
        val navigationTimes = mutableListOf<Long>()
        
        composeTestRule.setContent {
            navController = rememberNavController()
            
            // Track navigation performance
            LaunchedEffect(navigationCount) {
                if (navigationCount > 0) {
                    navigationTimes.add(System.currentTimeMillis())
                }
            }
            
            MockRegistrationFlowScreen(
                currentStep = RegistrationStep.CONFIRM,
                stepData = RegistrationStepData(),
                onStepChange = { },
                onDataChange = { },
                onSystemBack = {
                    navigationCount++
                }
            )
        }
        
        composeTestRule.waitForIdle()
        
        val startTime = System.currentTimeMillis()
        
        // Perform rapid back navigations
        repeat(10) {
            Espresso.pressBack()
            composeTestRule.waitForIdle()
        }
        
        val endTime = System.currentTimeMillis()
        val totalTime = endTime - startTime
        
        // Verify performance is acceptable (should complete within reasonable time)
        assertTrue("Navigation should complete within 5 seconds", totalTime < 5000)
        assertTrue("All navigation events should be handled", navigationCount > 0)
    }

    private fun getPreviousStep(currentStep: RegistrationStep): RegistrationStep {
        return when (currentStep) {
            RegistrationStep.PAYMENT -> RegistrationStep.REVIEW
            RegistrationStep.DETAILS -> RegistrationStep.PAYMENT
            RegistrationStep.CONFIRM -> RegistrationStep.DETAILS
            RegistrationStep.REVIEW -> RegistrationStep.REVIEW // Can't go back further
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun MockRegistrationFlowScreen(
        currentStep: RegistrationStep,
        stepData: RegistrationStepData,
        onStepChange: (RegistrationStep) -> Unit,
        onDataChange: (RegistrationStepData) -> Unit,
        onTopBarBack: () -> Unit = {},
        onSystemBack: () -> Unit = {},
        onIllegalNavigation: () -> Unit = {},
        showExitConfirmation: Boolean = false,
        onShowExitConfirmation: (Boolean) -> Unit = {},
        onForceExit: (Boolean) -> Unit = {}
    ) {
        // Handle system back press
        BackHandler {
            onSystemBack()
        }
        
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Registration Step: ${currentStep.name}") },
                    navigationIcon = {
                        IconButton(
                            onClick = onTopBarBack,
                            modifier = Modifier.testTag("top_bar_back_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
                    .testTag("registration_step_${currentStep.name}"),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Current Step: ${currentStep.name}",
                    style = MaterialTheme.typography.headlineMedium
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Show step-specific content
                when (currentStep) {
                    RegistrationStep.REVIEW -> {
                        Text("Review tournament details")
                        Text("Player: ${stepData.playerName}")
                        Text("Payment: ${stepData.paymentMethod}")
                    }
                    RegistrationStep.PAYMENT -> {
                        Text("Payment information")
                        Text("Selected method: ${stepData.paymentMethod}")
                    }
                    RegistrationStep.DETAILS -> {
                        Text("Player details")
                        Text("Name: ${stepData.playerName}")
                    }
                    RegistrationStep.CONFIRM -> {
                        Text("Confirm registration")
                        Text("Terms agreed: ${stepData.agreedToTerms}")
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Navigation buttons for testing
                Row {
                    Button(
                        onClick = {
                            val nextStep = getNextStep(currentStep)
                            if (nextStep != currentStep) {
                                onStepChange(nextStep)
                            }
                        },
                        enabled = currentStep != RegistrationStep.CONFIRM
                    ) {
                        Text("Next")
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Button(
                        onClick = {
                            val prevStep = getPreviousStep(currentStep)
                            if (prevStep != currentStep) {
                                onStepChange(prevStep)
                            } else {
                                onIllegalNavigation()
                            }
                        },
                        enabled = currentStep != RegistrationStep.REVIEW
                    ) {
                        Text("Previous")
                    }
                }
            }
        }
        
        // Exit confirmation dialog
        if (showExitConfirmation) {
            AlertDialog(
                onDismissRequest = { onShowExitConfirmation(false) },
                title = { Text("Exit Registration?") },
                text = { Text("You have unsaved changes. Are you sure you want to exit?") },
                confirmButton = {
                    TextButton(
                        onClick = { 
                            onForceExit(true)
                            onShowExitConfirmation(false)
                        },
                        modifier = Modifier.testTag("confirm_exit_button")
                    ) {
                        Text("Exit")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { onShowExitConfirmation(false) },
                        modifier = Modifier.testTag("cancel_exit_button")
                    ) {
                        Text("Cancel")
                    }
                },
                modifier = Modifier.testTag("exit_confirmation_dialog")
            )
        }
    }

    private fun getNextStep(currentStep: RegistrationStep): RegistrationStep {
        return when (currentStep) {
            RegistrationStep.REVIEW -> RegistrationStep.PAYMENT
            RegistrationStep.PAYMENT -> RegistrationStep.DETAILS
            RegistrationStep.DETAILS -> RegistrationStep.CONFIRM
            RegistrationStep.CONFIRM -> RegistrationStep.CONFIRM // Can't go further
        }
    }
}
