package com.example.email_client.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.example.email_client.LiquidMailApp
import com.example.email_client.ui.screens.AccountsScreen
import com.example.email_client.ui.screens.ComposeScreen
import com.example.email_client.ui.screens.InboxScreen
import com.example.email_client.ui.screens.OnboardingScreen
import com.example.email_client.ui.screens.ThreadScreen
import kotlinx.coroutines.flow.first
import androidx.compose.runtime.produceState

@Composable
fun AppNavHost(app: LiquidMailApp) {
    val onboarded by produceState<Boolean?>(initialValue = null) {
        value = app.settings.isOnboarded.first()
    }
    val start = onboarded ?: return // still loading; show blank frame
    val backStack = remember { mutableStateListOf<Route>(if (start) InboxRoute else OnboardingRoute) }

    val navigate: (Route) -> Unit = { backStack.add(it) }
    val back: () -> Unit = { if (backStack.size > 1) backStack.removeAt(backStack.lastIndex) }
    val resetTo: (Route) -> Unit = { backStack.clear(); backStack.add(it) }

    NavDisplay(
        backStack = backStack,
        onBack = back,
        entryProvider = entryProvider {
            entry<OnboardingRoute> {
                OnboardingScreen(app = app, onDone = { resetTo(InboxRoute) })
            }
            entry<InboxRoute> {
                InboxScreen(
                    app = app,
                    onOpenMessage = { navigate(ThreadRoute(it)) },
                    onCompose = { navigate(ComposeRoute()) },
                    onAccounts = { navigate(AccountsRoute) },
                )
            }
            entry<ThreadRoute> { r ->
                ThreadScreen(app = app, messageId = r.messageId, onBack = back, onReply = { navigate(ComposeRoute(replyTo = it)) })
            }
            entry<ComposeRoute> { r ->
                ComposeScreen(app = app, replyTo = r.replyTo, onBack = back)
            }
            entry<AccountsRoute> {
                AccountsScreen(app = app, onBack = back, onAddAccount = { navigate(OnboardingRoute) })
            }
            entry<SettingsRoute> { }
        }
    )
}
