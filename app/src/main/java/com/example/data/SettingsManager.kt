package com.example.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SettingsManager(context: Context) {
    private val prefs = context.getSharedPreferences("expense_tracker_settings", Context.MODE_PRIVATE)

    private val _displayCurrency = MutableStateFlow(prefs.getString("display_currency", "USD") ?: "USD")
    val displayCurrency: StateFlow<String> = _displayCurrency

    private val _monthlyBudget = MutableStateFlow(prefs.getFloat("monthly_budget", 500f).toDouble())
    val monthlyBudget: StateFlow<Double> = _monthlyBudget

    private val _currentUserEmail = MutableStateFlow(prefs.getString("current_user_email", null))
    val currentUserEmail: StateFlow<String?> = _currentUserEmail

    private val _isGmailConnected = MutableStateFlow(prefs.getBoolean("is_gmail_connected", false))
    val isGmailConnected: StateFlow<Boolean> = _isGmailConnected

    private val _isSmsScanEnabled = MutableStateFlow(prefs.getBoolean("is_sms_scan_enabled", false))
    val isSmsScanEnabled: StateFlow<Boolean> = _isSmsScanEnabled

    fun setGmailConnected(connected: Boolean) {
        prefs.edit().putBoolean("is_gmail_connected", connected).apply()
        _isGmailConnected.value = connected
    }

    fun setSmsScanEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("is_sms_scan_enabled", enabled).apply()
        _isSmsScanEnabled.value = enabled
    }

    fun setDisplayCurrency(currency: String) {
        prefs.edit().putString("display_currency", currency).apply()
        _displayCurrency.value = currency
    }

    fun setMonthlyBudget(limit: Double) {
        prefs.edit().putFloat("monthly_budget", limit.toFloat()).apply()
        _monthlyBudget.value = limit
    }

    fun setCurrentUserEmail(email: String?) {
        if (email == null) {
            prefs.edit().remove("current_user_email").apply()
        } else {
            prefs.edit().putString("current_user_email", email).apply()
        }
        _currentUserEmail.value = email
    }

    companion object {
        const val USD = "USD"
        const val INR = "INR"
        
        // Let's use a standard conversion rate: 1 USD = 83.0 INR
        const val EXCHANGE_RATE = 83.0

        fun convert(amount: Double, fromCurrency: String, toCurrency: String): Double {
            if (fromCurrency == toCurrency) return amount
            return if (fromCurrency == USD && toCurrency == INR) {
                amount * EXCHANGE_RATE
            } else {
                amount / EXCHANGE_RATE
            }
        }
    }
}
