package com.example.email_client.ui.inbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.email_client.LiquidMailApp
import com.example.email_client.core.mail.engine.MailEngineFactory
import com.example.email_client.core.security.CredentialStore
import com.example.email_client.data.MailRepository
import com.example.email_client.data.db.AccountEntity
import com.example.email_client.data.db.MessageEntity
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

class InboxViewModel(
    private val app: LiquidMailApp,
    private val repo: MailRepository = MailRepository(app.database, MailEngineFactory.create()),
    private val credentials: CredentialStore = CredentialStore(app),
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

    @OptIn(ExperimentalCoroutinesApi::class)
    fun messages(query: String): Flow<PagingData<MessageEntity>> = resolvedAccount
        .flatMapLatest { account ->
            if (account == null) flowOf(PagingData.empty())
            else if (query.isBlank()) repo.inboxPager(account.id).cachedIn(viewModelScope)
            else repo.searchInbox(account.id, query).cachedIn(viewModelScope)
        }

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
            val secret = runCatching { credentials.get(account.id)?.secret }.getOrNull()
            if (secret == null) {
                _syncError.value = "Sign in again to sync this account."
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

    fun markRead(id: String) {
        viewModelScope.launch { repo.markRead(id, true) }
    }

    fun toggleStar(id: String, starred: Boolean) {
        viewModelScope.launch { repo.setStarred(id, starred) }
    }
}

class InboxViewModelFactory(private val app: LiquidMailApp) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = InboxViewModel(app) as T
}
