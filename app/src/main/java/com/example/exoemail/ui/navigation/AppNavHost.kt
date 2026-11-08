package com.example.exoemail.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.example.exoemail.LiquidMailApp
import com.example.exoemail.ui.screens.AccountsScreen
import com.example.exoemail.ui.screens.ComposeScreen
import com.example.exoemail.ui.screens.DraftsScreen
import com.example.exoemail.ui.screens.FolderScreen
import com.example.exoemail.ui.screens.FoldersScreen
import com.example.exoemail.ui.screens.InboxScreen
import com.example.exoemail.ui.screens.OnboardingScreen
import com.example.exoemail.ui.screens.SentScreen
import com.example.exoemail.ui.screens.StarredScreen
import com.example.exoemail.ui.screens.ThreadScreen
import com.example.exoemail.ui.shell.LiquidShell
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

    var query by remember { mutableStateOf("") }
    val currentTab = backStack.firstOrNull() ?: OnboardingRoute

    // Resolve the active account's initial for the avatar once accounts load.
    val accountInitial by produceState<String>(initialValue = "") {
        val accounts = app.database.accountDao().observeAll().first()
        val activeId = app.settings.activeAccountId.first()
        val active = accounts.find { it.id == activeId } ?: accounts.firstOrNull()
        value = active?.email?.firstOrNull()?.uppercaseChar()?.toString() ?: ""
    }

    NavDisplay(
        backStack = backStack,
        onBack = back,
        entryProvider = entryProvider {
            entry<OnboardingRoute> {
                OnboardingScreen(app = app, onDone = { resetTo(InboxRoute) })
            }
            entry<InboxRoute> {
                LiquidShell(
                    title = "Mail",
                    query = query, onQueryChange = { query = it },
                    activeTab = currentTab, onTab = { switchTab(it as Route) },
                    onAvatar = { navigate(AccountsRoute) },
                    accountsExist = accountsExist,
                    onAddAccount = { resetTo(OnboardingRoute) },
                    accountInitial = accountInitial,
                ) {
                    InboxScreen(
                        app = app,
                        query = query,
                        accountsExist = accountsExist,
                        onOpenMessage = { navigate(ThreadRoute(it)) },
                    )
                }
            }
            entry<StarredRoute> {
                LiquidShell(
                    title = "Starred",
                    query = query, onQueryChange = { query = it },
                    activeTab = currentTab, onTab = { switchTab(it as Route) },
                    onAvatar = { navigate(AccountsRoute) },
                    accountsExist = accountsExist,
                    onAddAccount = { resetTo(OnboardingRoute) },
                    accountInitial = accountInitial,
                ) {
                    StarredScreen(app = app, onOpenMessage = { navigate(ThreadRoute(it)) })
                }
            }
            entry<SentRoute> {
                LiquidShell(
                    title = "Sent",
                    query = query, onQueryChange = { query = it },
                    activeTab = currentTab, onTab = { switchTab(it as Route) },
                    onAvatar = { navigate(AccountsRoute) },
                    accountsExist = accountsExist,
                    onAddAccount = { resetTo(OnboardingRoute) },
                    accountInitial = accountInitial,
                ) {
                    SentScreen(app = app, onOpenMessage = { navigate(ThreadRoute(it)) })
                }
            }
            entry<DraftsRoute> {
                LiquidShell(
                    title = "Drafts",
                    query = query, onQueryChange = { query = it },
                    activeTab = currentTab, onTab = { switchTab(it as Route) },
                    onAvatar = { navigate(AccountsRoute) },
                    accountsExist = accountsExist,
                    onAddAccount = { resetTo(OnboardingRoute) },
                    accountInitial = accountInitial,
                ) {
                    DraftsScreen(app = app, onOpenDraft = { navigate(ComposeRoute(draftId = it)) })
                }
            }
            entry<FoldersRoute> {
                LiquidShell(
                    title = "Folders",
                    query = query, onQueryChange = { query = it },
                    activeTab = currentTab, onTab = { switchTab(it as Route) },
                    onAvatar = { navigate(AccountsRoute) },
                    accountsExist = accountsExist,
                    onAddAccount = { resetTo(OnboardingRoute) },
                    accountInitial = accountInitial,
                ) {
                    FoldersScreen(
                        app = app,
                        onOpenFolder = { navigate(FolderRoute(it)) },
                        accountsExist = accountsExist,
                    )
                }
            }
            entry<FolderRoute> { r ->
                LiquidShell(
                    title = r.folder,
                    query = query, onQueryChange = { query = it },
                    activeTab = FoldersRoute, onTab = { switchTab(it as Route) },
                    onAvatar = { navigate(AccountsRoute) },
                    accountsExist = accountsExist,
                    onAddAccount = { resetTo(OnboardingRoute) },
                    accountInitial = accountInitial,
                ) {
                    FolderScreen(app = app, folder = r.folder, onOpenMessage = { navigate(ThreadRoute(it)) })
                }
            }
            entry<AccountsRoute> {
                LiquidShell(
                    title = "Accounts",
                    query = query, onQueryChange = { query = it },
                    activeTab = currentTab, onTab = { switchTab(it as Route) },
                    onAvatar = { },
                    accountsExist = accountsExist,
                    onAddAccount = { resetTo(OnboardingRoute) },
                    accountInitial = accountInitial,
                ) {
                    AccountsScreen(app = app, onBack = back, onAddAccount = { navigate(OnboardingRoute) })
                }
            }
            entry<ThreadRoute> { r ->
                ThreadScreen(app = app, messageId = r.messageId, onBack = back, onReply = { navigate(ComposeRoute(replyTo = it)) })
            }
            entry<ComposeRoute> { r ->
                ComposeScreen(app = app, replyTo = r.replyTo, draftId = r.draftId, onBack = back)
            }
        }
    )
}
