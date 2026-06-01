package com.example.ui.screens

import android.Manifest
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.SettingsManager
import com.example.ui.ExpenseViewModel
import com.example.ui.ScanUiState
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.io.File

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CameraScannerScreen(
    viewModel: ExpenseViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scanState by viewModel.scanUiState.collectAsStateWithLifecycle()

    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    // Camera capture tools
    val previewView = remember { PreviewView(context) }
    val imageCapture = remember { ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build() }
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    // local captured image display fallback
    var localBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Launcher to select from gallery
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri).use { stream ->
                    val bitmap = BitmapFactory.decodeStream(stream)
                    if (bitmap != null) {
                        localBitmap = bitmap
                        viewModel.parseReceipt(bitmap)
                    }
                }
            } catch (e: Exception) {
                Log.e("ScannerScreen", "Failed to load image from gallery", e)
            }
        }
    }

    // Bind current camera provider
    LaunchedEffect(cameraPermissionState.status.isGranted) {
        if (cameraPermissionState.status.isGranted) {
            cameraProviderFuture.addListener({
                try {
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageCapture
                    )
                } catch (e: Exception) {
                    Log.e("ScannerScreen", "Camera setup or binding failed", e)
                }
            }, ContextCompat.getMainExecutor(context))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Smart Receipt Scanner", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.resetScanState()
                        onNavigateBack()
                    }, modifier = Modifier.testTag("scanner_back_button")) {
                        Icon(imageVector = Icons.AutoMirrored.Default.ArrowBack, contentDescription = "Go Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black, titleContentColor = Color.White, navigationIconContentColor = Color.White)
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(innerPadding)
        ) {
            if (cameraPermissionState.status.isGranted) {
                // Display Live Camera Viewfinder
                AndroidView(
                    factory = { previewView },
                    modifier = Modifier.fillMaxSize()
                )

                // High Contrast Viewfinder Overlay grid (Simulated scanner outline)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.6f)
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                    )
                }
            } else {
                // Permission request explanation panel
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Camera Permission Required",
                        tint = Color.LightGray,
                        modifier = Modifier.size(72.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Camera Permission Needed",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "We require camera permission to live-shoot and OCR receipt bills automatically via Google Gemini AI intelligence.",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { cameraPermissionState.launchPermissionRequest() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Grant Permission")
                    }
                }
            }

            // Bottom control row (Capture + Gallery picking)
            if (scanState is ScanUiState.Idle) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(bottom = 48.dp, start = 16.dp, end = 16.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Gallery Launcher Button
                    IconButton(
                        onClick = { galleryLauncher.launch("image/*") },
                        modifier = Modifier
                            .size(56.dp)
                            .background(Color.DarkGray, CircleShape)
                            .testTag("gallery_picker_button")
                    ) {
                        Icon(imageVector = Icons.Default.PhotoLibrary, contentDescription = "Load Gallery Image", tint = Color.White)
                    }

                    // Native Snapping Capture Button (If permission granted)
                    if (cameraPermissionState.status.isGranted) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .background(Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            IconButton(
                                onClick = {
                                    val localFile = File(context.cacheDir, "captured_receipt.jpg")
                                    val options = ImageCapture.OutputFileOptions.Builder(localFile).build()

                                    imageCapture.takePicture(
                                        options,
                                        ContextCompat.getMainExecutor(context),
                                        object : ImageCapture.OnImageSavedCallback {
                                            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                                val capturedBitmap = BitmapFactory.decodeFile(localFile.absolutePath)
                                                if (capturedBitmap != null) {
                                                    localBitmap = capturedBitmap
                                                    viewModel.parseReceipt(capturedBitmap)
                                                }
                                            }
                                            override fun onError(exception: ImageCaptureException) {
                                                Log.e("ScannerScreen", "Camera capture error", exception)
                                            }
                                        }
                                    )
                                },
                                modifier = Modifier
                                    .size(72.dp)
                                    .background(Color.Black, CircleShape)
                                    .testTag("snap_capture_button")
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .background(Color.White, CircleShape)
                                )
                            }
                        }
                    }
                }
            }

            // High impact Overlays (Shimmering OCR load, success fill verification sheet)
            AnimatedVisibility(
                visible = scanState !is ScanUiState.Idle,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.85f)
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                        .padding(24.dp)
                ) {
                    when (val current = scanState) {
                        is ScanUiState.Loading -> {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(64.dp))
                                Spacer(modifier = Modifier.height(18.dp))
                                Text(
                                    "Processing Receipt via Gemini AI OCR...",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    "Extracting merchant details, total amounts, date, and suggested categories.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        is ScanUiState.Success -> {
                            var confirmAmount by remember { mutableStateOf(current.parsed.amount.toString()) }
                            var confirmCategory by remember { mutableStateOf(current.parsed.category) }
                            var confirmCurrency by remember { mutableStateOf(current.parsed.currency) }
                            var confirmNotes by remember { mutableStateOf(current.parsed.notes) }

                            var catExpanded by remember { mutableStateOf(false) }
                            var curExpanded by remember { mutableStateOf(false) }

                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text(
                                    "Is this information correct?",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    "Gemini parsed details successfully. Feel free to revise anything before saving.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Divider()

                                // Amount
                                OutlinedTextField(
                                    value = confirmAmount,
                                    onValueChange = { confirmAmount = it },
                                    label = { Text("Extracted Amount") },
                                    modifier = Modifier.fillMaxWidth().testTag("scanned_amount_input"),
                                    shape = RoundedCornerShape(12.dp)
                                )

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    // Currency Selection
                                    Box(modifier = Modifier.weight(1f)) {
                                        Button(
                                            onClick = { curExpanded = true },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Text(confirmCurrency)
                                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                        }
                                        DropdownMenu(expanded = curExpanded, onDismissRequest = { curExpanded = false }) {
                                            DropdownMenuItem(text = { Text("USD ($)") }, onClick = { confirmCurrency = "USD"; curExpanded = false })
                                            DropdownMenuItem(text = { Text("INR (₹)") }, onClick = { confirmCurrency = "INR"; curExpanded = false })
                                        }
                                    }

                                    // Category Selection
                                    Box(modifier = Modifier.weight(1f)) {
                                        Button(
                                            onClick = { catExpanded = true },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Text(confirmCategory)
                                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                        }
                                        DropdownMenu(expanded = catExpanded, onDismissRequest = { catExpanded = false }) {
                                            val list = listOf("Food", "Travel", "Bills", "Shopping", "Entertainment", "Others")
                                            list.forEach { cat ->
                                                DropdownMenuItem(text = { Text(cat) }, onClick = { confirmCategory = cat; catExpanded = false })
                                            }
                                        }
                                    }
                                }

                                // Notes / Merchant
                                OutlinedTextField(
                                    value = confirmNotes,
                                    onValueChange = { confirmNotes = it },
                                    label = { Text("Extracted Notes / Merchant") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                )

                                Spacer(modifier = Modifier.weight(1f))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    TextButton(
                                        onClick = { viewModel.resetScanState() },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Re-scan")
                                    }

                                    Button(
                                        onClick = {
                                            val doubleAmt = confirmAmount.toDoubleOrNull() ?: 0.0
                                            viewModel.insertExpense(
                                                amount = doubleAmt,
                                                currency = confirmCurrency,
                                                category = confirmCategory,
                                                date = System.currentTimeMillis(),
                                                notes = confirmNotes.trim().ifEmpty { null }
                                            )
                                            viewModel.resetScanState()
                                            onNavigateBack()
                                        },
                                        enabled = confirmAmount.toDoubleOrNull() != null,
                                        modifier = Modifier.weight(1f).testTag("scanned_confirm_save_button"),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Save Record")
                                    }
                                }
                            }
                        }

                        is ScanUiState.Error -> {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(imageVector = Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(56.dp))
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(current.message, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurface)
                                Spacer(modifier = Modifier.height(24.dp))
                                Button(
                                    onClick = { viewModel.resetScanState() },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Dismiss & Retry")
                                }
                            }
                        }
                        else -> {}
                    }
                }
            }
        }
    }
}
