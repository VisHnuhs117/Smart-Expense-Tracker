package com.example.util

import com.example.data.Expense
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

data class ParsedNlExpense(
    val amount: Double?,
    val category: String,
    val notes: String,
    val date: Long,
    val isRecurring: Boolean
)

data class SubscriptionItem(
    val id: String,
    val name: String,
    val cost: Double,
    val currency: String,
    val nextDueDate: String,
    val category: String,
    val isAutoAdded: Boolean
)

data class SplitBalance(
    val friendName: String,
    val balance: Double, // Positive if they owe us, negative if we owe them
    val reason: String
)

data class HeatmapDay(
    val dayOfMonth: Int,
    val dateString: String,
    val totalAmount: Double,
    val intensity: Float // 0f to 1f representation
)

data class WeeklyStoryData(
    val totalSpentThisWeek: Double,
    val topCategory: String,
    val topCategoryEmoji: String,
    val highestDayName: String,
    val highestDayAmount: Double,
    val trendComparisonMsg: String,
    val completionRatingMsg: String,
    val storyColorHex: Long
)

object FintechFeatureHelper {

    // 1. Natural Language Processing Parser
    fun parseNaturalLanguage(input: String): ParsedNlExpense {
        val raw = input.trim()
        val lowercase = raw.lowercase()

        // Extract amount: look for any continuous digits or decimals
        // Support formats like spent 250, ₹12000, 340.50, $120, paid 2000
        val amountRegex = """(?:\$|₹|inr|usd)?\s*(\d+(?:\.\d+)?)""".toRegex()
        val matchResult = amountRegex.find(lowercase)
        val amount = matchResult?.groupValues?.get(1)?.toDoubleOrNull()

        // Extract date keywords
        val calendar = Calendar.getInstance()
        var matchedDate = calendar.timeInMillis
        if (lowercase.contains("yesterday")) {
            calendar.add(Calendar.DAY_OF_YEAR, -1)
            matchedDate = calendar.timeInMillis
        } else if (lowercase.contains("day before yesterday")) {
            calendar.add(Calendar.DAY_OF_YEAR, -2)
            matchedDate = calendar.timeInMillis
        } else if (lowercase.contains("last week") || lowercase.contains("week ago")) {
            calendar.add(Calendar.DAY_OF_YEAR, -7)
            matchedDate = calendar.timeInMillis
        }

        // Detect Category
        var category = "Others"
        if (matchesAny(lowercase, listOf("pizza", "burger", "coffee", "tea", "chai", "lunch", "dinner", "breakfast", "groceries", "swiggy", "zomato", "restaurant", "starbucks", "mcdonald", "subway", "food", "eat", "cafe", "snack"))) {
            category = "Food"
        } else if (matchesAny(lowercase, listOf("uber", "ola", "cab", "taxi", "rapido", "ride", "auto", "metro", "bus", "train", "flight", "petrol", "gas", "fuel", "diesel", "trip", "travel"))) {
            category = "Travel"
        } else if (matchesAny(lowercase, listOf("rent", "electricity", "water", "wifi", "internet", "power", "utility", "broadband", "bill", "phone recharge", "mobile bill", "insurance", "emi", "payment"))) {
            category = "Bills"
        } else if (matchesAny(lowercase, listOf("amazon", "flipkart", "myntra", "clothes", "shoes", "shirt", "mall", "shopping", "gift", "spent on buy", "bought"))) {
            category = "Shopping"
        } else if (matchesAny(lowercase, listOf("netflix", "spotify", "youtube", "movie", "cinema", "club", "drinks", "party", "pub", "game", "gaming", "steam", "playstation", "xbox", "concert", "arcade", "fun"))) {
            category = "Entertainment"
        }

        // Subscriptions recurrence check
        val isRecurring = matchesAny(lowercase, listOf("netflix", "spotify", "rent", "wifi", "internet", "gym", "youtube premium", "sub", "recurring", "monthly", "broadband", "emi"))

        // Extract Notes/Merchant from input
        // Standard cleanup of words that aren't useful merchant info
        var words = raw.split(" ")
        val filterWords = listOf(
            "spent", "paid", "for", "on", "to", "yesterday", "today", "tomorrow", "for", "in", "the", "a", "an", "at", "my", "was", "giving",
            "usd", "inr", "$", "₹", "rs", "rupees", "dollar", "bought", "buying", "monthly", "weekly", "yearly", "recurring"
        )
        val cleanedWords = words.filter { word ->
            val wLower = word.lowercase()
            // filter numerical inputs completely from the label
            val isNumber = wLower.toDoubleOrNull() != null || wLower.replace("₹", "").replace("$", "").toDoubleOrNull() != null
            !filterWords.contains(wLower) && !isNumber
        }

        val notes = cleanedWords.joinToString(" ").trim().ifEmpty {
            if (category != "Others") "$category Expense" else "Personal Log"
        }

        // Ensure description capitalizes beautifully
        val finalNotes = notes.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

        return ParsedNlExpense(
            amount = amount,
            category = category,
            notes = finalNotes,
            date = matchedDate,
            isRecurring = isRecurring
        )
    }

    private fun matchesAny(input: String, keywords: List<String>): Boolean {
        for (kw in keywords) {
            if (input.contains(kw)) return true
        }
        return false
    }

    // 2. Subscription Detector
    fun detectSubscriptions(expenses: List<Expense>): List<SubscriptionItem> {
        val staticSubs = mutableListOf<SubscriptionItem>()
        
        // Let's create smart default mock dynamic simulations if user hasn't added any,
        // and also map actual repeated bills from the database context!
        val rentMatched = expenses.any { it.notes?.lowercase()?.contains("rent") == true }
        val netflixMatched = expenses.any { it.notes?.lowercase()?.contains("netflix") == true }
        val spotifyMatched = expenses.any { it.notes?.lowercase()?.contains("spotify") == true }
        val wifiMatched = expenses.any { it.notes?.lowercase()?.contains("wifi") == true || it.notes?.lowercase()?.contains("internet") == true }
        val gymMatched = expenses.any { it.notes?.lowercase()?.contains("gym") == true }

        val cal = Calendar.getInstance()
        val currentYear = cal.get(Calendar.YEAR)
        val currentMonth = cal.get(Calendar.MONTH) + 1
        
        // AddRent to static catalog if user doesn't have it
        staticSubs.add(
            SubscriptionItem(
                id = "sub_rent",
                name = "Monthly Office/Room Rent",
                cost = if (rentMatched) expenses.first { it.notes?.lowercase()?.contains("rent") == true }.amount else 12500.0,
                currency = if (rentMatched) expenses.first { it.notes?.lowercase()?.contains("rent") == true }.currency else "INR",
                nextDueDate = "01-${String.format("%02d", currentMonth + 1)}-$currentYear",
                category = "Bills",
                isAutoAdded = rentMatched
            )
        )

        staticSubs.add(
            SubscriptionItem(
                id = "sub_netflix",
                name = "Netflix UHD Premium",
                cost = if (netflixMatched) expenses.first { it.notes?.lowercase()?.contains("netflix") == true }.amount else 649.0,
                currency = if (netflixMatched) expenses.first { it.notes?.lowercase()?.contains("netflix") == true }.currency else "INR",
                nextDueDate = "14-${String.format("%02d", currentMonth + 1)}-$currentYear",
                category = "Entertainment",
                isAutoAdded = netflixMatched
            )
        )

        staticSubs.add(
            SubscriptionItem(
                id = "sub_spotify",
                name = "Spotify Premium Duo",
                cost = if (spotifyMatched) expenses.first { it.notes?.lowercase()?.contains("spotify") == true }.amount else 179.0,
                currency = if (spotifyMatched) expenses.first { it.notes?.lowercase()?.contains("spotify") == true }.currency else "INR",
                nextDueDate = "20-${String.format("%02d", currentMonth + 1)}-$currentYear",
                category = "Entertainment",
                isAutoAdded = spotifyMatched
            )
        )

        staticSubs.add(
            SubscriptionItem(
                id = "sub_wifi",
                name = "High-Speed Fiber WiFi",
                cost = if (wifiMatched) expenses.first { it.notes?.lowercase()?.contains("wifi") == true || it.notes?.lowercase()?.contains("internet") == true }.amount else 999.0,
                currency = if (wifiMatched) expenses.first { it.notes?.lowercase()?.contains("wifi") == true || it.notes?.lowercase()?.contains("internet") == true }.currency else "INR",
                nextDueDate = "08-${String.format("%02d", currentMonth + 1)}-$currentYear",
                category = "Bills",
                isAutoAdded = wifiMatched
            )
        )

        staticSubs.add(
            SubscriptionItem(
                id = "sub_gym",
                name = "Gold's Fitness Gym",
                cost = if (gymMatched) expenses.first { it.notes?.lowercase()?.contains("gym") == true }.amount else 2500.0,
                currency = if (gymMatched) expenses.first { it.notes?.lowercase()?.contains("gym") == true }.currency else "INR",
                nextDueDate = "05-${String.format("%02d", currentMonth + 1)}-$currentYear",
                category = "Entertainment",
                isAutoAdded = gymMatched
            )
        )

        // Make sure we sort subscriptions by cost descending (most expensive first)
        return staticSubs.sortedByDescending { it.cost }
    }

    // 3. Expense Prediction System
    fun predictMonthlySpend(totalSpendThisMonth: Double, budgetLimit: Double): Pair<Double, String> {
        val cal = Calendar.getInstance()
        val currentDay = cal.get(Calendar.DAY_OF_MONTH)
        val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        // Prevent division by zero
        val safeDay = if (currentDay == 0) 1 else currentDay
        val dailyBurnRate = totalSpendThisMonth / safeDay
        val projectedSpend = dailyBurnRate * maxDays

        val message = when {
            budgetLimit <= 0.0 -> "Set a monthly budget limit in Settings to calculate financial projections."
            projectedSpend > budgetLimit -> {
                "At your current daily burn rate, you'll reach ₹${String.format("%.2f", projectedSpend)} this month. You are projected to EXCEED your ₹$budgetLimit budget by ${(((projectedSpend - budgetLimit) / budgetLimit) * 100).roundToInt()}%! Limit entertainment purchases immediately."
            }
            projectedSpend >= budgetLimit * 0.85 -> {
                "Pacing warning: You are on track to spend ₹${String.format("%.2f", projectedSpend)}, which is very close to your ₹$budgetLimit budget. Consider deferring major shopping."
            }
            else -> {
                "Excellent pacing! You're on track to spend ₹${String.format("%.2f", projectedSpend)} this month, well within your safe ₹$budgetLimit budget limits. Keep it up! 👏"
            }
        }

        return Pair(projectedSpend, message)
    }

    // 4. Financial Health Score (0-100)
    fun calculateHealthScore(expenses: List<Expense>, totalSpendThisMonth: Double, budgetLimit: Double): Pair<Int, List<String>> {
        var baseScore = 100
        val suggestions = mutableListOf<String>()

        if (budgetLimit <= 0.0) {
            return Pair(70, listOf("Set a realistic budget limit in settings to configure dynamic health scoring.", "Record more than 5 expenses this week."))
        }

        // Budget Adherence rule
        val budgetRatio = totalSpendThisMonth / budgetLimit
        if (budgetRatio > 1.0) {
            val exceedAmount = (budgetRatio - 1.0) * 100
            val deduction = (exceedAmount * 1.5).coerceIn(0.0, 45.0).roundToInt()
            baseScore -= deduction
            suggestions.add("Save Budget: Over budget by ${exceedAmount.roundToInt()}%. Budget limit violation is draining your health rating.")
        } else if (budgetRatio > 0.8) {
            baseScore -= 10
            suggestions.add("Pacing Alert: Spent over 80% of budget. Limit premium purchases until the 1st of next month.")
        } else {
            suggestions.add("Healthy Budget: Under budget limits. Excellent self-control!")
        }

        // Spend category balance rules
        val totalAmountSum = expenses.sumOf { it.amount }
        if (totalAmountSum > 0.0) {
            val entertainmentSum = expenses.filter { it.category == "Entertainment" }.sumOf { it.amount }
            val entertainmentRatio = entertainmentSum / totalAmountSum
            if (entertainmentRatio > 0.35) {
                baseScore -= 15
                suggestions.add("Reduce Fun Spend: Entertainment consumes ${(entertainmentRatio * 100).roundToInt()}% of your total cash outbound. Try substituting premium hobbies with free local activities.")
            }

            val shoppingSum = expenses.filter { it.category == "Shopping" }.sumOf { it.amount }
            val shoppingRatio = shoppingSum / totalAmountSum
            if (shoppingRatio > 0.35) {
                baseScore -= 12
                suggestions.add("Shopping Spillover: Shopping represents ${(shoppingRatio * 100).roundToInt()}% of outbound. Unsubscribe from Amazon deals and promotional push notifications.")
            }

            val foodSum = expenses.filter { it.category == "Food" }.sumOf { it.amount }
            val foodRatio = foodSum / totalAmountSum
            if (foodRatio > 0.45) {
                baseScore -= 10
                suggestions.add("Home Cooking: Food & restaurants take up ${(foodRatio * 100).roundToInt()}% of spending. Reducing food delivery by 15% could save you around ₹1,500/month.")
            }
        }

        // Frequency penalty
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -7)
        val lastWeekTimestamp = cal.timeInMillis
        val lastWeekExpensesCount = expenses.filter { it.date >= lastWeekTimestamp }.size
        if (lastWeekExpensesCount > 12) {
            baseScore -= 8
            suggestions.add("Reduce Impulse Buying: You have logged $lastWeekExpensesCount microtransactions this week. Consolidate your grocery runs.")
        }

        val finalScore = baseScore.coerceIn(0, 100)
        if (suggestions.isEmpty()) {
            suggestions.add("No critical issues found. Log entries daily to generate smart suggestions.")
        }

        return Pair(finalScore, suggestions)
    }

    // 5. Shared Expenses / Split Bills Calculations
    fun getSplitBalances(): List<SplitBalance> {
        return emptyList()
    }

    // 6. GitHub-Style Heatmap Calendar Mapper (Current Month)
    fun buildMonthlyHeatmap(expenses: List<Expense>): List<HeatmapDay> {
        val daysList = mutableListOf<HeatmapDay>()
        val cal = Calendar.getInstance()
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val currentMonth = cal.get(Calendar.MONTH)
        val currentYear = cal.get(Calendar.YEAR)

        // Find standard max daily spend in the current month to scale intensity properly
        val dailySums = DoubleArray(daysInMonth + 1)
        expenses.forEach { exp ->
            val expCal = Calendar.getInstance().apply { timeInMillis = exp.date }
            if (expCal.get(Calendar.MONTH) == currentMonth && expCal.get(Calendar.YEAR) == currentYear) {
                val day = expCal.get(Calendar.DAY_OF_MONTH)
                if (day in 1..daysInMonth) {
                    dailySums[day] += exp.amount
                }
            }
        }

        val maxDailySpend = dailySums.maxOrNull() ?: 1.0
        val divisor = if (maxDailySpend == 0.0) 1.0 else maxDailySpend

        val format = SimpleDateFormat("MMM dd", Locale.getDefault())

        for (day in 1..daysInMonth) {
            val cellCal = Calendar.getInstance().apply {
                set(Calendar.MONTH, currentMonth)
                set(Calendar.YEAR, currentYear)
                set(Calendar.DAY_OF_MONTH, day)
            }
            
            val totalAmt = dailySums[day]
            // Calculate a score intensity between 0f (empty spent) to 1f (peak spent)
            val intensity = if (totalAmt == 0.0) 0f else (totalAmt / divisor).toFloat().coerceIn(0.12f, 1f)

            daysList.add(
                HeatmapDay(
                    dayOfMonth = day,
                    dateString = format.format(cellCal.time),
                    totalAmount = totalAmt,
                    intensity = intensity
                )
            )
        }
        return daysList
    }

    // 7. Weekly Spotify Wrapped-Style statistics generator
    fun buildWeeklyStory(expenses: List<Expense>, monthlyBudget: Double): WeeklyStoryData {
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()
        
        // Define past 7 days range
        cal.add(Calendar.DAY_OF_YEAR, -7)
        val lastWeekStart = cal.timeInMillis
        val thisWeekExpenses = expenses.filter { it.date >= lastWeekStart }

        // Fetch top category from last 7 days
        val categoryTotals = thisWeekExpenses.groupBy { it.category }.mapValues { (_, list) -> list.sumOf { it.amount } }
        val topCategory = categoryTotals.maxByOrNull { it.value }?.key ?: "No Spends"
        val topCategoryAmt = categoryTotals[topCategory] ?: 0.0

        val topCategoryEmoji = when(topCategory) {
            "Food" -> "🍔"
            "Travel" -> "🚗"
            "Bills" -> "⚡"
            "Shopping" -> "🛍️"
            "Entertainment" -> "🎬"
            else -> "✨"
        }

        // Peak spend day name of the week
        val dayOfWeekSums = mutableMapOf<String, Double>()
        val dayOfWeekFormat = SimpleDateFormat("EEEE", Locale.getDefault())
        thisWeekExpenses.forEach { exp ->
            val dayName = dayOfWeekFormat.format(Date(exp.date))
            dayOfWeekSums[dayName] = (dayOfWeekSums[dayName] ?: 0.0) + exp.amount
        }

        val peakDay = dayOfWeekSums.maxByOrNull { it.value }
        val highestDayName = peakDay?.key ?: "None"
        val highestDayAmount = peakDay?.value ?: 0.0

        // Weekly change statistics
        cal.add(Calendar.DAY_OF_YEAR, -7)
        val prevWeekStart = cal.timeInMillis
        val prevWeekExpenses = expenses.filter { it.date in prevWeekStart until lastWeekStart }
        val totalSpentThisWeek = thisWeekExpenses.sumOf { it.amount }
        val totalSpentPrevWeek = prevWeekExpenses.sumOf { it.amount }

        val diff = totalSpentPrevWeek - totalSpentThisWeek
        val trendComparisonMsg = when {
            totalSpentPrevWeek == 0.0 -> "Welcome! Realise your savings journey from next week."
            diff > 0.0 -> "Amazing speed! You spent ₹${String.format("%.0f", diff)} less compared to last week's bills! 📉"
            else -> "Warning spike! Spent ₹${String.format("%.0f", -diff)} more than last week. Revisit grocery list."
        }

        val completionRating = when {
            totalSpentThisWeek == 0.0 -> "Blank Slate! Log starting expenses below."
            totalSpentThisWeek <= monthlyBudget * 0.15 -> "Perfect execution! Extremely disciplined spending tracker level."
            totalSpentThisWeek <= monthlyBudget * 0.25 -> "Safe levels. Your financial habits are robust."
            else -> "High outbound alerts! Tighten weekend budgets."
        }

        // Select wrapped card visual flavor
        val storyColorHex = when(topCategory) {
            "Food" -> 0xFFE91E63 // Pink accent
            "Travel" -> 0xFF2196F3 // Blue accent
            "Bills" -> 0xFFFFEF39A // Amber/Orange accent
            "Shopping" -> 0xFF4CAF50 // Green accent
            "Entertainment" -> 0xFF9C27B0 // Purple accent
            else -> 0xFF009688 // Teal accent
        }

        return WeeklyStoryData(
            totalSpentThisWeek = totalSpentThisWeek,
            topCategory = topCategory,
            topCategoryEmoji = topCategoryEmoji,
            highestDayName = highestDayName,
            highestDayAmount = highestDayAmount,
            trendComparisonMsg = trendComparisonMsg,
            completionRatingMsg = completionRating,
            storyColorHex = storyColorHex
        )
    }
}
