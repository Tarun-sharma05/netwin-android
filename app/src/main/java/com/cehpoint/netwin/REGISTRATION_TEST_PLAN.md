# End-to-End Test Plan: Multi-Step Tournament Registration

## Test Environment Setup
- Device: Android emulator or physical device
- User: Logged in with sufficient wallet balance (>₹100)
- Tournament: Available tournament with open slots

## Test Scenarios

### 1. Happy Path - Complete Registration Flow
**Steps:**
1. Navigate to tournaments list
2. Tap "Join Tournament" on any available tournament
3. **Step 1 - Review Tournament:**
   - Verify tournament details display correctly
   - Verify player info shows correct username and wallet balance
   - Verify "Ready" status appears if sufficient funds
   - Tap "Continue to Payment"
4. **Step 2 - Payment Selection:**
   - Verify "Wallet" option is selected by default
   - Verify "Pay with Card" is disabled
   - Tap "Continue to Details"
5. **Step 3 - Game Details:** (When implemented)
   - Enter in-game ID
   - Enter team name (if required)
   - Accept terms and conditions
   - Tap "Review & Submit"
6. **Step 4 - Confirmation:** (When implemented)
   - Verify registration summary is correct
   - Tap "Complete Registration"
   - Verify success message appears
   - Verify navigation back to tournament list

**Expected Result:** User successfully registered for tournament

### 2. Insufficient Funds Flow
**Steps:**
1. Use account with wallet balance < tournament entry fee
2. Navigate to Step 1
3. Verify "Insufficient" status appears in red
4. Tap "Continue to Payment"
5. Verify insufficient funds dialog appears
6. Tap "Add Funds" or "Cancel"

**Expected Result:** User prevented from proceeding without sufficient funds

### 3. Navigation Flow
**Steps:**
1. Start registration flow
2. Navigate to Step 2, then use back button
3. Verify Step 1 reappears
4. Navigate to Step 2 again
5. Use system back button
6. Verify Step 1 appears
7. Use system back button again
8. Verify navigation returns to tournament details

**Expected Result:** Back navigation works correctly at all steps

### 4. Progress Indicator
**Steps:**
1. Start registration flow
2. Verify step 1 indicator is highlighted in cyan
3. Progress to step 2
4. Verify step 2 indicator is highlighted, step 1 remains cyan
5. Continue through all steps
6. Verify progress indicator updates correctly

**Expected Result:** Progress indicator accurately reflects current step

### 5. Data Persistence
**Steps:**
1. Complete Steps 1-2
2. Navigate to Step 3 and enter some data
3. Use back button to return to Step 2
4. Navigate forward to Step 3 again
5. Verify entered data is still present

**Expected Result:** User data persists when navigating between steps

## Manual Testing Commands

To test the flow manually:

```bash
# Build and install the app
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Launch the app
adb shell am start -n com.cehpoint.netwin/.MainActivity
```

## Automated Testing (Future)

```kotlin
@Test
fun testCompleteRegistrationFlow() {
    // 1. Navigate to tournament
    composeTestRule.onNodeWithText("Join Tournament").performClick()
    
    // 2. Step 1 - Verify and continue
    composeTestRule.onNodeWithText("Continue to Payment").performClick()
    
    // 3. Step 2 - Select payment and continue
    composeTestRule.onNodeWithText("Continue to Details").performClick()
    
    // 4. Step 3 - Enter details and continue
    composeTestRule.onNodeWithText("inGameId").performTextInput("TestUser123")
    composeTestRule.onNodeWithText("Review & Submit").performClick()
    
    // 5. Step 4 - Complete registration
    composeTestRule.onNodeWithText("Complete Registration").performClick()
    
    // 6. Verify success
    composeTestRule.onNodeWithText("Successfully registered").assertIsDisplayed()
}
```

## Edge Cases to Test

1. **Network Issues:**
   - Test with no internet connection
   - Test with slow connection during registration

2. **Tournament State Changes:**
   - Tournament becomes full during registration
   - Tournament gets cancelled during registration

3. **User State Changes:**
   - User logs out during registration
   - Wallet balance changes during registration

4. **App Lifecycle:**
   - Test registration flow survives app backgrounding
   - Test with device rotation

## Success Criteria

- ✅ All 4 steps navigate correctly
- ✅ Data persists between steps
- ✅ Progress indicator updates accurately
- ✅ Back navigation works properly
- ✅ Error states are handled gracefully
- ✅ Registration completes successfully
- ✅ User feedback is clear and helpful

## Bug Reporting Template

**Issue:** [Brief description]
**Steps to Reproduce:**
1. Step 1
2. Step 2
3. Step 3

**Expected:** [What should happen]
**Actual:** [What actually happened]
**Screenshot:** [If applicable]
**Device:** [Device model and Android version]
