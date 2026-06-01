package com.example.ui.screens

import android.app.DatePickerDialog
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import com.example.ui.AlertMessage
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Expense
import com.example.data.SettingsManager
import com.example.data.User
import com.example.ui.ExpenseViewModel
import com.example.ui.components.CategoryColors
import com.example.ui.components.DonutPieChart
import com.example.ui.components.SimpleBarChart
import com.example.util.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: ExpenseViewModel,
    onNavigateToSettings: () -> Unit,
    onNavigateToScanner: () -> Unit
) {
    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val rawExpenses by viewModel.expenses.collectAsStateWithLifecycle()
    val displayCurrency by viewModel.displayCurrency.collectAsStateWithLifecycle()
    val monthlyBudget by viewModel.monthlyBudget.collectAsStateWithLifecycle()

    val totalThisMonth by viewModel.totalThisMonth.collectAsStateWithLifecycle()
    val totalThisYear by viewModel.totalThisYear.collectAsStateWithLifecycle()
    val categoryBreakdown by viewModel.categoryBreakdown.collectAsStateWithLifecycle()
    val trendData by viewModel.monthlyTrend.collectAsStateWithLifecycle()

    // 12 Core Premium states
    val activeTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val alertsInbox by viewModel.alerts.collectAsStateWithLifecycle()
    val nlpInputText by viewModel.nlpInput.collectAsStateWithLifecycle()
    val parsedNlExp by viewModel.parsedNlExpense.collectAsStateWithLifecycle()
    val subscriptionsList by viewModel.subscriptions.collectAsStateWithLifecycle()
    val detectedSubsList by viewModel.detectedSubscriptions.collectAsStateWithLifecycle()
    val isGmailConnected by viewModel.isGmailConnected.collectAsStateWithLifecycle()
    val isSmsScanEnabled by viewModel.isSmsScanEnabled.collectAsStateWithLifecycle()
    val showGmailScanningAnim by viewModel.showGmailScanningAnim.collectAsStateWithLifecycle()
    val reminderEnabledMap by viewModel.reminderEnabledMap.collectAsStateWithLifecycle()
    val spentPacingPrediction by viewModel.pacingPrediction.collectAsStateWithLifecycle()
    val financialHealthScoreData by viewModel.healthScore.collectAsStateWithLifecycle()
    val dailyHeatmapDays by viewModel.heatmapDays.collectAsStateWithLifecycle()
    val weeklyStoryWrappedData by viewModel.weeklyWrappedStory.collectAsStateWithLifecycle()
    val roommateBalancesList by viewModel.friendBalances.collectAsStateWithLifecycle()

    val syncedContacts by viewModel.syncedContacts.collectAsStateWithLifecycle()
    val sharedSubscriptionContacts by viewModel.sharedSubscriptionContacts.collectAsStateWithLifecycle()

    val currencySymbol = if (displayCurrency == "INR") "₹" else "$"

    var showAddDialog by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }
    var showNotificationInboxDrawer by remember { mutableStateOf(false) }
    var showActiveVoiceSheet by remember { mutableStateOf(false) }
    var showSplitBillDialog by remember { mutableStateOf(false) }
    var subTabSelected by remember { mutableStateOf(0) } // 0: Splits, 1: Subscriptions
    var showCustomAddDialog by remember { mutableStateOf(false) }

    // Selected expense for editing
    var expenseToEdit by remember { mutableStateOf<Expense?>(null) }

    // Native Speech recogniser launcher integration
    val voiceSpeechIntentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        showActiveVoiceSheet = false
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val spokenList = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val textSpoken = spokenList?.firstOrNull() ?: ""
            if (textSpoken.isNotEmpty()) {
                viewModel.nlpInput.value = textSpoken
                Toast.makeText(context, "Voice heard: \"$textSpoken\"", Toast.LENGTH_LONG).show()
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "SpendWise AI",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier.testTag("settings_button")
                    ) {
                        val initials = currentUser?.name?.take(2)?.uppercase() ?: "G"
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = initials,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                },
                actions = {
                    // Notifications bell with badge indicator
                    val alertCount = alertsInbox.size
                    IconButton(onClick = { showNotificationInboxDrawer = !showNotificationInboxDrawer }) {
                        BadgedBox(
                            badge = {
                                if (alertCount > 0) {
                                    Badge { Text(alertCount.toString()) }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (alertCount > 0) Icons.Default.NotificationsActive else Icons.Default.Notifications,
                                contentDescription = "Alerts Center"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            // Premium Fintech Tab bar Selection with animations
            TabRow(
                selectedTabIndex = activeTab,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.navigationBarsPadding()
            ) {
                Tab(
                    selected = activeTab == 0,
                    onClick = { viewModel.selectedTab.value = 0 },
                    icon = { Icon(imageVector = if (activeTab == 0) Icons.Filled.Home else Icons.Outlined.Home, contentDescription = "Home") },
                    text = { Text("Home", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = activeTab == 1,
                    onClick = { viewModel.selectedTab.value = 1 },
                    icon = { Icon(imageVector = if (activeTab == 1) Icons.Filled.Analytics else Icons.Outlined.Analytics, contentDescription = "Stats") },
                    text = { Text("Analytics", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = activeTab == 2,
                    onClick = { viewModel.selectedTab.value = 2 },
                    icon = { Icon(imageVector = if (activeTab == 2) Icons.Filled.Group else Icons.Outlined.Group, contentDescription = "Subs") },
                    text = { Text("Split & Recurring", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = activeTab == 3,
                    onClick = { viewModel.selectedTab.value = 3 },
                    icon = { Icon(imageVector = if (activeTab == 3) Icons.Filled.AutoAwesome else Icons.Outlined.Stars, contentDescription = "Story") },
                    text = { Text("Weekly Story", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
            }
        },
        floatingActionButton = {
            if (activeTab == 0) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Smart Scan Float Actions
                    SmallFloatingActionButton(
                        onClick = onNavigateToScanner,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.testTag("quick_scan_fab")
                    ) {
                        Icon(imageVector = Icons.Default.DocumentScanner, contentDescription = "Scan Bill Rec.")
                    }

                    // Traditional FAB Record add
                    FloatingActionButton(
                        onClick = { showAddDialog = true },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.testTag("add_expense_fab")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Manual Form Record")
                    }
                }
            } else if (activeTab == 2) {
                FloatingActionButton(
                    onClick = {
                        if (subTabSelected == 0) {
                            showSplitBillDialog = true
                        } else {
                            showCustomAddDialog = true
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("split_contacts_fab")
                ) {
                    val description = if (subTabSelected == 0) "Add Split Bill" else "Add Subscription"
                    Icon(imageVector = Icons.Default.Add, contentDescription = description)
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .pointerInput(activeTab) {
                    var totalDragX = 0f
                    detectHorizontalDragGestures(
                        onDragStart = {
                            totalDragX = 0f
                        },
                        onDragEnd = {
                            val threshold = 120f
                            if (totalDragX < -threshold) {
                                // Swiped left -> move right (next tab)
                                if (activeTab < 3) {
                                    viewModel.selectedTab.value = activeTab + 1
                                }
                            } else if (totalDragX > threshold) {
                                // Swiped right -> move left (previous tab)
                                if (activeTab > 0) {
                                    viewModel.selectedTab.value = activeTab - 1
                                }
                            }
                        },
                        onDragCancel = {
                            totalDragX = 0f
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            totalDragX += dragAmount
                        }
                    )
                }
        ) {
            // Main Tab View controller
            AnimatedContent(
                targetState = activeTab,
                transitionSpec = {
                    val duration = 150
                    val slideSpec = tween<androidx.compose.ui.unit.IntOffset>(durationMillis = duration, easing = FastOutSlowInEasing)
                    val fadeSpec = tween<Float>(durationMillis = duration, easing = FastOutSlowInEasing)
                    if (targetState > initialState) {
                        // Slide in from right, slide out to left
                        (slideInHorizontally(animationSpec = slideSpec) { width -> width } + fadeIn(animationSpec = fadeSpec))
                            .togetherWith(slideOutHorizontally(animationSpec = slideSpec) { width -> -width } + fadeOut(animationSpec = fadeSpec))
                    } else {
                        // Slide in from left, slide out to right
                        (slideInHorizontally(animationSpec = slideSpec) { width -> -width } + fadeIn(animationSpec = fadeSpec))
                            .togetherWith(slideOutHorizontally(animationSpec = slideSpec) { width -> width } + fadeOut(animationSpec = fadeSpec))
                    }
                },
                label = "tab_transitions"
            ) { targetTab ->
                when (targetTab) {
                    0 -> HomeDashboardTab(
                        viewModel = viewModel,
                        rawExpenses = rawExpenses,
                        currencySymbol = currencySymbol,
                        displayCurrency = displayCurrency,
                        totalThisMonth = totalThisMonth,
                        monthlyBudget = monthlyBudget,
                        showFilterSheet = showFilterSheet,
                        onToggleFilter = { showFilterSheet = !showFilterSheet },
                        dailyHeatmapDays = dailyHeatmapDays,
                        onEditRequested = { expenseToEdit = it }
                    )

                    1 -> AnalyticsTab(
                        trendData = trendData,
                        categoryBreakdown = categoryBreakdown,
                        currencySymbol = currencySymbol,
                        healthScoreData = financialHealthScoreData,
                        spentPacingPrediction = spentPacingPrediction
                    )

                    2 -> SubscriptionsAndSplitTab(
                        roommateBalancesList = roommateBalancesList,
                        subscriptionsList = subscriptionsList,
                        detectedSubsList = detectedSubsList,
                        reminderEnabledMap = reminderEnabledMap,
                        isGmailConnected = isGmailConnected,
                        isSmsScanEnabled = isSmsScanEnabled,
                        showGmailScanningAnim = showGmailScanningAnim,
                        currencySymbol = currencySymbol,
                        subTabSelected = subTabSelected,
                        onSubTabSelectedChange = { subTabSelected = it },
                        showCustomAddDialog = showCustomAddDialog,
                        onShowCustomAddDialogChange = { showCustomAddDialog = it },
                        onSettleFriend = { viewModel.settleBalance(it) },
                        onSplitClick = { showSplitBillDialog = true },
                        onConfirmSubscription = { viewModel.confirmSubscription(it) },
                        onIgnoreSubscription = { viewModel.ignoreSubscription(it) },
                        onDeleteSubscription = { viewModel.deleteConfirmedSubscription(it) },
                        onToggleReminder = { viewModel.toggleSubscriptionReminder(it) },
                        onConnectGmail = { viewModel.connectGmailAndScan() },
                        onToggleSmsScan = { viewModel.toggleSmsScanMode(it) }
                    )

                    3 -> PremiumWeeklyStoryWrappedTab(
                        storyData = weeklyStoryWrappedData,
                        currencySymbol = currencySymbol
                    )
                }
            }

            // High aesthetic overlay notifications Tray Center
            if (showNotificationInboxDrawer) {
                NotificationsInboxOverlay(
                    alertsList = alertsInbox,
                    onDismiss = { viewModel.dismissAlert(it) },
                    onClose = { showNotificationInboxDrawer = false }
                )
            }

            // Active Listening voice simulation mic ring
            if (showActiveVoiceSheet) {
                ActiveVoiceListeningSimulationOverlay(onDismiss = { showActiveVoiceSheet = false })
            }

            // Split bills dialog form controller
            if (showSplitBillDialog) {
                SplitExpenseDialog(
                    syncedContacts = syncedContacts,
                    onDismiss = { showSplitBillDialog = false },
                    onConfirm = { friendName, amount, reason ->
                        viewModel.addSplitBill(friendName, amount, reason)
                        showSplitBillDialog = false
                    }
                )
            }

            // Add manual transaction form controller
            if (showAddDialog) {
                AddEditExpenseDialog(
                    onDismiss = { showAddDialog = false },
                    onConfirm = { amount, currency, category, timestamp, notes ->
                        viewModel.insertExpense(amount, currency, category, timestamp, notes)
                        showAddDialog = false
                    },
                    currentCurrency = displayCurrency
                )
            }

            // Edit manual transaction form controller
            if (expenseToEdit != null) {
                val editItem = expenseToEdit!!
                AddEditExpenseDialog(
                    expense = editItem,
                    onDismiss = { expenseToEdit = null },
                    onConfirm = { amount, currency, category, timestamp, notes ->
                        viewModel.updateExpense(
                            editItem.copy(
                                amount = amount,
                                currency = currency,
                                category = category,
                                date = timestamp,
                                notes = notes
                            )
                        )
                        expenseToEdit = null
                    },
                    currentCurrency = displayCurrency
                )
            }
        }
    }
}

// ==========================================
// TAB 0: HOME COMPOSABLE SUB-COMPONENTS
// ==========================================
@Composable
fun HomeDashboardTab(
    viewModel: ExpenseViewModel,
    rawExpenses: List<Expense>,
    currencySymbol: String,
    displayCurrency: String,
    totalThisMonth: Double,
    monthlyBudget: Double,
    showFilterSheet: Boolean,
    onToggleFilter: () -> Unit,
    dailyHeatmapDays: List<HeatmapDay>,
    onEditRequested: (Expense) -> Unit
) {
    val filteredExpenses by viewModel.filteredExpenses.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("dashboard_scroll_list"),
        contentPadding = PaddingValues(bottom = 96.dp, start = 16.dp, end = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Budget Card header
        item {
            BudgetOverviewCard(
                totalSpend = totalThisMonth,
                budgetLimit = monthlyBudget,
                currencySymbol = currencySymbol
            )
        }

        // Horizontal GitHub-style continuous calendar Heatmap Row
        item {
            GitHubStyleHeatmapSection(days = dailyHeatmapDays)
        }

        // Filter expand toggle row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Transaction Ledger",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(
                    onClick = onToggleFilter,
                    modifier = Modifier.testTag("filter_toggle_button")
                ) {
                    Icon(
                        imageVector = if (showFilterSheet) Icons.Filled.FilterListOff else Icons.Filled.FilterList,
                        contentDescription = "Show filters",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Filter fields sheet visibility sliding panel
        item {
            AnimatedVisibility(
                visible = showFilterSheet,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                FilterSettingsPanel(viewModel = viewModel)
            }
        }

        // Expenses lists
        if (filteredExpenses.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = "Empty LEDGER",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No recorded transactions match active filters.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(filteredExpenses, key = { it.id }) { expense ->
                ExpenseCard(
                    expense = expense,
                    displayCurrency = displayCurrency,
                    onEdit = { onEditRequested(expense) },
                    onDelete = { viewModel.deleteExpense(expense) }
                )
            }
        }
    }
}

// GitHub Style Continuous Heatmap Grid row
@Composable
fun GitHubStyleHeatmapSection(days: List<HeatmapDay>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.GridOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Expense Heatmap Calendar",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Darker squares indicate higher outbound spending days this month.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Render GitHub grid boxes scrollable row
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(days) { day ->
                    var showTooltip by remember { mutableStateOf(false) }

                    val baseColor = MaterialTheme.colorScheme.primary
                    val cellColor = if (day.totalAmount == 0.0) {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    } else {
                        baseColor.copy(alpha = day.intensity)
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { showTooltip = !showTooltip }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(cellColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = day.dayOfMonth.toString(),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                fontWeight = FontWeight.Black,
                                color = if (day.totalAmount > 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        // Small tooltip display
                        AnimatedVisibility(visible = showTooltip) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 4.dp)
                                    .background(Color.Black, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "₹${String.format("%.0f", day.totalAmount)}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// TAB 1: ANALYTICS VIEW COMPOSABLE
// ==========================================
@Composable
fun AnalyticsTab(
    trendData: Map<String, Double>,
    categoryBreakdown: Map<String, Double>,
    currencySymbol: String,
    healthScoreData: Pair<Int, List<String>>,
    spentPacingPrediction: Pair<Double, String>
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Financial Health indicator score
        FinancialHealthDialCard(score = healthScoreData.first, recommendations = healthScoreData.second)

        // Spending Prediction Pacing
        PacingPredictionCard(predictedAmount = spentPacingPrediction.first, message = spentPacingPrediction.second, currencySymbol = currencySymbol)

        // Bar and Pie charts
        if (categoryBreakdown.isNotEmpty()) {
            DonutPieChart(data = categoryBreakdown, currencySymbol = currencySymbol)
        }
        if (trendData.isNotEmpty()) {
            SimpleBarChart(trendData = trendData, currencySymbol = currencySymbol)
        }
    }
}

@Composable
fun FinancialHealthDialCard(score: Int, recommendations: List<String>) {
    val dialColor = when {
        score >= 80 -> Color(0xFF4CAF50) // Green healthy
        score >= 50 -> Color(0xFFFF9800) // Orange Warning
        else -> MaterialTheme.colorScheme.error
    }

    val ratingText = when {
        score >= 80 -> "Excellent Health"
        score >= 50 -> "Moderate Health"
        else -> "Overspent Danger"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                "Financial Health Score 🎯",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold
            )
            Text("Evaluates budget compliance, savings pace, and microtransaction rates.", style = MaterialTheme.typography.labelSmall, color = Color.Gray)

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Circular scoring dials meter representation
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .background(dialColor.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        progress = { score.toFloat() / 100f },
                        modifier = Modifier.size(80.dp),
                        color = dialColor,
                        strokeWidth = 8.dp,
                    )
                    Text(
                        score.toString(),
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                        color = dialColor
                    )
                }

                // Metadata block description
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        ratingText,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = dialColor
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Your score increased by 12% following recent shopping diet rules.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Divider()
            Spacer(modifier = Modifier.height(12.dp))

            Text("Improvement Recommendations:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(6.dp))

            // Action lists recommendations bullets
            recommendations.forEach { r ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text("• ", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = dialColor)
                    Text(r, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun PacingPredictionCard(predictedAmount: Double, message: String, currencySymbol: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.Timeline, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Spending Prediction engine", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                "Estimated end-of-month spend:",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "$currencySymbol${String.format("%.2f", predictedAmount)}",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}


// ==========================================
// TAB 2: SUBSCRIPTION & SPLIT CO-COMPOSABLES
// ==========================================
@Composable
fun SubscriptionsAndSplitTab(
    roommateBalancesList: List<SplitBalance>,
    subscriptionsList: List<SubscriptionItem>,
    detectedSubsList: List<SubscriptionItem>,
    reminderEnabledMap: Map<String, Boolean>,
    isGmailConnected: Boolean,
    isSmsScanEnabled: Boolean,
    showGmailScanningAnim: Boolean,
    currencySymbol: String,
    subTabSelected: Int,
    onSubTabSelectedChange: (Int) -> Unit,
    showCustomAddDialog: Boolean,
    onShowCustomAddDialogChange: (Boolean) -> Unit,
    onSettleFriend: (String) -> Unit,
    onSplitClick: () -> Unit,
    onConfirmSubscription: (SubscriptionItem) -> Unit,
    onIgnoreSubscription: (String) -> Unit,
    onDeleteSubscription: (String) -> Unit,
    onToggleReminder: (String) -> Unit,
    onConnectGmail: () -> Unit,
    onToggleSmsScan: (Boolean) -> Unit
) {
    var editingSubItem by remember { mutableStateOf<SubscriptionItem?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // High fidelity Material 3 Tab indicator selectors row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                .padding(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSubTabSelectedChange(0) }
                    .background(
                        color = if (subTabSelected == 0) MaterialTheme.colorScheme.primary else Color.Transparent,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Roommate Balances 🤝",
                    fontWeight = FontWeight.Bold,
                    color = if (subTabSelected == 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSubTabSelectedChange(1) }
                    .background(
                        color = if (subTabSelected == 1) MaterialTheme.colorScheme.primary else Color.Transparent,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Subscriptions 💳",
                    fontWeight = FontWeight.Bold,
                    color = if (subTabSelected == 1) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        if (subTabSelected == 0) {
            // Section: Shared Splits & Roommate Balances
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("balances_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Shared Expenses & Splits 🤝",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Track active splits with your roommate circle in real-time.",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray
                            )
                        }
                        Button(
                            onClick = onSplitClick,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text("Add Split", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (roommateBalancesList.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No active roommate splits. All settled up! 🎉",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    } else {
                        roommateBalancesList.forEach { friend ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
                                        RoundedCornerShape(16.dp)
                                    )
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = friend.friendName.take(1).uppercase(),
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = friend.friendName,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = friend.reason.ifBlank { "Group Expense Split" },
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 9.sp,
                                        color = Color.Gray
                                    )
                                }

                                // Balance color coding: positive = you are owed, negative = you owe
                                val isOwed = friend.balance > 0.0
                                val isSettled = friend.balance == 0.0
                                val color = if (isSettled) Color.Gray 
                                            else if (isOwed) Color(0xFF4CAF50) 
                                            else MaterialTheme.colorScheme.error
                                val label = if (isSettled) "settled" 
                                            else if (isOwed) "owes you" 
                                            else "you owe"

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = if (!isSettled) "$currencySymbol${String.format("%.0f", Math.abs(friend.balance))}" else "settled",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.ExtraBold),
                                        color = color
                                    )
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 8.sp,
                                        color = Color.Gray
                                    )

                                    if (!isSettled) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "Settle",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier
                                                .clickable { onSettleFriend(friend.friendName) }
                                                .background(
                                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                                    RoundedCornerShape(6.dp)
                                                )
                                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // ===================================
            // SUBSCRIPTION SCREEN SUB-CONTENTS
            // ===================================

            // 1. Dashboard Subscription Analytics Panel
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Subscription Dashboard 📊",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val monthlyTotalSum = subscriptionsList.sumOf { it.cost }
                    val annualTotalSum = monthlyTotalSum * 12
                    val activeSubsCount = subscriptionsList.size
                    val topSubscription = subscriptionsList.maxByOrNull { it.cost }

                    Row(modifier = Modifier.fillMaxWidth()) {
                        // Monthly cost
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Monthly Cost", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            Text(
                                text = "$currencySymbol${String.format("%.0f", monthlyTotalSum)}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        // Annual cost
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Annual Cost", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            Text(
                                text = "$currencySymbol${String.format("%.0f", annualTotalSum)}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Active subscriptions", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            Text(
                                text = "$activeSubsCount Services",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Most Expensive", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            Text(
                                text = topSubscription?.let { "${it.name.take(15)} ($currencySymbol${String.format("%.0f", it.cost)})" } ?: "None",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            // 2. Email Receipt Analyzer & SMS Tracker (Dynamic Integrations panel)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Private Sync Integrations Sync Center 🧬",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Connect local sources to capture receipts and match text statements securely.",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Gmail Connection Widget
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = if (isGmailConnected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(14.dp)
                            )
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isGmailConnected) Icons.Default.CloudDone else Icons.Default.Email,
                            contentDescription = "Gmail integration",
                            tint = if (isGmailConnected) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isGmailConnected) "Gmail Connected & Synced" else "Link Google Account",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isGmailConnected) "Last receipt scan occurred 1 min ago. All cloud slips updated." 
                                       else "Secure read-only parse for entertainment and hosting slips.",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray
                            )
                        }
                        if (showGmailScanningAnim) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Text(
                                text = if (isGmailConnected) "Synced ✔" else "Connect",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isGmailConnected) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .clickable { if (!isGmailConnected) onConnectGmail() }
                                    .background(
                                        color = if (isGmailConnected) Color.Transparent else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // SMS Connection Widget (Optional permissions check description)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = if (isSmsScanEnabled) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(14.dp)
                            )
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sms,
                            contentDescription = "SMS extraction",
                            tint = if (isSmsScanEnabled) Color(0xFF4CAF50) else Color.Gray,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Scan SMS Transaction Texts",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Extracts bank billing SMS directly into database ledger securely. 100% on-device.",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray
                            )
                        }
                        Switch(
                            checked = isSmsScanEnabled,
                            onCheckedChange = { onToggleSmsScan(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF4CAF50)
                            )
                        )
                    }
                }
            }

            // 3. Smart Discoveries & Recurring Detections
            if (detectedSubsList.isNotEmpty()) {
                Text(
                    text = "Smart Discoveries & Detections 🤖",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                detectedSubsList.forEach { p ->
                    val emoji = when {
                        p.name.lowercase().contains("netflix") -> "🍿"
                        p.name.lowercase().contains("spotify") -> "🎵"
                        p.name.lowercase().contains("gym") -> "💪"
                        p.name.lowercase().contains("amazon") || p.name.lowercase().contains("prime") -> "📦"
                        p.name.lowercase().contains("youtube") -> "📺"
                        p.name.lowercase().contains("wifi") || p.name.lowercase().contains("internet") -> "🌐"
                        p.name.lowercase().contains("google") || p.name.lowercase().contains("cloud") -> "☁️"
                        else -> "⚡"
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(emoji, fontSize = 20.sp)
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = p.name,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text("AI DETECTED", style = MaterialTheme.typography.labelSmall, fontSize = 7.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    Text(
                                        text = "Regularly transacted on your ledger. Matches Monthly cycle.",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 10.sp,
                                        color = Color.Gray
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "$currencySymbol${String.format("%.0f", p.cost)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Black
                                    )
                                    Text(text = "monthly", style = MaterialTheme.typography.labelSmall, fontSize = 8.sp, color = Color.Gray)
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Ignore",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Gray,
                                    modifier = Modifier
                                        .clickable { onIgnoreSubscription(p.id) }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Edit ✏️",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .clickable { editingSubItem = p }
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Button(
                                    onClick = { onConfirmSubscription(p) },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Text("Confirm & Track", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // 4. Main Subscriptions Catalog List
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Active Subscriptions Tracking list 💳",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "+ Add Custom",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clickable { onShowCustomAddDialogChange(true) }
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }

            if (subscriptionsList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No active subscriptions are being tracked. Sync receipts or add custom elements above!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                subscriptionsList.forEach { item ->
                    val isReminderOn = reminderEnabledMap[item.id] ?: true
                    val emojiIcon = when {
                        item.name.lowercase().contains("netflix") -> "🍿"
                        item.name.lowercase().contains("spotify") -> "🎵"
                        item.name.lowercase().contains("gym") -> "💪"
                        item.name.lowercase().contains("amazon") || item.name.lowercase().contains("prime") -> "📦"
                        item.name.lowercase().contains("youtube") -> "📺"
                        item.name.lowercase().contains("wifi") || item.name.lowercase().contains("internet") -> "🌐"
                        item.name.lowercase().contains("google") || item.name.lowercase().contains("cloud") -> "☁️"
                        else -> "⚡"
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.Top) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(emojiIcon, fontSize = 22.sp)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.name,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "Renews: ${item.nextDueDate}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.Gray,
                                            fontSize = 11.sp
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    color = if (isReminderOn) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                                                    shape = RoundedCornerShape(6.dp)
                                                )
                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = if (isReminderOn) "Reminders: ON" else "Reminders: OFF",
                                                color = if (isReminderOn) Color(0xFF2E7D32) else Color(0xFFC62828),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontSize = 7.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "$currencySymbol${String.format("%.0f", item.cost)}",
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Black),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete tracking",
                                            tint = Color.Gray.copy(alpha = 0.7f),
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clickable { onDeleteSubscription(item.id) }
                                        )
                                    }
                                    Text(text = "monthly", style = MaterialTheme.typography.labelSmall, fontSize = 8.sp, color = Color.Gray)
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable { onToggleReminder(item.id) }
                                ) {
                                    Icon(
                                        imageVector = if (isReminderOn) Icons.Default.Notifications else Icons.Default.NotificationsActive,
                                        contentDescription = null,
                                        tint = if (isReminderOn) MaterialTheme.colorScheme.primary else Color.Gray,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isReminderOn) "Alert toggled. Tap to disable." else "Alert off. Click to set active.",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isReminderOn) MaterialTheme.colorScheme.primary else Color.Gray
                                    )
                                }

                                Text(
                                    text = "Edit ✏️",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .clickable { editingSubItem = item }
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 5. Brain Advisory & Savings recommendations Segment
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("💡", fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Smart Advisory Advisory Hub",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    val activeCount = subscriptionsList.size
                    val monthlyTotal = subscriptionsList.sumOf { it.cost }

                    val insightsList = remember(activeCount, monthlyTotal) {
                        listOf(
                            "Subscriptions currently consume approx. 14% of your total entertainment allocation limit.",
                            "Your annual recurring commitments sum up to $currencySymbol${String.format("%.0f", monthlyTotal * 12)}/year across services.",
                            "You have ${if (activeCount > 2) "3" else "1"} major subscriptions renewing within 10 days.",
                            "Canceling unused digital services can save you up to $currencySymbol${String.format("%.0f", monthlyTotal * 0.25)} this calendar year!"
                        )
                    }

                    insightsList.forEach { text ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text("• ", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
                            Text(text = text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }

    // Inline edit Dialog form
    editingSubItem?.let { item ->
        var editName by remember { mutableStateOf(item.name) }
        var editCost by remember { mutableStateOf(item.cost.toString()) }
        var editDueDate by remember { mutableStateOf(item.nextDueDate) }
        var editCategory by remember { mutableStateOf(item.category) }

        AlertDialog(
            onDismissRequest = { editingSubItem = null },
            confirmButton = {
                Button(
                    onClick = {
                        val parsedCost = editCost.toDoubleOrNull() ?: item.cost
                        onConfirmSubscription(
                            item.copy(
                                name = editName,
                                cost = parsedCost,
                                nextDueDate = editDueDate,
                                category = editCategory
                            )
                        )
                        editingSubItem = null
                    }
                ) {
                    Text("Save Changes")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingSubItem = null }) {
                    Text("Cancel")
                }
            },
            title = { Text("Customize Subscription Details") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Service Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editCost,
                        onValueChange = { editCost = it },
                        label = { Text("Billing Cost") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editDueDate,
                        onValueChange = { editDueDate = it },
                        label = { Text("Renewal Date (DD-MM-YYYY)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editCategory,
                        onValueChange = { editCategory = it },
                        label = { Text("Category (e.g., Bills, Entertainment)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        )
    }

    // Manual custom subscription creator Dialog
    if (showCustomAddDialog) {
        var addName by remember { mutableStateOf("") }
        var addCost by remember { mutableStateOf("") }
        var addDueDate by remember { mutableStateOf("15-06-2026") }
        var addCategory by remember { mutableStateOf("Entertainment") }

        AlertDialog(
            onDismissRequest = { onShowCustomAddDialogChange(false) },
            confirmButton = {
                Button(
                    onClick = {
                        if (addName.isNotBlank() && addCost.isNotBlank()) {
                            val c = addCost.toDoubleOrNull() ?: 199.0
                            val uID = "manual_sub_${System.currentTimeMillis()}"
                            onConfirmSubscription(
                                SubscriptionItem(
                                    id = uID,
                                    name = addName,
                                    cost = c,
                                    currency = if (currencySymbol == "₹") "INR" else "USD",
                                    nextDueDate = addDueDate,
                                    category = addCategory,
                                    isAutoAdded = false
                                )
                            )
                        }
                        onShowCustomAddDialogChange(false)
                    }
                ) {
                    Text("Add Service")
                }
            },
            dismissButton = {
                TextButton(onClick = { onShowCustomAddDialogChange(false) }) {
                    Text("Cancel")
                }
            },
            title = { Text("Track New Subscription Plan") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = addName,
                        onValueChange = { addName = it },
                        label = { Text("Subscription Name (e.g., Netflix, Gym)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = addCost,
                        onValueChange = { addCost = it },
                        label = { Text("Frequency Billing Price") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = addDueDate,
                        onValueChange = { addDueDate = it },
                        label = { Text("Next Renewal Date (DD-MM-YYYY)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        )
    }
}


// ==========================================
// TAB 3: WEEKLY STORY WRAPPED SLIDESHOW
// ==========================================
@Composable
fun PremiumWeeklyStoryWrappedTab(
    storyData: WeeklyStoryData,
    currencySymbol: String
) {
    var activeSlideIndex by remember { mutableIntStateOf(0) }
    val totalSlides = 4

    val storyGradients = listOf(
        Brush.verticalGradient(listOf(Color(0xFF8E2DE2), Color(0xFF4A00E0))), // Purple gradient code
        Brush.verticalGradient(listOf(Color(0xFFE91E63), Color(0xFFFF9800))), // Sunset warm gradient
        Brush.verticalGradient(listOf(Color(0xFF00C9FF), Color(0xFF92FE9D))), // Soft Teal visual gradient
        Brush.verticalGradient(listOf(Color(0xFF11998e), Color(0xFF38ef7d)))  // GitHub Forest Green gradient
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(storyGradients[activeSlideIndex % storyGradients.size])
            .clickable { activeSlideIndex = (activeSlideIndex + 1) % totalSlides }
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Instagram horizontal status trackers lines
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for (i in 0 until totalSlides) {
                    val alphaValue = if (i <= activeSlideIndex) 1f else 0.3f
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .background(Color.White.copy(alpha = alphaValue), RoundedCornerShape(2.dp))
                    )
                }
            }

            // Custom wrapped content slide switchers
            AnimatedContent(
                targetState = activeSlideIndex,
                transitionSpec = {
                    slideInHorizontally { width -> width } + fadeIn() togetherWith
                            slideOutHorizontally { width -> -width } + fadeOut()
                },
                label = "wrapped_story_anim"
            ) { slide ->
                when (slide) {
                    0 -> Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(text = "🎸", fontSize = 64.sp)
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "SpendWise Wrapped",
                            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Black),
                            textAlign = TextAlign.Center,
                            color = Color.White
                        )
                        Text(
                            text = "YOUR RECENT WEEKLY SPEECH STORY",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                        Text(
                            text = "Tap to launch story slice panel",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }

                    1 -> Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(text = storyData.topCategoryEmoji, fontSize = 80.sp)
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "Your top spend category was:",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        Text(
                            text = storyData.topCategory,
                            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Black),
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "A total of $currencySymbol${String.format("%.2f", storyData.totalSpentThisWeek)} logged this week.",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White
                        )
                    }

                    2 -> Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(text = "🛍️", fontSize = 72.sp)
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "Your highest spending day:",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        Text(
                            text = storyData.highestDayName,
                            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Black),
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Log totals: $currencySymbol${String.format("%.0f", storyData.highestDayAmount)}",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White
                        )
                    }

                    3 -> Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(text = "📈", fontSize = 72.sp)
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "Weekly change analysis:",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        Text(
                            text = storyData.trendComparisonMsg,
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                            textAlign = TextAlign.Center,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "Score Verdict: ${storyData.completionRatingMsg}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}


// ==========================================
// HIGH END NOTIFICATIONS DRAWER COMPOSABLE
// ==========================================
@Composable
fun NotificationsInboxOverlay(
    alertsList: List<AlertMessage>,
    onDismiss: (String) -> Unit,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable { onClose() }
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .fillMaxHeight()
                .align(Alignment.CenterEnd)
                .clickable(enabled = false) {}, // Intercept
            shape = RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Notification Alerts Inbox 🔔",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold
                    )
                    IconButton(onClick = onClose) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "CloseDrawer")
                    }
                }

                Divider()
                Spacer(modifier = Modifier.height(14.dp))

                if (alertsList.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(imageVector = Icons.Default.Inbox, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(44.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("All clear! No pending notifications.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(alertsList, key = { it.id }) { item ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(item.title, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        IconButton(onClick = { onDismiss(item.id) }, modifier = Modifier.size(18.dp)) {
                                            Icon(imageVector = Icons.Default.Delete, contentDescription = "dismiss", modifier = Modifier.size(12.dp))
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(item.body, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Fullscreen high-aesthetic modal contact sync & split dashboard overlay screen
@Composable
fun ContactSyncWindowOverlay(
    syncedContacts: List<String>,
    isContactSyncActive: Boolean,
    onSyncContactsClick: () -> Unit,
    onAddSplitClick: () -> Unit,
    onClose: () -> Unit
) {
    if (true) return
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable { onClose() },
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f)
                .clickable(enabled = false) {}, // Intercept click
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // Header Bar inside the Window Screen
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Contacts,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Link Device Contacts 🔄",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = onClose) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close Window")
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 12.dp))

                // Search bar and core states declared once for stable composition
                var searchVal by remember { mutableStateOf("") }
                val filteredList = remember(syncedContacts, searchVal) {
                    if (searchVal.isBlank()) syncedContacts else syncedContacts.filter { it.contains(searchVal, ignoreCase = true) }
                }

                // Scrollable Content using high performance LazyColumn instead of heavy nested scrollable columns
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Sync Status Banner
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (isContactSyncActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                    else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                                    RoundedCornerShape(16.dp)
                                )
                                .padding(16.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (isContactSyncActive) Icons.Default.CloudDone else Icons.Default.CloudQueue,
                                    contentDescription = null,
                                    tint = if (isContactSyncActive) MaterialTheme.colorScheme.primary else Color.Gray,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = if (isContactSyncActive) "Secure Cloud Contacts Enabled!" else "Synchronize Locally & Offline",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (isContactSyncActive) "Your contacts card is kept local. You can now tap names to split costs instantaneously." else "Device contacts are never uploaded to any server. Synchronization is 100% private.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }
                    }

                    // Content based on state
                    if (!isContactSyncActive) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(160.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(20.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContactPhone,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                        modifier = Modifier.size(52.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        "No Sync Session Active",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        "Permit app to sync contacts to link custom roomies.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.Gray,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }

                        item {
                            Button(
                                onClick = onSyncContactsClick,
                                modifier = Modifier.fillMaxWidth().testTag("sync_btn_inside_window"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(imageVector = Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Import Secure Device Contacts", fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        item {
                            OutlinedTextField(
                                value = searchVal,
                                onValueChange = { searchVal = it },
                                placeholder = { Text("Search in ${syncedContacts.size} contacts...") },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                modifier = Modifier.fillMaxWidth().testTag("window_contacts_search"),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        item {
                            Text(
                                text = "Imported Contacts Directory (${filteredList.size})",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        items(filteredList) { contactName ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                    .clickable {
                                        // Handle selecting contact to quick split bill
                                        onAddSplitClick()
                                        onClose()
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val initial = contactName.take(1)
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = initial,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = contactName, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                    Text(text = "Secure Local Contact", style = MaterialTheme.typography.labelSmall, fontSize = 8.sp, color = Color.Gray)
                                }
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = Color.Gray,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        item {
                            Button(
                                onClick = onSyncContactsClick,
                                modifier = Modifier.fillMaxWidth().testTag("sync_btn_inside_window_resync"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                            ) {
                                Icon(imageVector = Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Re-import Contacts Ledger", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// Active voice recording speech waveform indicator overlay dialog
@Composable
fun ActiveVoiceListeningSimulationOverlay(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Voice Speech Logging 🎙️",
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Logging active. Try speaking:",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "\"Spent 300 rupees for cab today\"",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Pulsing speaker recording level graphic wave bars simulation
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.height(50.dp)
                ) {
                    val heights = listOf(20, 45, 15, 35, 48, 25, 40, 10, 30)
                    heights.forEach { h ->
                        Box(
                            modifier = Modifier
                                .width(6.dp)
                                .height(h.dp)
                                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(3.dp))
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    "Press back or click elsewhere to complete speech parse.",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Proceed")
            }
        }
    )
}

// Roommate Split Entry dialog popup form
@Composable
fun SplitExpenseDialog(
    syncedContacts: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (friendName: String, amount: Double, reason: String) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var friend by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    val matchingContacts = remember(syncedContacts, friend) {
        if (friend.isBlank()) {
            syncedContacts
        } else {
            syncedContacts.filter { 
                it.contains(friend, ignoreCase = true) && !it.equals(friend, ignoreCase = true) 
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Split Roommate Bill 🤝", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "ℹ️ Type any name below, or use the quick suggestions to select instantly.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                // Select split associate friend picker
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Type Name", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = friend,
                        onValueChange = { friend = it },
                        placeholder = { Text("Type name...") },
                        modifier = Modifier.fillMaxWidth().testTag("split_friend_name_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Live auto-suggestions chips row
                    if (matchingContacts.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (friend.isBlank()) "Quick Select Roommate:" else "Suggestions:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(matchingContacts.take(5)) { suggestionName ->
                                Box(
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
                                        .clickable { friend = suggestionName }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(16.dp)
                                                .background(MaterialTheme.colorScheme.primary, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = suggestionName.take(1),
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimary
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(suggestionName, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }
                }

                // Amount
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Total Bill Amount (Split will be 50%)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                // Reason notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Bill Description / Event") },
                    placeholder = { Text("Swiggy food, Cab fuel split...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsed = amount.toDoubleOrNull()
                    if (parsed != null && notes.isNotEmpty() && friend.isNotBlank()) {
                        onConfirm(friend, parsed, notes)
                    }
                },
                enabled = amount.toDoubleOrNull() != null && notes.isNotEmpty() && friend.isNotBlank()
            ) {
                Text("Confirm Split")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// Budget Overview card showing dynamic linear limit progress
@Composable
fun BudgetOverviewCard(
    totalSpend: Double,
    budgetLimit: Double,
    currencySymbol: String
) {
    val progress = if (budgetLimit > 0) (totalSpend / budgetLimit).toFloat().coerceIn(0f, 1f) else 0f
    val percentage = (progress * 100).toInt()
    val progressColor = when {
        progress >= 0.9f -> MaterialTheme.colorScheme.error
        progress >= 0.75f -> Color(0xFFFF9800)
        else -> Color(0xFF4CAF50)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("budget_overview_card"),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Monthly Pacing Limit Outbound",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "$currencySymbol${String.format("%,.2f", totalSpend)}",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-1).sp
                        ),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Box(
                    modifier = Modifier
                        .background(progressColor.copy(alpha = 0.12f), CircleShape)
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "$percentage% USED",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = progressColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
                color = progressColor,
                trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.08f)
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.TrendingUp,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = progressColor
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (totalSpend > budgetLimit) "Overspent limit bounds!" else "Pacing within limits",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
                Text(
                    text = "Budget: $currencySymbol${String.format("%,.0f", budgetLimit)}",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
        }
    }
}

// Sliding ledger search and filters controls
@Composable
fun FilterSettingsPanel(viewModel: ExpenseViewModel) {
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val categoryFilter by viewModel.selectedCategoryFilter.collectAsStateWithLifecycle()
    val minAmount by viewModel.minAmountFilter.collectAsStateWithLifecycle()
    val maxAmount by viewModel.maxAmountFilter.collectAsStateWithLifecycle()

    val categories = listOf("All", "Food", "Travel", "Bills", "Shopping", "Entertainment", "Others")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("filter_settings_pane"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.searchQuery.value = it },
                label = { Text("Search comments or category...") },
                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Column {
                Text(
                    text = "Outbound Categories:",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(categories) { category ->
                        val isSelected = categoryFilter == category
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.selectedCategoryFilter.value = category },
                            label = { Text(category) },
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = minAmount?.toString() ?: "",
                    onValueChange = { viewModel.minAmountFilter.value = it.toDoubleOrNull() },
                    label = { Text("Min Value") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = maxAmount?.toString() ?: "",
                    onValueChange = { viewModel.maxAmountFilter.value = it.toDoubleOrNull() },
                    label = { Text("Max Value") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            TextButton(
                onClick = {
                    viewModel.searchQuery.value = ""
                    viewModel.selectedCategoryFilter.value = "All"
                    viewModel.minAmountFilter.value = null
                    viewModel.maxAmountFilter.value = null
                },
                modifier = Modifier.align(Alignment.End)
            ) {
                Icon(imageVector = Icons.Default.ClearAll, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Reset Filters")
            }
        }
    }
}

// Gorgeous Category color-pill Accent expense line items card
@Composable
fun ExpenseCard(
    expense: Expense,
    displayCurrency: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val convertedAmount = SettingsManager.convert(expense.amount, expense.currency, displayCurrency)
    val formattedAmount = String.format("%,.2f", convertedAmount)
    val symbol = if (displayCurrency == "INR") "₹" else "$"

    val categoryColor = CategoryColors[expense.category] ?: Color.Gray
    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault()) }
    val formattedDate = dateFormatter.format(Date(expense.date))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("expense_card_${expense.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp, 36.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(categoryColor)
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = expense.notes ?: expense.category,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$formattedDate • ${expense.category}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.padding(horizontal = 6.dp)
            ) {
                Text(
                    text = "$symbol$formattedAmount",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Black),
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (expense.currency != displayCurrency) {
                    Text(
                        text = "Orig: ${expense.currency} ${expense.amount}",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Transaction",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete Transaction",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

// Integrated Outbound Logging Dialog manager supporting edits
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddEditExpenseDialog(
    expense: Expense? = null,
    onDismiss: () -> Unit,
    onConfirm: (Double, String, String, Long, String?) -> Unit,
    currentCurrency: String
) {
    var amount by remember { mutableStateOf(expense?.amount?.toString() ?: "") }
    var selectedCurrency by remember { mutableStateOf(expense?.currency ?: currentCurrency) }
    var selectedCategory by remember { mutableStateOf(expense?.category ?: "Food") }
    var notes by remember { mutableStateOf(expense?.notes ?: "") }
    var timestamp by remember { mutableLongStateOf(expense?.date ?: System.currentTimeMillis()) }

    var expandedCurrency by remember { mutableStateOf(false) }
    var expandedCategory by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val categories = listOf("Food", "Travel", "Bills", "Shopping", "Entertainment", "Others")
    val currencies = listOf("INR", "USD", "EUR")

    val sdf = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (expense == null) "Log Manual Transaction 📝" else "Edit Outbound Log ✏️",
                fontWeight = FontWeight.Black
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Log Amount Value") },
                    placeholder = { Text("0.00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        Button(
                            onClick = { expandedCurrency = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        ) {
                            Text(selectedCurrency)
                            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                        DropdownMenu(
                            expanded = expandedCurrency,
                            onDismissRequest = { expandedCurrency = false }
                        ) {
                            currencies.forEach { curr ->
                                DropdownMenuItem(
                                    text = { Text(curr) },
                                    onClick = {
                                        selectedCurrency = curr
                                        expandedCurrency = false
                                    }
                                )
                            }
                        }
                    }

                    Box(modifier = Modifier.weight(1.5f)) {
                        Button(
                            onClick = { expandedCategory = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        ) {
                            Text(selectedCategory)
                            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                        DropdownMenu(
                            expanded = expandedCategory,
                            onDismissRequest = { expandedCategory = false }
                        ) {
                            categories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat) },
                                    onClick = {
                                        selectedCategory = cat
                                        expandedCategory = false
                                    }
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
                            DatePickerDialog(
                                context,
                                { _, y, m, d ->
                                    val newCal = Calendar.getInstance().apply {
                                        set(Calendar.YEAR, y)
                                        set(Calendar.MONTH, m)
                                        set(Calendar.DAY_OF_MONTH, d)
                                    }
                                    timestamp = newCal.timeInMillis
                                },
                                cal.get(Calendar.YEAR),
                                cal.get(Calendar.MONTH),
                                cal.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        }
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.CalendarToday, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = sdf.format(Date(timestamp)),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "Change",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Black
                    )
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Log remarks / tags") },
                    placeholder = { Text("Pizza, Uber, Netflix subscriptions...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val doubleAmt = amount.toDoubleOrNull()
                    if (doubleAmt != null) {
                        onConfirm(
                            doubleAmt,
                            selectedCurrency,
                            selectedCategory,
                            timestamp,
                            notes.ifBlank { null }
                        )
                    }
                },
                enabled = amount.toDoubleOrNull() != null
            ) {
                Text(text = if (expense == null) "Log Outbound" else "Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
