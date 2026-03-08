package com.cehpoint.netwin.presentation.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.rememberAsyncImagePainter
import com.cehpoint.netwin.data.model.DocumentType
import com.cehpoint.netwin.data.model.KycDocument
import com.cehpoint.netwin.presentation.viewmodels.KycViewModel
import com.cehpoint.netwin.domain.repository.KycImageType
import com.google.firebase.auth.FirebaseAuth
import com.cehpoint.netwin.presentation.components.StatusChip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.navigation.NavController
import com.cehpoint.netwin.presentation.theme.NetwinTokens
import com.cehpoint.netwin.presentation.components.buttons.GradientPrimaryButton
import android.os.Environment
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.ui.draw.shadow
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KycScreen(
    navController: NavController,
    kycViewModel: KycViewModel = hiltViewModel()
) {
    val uiState by kycViewModel.uiState.collectAsState()
    val context = LocalContext.current
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    // State for form fields
    var documentType by remember { mutableStateOf(DocumentType.PAN) }
    var documentNumber by remember { mutableStateOf("") }
    var documentNumberError by remember { mutableStateOf<String?>(null) }
    var imageError by remember { mutableStateOf<String?>(null) }
    var showSuccess by remember { mutableStateOf(false) }
    val supportedCountries = listOf("IN", "NG") // Add more as needed
    val countryNames = mapOf("IN" to "India", "NG" to "Nigeria")
    var countryDropdownExpanded by remember { mutableStateOf(false) }
    var country by remember { mutableStateOf("") }

    // Image pickers
    val frontImageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { kycViewModel.uploadImage(userId, KycImageType.FRONT, it) }
    }
    val backImageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { kycViewModel.uploadImage(userId, KycImageType.BACK, it) }
    }

    // Selfie picker: system camera intent or gallery
    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }
//    val selfieCameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
//        if (success && cameraImageUri != null) {
//            kycViewModel.uploadImage(userId, KycImageType.SELFIE, cameraImageUri!!)
//        }
//    }
//    val selfieGalleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
//        uri?.let { kycViewModel.uploadImage(userId, KycImageType.SELFIE, it) }
//    }
    var showSelfieDialog by remember { mutableStateOf(false) }
    fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
    }

    val selfieCameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && cameraImageUri != null) {
            kycViewModel.uploadImage(userId, KycImageType.SELFIE, cameraImageUri!!)
        }
    }
    val selfieGalleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { kycViewModel.uploadImage(userId, KycImageType.SELFIE, it) }
    }

    // Camera permission launcher
    val cameraPermission = android.Manifest.permission.CAMERA
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            val photoFile = createImageFile()
            val photoUri = FileProvider.getUriForFile(
                context,
                context.packageName + ".provider",
                photoFile
            )
            cameraImageUri = photoUri
            selfieCameraLauncher.launch(photoUri)
            showSelfieDialog = false
        } else {
            Toast.makeText(context, "Camera permission is required", Toast.LENGTH_SHORT).show()
        }
    }
//    fun createImageFile(): File {
//        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
//        val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
//        return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
//    }
//    var showSelfieDialog by remember { mutableStateOf(false) }

    // Observe KYC status
    LaunchedEffect(userId) {
        if (userId.isNotEmpty()) kycViewModel.observeKyc(userId)
    }

    // Validation logic
    fun validate(): Boolean {
        documentNumberError = null
        imageError = null
        if (documentType == DocumentType.PAN) {
            if (documentNumber.length != 10) {
                documentNumberError = "PAN must be 10 characters."
                return false
            }
        } else if (documentType == DocumentType.AADHAR) {
            if (documentNumber.length != 12 || documentNumber.any { !it.isDigit() }) {
                documentNumberError = "Aadhaar must be 12 digits."
                return false
            }
        }
        if (uiState.frontImageUrl.isBlank()) {
            imageError = "Front image is required."
            return false
        }
        if (documentType == DocumentType.AADHAR && uiState.backImageUrl.isBlank()) {
            imageError = "Back image is required for Aadhaar."
            return false
        }
        if (uiState.selfieUrl.isBlank()) {
            imageError = "Selfie is required."
            return false
        }
        if (country.isBlank()) {
            imageError = "Country is required."
            return false
        }
        return true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                title = { Text("KYC Verification", color = Color.White, style = MaterialTheme.typography.headlineSmall) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xCC181A20)),
                modifier = Modifier.shadow(4.dp)
            )
        },
        containerColor = Color(0xFF121212)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF121212))
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("KYC Verification", style = MaterialTheme.typography.titleLarge, color = Color.White)
            Spacer(Modifier.height(16.dp))

            // KYC Status
            uiState.kycDocument?.let { kycDoc ->
                StatusChip(kycDoc.status.name)
                Spacer(Modifier.height(8.dp))
            }

            // Document Type Dropdown
            var expanded by remember { mutableStateOf(false) }
            Box {
                OutlinedButton(onClick = { expanded = true }) {
                    Text(documentType.name)
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    DocumentType.values().forEach { type ->
                        // Only show Aadhaar if country is India
                        if (type == DocumentType.AADHAR && country.uppercase() != "IN") return@forEach
                        DropdownMenuItem(
                            text = { Text(type.name) },
                            onClick = {
                                documentType = type
                                expanded = false
                            }
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))

            // Document Number
            OutlinedTextField(
                value = documentNumber,
                onValueChange = { documentNumber = it; documentNumberError = null },
                label = { Text("Document Number") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                isError = documentNumberError != null,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = NetwinTokens.Accent,
                    unfocusedBorderColor = Color.Gray,
                    cursorColor = NetwinTokens.Accent
                )
            )
            if (documentNumberError != null) {
                Text(documentNumberError ?: "", color = NetwinTokens.ErrorRed, fontSize = MaterialTheme.typography.bodySmall.fontSize)
            }
            Spacer(Modifier.height(16.dp))

            // Country
            ExposedDropdownMenuBox(
                expanded = countryDropdownExpanded,
                onExpandedChange = { countryDropdownExpanded = !countryDropdownExpanded }
            ) {
                OutlinedTextField(
                    value = countryNames[country] ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Country", color = Color.White) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = countryDropdownExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = NetwinTokens.Accent,
                        unfocusedBorderColor = Color.Gray,
                        cursorColor = NetwinTokens.Accent
                    )
                )
                ExposedDropdownMenu(
                    expanded = countryDropdownExpanded,
                    onDismissRequest = { countryDropdownExpanded = false }
                ) {
                    supportedCountries.forEach { code ->
                        DropdownMenuItem(
                            text = { Text(countryNames[code] ?: code) },
                            onClick = {
                                country = code
                                countryDropdownExpanded = false
                            }
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))

            // Image Pickers
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                ImagePickerButton(
                    label = "Front Image",
                    imageUrl = uiState.frontImageUrl,
                    onClick = { frontImageLauncher.launch("image/*") },
                    helperText = "JPG, PNG"
                )
                Spacer(Modifier.width(8.dp))
                ImagePickerButton(
                    label = "Back Image",
                    imageUrl = uiState.backImageUrl,
                    onClick = { backImageLauncher.launch("image/*") },
                    helperText = if (documentType == DocumentType.AADHAR) "Required" else "Optional"
                )
                Spacer(Modifier.width(8.dp))
                ImagePickerButton(
                    label = "Selfie",
                    imageUrl = uiState.selfieUrl,
                    onClick = { showSelfieDialog = true },
                    helperText = "Face must be visible"
                )
            }
            if (imageError != null) {
                Spacer(Modifier.height(4.dp))
                Text(imageError ?: "", color = NetwinTokens.ErrorRed, fontSize = MaterialTheme.typography.bodySmall.fontSize)
            }
            Spacer(Modifier.height(24.dp))

            // Submit Button with Gradient
            GradientPrimaryButton(
                text = if (uiState.isLoading) "Submitting..." else "Submit KYC",
                enabled = !uiState.isLoading,
                onClick = {
                    if (userId.isEmpty()) {
                        Toast.makeText(context, "User not logged in", Toast.LENGTH_SHORT).show()
                        return@GradientPrimaryButton
                    }
                    if (!validate()) return@GradientPrimaryButton
                    val kycDoc = KycDocument(
                        userId = userId,
                        documentType = documentType,
                        documentNumber = documentNumber,
                        frontImageUrl = uiState.frontImageUrl,
                        backImageUrl = uiState.backImageUrl,
                        selfieUrl = uiState.selfieUrl
                    )
                    kycViewModel.submitKyc(kycDoc)
                    showSuccess = true
                }
            )
            
            // Loading indicator
            if (uiState.isLoading) {
                Spacer(Modifier.height(8.dp))
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = NetwinTokens.Accent, modifier = Modifier.size(20.dp))
                }
            }
            if (showSuccess && uiState.error == null && !uiState.isLoading) {
                Spacer(Modifier.height(8.dp))
                Text("KYC submitted successfully!", color = NetwinTokens.SuccessGreen)
            }
            // Error
            uiState.error?.let {
                Spacer(Modifier.height(8.dp))
                Text("Error: $it", color = NetwinTokens.ErrorRed)
            }

            // Clear form fields after successful submission
            LaunchedEffect(showSuccess, uiState.error, uiState.isLoading) {
                if (showSuccess && uiState.error == null && !uiState.isLoading) {
                    documentType = DocumentType.PAN
                    documentNumber = ""
                    documentNumberError = null
                    imageError = null
                    kycViewModel.resetImages()
                    // Optionally, you can set showSuccess = false after a delay
                    // delay(2000)
                    // showSuccess = false
                }
            }
        }
    }

    // Selfie picker dialog
    if (showSelfieDialog) {
        AlertDialog(
            onDismissRequest = { showSelfieDialog = false },
            title = { Text("Select Selfie Source") },
            text = {
                Column {
                    Button(onClick = {
                        // Check and request camera permission
                        if (ContextCompat.checkSelfPermission(context, cameraPermission) == PackageManager.PERMISSION_GRANTED) {
                            val photoFile = createImageFile()
                            val photoUri = FileProvider.getUriForFile(
                                context,
                                context.packageName + ".provider",
                                photoFile
                            )
                            cameraImageUri = photoUri
                            selfieCameraLauncher.launch(photoUri)
                            showSelfieDialog = false
                        } else {
                            cameraPermissionLauncher.launch(cameraPermission)
                        }
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text("Take Photo")
                    }
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = {
                        selfieGalleryLauncher.launch("image/*")
                        showSelfieDialog = false
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text("Choose from Gallery")
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showSelfieDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun ImagePickerButton(label: String, imageUrl: String, onClick: () -> Unit, helperText: String = "") {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(Color.DarkGray, RoundedCornerShape(8.dp))
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            if (imageUrl.isNotBlank()) {
                Image(
                    painter = rememberAsyncImagePainter(imageUrl),
                    contentDescription = label,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(80.dp)
                )
            } else {
                Text(label, color = Color.White, modifier = Modifier.padding(8.dp))
            }
        }
        if (helperText.isNotBlank()) {
            Text(helperText, color = Color.Gray, fontSize = MaterialTheme.typography.bodySmall.fontSize)
        }
    }
} 