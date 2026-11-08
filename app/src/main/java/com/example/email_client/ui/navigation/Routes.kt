package com.example.email_client.ui.navigation

sealed interface Route
data object OnboardingRoute : Route
data object InboxRoute : Route
data class ThreadRoute(val messageId: String) : Route
data class ComposeRoute(val replyTo: String? = null) : Route
data object AccountsRoute : Route
data object SettingsRoute : Route
