package com.example.email_client.ui.navigation

sealed interface Route
data object OnboardingRoute : Route

// Top-level tabs inside the shell
data object InboxRoute : Route
data object StarredRoute : Route
data object SentRoute : Route
data object DraftsRoute : Route
data object FoldersRoute : Route
data class FolderRoute(val folder: String) : Route
data object AccountsRoute : Route
data object SettingsRoute : Route

// Pushed detail routes
data class ThreadRoute(val messageId: String) : Route

/** replyTo = sender address for replies; draftId = Room message id for draft resume. */
data class ComposeRoute(val replyTo: String? = null, val draftId: String? = null) : Route
