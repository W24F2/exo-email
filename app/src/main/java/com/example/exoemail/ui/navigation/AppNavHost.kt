package com.example.exoemail.ui.navigation

import androidx.activity.compose.rememberLauncherForActivityResult
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

    // POST_NOTIFICATIONS is a runtime permission on Android 13+ — without this
    // request the new-mail notifications and badges would be silently blocked.
    val notifPermission = rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission(),
    ) { }
    androidx.compose.runtime.LaunchedEffect(accountsExist) {
        if (accountsExist &&
            android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU &&
            androidx.core.content.ContextCompat.checkSelfPermission(
                app, android.Manifest.permission.POST_NOTIFICATIONS,
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            notifPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Resolve the active account's initial for the avatar once accounts load.
    val accountInitial by produceState<String>(initialValue = "") {
        val accounts = app.database.accountDao().observeAll().first()
        val activeId = app.settings.activeAccountId.first()
        val active = accounts.find { it.id == activeId } ?: accounts.firstOrNull()
        value = active?.email?.firstOrNull()?.uppercaseChar()?.toString() ?: ""
    }

    // Live per-folder unread counts for dock badges (re-queries on message changes).
    val activeAccountId by produceState<String?>(initialValue = null) {
        val accounts = app.database.accountDao().observeAll().first()
        value = app.settings.activeAccountId.first() ?: accounts.firstOrNull()?.id
    }
    val unreadByFolder by produceState<Map<String, Int>>(initialValue = emptyMap(), activeAccountId) {
        val id = activeAccountId ?: return@produceState
        app.database.messageDao().observeUnreadByFolder(id).collect { list ->
            value = list.associate { it.folder to it.cnt }
        }
    }
    // Map server folder names onto tab routes — remembered so LiquidShell
    // gets a stable Map instance and stays skippable.
    val tabUnread = remember(unreadByFolder) {
        mapOf(
            "InboxRoute" to (unreadByFolder["INBOX"] ?: 0),
            "StarredRoute" to (unreadByFolder["Starred"] ?: 0),
            "SentRoute" to (unreadByFolder["Sent"] ?: 0),
            "DraftsRoute" to (unreadByFolder["Drafts"] ?: 0),
        )
    }

    NavDisplay(
        backStack = backStack,
        onBack = back,
        entryProvider = entryProvider {
            entry<OnboardingRoute> {
                OnboardingScreen(
                    app = app,
                    onDone = { resetTo(InboxRoute) },
                    onBack = if (backStack.size > 1) back else null,
                )
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
                    onCompose = { navigate(ComposeRoute()) },
                    unreadCounts = tabUnread,
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
                    onCompose = { navigate(ComposeRoute()) },
                    unreadCounts = tabUnread,
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
                    onCompose = { navigate(ComposeRoute()) },
                    unreadCounts = tabUnread,
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
                    onCompose = { navigate(ComposeRoute()) },
                    unreadCounts = tabUnread,
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
                    onCompose = { navigate(ComposeRoute()) },
                    unreadCounts = tabUnread,
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
                    onBack = back,
                    onCompose = { navigate(ComposeRoute()) },
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
                    onCompose = { navigate(ComposeRoute()) },
                    unreadCounts = tabUnread,
                ) {
                    AccountsScreen(app = app, onBack = back, onAddAccount = { navigate(OnboardingRoute) })
                }
            }
            entry<ThreadRoute> { r ->
                ThreadScreen(
                    app = app, messageId = r.messageId, onBack = back,
                    onReply = { msg -> navigate(ComposeRoute(replyTo = msg.sender, messageId = msg.messageId, references = msg.references, subject = msg.subject, quotedBody = msg.body)) },
                    onForward = { msg -> navigate(ComposeRoute(subject = msg.subject, quotedBody = msg.body, messageId = msg.messageId, references = msg.references, forward = true)) },
                )
            }
            entry<ComposeRoute> { r ->
                ComposeScreen(app = app, replyTo = r.replyTo, draftId = r.draftId, inReplyTo = r.messageId, references = r.references, subject = r.subject, quotedBody = r.quotedBody, forward = r.forward, onBack = back)
            }
        }
    )
}
