package com.example.email_client.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.example.email_client.LiquidMailApp
import com.example.email_client.ui.screens.AccountsScreen
import com.example.email_client.ui.screens.ComposeScreen
import com.example.email_client.ui.screens.FolderScreen
import com.example.email_client.ui.screens.FoldersScreen
import com.example.email_client.ui.screens.InboxScreen
import com.example.email_client.ui.screens.OnboardingScreen
import com.example.email_client.ui.screens.ThreadScreen
import com.example.email_client.ui.shell.LiquidShell
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.flow.first

@Composable
fun AppNavHost(app: LiquidMailApp) {
    val onboarded by produceState<Boolean?>(initialValue = null) {
        value = app.settings.isOnboarded.first()
    }
    val start = onboarded ?: return
    val backStack = remember { mutableStateListOf<Route>(if (start) InboxRoute else OnboardingRoute) }

    val navigate: (Route) -> Unit = { backStack.add(it) }
    val back: () -> Unit = { if (backStack.size > 1) backStack.removeAt(backStack.lastIndex) }
    val resetTo: (Route) -> Unit = { backStack.clear(); backStack.add(it) }
    val switchTab: (Route) -> Unit = { tab ->
        if (backStack.isNotEmpty() && backStack.first() is OnboardingRoute) {
            resetTo(tab)
        } else {
            // keep detail routes above; replace the tab root
            if (backStack.size > 1 && backStack[0] != tab) {
                backStack[0] = tab
            } else if (backStack.isEmpty() || backStack.last() != tab) {
                backStack.clear(); backStack.add(tab)
            }
        }
    }

    val haze = remember { HazeState() }
    var query by remember { mutableStateOf("") }
    val currentTab = backStack.firstOrNull() ?: InboxRoute

    NavDisplay(
        backStack = backStack,
        onBack = back,
        entryProvider = entryProvider {
            entry<OnboardingRoute> {
                OnboardingScreen(app = app, onDone = { resetTo(InboxRoute) })
            }

            // Tab routes render inside the shared shell
            entry<InboxRoute> {
                LiquidShell(
                    haze = haze, title = "Mail",
                    query = query, onQueryChange = { query = it },
                    activeTab = currentTab, onTab = { switchTab(it as Route) },
                    onAvatar = { switchTab(AccountsRoute) },
                    accountInitial = "L",
                ) {
                    InboxScreen(app = app, haze = haze, query = query, onOpenMessage = { navigate(ThreadRoute(it)) })
                }
            }
            entry<FoldersRoute> {
                LiquidShell(
                    haze = haze, title = "Folders",
                    query = query, onQueryChange = { query = it },
                    activeTab = currentTab, onTab = { switchTab(it as Route) },
                    onAvatar = { switchTab(AccountsRoute) },
                    accountInitial = "L",
                ) {
                    FoldersScreen(app = app, haze = haze, onOpenFolder = { navigate(FolderRoute(it)) })
                }
            }
            entry<FolderRoute> { r ->
                LiquidShell(
                    haze = haze, title = r.folder,
                    query = query, onQueryChange = { query = it },
                    activeTab = FoldersRoute, onTab = { switchTab(it as Route) },
                    onAvatar = { switchTab(AccountsRoute) },
                    accountInitial = "L",
                ) {
                    FolderScreen(app = app, haze = haze, folder = r.folder, onOpenMessage = { navigate(ThreadRoute(it)) })
                }
            }
            entry<AccountsRoute> {
                LiquidShell(
                    haze = haze, title = "Accounts",
                    query = query, onQueryChange = { query = it },
                    activeTab = currentTab, onTab = { switchTab(it as Route) },
                    onAvatar = { },
                    accountInitial = "A",
                ) {
                    AccountsScreen(app = app, onBack = back, onAddAccount = { navigate(OnboardingRoute) })
                }
            }

            // Detail routes render without the shell chrome
            entry<ThreadRoute> { r ->
                ThreadScreen(app = app, messageId = r.messageId, onBack = back, onReply = { navigate(ComposeRoute(replyTo = it)) })
            }
            entry<ComposeRoute> { r ->
                ComposeScreen(app = app, replyTo = r.replyTo, onBack = back)
            }
            entry<SettingsRoute> { }
        }
    )
}
