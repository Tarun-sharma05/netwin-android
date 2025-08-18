# Quick Registration Flow Verification Script
Write-Host "🔍 VERIFYING MULTI-STEP REGISTRATION IMPLEMENTATION" -ForegroundColor Cyan
Write-Host "====================================================" -ForegroundColor Cyan

# Check if key files exist and have required content
$checks = @(
    @{
        File = "src\main\java\com\cehpoint\netwin\presentation\screens\RegistrationFlowScreen.kt"
        Pattern = "RegistrationStep1.*tournament.*onNext.*viewModel"
        Name = "Step 1 Implementation"
    },
    @{
        File = "src\main\java\com\cehpoint\netwin\presentation\screens\RegistrationFlowScreen.kt"
        Pattern = "RegistrationStep2.*stepData.*paymentMethod"
        Name = "Step 2 Implementation"
    },
    @{
        File = "src\main\java\com\cehpoint\netwin\presentation\screens\TournamentDetailsScreen.kt"
        Pattern = "TournamentRegistration.*tournamentId.*stepIndex"
        Name = "Tournament Details Navigation"
    },
    @{
        File = "src\main\java\com\cehpoint\netwin\presentation\screens\TournamentsScreen.kt"
        Pattern = "TournamentRegistration.*tournament\.id"
        Name = "Tournament Card Navigation"
    },
    @{
        File = "src\main\java\com\cehpoint\netwin\presentation\navigation\NavGraph.kt"
        Pattern = "RegistrationFlowScreen.*tournamentId.*stepIndex"
        Name = "Navigation Setup"
    }
)

$passedChecks = 0
$totalChecks = $checks.Count

foreach ($check in $checks) {
    if (Test-Path $check.File) {
        $content = Get-Content $check.File -Raw
        if ($content -match $check.Pattern) {
            Write-Host "✅ $($check.Name) - IMPLEMENTED" -ForegroundColor Green
            $passedChecks++
        } else {
            Write-Host "❌ $($check.Name) - MISSING IMPLEMENTATION" -ForegroundColor Red
        }
    } else {
        Write-Host "❌ $($check.Name) - FILE NOT FOUND" -ForegroundColor Red
    }
}

Write-Host "`n📊 VERIFICATION RESULTS" -ForegroundColor Cyan
Write-Host "======================" -ForegroundColor Cyan
Write-Host "Passed: $passedChecks/$totalChecks checks" -ForegroundColor $(if ($passedChecks -eq $totalChecks) { "Green" } else { "Yellow" })

$completionPercentage = [math]::Round(($passedChecks / $totalChecks) * 100, 1)
Write-Host "Completion: $completionPercentage%" -ForegroundColor $(if ($completionPercentage -ge 80) { "Green" } elseif ($completionPercentage -ge 60) { "Yellow" } else { "Red" })

Write-Host "`n🚀 NEXT STEPS" -ForegroundColor Cyan
Write-Host "=============" -ForegroundColor Cyan
if ($completionPercentage -ge 80) {
    Write-Host "✨ Implementation is mostly complete!" -ForegroundColor Green
    Write-Host "→ Implement remaining Step 3 & 4 UIs" -ForegroundColor Yellow
    Write-Host "→ Test the registration flow end-to-end" -ForegroundColor Yellow
    Write-Host "→ Wire up final registration submission" -ForegroundColor Yellow
} else {
    Write-Host "⚠️  More work needed on core components" -ForegroundColor Red
    Write-Host "→ Fix missing implementations marked with ❌" -ForegroundColor Yellow
}

Write-Host "`n💡 TO TEST THE CURRENT IMPLEMENTATION:" -ForegroundColor Cyan
Write-Host "1. Build the app: ./gradlew assembleDebug" -ForegroundColor Gray
Write-Host "2. Install: adb install -r app/build/outputs/apk/debug/app-debug.apk" -ForegroundColor Gray
Write-Host "3. Navigate to any tournament and tap 'Join Tournament'" -ForegroundColor Gray
Write-Host "4. Verify the 4-step registration flow appears" -ForegroundColor Gray
