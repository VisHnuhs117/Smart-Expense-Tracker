# ExpenseAI – Smart Expense Tracker 📊💰

# Overview

ExpenseAI is a modern Android expense tracking application designed to help users effortlessly manage their finances through automation, smart insights, and intuitive analytics.

Unlike traditional expense trackers, ExpenseAI reduces manual work by using OCR-powered bill scanning, natural language expense entry, subscription detection, and intelligent spending insights.

The goal is to create a fintech-style experience that helps users understand where their money goes and make better financial decisions.

---

# Problem Statement

Many people struggle to track expenses consistently because:

- Manual expense entry is time-consuming
- Subscription costs are often forgotten
- Spending patterns are difficult to understand
- Budget overruns are noticed too late
- Financial data is scattered across receipts and transactions

ExpenseAI solves these problems through automation and data-driven insights.

---

# Key Features
💸 Expense Management
- Add, edit, and delete expenses
- Categorize expenses
- Date and note support
- Swipe actions for quick management

---

# 🧾 AI-Powered Receipt Scanner

Using Google ML Kit OCR:

- Scan receipts using the camera
- Extract:
    - Amount
    - Date
    - Merchant Name
- Auto-fill expense details
- Edit before saving

---

# 💬 Natural Language Expense Entry

Examples:

Spent 250 on pizza
Paid rent 12000
Uber 340 yesterday

The app automatically identifies:

- Amount
- Category
- Date

Reducing expense logging time significantly.

---

# 📈 Analytics Dashboard

Visualize spending through:

- Pie Charts
- Bar Charts
- Monthly Trends
- Yearly Trends

Track:

- Monthly expenses
- Yearly expenses
- Category-wise spending

---

# 🧠 Smart Insights Engine

Generate insights such as:

- You spent 30% more on food this month
- Weekend spending is higher than weekdays
- Shopping expenses increased this week
- Food delivery is your top category

---

# 🎯 Budget Management

Users can:

- Set monthly budgets
- Create category budgets
- Track remaining budget
- Receive overspending alerts

---

# 🔄 Subscription Detection System

Automatically detects recurring expenses like:

- Netflix
- Spotify
- Amazon Prime
- Google One
- Gym Memberships

Displays:

- Monthly subscription cost
- Annual subscription cost
- Upcoming renewals

---

# 📅 Subscription Renewal Reminders

Examples:

- Spotify renews tomorrow
- Netflix renews in 3 days

Helps users avoid surprise charges.

---

# 📊 Financial Health Score

A custom score calculated from:

- Budget adherence
- Spending habits
- Savings consistency
- Expense distribution

Example:

Financial Health Score: 82/100

---

# 💡 Savings Recommendations

Examples:

- Reduce food delivery spending to save ₹1200/month
- Subscriptions account for 20% of your expenses
- Shopping spending increased by 18%

---

# 🔥 Expense Heatmap

GitHub-style spending calendar:

- Darker colors = More spending
- Lighter colors = Less spending

Allows users to quickly identify spending patterns.

---

# 👥 Shared Expenses

Track expenses with:

- Friends
- Family
- Roommates

Supports:

- Bill splitting
- Group expenses
- Outstanding balances

---

# 💱 Multi-Currency Support

Supported currencies:

- Indian Rupee (₹)
- US Dollar ($)

Users can switch currencies from settings.

---

# 🌙 Modern UI

| Feature                | User Problem Solved                  |
| ---------------------- | ------------------------------------ |
| OCR Receipt Scanner    | Eliminates manual entry              |
| Natural Language Input | Faster expense logging               |
| Budget Alerts          | Prevents overspending                |
| Subscription Detector  | Identifies hidden recurring costs    |
| Smart Insights         | Helps understand spending habits     |
| Financial Health Score | Encourages better financial behavior |
| Voice Input            | Improves accessibility and speed     |

---

# Tech Stack
# Android
- Kotlin
- Jetpack Compose
- Material 3
# Architecture
- MVVM
- Clean Architecture
# Jetpack Components
- ViewModel
- StateFlow
- Navigation Component
- Room Database
# Dependency Injection
- Hilt
# Machine Learning
- Google ML Kit OCR
- Speech Recognition APIs

---
