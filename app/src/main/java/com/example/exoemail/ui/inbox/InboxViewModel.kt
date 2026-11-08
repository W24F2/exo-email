package com.example.exoemail.ui.inbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.exoemail.LiquidMailApp
import com.example.exoemail.core.mail.engine.MailEngineFactory
import com.example.exoemail.core.security.CredentialStore
import com.example.exoemail.data.MailRepository
import com.example.exoemail.data.db.AccountEntity
import com.example.exoemail.data.db.MessageEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class InboxFilter { ALL, UNREAD, STARRED }

class InboxViewModel(
    private val app: LiquidMailApp,
    private val repo: MailRepository = MailRepository(app.database, MailEngineFactory.create()),
    private val credentials: CredentialStore = CredentialStore(app),
    private val engine: com.example.exoemail.core.mail.MailEngine = MailEngineFactory.create(),
) : ViewModel() {

    val accounts: StateFlow<List<AccountEntity>> = repo.observeAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val resolvedAccount: StateFlow<AccountEntity?> = kotlinx.coroutines.flow.combine(
        app.settings.activeAccountId,
        accounts,
    ) { activeId, list ->
        list.find { it.id == activeId } ?: list.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing

    private val _syncError = MutableStateFlow<String?>(null)
    val syncError: StateFlow<String?> = _syncError

    private val autoSyncedAccountIds = mutableSetOf<String>()

    init {
        // Fetch immediately after account onboarding, once per account per ViewModel lifetime.
        viewModelScope.launch {
            resolvedAccount.filterNotNull().collectLatest { account ->
                if (autoSyncedAccountIds.add(account.id)) sync(account)
            }
        }
    }

    private val _filter = MutableStateFlow(InboxFilter.ALL)
    val filter: StateFlow<InboxFilter> = _filter

    private val _query = MutableStateFlow("")

    fun setFilter(f: InboxFilter) { _filter.value = f }
    fun setQuery(q: String) { _query.value = q }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun messages(): Flow<PagingData<MessageEntity>> =
        kotlinx.coroutines.flow.combine(resolvedAccount, _filter, _query) { acc, f, q -> Triple(acc, f, q) }
            .flatMapLatest { (account, filter, query) ->
                if (account == null) flowOf(PagingData.empty())
                else {
                    val base = when {
                        query.isNotBlank() -> repo.searchAll(account.id, query)
                        filter == InboxFilter.UNREAD -> repo.unreadPager(account.id)
                        filter == InboxFilter.STARRED -> repo.starredPager(account.id)
                        else -> repo.inboxPager(account.id)
                    }
                    base.cachedIn(viewModelScope)
                }
            }

    fun starred(accountId: String) = repo.starredPager(accountId).cachedIn(viewModelScope)
    fun sent(accountId: String) = repo.sentPager(accountId).cachedIn(viewModelScope)
    fun drafts(accountId: String) = repo.draftsPager(accountId).cachedIn(viewModelScope)

    fun folders(accountId: String) = repo.observeFolders(accountId)

    fun folderMessages(accountId: String, folder: String) =
        repo.folderPager(accountId, folder).cachedIn(viewModelScope)

    fun refresh() {
        val account = resolvedAccount.value ?: return
        viewModelScope.launch { sync(account) }
    }

    private suspend fun sync(account: AccountEntity) {
        _refreshing.value = true
        _syncError.value = null
        try {
            val creds = runCatching { credentials.get(account.id) }.getOrNull()
            val secret = creds?.secret
            if (secret == null) {
                // No stored credential (e.g. demo/offline account) â€” don't treat as error
                _syncError.value = null
                return
            }
            repo.refreshInbox(account, secret)
                .onSuccess { _syncError.value = null }
                .onFailure { _syncError.value = it.message ?: "Couldn't load mail. Check your connection and account settings." }
        } catch (e: Exception) {
            _syncError.value = e.message ?: "Couldn't load mail. Check your connection and account settings."
        } finally {
            _refreshing.value = false
        }
    }

    /** Optimistic local update, then push the flag to the server best-effort. */
    fun markRead(id: String, read: Boolean = true) {
        viewModelScope.launch {
            repo.markRead(id, read)
            syncFlag(id) { acc, folder, uid, cfg -> engine.markRead(cfg, folder, uid, read) }
        }
    }

    fun toggleStar(id: String, starred: Boolean) {
        viewModelScope.launch {
            repo.setStarred(id, starred)
            syncFlag(id) { acc, folder, uid, cfg -> engine.setStarred(cfg, folder, uid, starred) }
        }
    }

    private suspend fun syncFlag(
        id: String,
        call: suspend (AccountEntity, String, Long, com.example.exoemail.core.mail.AccountConfig) -> Result<Unit>,
    ) {
        val msg = app.database.messageDao().get(id) ?: return
        val account = app.database.accountDao().get(msg.accountId) ?: return
        val secret = runCatching { credentials.get(account.id)?.secret }.getOrNull() ?: return
        val config = repo.accountConfig(account, secret)
        // uid=0 rows (drafts/sent placeholders) have no server counterpart
        if (msg.uid == 0L) return
        runCatching { call(account, msg.folder, msg.uid, config) }
    }
}

class InboxViewModelFactory(private val app: LiquidMailApp) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = InboxViewModel(app) as T
}
