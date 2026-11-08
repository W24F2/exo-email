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

    val accountsCount by produceState<Int>(initialValue = -1) {
        value = app.database.accountDao().observeAll().first().size
    }
    val accountsExist = accountsCount > 0

    val backStack = remember(accountsExist) {
        mutableStateListOf<Route>().apply {
            add(if (accountsExist) InboxRoute else OnboardingRoute)
        }
    }

    val navigate: (Route) -> Unit = { backStack.add(it) }
    val back: () -> Unit = { if (backStack.size > 1) backStack.removeAt(backStack.lastIndex) }
    val resetTo: (Route) -> Unit = { backStack.clear(); backStack.add(it) }
    val switchTab: (Route) -> Unit = { tab ->
        if (backStack.isNotEmpty() && backStack.first() is OnboardingRoute) {
            resetTo(tab)
        } else {
            if (backStack.size > 1 && backStack[0] != tab) {
                backStack[0] = tab
            } else if (backStack.isEmpty() || backStack.last() != tab) {
                backStack.clear()
                backStack.add(tab)
            }
        }
    }

    val haze = remember { HazeState() }
    var query by remember { mutableStateOf("") }
    val currentTab = backStack.firstOrNull() ?: OnboardingRoute

    NavDisplay(
        backStack = backStack,
        onBack = back,
        entryProvider = entryProvider {
            entry<OnboardingRoute> {
                OnboardingScreen(app = app, onDone = { resetTo(InboxRoute) })
            }
            entry<InboxRoute> {
                LiquidShell(
                    haze = haze, title = "Mail",
                    query = query, onQueryChange = { query = it },
                    activeTab = currentTab, onTab = { switchTab(it as Route) },
                    onAvatar = { switchTab(AccountsRoute) },
                    accountsExist = accountsExist,
                    onAddAccount = { resetTo(OnboardingRoute) },
                    accountInitial = "",
                ) {
                    InboxScreen(
                        app = app, haze = haze, query = query,
                        accountsExist = accountsExist,
                        onOpenMessage = { navigate(ThreadRoute(it)) },
                    )
                }
            }
            entry<FoldersRoute> {
                LiquidShell(
                    haze = haze, title = "Folders",
                    query = query, onQueryChange = { query = it },
                    activeTab = currentTab, onTab = { switchTab(it as Route) },
                    onAvatar = { switchTab(AccountsRoute) },
                    accountsExist = accountsExist,
                    onAddAccount = { resetTo(OnboardingRoute) },
                    accountInitial = "",
                ) {
                    FoldersScreen(
                        app = app, haze = haze,
                        onOpenFolder = { navigate(FolderRoute(it)) },
                        accountsExist = accountsExist,
                    )
                }
            }
            entry<FolderRoute> { r ->
                LiquidShell(
                    haze = haze, title = r.folder,
                    query = query, onQueryChange = { query = it },
                    activeTab = FoldersRoute, onTab = { switchTab(it as Route) },
                    onAvatar = { switchTab(AccountsRoute) },
                    accountsExist = accountsExist,
                    onAddAccount = { resetTo(OnboardingRoute) },
                    accountInitial = "",
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
                    accountsExist = accountsExist,
                    onAddAccount = { resetTo(OnboardingRoute) },
                    accountInitial = "A",
                ) {
                    AccountsScreen(app = app, onBack = back, onAddAccount = { navigate(OnboardingRoute) })
                }
            }
            entry<ThreadRoute> { r ->
                ThreadScreen(app = app, messageId = r.messageId, onBack = back, onReply = { navigate(ComposeRoute(replyTo = it)) })
            }
            entry<ComposeRoute> { r ->
                ComposeScreen(app = app, replyTo = r.replyTo, onBack = back)
            }
        }
    )
}
