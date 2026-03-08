# NetWin - Presentation Layer Documentation

**Location:** `app/src/main/java/com/cehpoint/netwin/presentation/`

---

## 1. Structure

```
presentation/
├── viewmodels/           # ViewModels
│   ├── WalletViewModel.kt
│   ├── TournamentViewModel.kt
│   ├── AuthViewModel.kt
│   └── ...
├── screens/             # Full-screen composables
│   ├── WalletScreen.kt
│   ├── TournamentsScreen.kt
│   ├── TournamentDetailsScreen.kt
│   ├── RegistrationFlowScreen.kt
│   ├── VictoryPassScreen.kt
│   ├── MyTournamentsScreen.kt
│   ├── ManualUpiDepositScreen.kt
│   ├── KycScreen.kt
│   ├── ProfileScreen.kt
│   ├── MoreScreen.kt
│   └── ...
├── components/          # Reusable UI components
│   ├── TournamentCard.kt
│   ├── BalanceCard.kt
│   ├── TransactionItem.kt
│   └── ...
├── navigation/          # Navigation
│   ├── NavGraph.kt
│   └── NavigationRoutes.kt
└── theme/              # UI theming
    ├── Color.kt
    ├── Theme.kt
    └── Type.kt
```

---

## 2. ViewModels

### 2.1 WalletViewModel.kt

**Location:** `app/src/main/java/com/cehpoint/netwin/presentation/viewmodels/WalletViewModel.kt`

**Purpose:** Manage wallet screen state and operations

**Dependencies:**
```kotlin
@HiltViewModel
class WalletViewModel @Inject constructor(
    private val walletRepository: WalletRepository,
    private val adminConfigRepository: AdminConfigRepository,
    private val kycRepository: KycRepository
) : ViewModel()
```

**UI State:**
```kotlin
data class WalletUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    
    // Balance
    val balance: Double = 0.0,
    val withdrawableBalance: Double = 0.0,
    val bonusBalance: Double = 0.0,
    val currency: String = "INR",
    
    // Transactions
    val transactions: List<Transaction> = emptyList(),
    val isLoadingTransactions: Boolean = false,
    
    // Pending deposits
    val pendingDeposits: List<PendingDeposit> = emptyList(),
    
    // UPI settings
    val upiSettings: UpiSettings? = null,
    
    // KYC status
    val kycStatus: String? = null,
    
    // Deposit submission
    val depositSubmitted: Boolean = false,
    val depositError: String? = null
)
```

**Key Functions:**
```kotlin
class WalletViewModel : ViewModel() {
    
    private val _uiState = MutableStateFlow(WalletUiState())
    val uiState: StateFlow<WalletUiState> = _uiState.asStateFlow()
    
    init {
        loadWalletData()
        loadUpiSettings()
        loadKycStatus()
    }
    
    // Load wallet balance
    private fun loadWalletData() {
        viewModelScope.launch {
            val userId = walletRepository.getCurrentUserId()
            
            walletRepository.getWallet(userId)
                .collect { result ->
                    result.onSuccess { wallet ->
                        _uiState.update {
                            it.copy(
                                balance = wallet.balance,
                                withdrawableBalance = wallet.withdrawableBalance,
                                bonusBalance = wallet.bonusBalance,
                                currency = wallet.currency
                            )
                        }
                    }.onFailure { error ->
                        _uiState.update { it.copy(error = error.message) }
                    }
                }
        }
    }
    
    // Load UPI settings
    fun loadUpiSettings() {
        viewModelScope.launch {
            val settings = adminConfigRepository.getUpiSettings("INR")
            _uiState.update { it.copy(upiSettings = settings) }
        }
    }
    
    // Submit manual deposit
    fun submitManualDeposit(deposit: ManualUpiDeposit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, depositError = null) }
            
            walletRepository.submitManualDeposit(deposit)
                .onSuccess { depositId ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            depositSubmitted = true
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            depositError = error.message
                        )
                    }
                }
        }
    }
    
    // Load transactions
    fun loadTransactions() {
        viewModelScope.launch {
            val userId = walletRepository.getCurrentUserId()
            
            walletRepository.getTransactions(userId)
                .collect { transactions ->
                    _uiState.update { it.copy(transactions = transactions) }
                }
        }
    }
    
    // Load pending deposits
    fun loadPendingDeposits() {
        viewModelScope.launch {
            val userId = walletRepository.getCurrentUserId()
            
            walletRepository.getPendingDeposits(userId)
                .collect { deposits ->
                    _uiState.update { it.copy(pendingDeposits = deposits) }
                }
        }
    }
}
```

**Used By:**
- `WalletScreen`
- `ManualUpiDepositScreen`

---

### 2.2 TournamentViewModel.kt

**Location:** `app/src/main/java/com/cehpoint/netwin/presentation/viewmodels/TournamentViewModel.kt`

**Purpose:** Manage tournament-related state and operations

**UI State:**
```kotlin
data class TournamentUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    
    // Tournaments
    val tournaments: List<Tournament> = emptyList(),
    val selectedTournament: Tournament? = null,
    
    // Registrations
    val userRegistrations: List<TournamentRegistration> = emptyList(),
    val registrationInProgress: Boolean = false,
    val registrationSuccess: Boolean = false,
    val registrationError: String? = null,
    
    // Filters
    val filterStatus: String? = null,
    val filterGame: String? = null
)
```

**Key Functions:**
```kotlin
class TournamentViewModel : ViewModel() {
    
    // Load all tournaments
    fun loadTournaments() {
        viewModelScope.launch {
            tournamentRepository.getTournaments()
                .collect { tournaments ->
                    _uiState.update { it.copy(tournaments = tournaments) }
                }
        }
    }
    
    // Load tournament details
    fun loadTournamentDetails(tournamentId: String) {
        viewModelScope.launch {
            val tournament = tournamentRepository.getTournamentById(tournamentId)
            _uiState.update { it.copy(selectedTournament = tournament) }
        }
    }
    
    // Register for tournament
    fun registerForTournament(registration: TournamentRegistration) {
        viewModelScope.launch {
            _uiState.update { it.copy(registrationInProgress = true) }
            
            tournamentRepository.registerForTournament(registration)
                .onSuccess { registrationId ->
                    _uiState.update {
                        it.copy(
                            registrationInProgress = false,
                            registrationSuccess = true
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            registrationInProgress = false,
                            registrationError = error.message
                        )
                    }
                }
        }
    }
    
    // Load user's registered tournaments
    fun loadUserRegistrations() {
        viewModelScope.launch {
            val userId = authRepository.getCurrentUserId()
            
            tournamentRepository.getUserRegistrations(userId)
                .collect { registrations ->
                    _uiState.update { it.copy(userRegistrations = registrations) }
                }
        }
    }
}
```

**Used By:**
- `TournamentsScreen`
- `TournamentDetailsScreen`
- `RegistrationFlowScreen`
- `MyTournamentsScreen`
- `VictoryPassScreen`

---

## 3. Screens

### 3.1 WalletScreen.kt

**Purpose:** Display wallet balance, transactions, and quick actions

**Key Components:**
- Balance cards (Withdrawable, Bonus, Total)
- Quick action buttons (Add Cash, Withdraw)
- Pending deposits list
- Transaction history

**Color Theme:**
- Gradient theme: Purple → Pink → Cyan
- Green "Add Cash" button
- Red "Withdraw" button
- Yellow pending status indicator

**Layout:**
```kotlin
@Composable
fun WalletScreen(viewModel: WalletViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    
    Column {
        // Header with total balance
        TotalBalanceHeader(balance = uiState.balance)
        
        // Balance breakdown cards
        BalanceCardsRow(
            withdrawable = uiState.withdrawableBalance,
            bonus = uiState.bonusBalance
        )
        
        // Quick actions
        QuickActionsRow(
            onAddCash = { /* Navigate to deposit */ },
            onWithdraw = { /* Navigate to withdrawal */ },
            canWithdraw = uiState.kycStatus == "verified"
        )
        
        // Pending deposits
        if (uiState.pendingDeposits.isNotEmpty()) {
            PendingDepositsSection(deposits = uiState.pendingDeposits)
        }
        
        // Transaction history
        TransactionHistorySection(transactions = uiState.transactions)
    }
}
```

---

### 3.2 ManualUpiDepositScreen.kt

**Purpose:** Manual UPI deposit submission

**Key Components:**
- UPI ID display with QR code
- Amount input (quick select + custom)
- UPI Transaction ID input
- Screenshot upload
- Submit button

**Data Flow:**
```kotlin
@Composable
fun ManualUpiDepositScreen(
    viewModel: WalletViewModel = hiltViewModel(),
    onSuccess: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val upiSettings = uiState.upiSettings
    
    var selectedAmount by remember { mutableStateOf(0.0) }
    var customAmount by remember { mutableStateOf("") }
    var upiTransactionId by remember { mutableStateOf("") }
    var screenshotUri by remember { mutableStateOf<Uri?>(null) }
    
    Column {
        // Step 1: Display UPI ID and QR code
        UpiIdSection(
            upiId = upiSettings?.upiId ?: "",
            merchantName = upiSettings?.displayName ?: ""
        )
        
        // Step 2: Amount selection
        AmountSelectionSection(
            selectedAmount = selectedAmount,
            customAmount = customAmount,
            onAmountSelected = { selectedAmount = it },
            onCustomAmountChanged = { customAmount = it }
        )
        
        // Step 3: Transaction ID input
        UpiTransactionIdInput(
            value = upiTransactionId,
            onValueChange = { upiTransactionId = it }
        )
        
        // Step 4: Screenshot upload
        ScreenshotUploadSection(
            screenshotUri = screenshotUri,
            onScreenshotSelected = { screenshotUri = it }
        )
        
        // Submit button
        Button(
            onClick = {
                val finalAmount = if (customAmount.isNotEmpty()) {
                    customAmount.toDoubleOrNull() ?: 0.0
                } else {
                    selectedAmount
                }
                
                val deposit = ManualUpiDeposit(
                    amount = finalAmount,
                    upiTransactionId = upiTransactionId,
                    screenshotUri = screenshotUri.toString(),
                    netwinUpiId = upiSettings?.upiId ?: "",
                    merchantDisplayName = upiSettings?.displayName ?: ""
                )
                
                viewModel.submitManualDeposit(deposit)
            },
            enabled = canSubmit()
        ) {
            Text("Submit Deposit")
        }
    }
    
    // Handle success
    LaunchedEffect(uiState.depositSubmitted) {
        if (uiState.depositSubmitted) {
            onSuccess()
        }
    }
}
```

---

### 3.3 TournamentsScreen.kt

**Purpose:** Browse and discover tournaments

**Key Components:**
- Filter chips (All, Upcoming, Live, Completed)
- Tournament cards grid
- Search functionality
- Pull-to-refresh

**Layout:**
```kotlin
@Composable
fun TournamentsScreen(
    viewModel: TournamentViewModel = hiltViewModel(),
    onTournamentClick: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Column {
        // Search bar
        SearchBar(
            onSearch = { query -> viewModel.searchTournaments(query) }
        )
        
        // Filter chips
        FilterChipsRow(
            selectedStatus = uiState.filterStatus,
            onStatusSelected = { viewModel.filterByStatus(it) }
        )
        
        // Tournament grid
        LazyColumn {
            items(uiState.tournaments) { tournament ->
                TournamentCard(
                    tournament = tournament,
                    onClick = { onTournamentClick(tournament.id) }
                )
            }
        }
    }
}
```

---

### 3.4 RegistrationFlowScreen.kt

**Purpose:** Multi-step tournament registration

**Steps:**
1. Game information (in-game name, ID)
2. Team details (if squad mode)
3. Contact information
4. Review and confirm

**Color Theme Updates Needed:**
- Replace solid cyan with gradient
- Gradient step indicator
- Gradient form field borders
- Gradient submit button

**Current Issues (from memory):**
- Heavy use of Color.Cyan
- No gradient elements
- Radio buttons use cyan selection

**Target Design:**
```kotlin
@Composable
fun RegistrationFlowScreen(
    tournamentId: String,
    viewModel: TournamentViewModel = hiltViewModel()
) {
    var currentStep by remember { mutableStateOf(1) }
    
    Column {
        // Step indicator with gradient
        GradientStepIndicator(
            currentStep = currentStep,
            totalSteps = 4
        )
        
        when (currentStep) {
            1 -> Step1GameInfo(
                onNext = { data -> 
                    // Save data
                    currentStep = 2
                }
            )
            2 -> Step2TeamDetails(
                onNext = { data ->
                    currentStep = 3
                },
                onBack = { currentStep = 1 }
            )
            3 -> Step3ContactInfo(
                onNext = { data ->
                    currentStep = 4
                },
                onBack = { currentStep = 2 }
            )
            4 -> Step4ReviewConfirm(
                onSubmit = {
                    viewModel.registerForTournament(registration)
                },
                onBack = { currentStep = 3 }
            )
        }
    }
}
```

---

### 3.5 VictoryPassScreen.kt

**Purpose:** Display room ID and password after successful registration

**Key Components:**
- Tournament banner
- Room ID with copy button
- Room password with copy button
- Match timing
- Important instructions
- Navigate to My Tournaments button

**Current State:**
- Partially gradient aligned
- Uses gradient for header
- Still has solid cyan buttons

**Target:**
```kotlin
@Composable
fun VictoryPassScreen(
    registrationId: String,
    viewModel: TournamentViewModel = hiltViewModel()
) {
    val registration = viewModel.getRegistration(registrationId)
    val tournament = viewModel.getTournamentById(registration.tournamentId)
    
    Column {
        // Header with gradient background
        GradientHeader {
            Text("Victory Pass", style = MaterialTheme.typography.h4)
            Icon(Icons.Default.CheckCircle, tint = Color.Green)
        }
        
        // Tournament info card
        TournamentInfoCard(tournament = tournament)
        
        // Room credentials
        Card(modifier = Modifier.gradientBorder()) {
            Column {
                // Room ID
                CredentialRow(
                    label = "Room ID",
                    value = tournament.roomId ?: "",
                    onCopy = { /* Copy to clipboard */ }
                )
                
                // Room Password
                CredentialRow(
                    label = "Room Password",
                    value = tournament.roomPassword ?: "",
                    onCopy = { /* Copy to clipboard */ }
                )
            }
        }
        
        // Match timing
        MatchTimingCard(tournament = tournament)
        
        // Instructions
        InstructionsCard()
        
        // Navigate to My Tournaments
        GradientButton(
            onClick = { /* Navigate */ },
            text = "View My Tournaments"
        )
    }
}
```

---

### 3.6 MyTournamentsScreen.kt

**Purpose:** Display user's registered tournaments

**Key Components:**
- Registered tournament cards
- Status indicators (Upcoming, Live, Completed)
- Victory Pass access
- Tournament details
- Empty state

**Layout:**
```kotlin
@Composable
fun MyTournamentsScreen(
    viewModel: TournamentViewModel = hiltViewModel(),
    onTournamentClick: (String) -> Unit,
    onVictoryPassClick: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    
    if (uiState.userRegistrations.isEmpty()) {
        EmptyState(
            message = "No tournaments yet",
            actionText = "Browse Tournaments",
            onAction = { /* Navigate to tournaments */ }
        )
    } else {
        LazyColumn {
            items(uiState.userRegistrations) { registration ->
                RegisteredTournamentCard(
                    registration = registration,
                    tournament = viewModel.getTournamentById(registration.tournamentId),
                    onCardClick = { onTournamentClick(registration.tournamentId) },
                    onVictoryPassClick = { onVictoryPassClick(registration.id) }
                )
            }
        }
    }
}
```

---

### 3.7 KycScreen.kt

**Purpose:** KYC document submission

**Current Issues:**
- Basic form design
- Doesn't match gradient theme
- No visual polish

**Target Updates:**
- Gradient borders on upload cards
- Gradient submit button
- Document type selector with gradient
- Progress indicator with gradient

**Indian KYC Documents:**
- Aadhaar Card
- PAN Card
- Passport

**Nigerian KYC Documents (Research needed):**
- NIN (National Identity Number)
- Driver's License
- Voter's Card
- International Passport

---

### 3.8 ProfileScreen.kt

**Purpose:** User profile display and editing

**Current Issues:**
- Basic layout
- Needs gradient theme alignment

**Target:**
- User avatar with gradient border
- Stat cards with gradient accents
- Edit button with gradient background
- Match web app profile design

---

### 3.9 MoreScreen.kt

**Purpose:** Additional app features and settings

**Current State:**
- List-based layout
- Basic styling

**Required Actions:**
1. Analysis of current features
2. Determine what to keep/remove/update
3. Apply gradient theme
4. Modern card-based layout

**Potential Features:**
- Settings
- Help & Support
- Terms & Conditions
- Privacy Policy
- About Us
- Logout

---

## 4. Reusable Components

### 4.1 GradientButton.kt

**Purpose:** Consistent gradient button across app

```kotlin
@Composable
fun GradientButton(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF6C3AFF),  // Purple
                        Color(0xFFFF3A8C),  // Pink
                        Color(0xFF3AFFDC)   // Cyan
                    )
                )
            ),
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent
        )
    ) {
        Text(text, color = Color.White)
    }
}
```

### 4.2 TournamentCard.kt

**Purpose:** Display tournament information in card format

```kotlin
@Composable
fun TournamentCard(
    tournament: Tournament,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .gradientBorder()
            .clickable(onClick = onClick)
    ) {
        Column {
            // Banner image
            AsyncImage(
                model = tournament.bannerImageUrl,
                contentDescription = null
            )
            
            // Tournament info
            Column(modifier = Modifier.padding(16.dp)) {
                Text(tournament.title, style = MaterialTheme.typography.h6)
                Text(tournament.game, style = MaterialTheme.typography.body2)
                
                Row {
                    StatusBadge(status = tournament.status)
                    PrizePoolChip(amount = tournament.prizePool)
                }
                
                Row {
                    Icon(Icons.Default.People)
                    Text("${tournament.currentParticipants}/${tournament.maxParticipants}")
                }
            }
        }
    }
}
```

---

## 5. Theme & Colors

### 5.1 Current Issue

**From Theme.kt:**
```kotlin
// Current - Wrong!
val Primary = Color(0xFF00BCD4)  // Solid cyan
```

**Target:**
```kotlin
// NetWin gradient colors
val NetWinPurple = Color(0xFF6C3AFF)
val NetWinPink = Color(0xFFFF3A8C)
val NetWinCyan = Color(0xFF3AFFDC)

// Background colors
val NetWinBackground = Color(0xFF121212)
val NetWinCardBackground = Color(0xFF1E1E2F)

// Status colors
val StatusUpcoming = Color(0xFFFFC107)  // Yellow
val StatusLive = Color(0xFFF44336)      // Red
val StatusCompleted = Color(0xFF4CAF50) // Green

// Action colors
val SuccessGreen = Color(0xFF4CAF50)
val ErrorRed = Color(0xFFF44336)

// Gradient brush
val netwinGradient = Brush.linearGradient(
    colors = listOf(NetWinPurple, NetWinPink, NetWinCyan)
)
```

---

## 6. Navigation Integration

**State Management:**
```kotlin
@Composable
fun WalletScreen(
    viewModel: WalletViewModel = hiltViewModel(),
    navController: NavController
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // Navigate on success
    LaunchedEffect(uiState.depositSubmitted) {
        if (uiState.depositSubmitted) {
            navController.navigate(NavigationRoutes.WalletScreen) {
                popUpTo(NavigationRoutes.ManualUpiDepositScreen) { inclusive = true }
            }
        }
    }
}
```

---

## Summary

**Presentation Layer Responsibilities:**
- ✅ Display UI
- ✅ Handle user interactions
- ✅ Observe ViewModel state
- ✅ Navigate between screens
- ✅ Apply theming and styling
- ❌ NO business logic
- ❌ NO direct Firebase calls
- ❌ NO data manipulation
